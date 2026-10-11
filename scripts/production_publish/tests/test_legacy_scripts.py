"""Los otros escritores de producción: el camino por plan, el de nombres y el de consignas.

El camino por plan se niega a escribir el plan de una tanda (F-PUBPROD-R006, inv. 5)
y los tres dejan su renglón en el registro, con el respaldo antes de escribir
(F-PUBPROD-R008, inv. 4). Corren contra el pymongo de juguete de fake_driver.
"""

import contextlib
import io
import json
import os
import unittest
from unittest import mock

from production_publish import registry
from production_publish.tests import fakes
from production_publish.tests.fake_driver import FakeMongo, ObjectId, ServerSelectionTimeoutError, load_script

PLAN = "2026-10-05T10-00-00"
QUIZ = "665f000000000000000000b1"
QUIZ_MC = "665f000000000000000000b2"
KNOWLEDGE = "665f000000000000000000c1"
BAD_URI = "mongodb://%s:cl@ve/x@host/learney" % fakes.FAKE_USER


def write_json(path, value):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(value, fh, ensure_ascii=False, indent=2)


def run(main, argv):
    """Corre main(argv) juntando lo que imprime. Devuelve (código, texto)."""
    out = io.StringIO()
    with contextlib.redirect_stdout(out):
        code = main(argv)
    return code, out.getvalue()


class LegacyTestCase(unittest.TestCase):
    script = None

    def setUp(self):
        self.repo = fakes.temp_workdir(self)
        self.mongo = FakeMongo()
        self.mongo.install(self)
        self.module = load_script(self.script)
        # El camino por plan también mira las tandas del repo por defecto: acá, el temporal.
        patcher = mock.patch.object(self.module, "REPO_DEFAULT", self.repo)
        patcher.start()
        self.addCleanup(patcher.stop)

    def args(self, *extra, uri=fakes.URI):
        return ["--uri", uri, "--db", fakes.DB, "--repo", self.repo] + list(extra)

    def rows(self):
        return fakes.registry_rows(self.repo)

    def assert_started_before_first_write(self):
        seen = []

        def hook(collection, query, update):
            if not seen:
                seen.append([r["result"] for r in self.rows()])

        self.mongo.write_hooks.append(hook)
        return seen

    def assert_no_secrets(self, text):
        self.assertNotIn(fakes.FAKE_PASSWORD, text)
        self.assertNotIn(fakes.FAKE_USER, text)

    def assert_lost_final_row_fails(self, *argvs):
        """Con el registro roto justo en el renglón final: avisa con !! y sale con error."""
        real_append = registry.Registry.append

        def append(reg, row):
            if row["result"] != registry.STARTED:
                raise registry.RegistryError("no se pudo escribir el registro (OSError)")
            return real_append(reg, row)

        for argv in argvs:
            with self.subTest(argv=argv):
                with mock.patch.object(registry.Registry, "append", append):
                    code, out = run(self.module.main, argv)
                self.assertNotIn(code, (None, 0), out)
                self.assertIn("!! no se pudo escribir el registro (OSError)", out)
                self.assertIn("!! La corrida no quedó registrada", out)


