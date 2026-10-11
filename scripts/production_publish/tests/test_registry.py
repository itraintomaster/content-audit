"""El registro de producción: destino sin credenciales, solo crece, y el ensayo que habilita."""

import json
import os
import re
import unittest

from production_publish import registry
from production_publish.registry import (APPLY, DRY_RUN, DRY_RUN_CLEAN, DRY_RUN_WITH_PROBLEMS, REFUSED, STARTED,
                                         Registry, RegistryError, clean_dry_run_problem, destination_of,
                                         parse_expected_destination)
from production_publish.tests import fakes


class DestinationTest(unittest.TestCase):
    def test_F_PUBPROD_R008_el_destino_es_servidor_y_base_sin_credenciales(self):
        cases = {
            fakes.URI: fakes.HOST,
            "mongodb://localhost:27017": "localhost:27017",
            "mongodb://u:p@a.example.com:27017,b.example.com:27018/admin?replicaSet=rs0": "a.example.com:27017,b.example.com:27018",
            "mongodb+srv://u%40x:p%2Fq@Cluster0.ABC.mongodb.net/learney": "cluster0.abc.mongodb.net",
            "mongodb://[::1]:27017": "[::1]:27017",
            "mongodb://u:p@h.example.com/?authMechanismProperties=AWS_SESSION_TOKEN:secreto": "h.example.com",
        }
        for uri, host in cases.items():
            with self.subTest(uri=uri):
                self.assertEqual(destination_of(uri, "learney"), {"host": host, "db": "learney"})

    def test_F_PUBPROD_R008_un_uri_dudoso_se_niega_sin_repetirlo(self):
        bad = (
            "http://%s:%s@host" % (fakes.FAKE_USER, fakes.FAKE_PASSWORD),
            "mongodb://%s:%s@" % (fakes.FAKE_USER, fakes.FAKE_PASSWORD),
            # Una clave con @ o / sin escapar: lo que parecería servidor puede ser parte de la clave.
            "mongodb://%s:cl@ve/x@host/learney" % fakes.FAKE_USER,
            "mongodb://%s:%s@host/?x=a@b" % (fakes.FAKE_USER, fakes.FAKE_PASSWORD),
            "mongodb://host name/",
            "",
            None,
        )
        for uri in bad:
            with self.subTest(uri=uri):
                with self.assertRaises(ValueError) as ctx:
                    destination_of(uri, "learney")
                self.assertNotIn(fakes.FAKE_PASSWORD, str(ctx.exception))
                self.assertNotIn(fakes.FAKE_USER, str(ctx.exception))

    def test_F_PUBPROD_R009_el_destino_nombrado_es_servidor_y_base(self):
        self.assertEqual(parse_expected_destination("Cluster-Falso.abc12.mongodb.net/learney"),
                         {"host": "cluster-falso.abc12.mongodb.net", "db": "learney"})
        self.assertEqual(parse_expected_destination("a:1,b:2/learney"), {"host": "a:1,b:2", "db": "learney"})
        for bad in ("learney", "/learney", "host/", "u:p@host/learney", "host/le@rney", "host/db:1", None):
            with self.subTest(bad=bad):
                with self.assertRaises(ValueError):
                    parse_expected_destination(bad)

    def test_F_PUBPROD_R009_ningun_archivo_nombra_un_servidor_de_verdad(self):
        # Un servidor de Atlas tiene la forma <nombre>.<5 letras o números>.mongodb.net. En el
        # código, las pruebas y los ejemplos solo pueden aparecer los inventados de fakes.
        atlas = re.compile(r"[a-z0-9-]+\.[a-z0-9]{5}\.mongodb\.net", re.IGNORECASE)
        invented = {fakes.HOST, fakes.OTHER_DESTINATION.split("/")[0]}
        package = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        scripts = os.path.dirname(package)
        paths = [os.path.join(scripts, name) for name in
                 ("publish_revisions.py", "publish_knowledge_labels.py", "publish_knowledge_instructions.py")]
        for root, dirs, files in os.walk(package):
            dirs[:] = [d for d in dirs if d != "__pycache__"]
            paths += [os.path.join(root, f) for f in files if f.endswith((".py", ".json", ".sh", ".md"))]
        found = {}
        for path in paths:
            with open(path, encoding="utf-8") as fh:
                hosts = {h.lower() for h in atlas.findall(fh.read())} - invented
            if hosts:
                found[os.path.relpath(path, scripts)] = sorted(hosts)
        self.assertGreater(len(paths), 10)
        self.assertEqual(found, {})


