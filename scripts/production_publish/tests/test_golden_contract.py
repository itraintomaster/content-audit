"""El contrato con el lado Java: el golden de la lista (esquema 1) que escribe Java.

El golden es un solo archivo para los dos lados: vive en
revision-domain/src/test/resources/publication/manifest-golden.json, y del lado Java lo leen
PublicationManifestContractTest (escribe la misma lista y la compara clave por clave) y
FileSystemBatchStoreTest (lo lee y lo vuelve a escribir igual). Si cambia el contrato, cambia ahí
y las dos suites lo ven. Para correr estas pruebas contra otra copia, PUBPROD_GOLDEN=<ruta>.
Si el archivo no está (alguien lo borró o lo movió), las pruebas se saltean diciendo dónde lo buscaron.

Las claves están escritas acá a mano, a propósito: son el contrato, no lo que dice el lector.
"""

import os
import unittest

from production_publish import manifest_reader, write_plan
from production_publish.tests import fakes

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


def missing_golden():
    return ("no está el golden de la lista en %s: o lo borraron o lo movieron (se lo puede apuntar con "
            "PUBPROD_GOLDEN=<ruta>)" % GOLDEN)


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
            self.skipTest(missing_golden())
        with open(GOLDEN, "rb") as fh:
            manifest = manifest_reader._strict_json(fh.read(), "el golden")
        check_contract(self, manifest)

    def test_F_PUBPROD_R006_lo_que_declara_el_golden_se_ensaya_limpio_contra_una_base_con_su_antes(self):
        """Lo que Java declara, Python lo puede escribir: ni bloqueos ni derivas con la base en el «antes»."""
        if not os.path.isfile(GOLDEN):
            self.skipTest(missing_golden())
        with open(GOLDEN, "rb") as fh:
            manifest = manifest_reader._strict_json(fh.read(), "el golden")
        plan = write_plan.build(manifest, fakes.FakeStore(base_with_before(manifest)), skip_drift=False)
        self.assertEqual([d.describe() for d in plan.problems()], [])
        counts = plan.counts()
        self.assertEqual(counts["documentsToWrite"], manifest["counts"]["documents"])
        self.assertEqual(counts["fieldsToWrite"], manifest["counts"]["fields"])
        self.assertEqual(counts["byPath"], manifest["counts"]["byPath"])


def base_with_before(manifest):
    """Una base falsa con el «antes» de cada campo; el ejercicio que trae opciones es de opción múltiple."""
    data = {collection: {} for collection in manifest_reader.COLLECTIONS}
    data[write_plan.INSTANCES] = {}  # sin instancias: las copias embebidas no cuentan acá
    for entry in manifest["documents"]:
        doc = {"_id": entry["id"]}
        for field in entry["fields"]:
            fakes.set_path(doc, field["path"], field["before"])
        if entry["collection"] == manifest_reader.QUIZ_TEMPLATES:
            kind = ("MULTIPLE_CHOICE" if any(f["path"] == "form.items" for f in entry["fields"])
                    else "FILL_GAPS")
            doc["kind"] = kind
            fakes.set_path(doc, "form.kind", kind)
        data[entry["collection"]][entry["id"]] = doc
    return data


if __name__ == "__main__":
    unittest.main()
