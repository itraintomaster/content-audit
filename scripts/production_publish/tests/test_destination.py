"""El destino de una corrida: servidor y base, sin credenciales (F-PUBPROD R008 y R009)."""

import os
import re
import unittest

from production_publish.destination import destination_of, parse_expected_destination
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

    def test_F_PUBPROD_R009_el_destino_nombrado_y_el_del_uri_se_normalizan_igual(self):
        # Mayúsculas, espacios y varios servidores: lo que José escribe coincide con lo que dice el URI.
        cases = (
            ("mongodb://u:p@Cluster-Falso.ABC12.mongodb.net/learney", " Cluster-Falso.abc12.MONGODB.net /learney"),
            ("mongodb://u:p@A.example.com:1, b.example.com:2/admin", "a.example.com:1 ,B.example.com:2/learney"),
        )
        for uri, named in cases:
            with self.subTest(named=named):
                self.assertEqual(parse_expected_destination(named), destination_of(uri, "learney"))

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


if __name__ == "__main__":
    unittest.main()
