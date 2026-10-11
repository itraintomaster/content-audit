"""El contrato con el lado Java: la lista (esquema 1) y las claves del registro de la tanda.

La lista de ejemplo de estas pruebas sigue el esquema 1 del diseño. El golden que
escribe el lado Java (revision-domain/src/test/resources/publication/manifest-golden.json)
se valida igual cuando existe; si todavía no existe, la prueba se saltea diciendo por qué.
"""

import json
import os
import unittest

from production_publish import manifest_reader
from production_publish.tests import fakes

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
GOLDEN = os.path.join(REPO, "revision-domain", "src", "test", "resources", "publication", "manifest-golden.json")

TOP_LEVEL_KEYS = {"schema", "batchId", "planId", "level", "publishedAt", "approvedBy", "source", "verdict",
                  "reviews", "documents", "unsupported", "excluded", "counts"}
DOCUMENT_KEYS = {"collection", "id", "nodeId", "changeIds", "proposalIds", "reviewStatus", "fields"}
FIELD_KEYS = {"path", "before", "after"}


def check_contract(test, manifest):
    """Las claves del esquema 1 están todas (una clave de más no se lee) y la lista pasa la validación."""
    test.assertLessEqual(TOP_LEVEL_KEYS, set(manifest))
    test.assertLessEqual({"kind", "gatePassed", "undecidableFloors"}, set(manifest["verdict"]))
    test.assertLessEqual({"approved", "pending", "rejected"}, set(manifest["reviews"]))
    for document in manifest["documents"]:
        test.assertLessEqual(DOCUMENT_KEYS, set(document))
        for field in document["fields"]:
            test.assertLessEqual(FIELD_KEYS, set(field))
    manifest_reader.validate_manifest(manifest, manifest["batchId"], manifest["planId"])


class GoldenContractTest(unittest.TestCase):
    def test_F_PUBPROD_R001_la_lista_de_ejemplo_cumple_el_contrato_del_esquema_1(self):
        manifest = fakes.example_manifest()
        check_contract(self, manifest)
        self.assertEqual(set(manifest), TOP_LEVEL_KEYS)  # la de ejemplo, exacta

    def test_F_PUBPROD_R001_el_golden_del_lado_java_cumple_el_contrato(self):
        if not os.path.isfile(GOLDEN):
            self.skipTest("todavía no existe el golden del lado Java (%s); se valida cuando se junten las ramas"
                          % os.path.relpath(GOLDEN, REPO))
        with open(GOLDEN, "rb") as fh:
            raw = fh.read()
        manifest = manifest_reader._strict_json(raw.decode("utf-8"), "el golden")
        check_contract(self, manifest)

    def test_F_PUBPROD_R006_lee_las_claves_del_registro_de_la_tanda_con_los_nombres_de_java(self):
        # BatchRecord con BatchPublication (Jackson escribe los null): las claves que lee el paso.
        workdir = fakes.temp_workdir(self)
        fakes.write_batch(workdir)
        with open(manifest_reader.batch_record_path(workdir, fakes.BATCH_ID), encoding="utf-8") as fh:
            record = json.load(fh)
        self.assertTrue({"id", "planId", "status", "publication"} <= set(record))
        self.assertTrue({"manifestDigest", "verdictKind", "gatePassed", "undecidableFloors"} <= set(record["publication"]))
        manifest_reader.load(workdir, fakes.BATCH_ID)


if __name__ == "__main__":
    unittest.main()
