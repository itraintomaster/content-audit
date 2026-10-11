"""El registro de producción: solo crece, y el ensayo que habilita."""

import io
import json
import os
import unittest

from production_publish import registry
from production_publish.registry import (APPLY, DRY_RUN, DRY_RUN_CLEAN, DRY_RUN_WITH_PROBLEMS, REFUSED, STARTED,
                                         Registry, RegistryError, clean_dry_run_problem)
from production_publish.tests import fakes


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
        out = io.StringIO()
        log = registry.RunLog(self.workdir, "prueba", DRY_RUN, fakes.Clock(), out=out)
        self.assertFalse(log.record(DRY_RUN_CLEAN))
        self.assertIn("!! no se pudo escribir el registro", out.getvalue())
        self.assertIn("!! La corrida no quedó registrada (DRY_RUN_CLEAN)", out.getvalue())
        self.assertTrue(log.row_lost)

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
        not_enabling = {
            "sin ensayo": [],
            "ensayo con problemas": [self.row(DRY_RUN, DRY_RUN_WITH_PROBLEMS)],
            "otra lista": [self.row(DRY_RUN, DRY_RUN_CLEAN, digest="d2")],
            "otro destino": [self.row(DRY_RUN, DRY_RUN_CLEAN, destination={"host": "x", "db": "learney"})],
            "otra tanda": [self.row(DRY_RUN, DRY_RUN_CLEAN, batch="t2")],
            "otras opciones": [self.row(DRY_RUN, DRY_RUN_CLEAN, flags={"skipDrift": True, "skipUnsupported": False})],
            "aplicación después del ensayo": [self.row(DRY_RUN, DRY_RUN_CLEAN), self.row(APPLY, STARTED)],
            "ensayo con problemas después del limpio": [self.row(DRY_RUN, DRY_RUN_CLEAN),
                                                        self.row(DRY_RUN, DRY_RUN_WITH_PROBLEMS)],
        }
        for case, rows in not_enabling.items():
            with self.subTest(case=case):
                self.assertIsNotNone(self.problem(rows))

    def test_F_PUBPROD_R009_una_corrida_negada_no_borra_el_ensayo_limpio(self):
        self.assertIsNone(self.problem([self.row(DRY_RUN, DRY_RUN_CLEAN), self.row(APPLY, REFUSED)]))


if __name__ == "__main__":
    unittest.main()