class PlanPathTest(LegacyTestCase):
    script = "publish_revisions"

    def setUp(self):
        super().setUp()
        revisions = os.path.join(self.repo, ".content-audit", "revisions", PLAN)
        write_json(os.path.join(revisions, "p1.json"), {
            "verdict": "APPROVED",
            "proposal": {"proposalId": "p1", "elementAfter": {"nodeTarget": "QUIZ", "nodeId": QUIZ}}})
        write_json(os.path.join(self.repo, "db", "english-course", "u1", "quizzes.json"), [
            {"_id": {"$oid": QUIZ}, "title": "To be", "translation": "Ella es alta.",
             "form": {"kind": "FILL", "sentences": ["She is tall."],
                      "sentenceParts": [{"kind": "TEXT", "text": "She is tall."}]}}])
        self.mongo.collection(fakes.DB, "knowledges")
        self.mongo.collection(fakes.DB, "quizTemplates").append(
            {"_id": ObjectId(QUIZ), "title": "To be", "translation": "Ella es muy alta.",
             "form": {"kind": "FILL", "incidence": 1, "sentences": ["She is tall."],
                      "sentenceParts": [{"kind": "TEXT", "text": "She is tall."}]}})

    def plan_args(self, *extra, uri=fakes.URI):
        return self.args("--plan", PLAN, "--no-sync-quiz-titles", *extra, uri=uri)

    def test_F_PUBPROD_R006_el_camino_por_plan_se_niega_a_escribir_el_plan_de_una_tanda(self):
        fakes.write_batch(self.repo, plan_id=PLAN)
        code, out = run(self.module.main, self.plan_args("--apply"))
        self.assertEqual(code, 2)
        self.assertIn("NEGADO", out)
        self.assertEqual(self.mongo.clients, [])  # se niega sin conectarse
        row = self.rows()[-1]
        self.assertEqual((row["tool"], row["result"], row["reason"]["code"]),
                         ("publish_revisions", registry.REFUSED, "PLAN_OF_BATCH"))
        self.assertEqual(row["batches"], [fakes.BATCH_ID])
        self.assertEqual(row["destination"], {"host": fakes.HOST, "db": fakes.DB})

    def test_F_PUBPROD_R006_tambien_encuentra_la_tanda_dentro_de_una_campana(self):
        fakes.write_batch(os.path.join(self.repo, ".content-audit", "campaigns", "a1-v11"), plan_id=PLAN,
                          status="CLOSED")
        code, _ = run(self.module.main, self.plan_args("--apply"))
        self.assertEqual(code, 2)
        self.assertEqual(self.mongo.clients, [])

    def test_F_PUBPROD_R006_si_no_se_pueden_leer_las_tandas_el_camino_por_plan_no_escribe(self):
        os.makedirs(os.path.join(self.repo, ".content-audit", "batches"))
        with open(os.path.join(self.repo, ".content-audit", "batches", "rota.json"), "w") as fh:
            fh.write('{"id": "rota", "planId": ')
        code, _ = run(self.module.main, self.plan_args("--apply"))
        self.assertEqual(code, 2)
        self.assertEqual(self.mongo.clients, [])
        self.assertEqual(self.rows()[-1]["reason"]["code"], "BATCHES_UNREADABLE")

    def test_F_PUBPROD_R006_el_ensayo_del_plan_de_una_tanda_avisa_y_no_escribe(self):
        fakes.write_batch(self.repo, plan_id=PLAN)
        code, out = run(self.module.main, self.plan_args())
        self.assertIn(code, (None, 0))
        self.assertIn("AVISO", out)
        self.assertEqual(self.mongo.writes, [])
        self.assertEqual(self.rows()[-1]["result"], registry.DRY_RUN_CLEAN)

    def test_F_PUBPROD_R006_un_plan_que_no_es_de_ninguna_tanda_se_sigue_aplicando(self):
        fakes.write_batch(self.repo, plan_id="otro-plan")
        code, _ = run(self.module.main, self.plan_args("--apply"))
        self.assertIn(code, (None, 0))
        doc = self.mongo.find(fakes.DB, "quizTemplates", QUIZ)
        self.assertEqual(doc["translation"], "Ella es alta.")
        self.assertEqual(doc["form"]["incidence"], 1)  # lo protegido sigue sin tocarse

    def test_F_PUBPROD_R008_el_camino_por_plan_deja_renglones_y_el_respaldo_antes_de_escribir(self):
        code, _ = run(self.module.main, self.plan_args())
        self.assertIn(code, (None, 0))
        dry = self.rows()[-1]
        self.assertEqual((dry["mode"], dry["result"]), (registry.DRY_RUN, registry.DRY_RUN_CLEAN))
        self.assertEqual(dry["counts"]["quizTemplateUpdates"], 1)
        self.assertEqual(self.mongo.writes, [])

        seen = self.assert_started_before_first_write()
        code, _ = run(self.module.main, self.plan_args("--apply"))
        self.assertIn(code, (None, 0))
        self.assertEqual(seen, [[registry.DRY_RUN_CLEAN, registry.STARTED]])
        started, final = self.rows()[-2:]
        self.assertEqual(started["runId"], final["runId"])
        self.assertEqual(final["result"], registry.APPLIED_VERIFIED)
        self.assertEqual(final["writtenIds"], ["quizTemplates/" + QUIZ])
        self.assertTrue(final["verified"])
        detail = fakes.run_detail(self.repo, final)
        self.assertEqual(detail["documents"][0]["old"], {"translation": "Ella es muy alta."})
        self.assertEqual(detail["documents"][0]["new"], {"translation": "Ella es alta."})
        with open(os.path.join(self.repo, final["report"]), encoding="utf-8") as fh:
            report = json.load(fh)
        self.assertEqual(report["updates"]["quizTemplates"][0]["old"], {"translation": "Ella es muy alta."})
        self.assert_no_secrets(fakes.all_text_under(self.repo))

    def test_F_PUBPROD_R008_el_camino_por_plan_conserva_la_guarda_de_opcion_multiple(self):
        write_json(os.path.join(self.repo, ".content-audit", "revisions", PLAN, "p2.json"), {
            "verdict": "APPROVED",
            "proposal": {"proposalId": "p2", "elementAfter": {"nodeTarget": "QUIZ", "nodeId": QUIZ_MC}}})
        write_json(os.path.join(self.repo, "db", "english-course", "u2", "quizzes.json"), [
            {"_id": {"$oid": QUIZ_MC}, "title": "MC", "translation": "nueva",
             "form": {"kind": "MULTIPLE_CHOICE", "sentences": ["otra"], "items": []}}])
        self.mongo.collection(fakes.DB, "quizTemplates").append(
            {"_id": ObjectId(QUIZ_MC), "title": "MC", "translation": "vieja",
             "form": {"kind": "MULTIPLE_CHOICE", "sentences": ["una"], "items": []}})
        run(self.module.main, self.plan_args("--apply"))
        doc = self.mongo.find(fakes.DB, "quizTemplates", QUIZ_MC)
        self.assertEqual((doc["translation"], doc["form"]["sentences"]), ("nueva", ["una"]))

    def test_F_PUBPROD_R008_con_problemas_duros_no_escribe_y_lo_registra(self):
        write_json(os.path.join(self.repo, ".content-audit", "revisions", PLAN, "p3.json"), {
            "verdict": "APPROVED",
            "proposal": {"proposalId": "p3", "elementAfter": {"nodeTarget": "QUIZ", "nodeId": QUIZ_MC}}})
        with self.assertRaises(SystemExit):
            run(self.module.main, self.plan_args("--apply"))
        self.assertEqual(self.mongo.writes, [])
        row = self.rows()[-1]
        self.assertEqual((row["result"], row["reason"]["code"]), (registry.ABORTED, "HARD_PROBLEMS"))
        self.assertNotIn(registry.STARTED, [r["result"] for r in self.rows()])

    def test_F_PUBPROD_R008_si_no_puede_dejar_el_respaldo_no_escribe(self):
        os.makedirs(os.path.join(self.repo, ".content-audit"), exist_ok=True)
        with open(os.path.join(self.repo, ".content-audit", "production"), "w") as fh:
            fh.write("un archivo donde va la carpeta del registro")
        with self.assertRaises(SystemExit) as ctx:
            run(self.module.main, self.plan_args("--apply"))
        self.assertIn("no se escribió nada", str(ctx.exception.code))
        self.assertEqual(self.mongo.writes, [])

    def test_F_PUBPROD_R008_si_la_verificacion_falla_queda_registrado(self):
        def corrupt(collection, doc):
            doc["translation"] = "otra cosa"

        self.mongo.after_write = corrupt
        with self.assertRaises(SystemExit):
            run(self.module.main, self.plan_args("--apply"))
        row = self.rows()[-1]
        self.assertEqual(row["result"], registry.VERIFY_FAILED)
        self.assertFalse(row["verified"])
        self.assertEqual(row["writtenIds"], ["quizTemplates/" + QUIZ])

    def test_F_PUBPROD_R008_un_error_de_la_base_se_muestra_solo_por_su_clase(self):
        self.mongo.fail_connect = ServerSelectionTimeoutError(
            "%s:%s@%s no responde" % (fakes.FAKE_USER, fakes.FAKE_PASSWORD, fakes.HOST))
        code, out = run(self.module.main, self.plan_args("--apply"))
        self.assertEqual(code, 1)
        self.assertIn("ServerSelectionTimeoutError", out)
        self.assert_no_secrets(out)
        row = self.rows()[-1]
        self.assertEqual((row["result"], row["errorClass"]), (registry.ABORTED, "ServerSelectionTimeoutError"))
        self.assert_no_secrets(fakes.all_text_under(self.repo))

    def test_F_PUBPROD_R008_un_uri_ilegible_se_niega_sin_conectarse_ni_repetirlo(self):
        code, out = run(self.module.main, self.plan_args("--apply", uri=BAD_URI))
        self.assertEqual(code, 2)
        self.assertEqual(self.mongo.clients, [])
        self.assert_no_secrets(out)
        self.assertEqual(self.rows()[-1]["reason"]["code"], "BAD_URI")

    def test_F_PUBPROD_R008_el_camino_por_plan_sin_renglon_final_avisa_y_sale_mal(self):
        self.assert_lost_final_row_fails(self.plan_args(), self.plan_args("--apply"))
        self.assertEqual(self.mongo.find(fakes.DB, "quizTemplates", QUIZ)["translation"], "Ella es alta.")

    def test_F_PUBPROD_R006_con_batch_va_al_paso_por_tanda(self):
        workdir = fakes.temp_workdir(self)
        for argv in (["--batch", "no-existe"], ["--batch=no-existe"]):
            with self.subTest(argv=argv):
                code, out = run(self.module.main, argv + ["--workdir", workdir, "--uri", fakes.URI])
                self.assertEqual(code, 2)
                row = fakes.registry_rows(workdir)[-1]
                self.assertEqual((row["tool"], row["reason"]["code"]), ("production_publish", "BATCH_NOT_FOUND"))
        self.assertEqual(self.mongo.clients, [])
        self.assertEqual(self.rows(), [])  # el camino por plan no corrió


