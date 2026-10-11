"""Comparar-y-fijar campo por campo, sin escribir (F-PUBPROD-R006), y la huella del resto (R007)."""

import unittest

from production_publish import write_plan
from production_publish.store import MISSING, fingerprint_without, same_value
from production_publish.tests import fakes
from production_publish.write_plan import ALREADY, APPLY, DRIFT, MISSING_DOC, FieldPlan, build


def plan_for(store=None, skip_drift=False, manifest=None):
    return build(manifest or fakes.example_manifest(), store or fakes.FakeStore(), skip_drift)


def find(plan, collection, doc_id):
    return next(d for d in plan.all() if d.collection == collection and d.doc_id == doc_id)


class FieldPlanTest(unittest.TestCase):
    def test_F_PUBPROD_R006_con_el_antes_se_escribe_con_el_despues_ya_estaba_con_otra_cosa_es_deriva(self):
        self.assertEqual(FieldPlan("translation", "a", "b", "a").status, APPLY)
        self.assertEqual(FieldPlan("translation", "a", "b", "b").status, ALREADY)
        self.assertEqual(FieldPlan("translation", "a", "b", "c").status, DRIFT)
        self.assertEqual(FieldPlan("translation", "a", "b", MISSING).status, DRIFT)

    def test_F_PUBPROD_R006_la_comparacion_no_confunde_tipos_ni_vacios(self):
        self.assertEqual(FieldPlan("miniTheory", "", "x", None).status, DRIFT)
        self.assertEqual(FieldPlan("miniTheory", "", "x", MISSING).status, DRIFT)
        items_before = [{"id": "a", "incidence": 1.0}]
        self.assertEqual(FieldPlan("form.items", items_before, [{"id": "b"}], [{"id": "a", "incidence": 1}]).status,
                         DRIFT)
        self.assertEqual(FieldPlan("form.items", items_before, [{"id": "b"}], [{"id": "a", "incidence": True}]).status,
                         DRIFT)
        self.assertFalse(same_value(["a", "b"], ["b", "a"]))

    def test_F_PUBPROD_R006_el_orden_de_las_claves_no_es_deriva(self):
        before = [{"kind": "TEXT", "text": "I "}]
        current = [{"text": "I ", "kind": "TEXT"}]
        self.assertEqual(FieldPlan("form.sentenceParts", before, [{"kind": "TEXT", "text": "x"}], current).status,
                         APPLY)

    def test_F_PUBPROD_R002_la_oracion_guardada_ausente_es_la_lista_vacia(self):
        for current in (MISSING, None, []):
            with self.subTest(current=current):
                field = FieldPlan("form.sentences", [], ["She is tall."], current)
                self.assertEqual(field.status, APPLY)
                self.assertIs(field.current, current)  # el filtro de la escritura usa lo leído
        # Solo para la oración guardada: en otro campo, ausente no es vacío.
        self.assertEqual(FieldPlan("form.items", [], [{"id": "a"}], None).status, DRIFT)


