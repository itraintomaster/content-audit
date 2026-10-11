"""El contrato con el lado Java: el golden de la lista (esquema 1) que escribe Java.

El golden vive del lado Java (revision-domain/src/test/resources/publication/manifest-golden.json)
y llega a esta rama al juntarlas; mientras tanto la prueba se saltea diciendo por qué. Para
correrla contra otra copia (por ejemplo, la del worktree de Java), PUBPROD_GOLDEN=<ruta>.

Las claves están escritas acá a mano, a propósito: son el contrato, no lo que dice el lector.
"""

import os
import unittest

from production_publish import manifest_reader

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
GOLDEN = os.environ.get("PUBPROD_GOLDEN") or os.path.join(
    REPO, "revision-domain", "src", "test", "resources", "publication", "manifest-golden.json")

TOP_LEVEL_KEYS = {"schema", "batchId", "planId", "level", "publishedAt", "approvedBy", "source", "verdict",
                  "reviews", "documents", "unsupported", "excluded", "counts"}
SOURCE_KEYS = {"baselineAuditId", "startAuditId", "publishedAuditId"}
VERDICT_KEYS = {"kind", "gatePassed", "undecidableFloors", "brokenFloorGoalIds", "detail"}
REVIEWS_KEYS = {"approved", "pending", "rejected", "droppedForReviewChangeIds", "collateralDroppedChangeIds"}
DOCUMENT_KEYS = {"collection", "id", "nodeId", "changeIds", "proposalIds", "reviewStatus", "fields"}
FIELD_KEYS = {"path", "before", "after"}
UNSUPPORTED_KEYS = {"changeId", "nodeId", "part", "reason"}
EXCLUDED_KEYS = {"changeId", "reason"}
COUNTS_KEYS = {"documents", "fields", "byPath"}


def check_contract(test, manifest):
    """Cada parte trae exactamente las claves del esquema 1 y la lista pasa la validación."""
    test.assertEqual(set(manifest), TOP_LEVEL_KEYS)
    test.assertEqual(set(manifest["source"]), SOURCE_KEYS)
    test.assertEqual(set(manifest["verdict"]), VERDICT_KEYS)
    test.assertEqual(set(manifest["reviews"]), REVIEWS_KEYS)
    test.assertEqual(set(manifest["counts"]), COUNTS_KEYS)
    for document in manifest["documents"]:
        test.assertEqual(set(document), DOCUMENT_KEYS)
        for field in document["fields"]:
            test.assertEqual(set(field), FIELD_KEYS)
    for item in manifest["unsupported"]:
        test.assertEqual(set(item), UNSUPPORTED_KEYS)
    for item in manifest["excluded"]:
        test.assertEqual(set(item), EXCLUDED_KEYS)
    manifest_reader.validate_manifest(manifest, manifest["batchId"], manifest["planId"])


class GoldenContractTest(unittest.TestCase):
    def test_F_PUBPROD_R001_el_golden_del_lado_java_cumple_el_contrato(self):
        if not os.path.isfile(GOLDEN):
            self.skipTest("todavía no existe el golden del lado Java (%s); se valida cuando se junten las ramas "
                          "o con PUBPROD_GOLDEN=<ruta>" % GOLDEN)
        with open(GOLDEN, "rb") as fh:
            raw = fh.read()
        manifest = manifest_reader._strict_json(raw.decode("utf-8"), "el golden")
        check_contract(self, manifest)


if __name__ == "__main__":
    unittest.main()