class KnowledgeWritersMixin:
    """Lo común a los escritores de nombres y de consignas de los temas."""

    field = None
    file_value = None
    mongo_value = None

    def setUp(self):
        super().setUp()
        write_json(os.path.join(self.repo, "db", "english-course", "u1", "_knowledge.json"), {
            "_id": {"$oid": KNOWLEDGE}, "kind": "KNOWLEDGE", "label": "El verbo $to be$",
            "instructions": "Completá con el verbo $to be$."})
        self.mongo.collection(fakes.DB, "knowledges").append(
            {"_id": ObjectId(KNOWLEDGE), "kind": "KNOWLEDGE", "label": "To be",
             "instructions": "Completá con to be.", "order": 3})

    def test_F_PUBPROD_R008_el_ensayo_deja_su_renglon_y_no_escribe(self):
        code, _ = run(self.module.main, self.args())
        self.assertIn(code, (None, 0))
        self.assertEqual(self.mongo.writes, [])
        row = self.rows()[-1]
        self.assertEqual((row["tool"], row["mode"], row["result"]),
                         (self.script, registry.DRY_RUN, registry.DRY_RUN_CLEAN))
        self.assertEqual(row["destination"], {"host": fakes.HOST, "db": fakes.DB})

    def test_F_PUBPROD_R008_aplicar_deja_el_respaldo_y_el_inicio_antes_de_escribir(self):
        seen = self.assert_started_before_first_write()
        code, _ = run(self.module.main, self.args("--apply"))
        self.assertIn(code, (None, 0))
        self.assertEqual(seen, [[registry.STARTED]])
        started, final = self.rows()
        self.assertEqual(started["runId"], final["runId"])
        self.assertEqual(final["result"], registry.APPLIED_VERIFIED)
        self.assertEqual(final["writtenIds"], ["knowledges/" + KNOWLEDGE])
        detail = fakes.run_detail(self.repo, final)
        self.assertEqual(detail["documents"][0]["old"], {self.field: self.mongo_value})
        self.assertEqual(detail["documents"][0]["new"], {self.field: self.file_value})
        doc = self.mongo.find(fakes.DB, "knowledges", KNOWLEDGE)
        self.assertEqual((doc[self.field], doc["order"]), (self.file_value, 3))
        self.assert_no_secrets(fakes.all_text_under(self.repo))

    def test_F_PUBPROD_R008_si_la_verificacion_falla_termina_con_error_y_lo_registra(self):
        def corrupt(collection, doc):
            doc[self.field] = "otra cosa"

        self.mongo.after_write = corrupt
        code, _ = run(self.module.main, self.args("--apply"))
        self.assertEqual(code, 1)
        row = self.rows()[-1]
        self.assertEqual((row["result"], row["verified"]), (registry.VERIFY_FAILED, False))

    def test_F_PUBPROD_R008_si_no_puede_dejar_el_inicio_no_escribe(self):
        os.makedirs(os.path.join(self.repo, ".content-audit"), exist_ok=True)
        with open(os.path.join(self.repo, ".content-audit", "production"), "w") as fh:
            fh.write("un archivo donde va la carpeta del registro")
        code, out = run(self.module.main, self.args("--apply"))
        self.assertEqual(code, 1)
        self.assertEqual(self.mongo.writes, [])

    def test_F_PUBPROD_R008_un_error_de_la_base_se_muestra_solo_por_su_clase(self):
        self.mongo.fail_connect = ServerSelectionTimeoutError(
            "%s:%s@%s no responde" % (fakes.FAKE_USER, fakes.FAKE_PASSWORD, fakes.HOST))
        code, out = run(self.module.main, self.args("--apply"))
        self.assertEqual(code, 1)
        self.assertIn("ServerSelectionTimeoutError", out)
        self.assert_no_secrets(out)
        self.assertEqual(self.rows()[-1]["errorClass"], "ServerSelectionTimeoutError")

    def test_F_PUBPROD_R008_sin_renglon_final_avisa_y_sale_mal(self):
        self.assert_lost_final_row_fails(self.args(), self.args("--apply"))
        self.assertEqual(self.mongo.find(fakes.DB, "knowledges", KNOWLEDGE)[self.field], self.file_value)

    def test_F_PUBPROD_R008_un_uri_ilegible_se_niega_sin_conectarse_ni_repetirlo(self):
        code, out = run(self.module.main, self.args("--apply", uri=BAD_URI))
        self.assertEqual(code, 2)
        self.assertEqual(self.mongo.clients, [])
        self.assert_no_secrets(out)
        self.assertEqual(self.rows()[-1]["result"], registry.REFUSED)


