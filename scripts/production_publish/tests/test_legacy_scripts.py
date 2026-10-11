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
from production_publish.tests.fake_driver import (FakeCollection, FakeMongo, ObjectId, OperationFailure,
                                                  ServerSelectionTimeoutError, load_script)

PLAN = "2026-10-05T10-00-00"
QUIZ = "665f000000000000000000b1"
QUIZ_MC = "665f000000000000000000b2"
KNOWLEDGE = "665f000000000000000000c1"
INSTANCE = "665f000000000000000000d1"
MISSING_KNOWLEDGE = "665f000000000000000000c9"
KNOWLEDGE_2 = "665f000000000000000000c2"
LABEL = "El verbo $to be$"
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


    def assert_begin_writing_guards(self, argv):
        """Sin el respaldo de la corrida o sin el renglón de inicio, no escribe nada y lo dice."""
        real_append = registry.Registry.append

        def no_detail(reg, run_id, detail):
            raise registry.RegistryError("no se pudo escribir el detalle (OSError)")

        def no_started(reg, row):
            if row["result"] == registry.STARTED:
                raise registry.RegistryError("no se pudo escribir el registro (OSError)")
            return real_append(reg, row)

        for name, broken in (("write_run", no_detail), ("append", no_started)):
            with self.subTest(falla=name):
                before = len(self.rows())
                with mock.patch.object(registry.Registry, name, broken):
                    try:
                        code, out = run(self.module.main, argv)
                    except SystemExit as e:
                        code, out = 1, str(e.code)
                self.assertEqual(code, registry.EXIT_PROBLEMS, out)
                self.assertIn("no se escribió nada", out)
                self.assertEqual(self.mongo.writes, [])
                new_rows = self.rows()[before:]
                self.assertNotIn(registry.STARTED, [r["result"] for r in new_rows])
                if name == "write_run":
                    self.assertEqual((new_rows[-1]["result"], new_rows[-1]["reason"]["code"]),
                                     (registry.ABORTED, "DETAIL_NOT_WRITTEN"))


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

    def test_F_PUBPROD_R006_encuentra_las_tandas_de_un_plan_en_el_repo_y_en_las_campanas(self):
        fakes.write_batch(self.repo, batch_id="tanda-raiz", plan_id="plan-x")
        campaign = os.path.join(self.repo, ".content-audit", "campaigns", "a1-v11")
        fakes.write_batch(campaign, batch_id="tanda-campana", plan_id="plan-x")
        fakes.write_batch(campaign, batch_id="tanda-otra", plan_id="plan-y")
        # La misma raíz dos veces (--repo y el repo del script): cada tanda cuenta una sola vez.
        matches, unreadable = self.module.batches_of_plan((self.repo, self.repo + os.sep), "plan-x")
        self.assertEqual(sorted(i for _, i in matches), ["tanda-campana", "tanda-raiz"])
        self.assertEqual(unreadable, [])

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

    def test_F_PUBPROD_R008_el_ensayo_del_camino_por_plan_con_problemas_duros_termina_con_error(self):
        write_json(os.path.join(self.repo, ".content-audit", "revisions", PLAN, "p3.json"), {
            "verdict": "APPROVED",
            "proposal": {"proposalId": "p3", "elementAfter": {"nodeTarget": "QUIZ", "nodeId": QUIZ_MC}}})
        code, out = run(self.module.main, self.plan_args())
        self.assertEqual(code, registry.EXIT_PROBLEMS, out)
        self.assertEqual(self.mongo.writes, [])
        self.assertEqual(self.rows()[-1]["result"], registry.DRY_RUN_WITH_PROBLEMS)

    def test_F_PUBPROD_R008_si_no_puede_dejar_el_respaldo_no_escribe(self):
        os.makedirs(os.path.join(self.repo, ".content-audit"), exist_ok=True)
        with open(os.path.join(self.repo, ".content-audit", "production"), "w") as fh:
            fh.write("un archivo donde va la carpeta del registro")
        with self.assertRaises(SystemExit) as ctx:
            run(self.module.main, self.plan_args("--apply"))
        self.assertIn("no se escribió nada", str(ctx.exception.code))
        self.assertEqual(self.mongo.writes, [])

    def test_F_PUBPROD_R008_el_camino_por_plan_sin_respaldo_o_sin_inicio_no_escribe(self):
        self.assert_begin_writing_guards(self.plan_args("--apply"))

    def test_F_PUBPROD_R008_el_camino_por_plan_respalda_y_verifica_los_campos_anidados(self):
        write_json(os.path.join(self.repo, "db", "english-course", "u1", "quizzes.json"), [
            {"_id": {"$oid": QUIZ}, "title": "To be", "translation": "Ella es muy alta.",
             "form": {"kind": "FILL", "sentences": ["She is tall."],
                      "sentenceParts": [{"kind": "TEXT", "text": "She is very tall."}]}}])

        def corrupt(collection, doc):
            doc["form"]["sentenceParts"] = [{"kind": "TEXT", "text": "otra cosa"}]

        self.mongo.after_write = corrupt
        out = io.StringIO()
        with contextlib.redirect_stdout(out), self.assertRaises(SystemExit):
            self.module.main(self.plan_args("--apply"))
        self.assertIn("FAIL quizTemplates %s form.sentenceParts (p1)" % QUIZ, out.getvalue())
        row = self.rows()[-1]
        self.assertEqual((row["result"], row["reason"]["message"]), (registry.VERIFY_FAILED, "1 campos no coinciden"))
        detail = fakes.run_detail(self.repo, row)
        self.assertEqual(detail["documents"][0]["old"],
                         {"form.sentenceParts": [{"kind": "TEXT", "text": "She is tall."}]})
        self.assertEqual(detail["documents"][0]["new"],
                         {"form.sentenceParts": [{"kind": "TEXT", "text": "She is very tall."}]})

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

    def setup_title_sync(self):
        """Un tema con su nombre nuevo, su ejercicio con el título viejo y una instancia con la copia vieja."""
        write_json(os.path.join(self.repo, "db", "english-course", "u1", "_knowledge.json"), {
            "_id": {"$oid": KNOWLEDGE}, "kind": "KNOWLEDGE", "label": LABEL})
        with open(os.path.join(self.repo, "db", "english-course", "u1", "quizzes.json"), "w", encoding="utf-8") as fh:
            fh.write('[{ "_id" : { "$oid" : "%s" }, "title" : "To be", "translation" : "Ella es alta.", '
                     '"form" : { "kind" : "FILL", "sentences" : ["She is tall."], '
                     '"sentenceParts" : [{ "kind" : "TEXT", "text" : "She is tall." }] } }]' % QUIZ)
        template = self.mongo.find(fakes.DB, "quizTemplates", QUIZ)
        template.update({"title": "Ser", "instructions": "Completá."})
        self.mongo.collection(fakes.DB, "quizzes").append(
            {"_id": ObjectId(INSTANCE), "student": "s1",
             "template": {"_id": ObjectId(QUIZ), "title": "Ser", "instructions": "Completá con to be."}})

    def test_F_PUBPROD_R008_la_sincronizacion_de_titulos_deja_su_respaldo_antes_de_escribir_y_sus_ids(self):
        self.setup_title_sync()
        sync_args = self.args("--plan", PLAN)
        code, _ = run(self.module.main, sync_args)
        self.assertIn(code, (None, 0))
        self.assertEqual(self.mongo.writes, [])  # el ensayo arma la sincronización sin escribirla
        self.assertEqual(self.rows()[-1]["quizTitleSync"]["embedded_instances_synced"], 1)

        backups = []

        def hook(collection, query, update):
            if not backups:
                started = self.rows()[-1]
                self.assertEqual(started["result"], registry.STARTED)
                backups.append(fakes.run_detail(self.repo, started))

        self.mongo.write_hooks.append(hook)
        code, _ = run(self.module.main, self.args("--plan", PLAN, "--apply"))
        self.assertIn(code, (None, 0))
        sync = {(e["collection"], e["_id"]): (e["old"], e["new"]) for e in backups[0]["titleSync"]}
        self.assertEqual(sync, {
            ("quizTemplates", QUIZ): ({"title": "Ser"}, {"title": LABEL}),
            ("quizzes", INSTANCE): ({"template.title": "Ser", "template.instructions": "Completá con to be."},
                                    {"template.title": LABEL, "template.instructions": "Completá."})})
        final = self.rows()[-1]
        self.assertEqual(final["result"], registry.APPLIED_VERIFIED)
        self.assertEqual(final["writtenIds"], ["quizTemplates/" + QUIZ, "quizzes/" + INSTANCE])
        self.assertEqual(self.mongo.find(fakes.DB, "quizTemplates", QUIZ)["title"], LABEL)
        instance = self.mongo.find(fakes.DB, "quizzes", INSTANCE)
        self.assertEqual((instance["template"]["title"], instance["template"]["instructions"], instance["student"]),
                         (LABEL, "Completá.", "s1"))
        with open(os.path.join(self.repo, "db", "english-course", "u1", "quizzes.json"), encoding="utf-8") as fh:
            self.assertIn('"title" : "%s"' % LABEL, fh.read())

    def test_F_PUBPROD_R008_si_la_sincronizacion_de_titulos_no_queda_lo_registra_con_sus_ids(self):
        self.setup_title_sync()

        def corrupt(collection, doc):
            if collection == "quizzes":
                doc["template"]["title"] = "otra cosa"

        self.mongo.after_write = corrupt
        with self.assertRaises(SystemExit) as ctx:
            run(self.module.main, self.args("--plan", PLAN, "--apply"))
        self.assertIn("sincronización de títulos", str(ctx.exception.code))
        row = self.rows()[-1]
        self.assertEqual((row["result"], row["verified"]), (registry.VERIFY_FAILED, False))
        self.assertEqual(row["writtenIds"], ["quizTemplates/" + QUIZ, "quizzes/" + INSTANCE])

    def test_F_PUBPROD_R008_si_la_sincronizacion_de_titulos_se_corta_el_renglon_de_fin_nombra_lo_escrito(self):
        self.setup_title_sync()

        def fail_on_instance(collection, query, update):
            if collection == "quizzes":
                raise OperationFailure("la base no responde")

        self.mongo.write_hooks.append(fail_on_instance)
        code, out = run(self.module.main, self.args("--plan", PLAN, "--apply"))
        self.assertEqual(code, registry.EXIT_PROBLEMS, out)
        started, row = self.rows()[-2:]
        self.assertEqual((started["result"], row["result"], row["reason"]["code"]),
                         (registry.STARTED, registry.ABORTED, "STORE_ERROR"))
        self.assertEqual(row["runId"], started["runId"])
        self.assertEqual(row["writtenIds"], ["quizTemplates/" + QUIZ])
        self.assertEqual(row["indeterminate"], ["quizzes/" + INSTANCE])
        self.assertIs(row["verified"], False)
        self.assertIn("NO SE SABE si quedó escrito quizzes/%s" % INSTANCE, out)
        detail = fakes.run_detail(self.repo, row)
        self.assertEqual((detail["documents"][0]["old"], detail["documents"][0]["new"]),
                         ({"title": "Ser", "translation": "Ella es muy alta."},
                          {"title": "To be", "translation": "Ella es alta."}))
        sync = {(e["collection"], e["_id"]): (e["old"], e["new"]) for e in detail["titleSync"]}
        self.assertEqual(sync[("quizzes", INSTANCE)],
                         ({"template.title": "Ser", "template.instructions": "Completá con to be."},
                          {"template.title": LABEL, "template.instructions": "Completá."}))

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

    def test_F_PUBPROD_R008_un_tema_del_arbol_que_no_esta_en_la_base_hace_un_ensayo_con_problemas_que_termina_con_error(self):
        write_json(os.path.join(self.repo, "db", "english-course", "u2", "_knowledge.json"), {
            "_id": {"$oid": MISSING_KNOWLEDGE}, "kind": "KNOWLEDGE", "label": "Los pronombres",
            "instructions": "Elegí el pronombre."})
        code, out = run(self.module.main, self.args())
        self.assertEqual(code, registry.EXIT_PROBLEMS, out)
        self.assertIn(MISSING_KNOWLEDGE, out)
        self.assertEqual(self.mongo.writes, [])
        row = self.rows()[-1]
        self.assertEqual((row["mode"], row["result"]), (registry.DRY_RUN, registry.DRY_RUN_WITH_PROBLEMS))
        self.assertEqual(row["counts"]["missing"], 1)

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

    def add_second_knowledge(self):
        """Otro tema con su nombre y su consigna nuevos: la escritura en lote lleva dos."""
        write_json(os.path.join(self.repo, "db", "english-course", "u2", "_knowledge.json"), {
            "_id": {"$oid": KNOWLEDGE_2}, "kind": "KNOWLEDGE", "label": "Los pronombres",
            "instructions": "Elegí el pronombre."})
        self.mongo.collection(fakes.DB, "knowledges").append(
            {"_id": ObjectId(KNOWLEDGE_2), "kind": "KNOWLEDGE", "label": "Pronombres",
             "instructions": "Elegí.", "order": 4})

    def test_F_PUBPROD_R008_si_la_base_falla_a_mitad_de_escribir_el_renglon_de_fin_dice_que_no_se_sabe_que_quedo(self):
        self.add_second_knowledge()

        def fail_on_second(collection, query, update):
            if len(self.mongo.writes) == 1:
                raise OperationFailure("la base no responde")

        self.mongo.write_hooks.append(fail_on_second)
        code, out = run(self.module.main, self.args("--apply"))
        self.assertEqual(code, registry.EXIT_PROBLEMS, out)
        self.assertEqual(len(self.mongo.writes), 1)  # el primero llegó; el lote no terminó
        started, final = self.rows()
        self.assertEqual((started["result"], final["result"], final["reason"]["code"]),
                         (registry.STARTED, registry.ABORTED, "STORE_ERROR"))
        self.assertEqual(final["runId"], started["runId"])
        self.assertEqual(final["writtenIds"], [])
        self.assertEqual(sorted(final["indeterminate"]), ["knowledges/" + KNOWLEDGE, "knowledges/" + KNOWLEDGE_2])
        self.assertIs(final["verified"], False)
        self.assertIn("NO SE SABE si quedó escrito knowledges/%s" % KNOWLEDGE, out)
        backup = {d["_id"]: (d["old"], d["new"]) for d in fakes.run_detail(self.repo, final)["documents"]}
        self.assertEqual(set(backup), {KNOWLEDGE, KNOWLEDGE_2})
        self.assertEqual(backup[KNOWLEDGE], ({self.field: self.mongo_value}, {self.field: self.file_value}))

    def test_F_PUBPROD_R008_un_ctrl_c_despues_de_escribir_deja_el_renglon_de_fin_con_lo_escrito(self):
        real_find_one = FakeCollection.find_one

        def find_one(collection, query=None, projection=None):
            if self.mongo.writes:  # ya escribió: el Ctrl-C llega al verificar
                raise KeyboardInterrupt
            return real_find_one(collection, query, projection)

        with mock.patch.object(FakeCollection, "find_one", find_one), self.assertRaises(KeyboardInterrupt):
            run(self.module.main, self.args("--apply"))
        started, final = self.rows()
        self.assertEqual((final["result"], final["reason"]["code"]), (registry.ABORTED, "INTERRUPTED"))
        self.assertEqual(final["runId"], started["runId"])
        self.assertEqual(final["writtenIds"], ["knowledges/" + KNOWLEDGE])
        self.assertEqual(final["indeterminate"], [])
        self.assertIs(final["verified"], False)
        document = fakes.run_detail(self.repo, final)["documents"][0]
        self.assertEqual((document["old"], document["new"]), ({self.field: self.mongo_value}, {self.field: self.file_value}))

    def test_F_PUBPROD_R008_si_no_puede_dejar_el_inicio_no_escribe(self):
        os.makedirs(os.path.join(self.repo, ".content-audit"), exist_ok=True)
        with open(os.path.join(self.repo, ".content-audit", "production"), "w") as fh:
            fh.write("un archivo donde va la carpeta del registro")
        code, out = run(self.module.main, self.args("--apply"))
        self.assertEqual(code, 1)
        self.assertEqual(self.mongo.writes, [])

    def test_F_PUBPROD_R008_sin_respaldo_o_sin_inicio_no_escribe(self):
        self.assert_begin_writing_guards(self.args("--apply"))

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

    def test_F_PUBPROD_R008_si_el_informe_no_se_puede_guardar_despues_de_escribir_el_renglon_de_fin_nombra_lo_escrito(self):
        os.makedirs(os.path.join(self.repo, ".content-audit"), exist_ok=True)
        with open(os.path.join(self.repo, ".content-audit", "publish-reports"), "w") as fh:
            fh.write("un archivo donde va la carpeta de los informes")
        with self.assertRaises(OSError):
            run(self.module.main, self.args("--apply"))
        row = self.rows()[-1]
        self.assertEqual((row["result"], row["reason"]["code"], row["errorClass"]),
                         (registry.ABORTED, "ERROR", "FileExistsError"))
        self.assertEqual(row["writtenIds"], ["knowledges/" + KNOWLEDGE])
        self.assertEqual(row["indeterminate"], [])
        self.assertEqual(fakes.run_detail(self.repo, row)["documents"][0]["old"], {"label": "To be"})

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