class AppendOnlyTest(unittest.TestCase):
    def setUp(self):
        self.workdir = fakes.temp_workdir(self)
        self.registry = Registry(self.workdir)

    def test_F_PUBPROD_R008_el_registro_solo_crece(self):
        self.registry.append({"runId": "1", "result": DRY_RUN_CLEAN})
        with open(self.registry.path, "rb") as fh:
            first = fh.read()
        self.registry.append({"runId": "2", "result": STARTED})
        with open(self.registry.path, "rb") as fh:
            both = fh.read()
        self.assertTrue(both.startswith(first))
        self.assertEqual([r["runId"] for r in self.registry.rows()], ["1", "2"])

    def test_F_PUBPROD_R008_un_renglon_cortado_no_se_mezcla_con_el_siguiente(self):
        os.makedirs(self.registry.dir)
        with open(self.registry.path, "w") as fh:
            fh.write('{"runId": "1"}\n{"runId": "cort')
        self.registry.append({"runId": "2"})
        with open(self.registry.path, encoding="utf-8") as fh:
            lines = fh.read().splitlines()
        self.assertEqual(json.loads(lines[-1]), {"runId": "2"})
        with self.assertRaises(RegistryError):
            self.registry.rows()  # el renglón cortado se ve: hay que revisarlo a mano

    def test_F_PUBPROD_R008_si_no_se_puede_escribir_avisa(self):
        os.makedirs(os.path.join(self.workdir, ".content-audit"))
        with open(os.path.join(self.workdir, ".content-audit", "production"), "w") as fh:
            fh.write("un archivo donde va la carpeta")
        with self.assertRaises(RegistryError):
            self.registry.append({"runId": "1"})
        self.assertFalse(registry.record_legacy(self.workdir, {"runId": "1"}))

    def test_F_PUBPROD_R008_el_detalle_de_la_corrida_se_escribe_entero(self):
        path = self.registry.write_run("corrida-1", {"documents": [{"old": "a", "new": "b"}]})
        with open(path, encoding="utf-8") as fh:
            self.assertEqual(json.load(fh)["documents"][0]["old"], "a")
        self.assertEqual(os.listdir(self.registry.runs_dir), ["corrida-1.json"])


class CleanDryRunTest(unittest.TestCase):
    DEST = {"host": "h", "db": "learney"}
    FLAGS = {"skipDrift": False, "skipUnsupported": False}

    def row(self, mode, result, digest="d1", destination=None, flags=None, batch="t1"):
        return {"tool": "production_publish", "batchId": batch, "manifestDigest": digest, "mode": mode,
                "result": result, "destination": destination or self.DEST, "flags": flags or self.FLAGS,
                "runId": "r"}

    def problem(self, rows):
        return clean_dry_run_problem(rows, "production_publish", "t1", "d1", self.DEST, self.FLAGS)

    def test_F_PUBPROD_R009_habilita_solo_el_ultimo_ensayo_limpio_de_la_misma_lista_y_destino(self):
        self.assertIsNone(self.problem([self.row(DRY_RUN, DRY_RUN_CLEAN)]))
        self.assertIsNotNone(self.problem([]))
        self.assertIsNotNone(self.problem([self.row(DRY_RUN, DRY_RUN_WITH_PROBLEMS)]))
        self.assertIsNotNone(self.problem([self.row(DRY_RUN, DRY_RUN_CLEAN, digest="d2")]))
        self.assertIsNotNone(self.problem([self.row(DRY_RUN, DRY_RUN_CLEAN, destination={"host": "x", "db": "learney"})]))
        self.assertIsNotNone(self.problem([self.row(DRY_RUN, DRY_RUN_CLEAN, batch="t2")]))
        self.assertIsNotNone(self.problem([self.row(DRY_RUN, DRY_RUN_CLEAN,
                                                    flags={"skipDrift": True, "skipUnsupported": False})]))
        self.assertIsNotNone(self.problem([self.row(DRY_RUN, DRY_RUN_CLEAN), self.row(APPLY, STARTED)]))
        self.assertIsNotNone(self.problem([self.row(DRY_RUN, DRY_RUN_CLEAN), self.row(DRY_RUN, DRY_RUN_WITH_PROBLEMS)]))

    def test_F_PUBPROD_R009_una_corrida_negada_no_borra_el_ensayo_limpio(self):
        self.assertIsNone(self.problem([self.row(DRY_RUN, DRY_RUN_CLEAN), self.row(APPLY, REFUSED)]))


if __name__ == "__main__":
    unittest.main()