class LabelsTest(KnowledgeWritersMixin, LegacyTestCase):
    script = "publish_knowledge_labels"
    field = "label"
    file_value = "El verbo $to be$"
    mongo_value = "To be"

    def test_F_PUBPROD_R008_un_tema_que_no_esta_en_la_base_es_un_ensayo_con_problemas(self):
        self.mongo.data[fakes.DB]["knowledges"].clear()
        run(self.module.main, self.args())
        self.assertEqual(self.rows()[-1]["result"], registry.DRY_RUN_WITH_PROBLEMS)

    def test_F_PUBPROD_R008_conserva_el_informe_con_los_nombres_viejos(self):
        run(self.module.main, self.args("--apply"))
        final = self.rows()[-1]
        with open(os.path.join(self.repo, final["report"]), encoding="utf-8") as fh:
            report = json.load(fh)
        self.assertEqual(report["oldLabels"][0]["oldLabel"], "To be")


class InstructionsTest(KnowledgeWritersMixin, LegacyTestCase):
    script = "publish_knowledge_instructions"
    field = "instructions"
    file_value = "Completá con el verbo $to be$."
    mongo_value = "Completá con to be."

    def test_F_PUBPROD_R008_sin_nada_que_hacer_tambien_deja_su_renglon(self):
        self.mongo.data[fakes.DB]["knowledges"][0]["instructions"] = self.file_value
        for argv, result in ((self.args(), registry.DRY_RUN_CLEAN), (self.args("--apply"), registry.APPLIED_VERIFIED)):
            with self.subTest(argv=argv):
                code, out = run(self.module.main, argv)
                self.assertEqual(code, 0)
                self.assertIn("Nada que hacer", out)
                self.assertEqual(self.rows()[-1]["result"], result)
        self.assertEqual(self.mongo.writes, [])


if __name__ == "__main__":
    unittest.main()
