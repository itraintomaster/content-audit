"""La base real, sobre el pymongo de juguete: lee por _id y fija con comparar-y-fijar."""

import unittest

from production_publish.mongo_store import open_store
from production_publish.store import MISSING
from production_publish.tests import fakes
from production_publish.tests.fake_driver import FakeMongo, ObjectId

OID = "665f000000000000000000d1"
TEXT_ID = "665f000000000000000000d2"


class MongoStoreTest(unittest.TestCase):
    def setUp(self):
        self.mongo = FakeMongo()
        self.mongo.install(self)
        self.mongo.collection(fakes.DB, "quizTemplates").extend([
            {"_id": ObjectId(OID), "translation": "vieja", "form": {"kind": "FILL"}},
            {"_id": TEXT_ID, "translation": "vieja"},
        ])
        self.mongo.collection(fakes.DB, "quizzes").extend([
            {"_id": ObjectId("770000000000000000000001"), "template": {"_id": OID, "instructions": "a"}},
            {"_id": ObjectId("770000000000000000000002"), "template": {"_id": ObjectId(OID), "instructions": "a"}},
            {"_id": ObjectId("770000000000000000000003"), "template": {"_id": "otro", "instructions": "a"}},
        ])
        self.store = open_store(fakes.URI, fakes.DB)

    def test_F_PUBPROD_R006_lee_por_object_id_y_tambien_el_id_guardado_como_texto(self):
        self.assertEqual(self.store.find_document("quizTemplates", OID)["translation"], "vieja")
        self.assertEqual(self.store.find_document("quizTemplates", TEXT_ID)["_id"], TEXT_ID)
        self.assertIsNone(self.store.find_document("quizTemplates", "665f0000000000000000ffff"))
        self.assertEqual(self.mongo.writes, [])

    def test_F_PUBPROD_R006_escribe_solo_si_el_documento_tiene_lo_que_se_leyo(self):
        self.store.find_document("quizTemplates", OID)
        ok = self.store.compare_and_set("quizTemplates", OID, {"translation": "vieja", "form.sentences": MISSING},
                                        {"translation": "nueva", "form.sentences": ["x"]})
        self.assertTrue(ok)
        _, query, update = self.mongo.writes[-1]
        self.assertEqual(query, {"_id": ObjectId(OID), "translation": "vieja", "form.sentences": {"$exists": False}})
        self.assertEqual(update, {"$set": {"translation": "nueva", "form.sentences": ["x"]}})
        doc = self.mongo.find(fakes.DB, "quizTemplates", OID)
        self.assertEqual(doc["form"], {"kind": "FILL", "sentences": ["x"]})
        # Si alguien lo cambió entre la lectura y la escritura, no pisa nada.
        self.assertFalse(self.store.compare_and_set("quizTemplates", OID, {"translation": "vieja"},
                                                    {"translation": "otra"}))
        self.assertEqual(self.mongo.find(fakes.DB, "quizTemplates", OID)["translation"], "nueva")

    def test_F_PUBPROD_R006_el_id_de_texto_se_escribe_sobre_el_mismo_documento(self):
        self.store.find_document("quizTemplates", TEXT_ID)
        self.assertTrue(self.store.compare_and_set("quizTemplates", TEXT_ID, {"translation": "vieja"},
                                                   {"translation": "nueva"}))
        self.assertEqual(self.mongo.writes[-1][1]["_id"], TEXT_ID)

    def test_F_PUBPROD_R006_busca_las_copias_embebidas_por_id_en_texto_o_object_id(self):
        found = self.store.find_instances([OID])
        self.assertEqual(sorted(i for i, _ in found), ["770000000000000000000001", "770000000000000000000002"])

    def test_F_PUBPROD_R008_se_conecta_con_un_nombre_propio_y_un_tiempo_limite(self):
        uri, options = self.mongo.clients[0]
        self.assertEqual(uri, fakes.URI)
        self.assertEqual(options["appname"], "content-audit-production-publish")
        self.assertIn("serverSelectionTimeoutMS", options)
        self.store.close()


if __name__ == "__main__":
    unittest.main()