class BuildTest(unittest.TestCase):
    def test_F_PUBPROD_R006_lee_cada_documento_y_ordena_knowledges_antes_que_quizTemplates(self):
        plan = plan_for()
        collections = [d.collection for d in plan.to_write()]
        self.assertEqual(collections[0], "knowledges")
        self.assertEqual(collections[-1], "quizzes")
        self.assertTrue(plan.is_clean())
        self.assertEqual(plan.counts()["fieldsToWrite"], 8)

    def test_F_PUBPROD_R006_escribe_solo_los_campos_de_la_lista_con_su_despues(self):
        plan = plan_for()
        quiz = find(plan, "quizTemplates", fakes.QUIZ_1)
        manifest_doc = next(d for d in fakes.example_manifest()["documents"] if d["id"] == fakes.QUIZ_1)
        self.assertEqual(quiz.writes(), {f["path"]: f["after"] for f in manifest_doc["fields"]})

    def test_F_PUBPROD_R006_un_documento_ya_aplicado_no_se_escribe(self):
        store = fakes.FakeStore()
        store.data["knowledges"][fakes.KNOWLEDGE]["instructions"] = "El verbo $to be$ en presente."
        plan = plan_for(store)
        self.assertEqual(find(plan, "knowledges", fakes.KNOWLEDGE).status, ALREADY)
        self.assertNotIn("knowledges", [d.collection for d in plan.to_write()])

    def test_F_PUBPROD_R006_la_deriva_frena_todo_sin_skip_drift(self):
        store = fakes.FakeStore()
        store.data["quizTemplates"][fakes.QUIZ_2]["translation"] = "Ella es muy alta."
        plan = plan_for(store)
        self.assertFalse(plan.is_clean())
        self.assertEqual([d.key for d in plan.problems()], ["quizTemplates/" + fakes.QUIZ_2])
        self.assertTrue(any("DERIVA" in line for line in plan.report_lines()))

    def test_F_PUBPROD_R006_con_skip_drift_saltea_el_documento_entero_y_sus_copias(self):
        store = fakes.FakeStore()
        store.data["quizTemplates"][fakes.QUIZ_1]["miniTheory"] = "Otra cosa"
        plan = plan_for(store, skip_drift=True)
        self.assertTrue(plan.is_clean())
        keys = [d.key for d in plan.to_write()]
        self.assertNotIn("quizTemplates/" + fakes.QUIZ_1, keys)
        self.assertNotIn("quizzes/" + fakes.INSTANCE_IN_SYNC, keys)
        self.assertIn("quizTemplates/" + fakes.QUIZ_2, keys)
        self.assertEqual([d.key for d in plan.skipped(write_plan.SKIP_DRIFT)], ["quizTemplates/" + fakes.QUIZ_1])

    def test_F_PUBPROD_R006_un_documento_que_no_existe_cuenta_como_deriva(self):
        store = fakes.FakeStore()
        del store.data["quizTemplates"][fakes.QUIZ_3]
        plan = plan_for(store)
        self.assertEqual(find(plan, "quizTemplates", fakes.QUIZ_3).status, MISSING_DOC)
        self.assertFalse(plan.is_clean())
        self.assertTrue(plan_for(store, skip_drift=True).is_clean())

    def test_F_PUBPROD_R006_las_copias_embebidas_al_dia_se_llevan_y_las_desfasadas_no_se_tocan(self):
        plan = plan_for()
        in_sync = find(plan, "quizzes", fakes.INSTANCE_IN_SYNC)
        stale = find(plan, "quizzes", fakes.INSTANCE_STALE)
        self.assertEqual(in_sync.writes(), {"template.instructions": "Completá con el verbo $to be$."})
        self.assertTrue(stale.skipped)
        self.assertEqual(stale.skip_reason, write_plan.SKIP_STALE_COPY)
        self.assertNotIn(stale, plan.to_write())
        self.assertTrue(plan.is_clean())  # una copia que ya estaba desfasada no frena
        self.assertNotIn(fakes.INSTANCE_OTHER, [d.doc_id for d in plan.all()])

    def test_F_PUBPROD_R004_cuenta_los_ejercicios_sin_revisar_que_viajan(self):
        plan = plan_for()
        self.assertEqual(plan.pending_documents(plan.to_write()), 2)


class FingerprintTest(unittest.TestCase):
    def test_F_PUBPROD_R007_la_huella_ignora_las_rutas_escritas_y_el_orden_de_las_claves(self):
        a = {"_id": "1", "form": {"kind": "X", "sentences": ["a"]}, "title": "t"}
        b = {"title": "t", "form": {"sentences": ["b"], "kind": "X"}, "_id": "1"}
        self.assertEqual(fingerprint_without(a, ["form.sentences"]), fingerprint_without(b, ["form.sentences"]))

    def test_F_PUBPROD_R007_la_huella_ve_cualquier_otro_cambio(self):
        a = {"_id": "1", "form": {"kind": "X", "incidence": 1}, "title": "t"}
        for b in ({"_id": "1", "form": {"kind": "Y", "incidence": 1}, "title": "t"},
                  {"_id": "1", "form": {"kind": "X", "incidence": 1.0}, "title": "t"},
                  {"_id": "1", "form": {"kind": "X", "incidence": 1}, "title": "t", "nuevo": None},
                  {"_id": "1", "form": {"kind": "X"}, "title": "t"}):
            with self.subTest(b=b):
                self.assertNotEqual(fingerprint_without(a, ["translation"]), fingerprint_without(b, ["translation"]))


if __name__ == "__main__":
    unittest.main()
