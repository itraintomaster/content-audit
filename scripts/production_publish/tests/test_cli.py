"""El paso a producción entero con la colección falsa: ensayo, aplicación, verificación y registro."""

import builtins
import copy
import io
import json
import os
import unittest
from unittest import mock

from production_publish import cli, manifest_reader, registry
from production_publish.registry import (ABORTED, APPLIED_VERIFIED, DRY_RUN_CLEAN, DRY_RUN_WITH_PROBLEMS,
                                         REFUSED, STARTED, VERIFY_FAILED, RegistryError)
from production_publish.tests import fakes
from production_publish.tests.fakes import APPLY_ARGS, run_cli


class CliTestCase(unittest.TestCase):
    def setUp(self):
        self.workdir = fakes.temp_workdir(self)
        self.clock = fakes.Clock()
        self.store = fakes.FakeStore()
        self.original = copy.deepcopy(self.store.data)

    def factory(self, **kwargs):
        return fakes.FakeStoreFactory(self.store, **kwargs)

    def run_cli(self, *extra, factory=None, uri=fakes.URI):
        return run_cli(self.workdir, *extra, factory=factory or self.factory(), clock=self.clock, uri=uri)

    def rows(self):
        return fakes.registry_rows(self.workdir)

    def results(self):
        return [r["result"] for r in self.rows()]

    def dry_run_then_apply(self, *flags):
        code, out, _ = self.run_cli(*flags)
        self.assertEqual(code, 0, out)
        return self.run_cli(*(APPLY_ARGS + flags))


class FullCycleTest(CliTestCase):
    def setUp(self):
        super().setUp()
        fakes.write_batch(self.workdir)

    def test_F_PUBPROD_R009_el_ensayo_no_escribe_nada(self):
        code, out, factory = self.run_cli()
        self.assertEqual(code, 0, out)
        self.assertEqual(self.store.writes, [])
        self.assertEqual(self.store.attempts, [])
        self.assertEqual(self.store.data, self.original)
        self.assertEqual(self.results(), [DRY_RUN_CLEAN])
        self.assertIn("ENSAYO (no escribe nada)", out)
        self.assertEqual(self.rows()[0]["wouldWrite"], 5)

    def test_F_PUBPROD_R009_el_ensayo_no_toca_el_curso_en_el_disco(self):
        before = fakes.all_text_under(os.path.join(self.workdir, ".content-audit", "batch-publications"))
        batches = fakes.all_text_under(os.path.join(self.workdir, ".content-audit", "batches"))
        self.run_cli()
        self.assertEqual(before, fakes.all_text_under(
            os.path.join(self.workdir, ".content-audit", "batch-publications")))
        self.assertEqual(batches, fakes.all_text_under(os.path.join(self.workdir, ".content-audit", "batches")))
        written = sorted(os.listdir(os.path.join(self.workdir, ".content-audit")))
        self.assertEqual(written, ["batch-publications", "batches", "production"])

    def test_F_PUBPROD_R006_ensayo_aplicacion_y_verificacion_de_punta_a_punta(self):
        code, out, _ = self.dry_run_then_apply()
        self.assertEqual(code, 0, out)
        self.assertEqual(self.results(), [DRY_RUN_CLEAN, STARTED, APPLIED_VERIFIED])
        data = self.store.data
        manifest = fakes.example_manifest()
        for entry in manifest["documents"]:
            current = data[entry["collection"]][entry["id"]]
            for field in entry["fields"]:
                from production_publish.store import get_path, same_value
                self.assertTrue(same_value(get_path(current, field["path"]), field["after"]), field["path"])
        # Lo que nunca se toca quedó igual.
        for doc_id in (fakes.QUIZ_1, fakes.QUIZ_2, fakes.QUIZ_3):
            for key in ("kind", "incidence", "label", "name"):
                self.assertEqual(data["quizTemplates"][doc_id]["form"][key],
                                 self.original["quizTemplates"][doc_id]["form"][key])
            self.assertEqual(data["quizTemplates"][doc_id]["kind"], self.original["quizTemplates"][doc_id]["kind"])
            self.assertEqual(data["quizTemplates"][doc_id]["title"], self.original["quizTemplates"][doc_id]["title"])
        self.assertEqual(data["quizTemplates"]["665f00000000000000000099"],
                         self.original["quizTemplates"]["665f00000000000000000099"])
        self.assertEqual(data["knowledges"][fakes.KNOWLEDGE]["label"],
                         self.original["knowledges"][fakes.KNOWLEDGE]["label"])
        # La copia embebida al día se llevó; la desfasada y la ajena, no.
        self.assertEqual(data["quizzes"][fakes.INSTANCE_IN_SYNC]["template"]["instructions"],
                         "Completá con el verbo $to be$.")
        self.assertEqual(data["quizzes"][fakes.INSTANCE_STALE], self.original["quizzes"][fakes.INSTANCE_STALE])
        self.assertEqual(data["quizzes"][fakes.INSTANCE_OTHER], self.original["quizzes"][fakes.INSTANCE_OTHER])

    def test_F_PUBPROD_R006_escribe_con_el_despues_de_la_lista_y_nunca_lee_db(self):
        os.makedirs(os.path.join(self.workdir, "db", "english-course"))
        with open(os.path.join(self.workdir, "db", "english-course", "quizzes.json"), "w") as fh:
            fh.write("{}")
        opened = []
        real_open = builtins.open
        real_os_open = os.open

        def spy_open(path, *args, **kwargs):
            opened.append(str(path))
            return real_open(path, *args, **kwargs)

        def spy_os_open(path, *args, **kwargs):
            opened.append(str(path))
            return real_os_open(path, *args, **kwargs)

        with mock.patch("builtins.open", spy_open), mock.patch("os.open", spy_os_open):
            code, out, _ = self.dry_run_then_apply()
        self.assertEqual(code, 0, out)
        self.assertFalse([p for p in opened if os.sep + "db" + os.sep in p], opened)

    def test_F_PUBPROD_R006_volver_a_aplicar_no_escribe_lo_que_ya_estaba(self):
        self.dry_run_then_apply()
        writes = len(self.store.writes)
        code, out, _ = self.dry_run_then_apply()
        self.assertEqual(code, 0, out)
        self.assertEqual(len(self.store.writes), writes)
        last = self.rows()[-1]
        self.assertEqual(last["result"], APPLIED_VERIFIED)
        self.assertEqual(last["writtenIds"], [])
        self.assertEqual(last["counts"]["already"], 5)

    def test_F_PUBPROD_R006_una_aplicacion_que_no_escribe_nada_dice_cero_y_cuantos_salteo(self):
        code, out, _ = self.dry_run_then_apply()
        self.assertIn("se escribieron 5 documento(s)", out)
        code, out, _ = self.dry_run_then_apply()
        self.assertEqual(code, 0, out)
        self.assertIn("se escribieron 0 documentos (ya tenían lo que dice la lista: 5; salteados: 1", out)
        self.assertNotIn("quedaron como dice la lista", out)

    def test_F_PUBPROD_R008_el_renglon_final_dice_tanda_huella_modo_resultado_escritos_y_verificado(self):
        self.dry_run_then_apply()
        row = self.rows()[-1]
        loaded = manifest_reader.load(self.workdir, fakes.BATCH_ID)
        self.assertEqual(row["batchId"], fakes.BATCH_ID)
        self.assertEqual(row["planId"], fakes.PLAN_ID)
        self.assertEqual(row["manifestDigest"], loaded.digest)
        self.assertEqual(row["mode"], "APPLY")
        self.assertEqual(row["result"], APPLIED_VERIFIED)
        self.assertIs(row["verified"], True)
        self.assertEqual(row["destination"], {"host": fakes.HOST, "db": fakes.DB})
        self.assertEqual(sorted(row["writtenIds"]), sorted([
            "knowledges/" + fakes.KNOWLEDGE, "quizTemplates/" + fakes.QUIZ_1, "quizTemplates/" + fakes.QUIZ_2,
            "quizTemplates/" + fakes.QUIZ_3, "quizzes/" + fakes.INSTANCE_IN_SYNC]))
        self.assertEqual(row["tool"], cli.TOOL)

    def test_F_PUBPROD_R004_el_registro_cuenta_cuantos_sin_revisar_viajaron(self):
        self.dry_run_then_apply()
        row = self.rows()[-1]
        self.assertEqual(row["pendingTraveled"], 2)  # QUIZ_1 y QUIZ_3 están PENDING
        self.assertEqual(row["reviews"]["pending"], 3)
        self.assertEqual(row["reviews"]["rejected"], 1)

    def test_F_PUBPROD_R008_el_detalle_guarda_el_valor_previo_de_cada_campo_escrito(self):
        self.dry_run_then_apply()
        detail = fakes.run_detail(self.workdir, self.rows()[-1])
        self.assertEqual(detail["result"], APPLIED_VERIFIED)
        by_key = {"%s/%s" % (d["collection"], d["id"]): d for d in detail["documents"]}
        knowledge = by_key["knowledges/" + fakes.KNOWLEDGE]["fields"][0]
        self.assertEqual(knowledge["old"], "El verbo to be.")
        self.assertEqual(knowledge["new"], "El verbo $to be$ en presente.")
        sentences = next(f for f in by_key["quizTemplates/" + fakes.QUIZ_2]["fields"]
                         if f["path"] == "form.sentences")
        self.assertIsNone(sentences["old"])  # lo que había de verdad, no la lista vacía de la lista
        self.assertEqual(len(detail["verification"]), 5)
        self.assertTrue(all(v["ok"] for v in detail["verification"]))

    def test_F_PUBPROD_R008_el_respaldo_y_el_renglon_de_inicio_existen_antes_de_la_primera_escritura(self):
        self.run_cli()
        seen = {}

        def check(store, collection, doc_id):
            if seen:
                return
            rows = fakes.registry_rows(self.workdir)
            seen["last"] = rows[-1]["result"]
            seen["detail"] = fakes.run_detail(self.workdir, rows[-1])

        self.store.on_write = check
        code, out, _ = self.run_cli(*APPLY_ARGS)
        self.assertEqual(code, 0, out)
        self.assertEqual(seen["last"], STARTED)
        self.assertEqual(seen["detail"]["result"], STARTED)
        olds = [f.get("old") for d in seen["detail"]["documents"] for f in d["fields"] if f["status"] == "APPLY"]
        self.assertIn("El verbo to be.", olds)


class DryRunRequiredTest(CliTestCase):
    def setUp(self):
        super().setUp()
        fakes.write_batch(self.workdir)

    def assertRefusedWithoutConnecting(self, code, out, factory, reason_code):
        self.assertEqual(code, 2, out)
        self.assertEqual(factory.calls, [])
        self.assertEqual(self.store.writes, [])
        self.assertEqual(self.rows()[-1]["result"], REFUSED)
        self.assertEqual(self.rows()[-1]["reason"]["code"], reason_code)

    def test_F_PUBPROD_R009_aplicar_sin_ensayo_previo_se_niega(self):
        code, out, factory = self.run_cli(*APPLY_ARGS)
        self.assertRefusedWithoutConnecting(code, out, factory, "NO_CLEAN_DRY_RUN")

    def test_F_PUBPROD_R009_aplicar_sin_nombrar_el_destino_se_niega(self):
        self.run_cli()
        code, out, factory = self.run_cli("--apply")
        self.assertRefusedWithoutConnecting(code, out, factory, "DESTINATION_NOT_NAMED")

    def test_F_PUBPROD_R009_si_el_destino_nombrado_no_es_el_real_no_escribe(self):
        self.run_cli()
        for named in (fakes.OTHER_DESTINATION, fakes.HOST + "/otra", "localhost/learney"):
            with self.subTest(named=named):
                code, out, factory = self.run_cli("--apply", "--expect-destination", named)
                self.assertRefusedWithoutConnecting(code, out, factory, "DESTINATION_MISMATCH")

    def test_F_PUBPROD_R009_el_destino_nombrado_no_admite_usuario_ni_clave(self):
        self.run_cli()
        code, out, factory = self.run_cli("--apply", "--expect-destination", "yo:clave@%s" % fakes.DESTINATION)
        self.assertRefusedWithoutConnecting(code, out, factory, "BAD_EXPECTED_DESTINATION")

    def test_F_PUBPROD_R009_el_ensayo_tambien_controla_el_destino_si_se_nombra(self):
        code, out, factory = self.run_cli("--expect-destination", "otro.host/learney")
        self.assertRefusedWithoutConnecting(code, out, factory, "DESTINATION_MISMATCH")

    def test_F_PUBPROD_R009_un_ensayo_contra_otro_destino_no_habilita(self):
        self.run_cli(uri="mongodb://localhost:27017")
        code, out, factory = self.run_cli(*APPLY_ARGS)
        self.assertRefusedWithoutConnecting(code, out, factory, "NO_CLEAN_DRY_RUN")

    def test_F_PUBPROD_R009_un_ensayo_de_otra_lista_no_habilita(self):
        self.run_cli()
        manifest = fakes.example_manifest()
        manifest["documents"][0]["fields"][1]["after"] = "Otra miniteoría."
        fakes.write_batch(self.workdir, manifest)
        code, out, factory = self.run_cli(*APPLY_ARGS)
        self.assertRefusedWithoutConnecting(code, out, factory, "NO_CLEAN_DRY_RUN")

    def test_F_PUBPROD_R009_un_ensayo_con_problemas_no_habilita(self):
        self.store.data["quizTemplates"][fakes.QUIZ_2]["translation"] = "Ella es muy alta."
        code, out, _ = self.run_cli()
        self.assertEqual(code, 1, out)
        self.assertEqual(self.results(), [DRY_RUN_WITH_PROBLEMS])
        code, out, factory = self.run_cli(*APPLY_ARGS)
        self.assertRefusedWithoutConnecting(code, out, factory, "NO_CLEAN_DRY_RUN")

    def test_F_PUBPROD_R009_un_ensayo_con_otras_opciones_no_habilita(self):
        self.run_cli()
        code, out, factory = self.run_cli(*(APPLY_ARGS + ("--skip-drift",)))
        self.assertRefusedWithoutConnecting(code, out, factory, "NO_CLEAN_DRY_RUN")

    def test_F_PUBPROD_R009_despues_de_aplicar_hay_que_volver_a_ensayar(self):
        self.dry_run_then_apply()
        self.store.writes.clear()
        code, out, factory = self.run_cli(*APPLY_ARGS)
        self.assertRefusedWithoutConnecting(code, out, factory, "NO_CLEAN_DRY_RUN")

    def test_F_PUBPROD_R009_no_hay_forma_de_saltear_el_ensayo(self):
        options = [s for a in cli.build_parser()._actions for s in a.option_strings]
        for option in options:
            self.assertNotRegex(option, r"(?i)(dry|ensayo|force|skip-check|yes)")
        for attempt in ("--skip-dry-run", "--no-dry-run", "--force", "--dry"):
            with self.subTest(attempt=attempt):
                with mock.patch("sys.stderr", io.StringIO()):
                    with self.assertRaises(SystemExit) as ctx:
                        self.run_cli(*(APPLY_ARGS + (attempt,)))
                self.assertEqual(ctx.exception.code, 2)
        self.assertEqual(self.store.writes, [])


class RefusalsTest(CliTestCase):
    """Cada negativa: antes de conectarse, sin escribir, con su renglón REFUSED."""

    def assertRefused(self, reason_code, *extra):
        code, out, factory = self.run_cli(*extra)
        self.assertEqual(code, 2, out)
        self.assertEqual(factory.calls, [], "se conectó antes de negarse")
        self.assertEqual(self.store.writes, [])
        row = self.rows()[-1]
        self.assertEqual(row["result"], REFUSED)
        codes = [p["code"] for p in row["reason"]["problems"]] + [row["reason"]["code"]]
        self.assertIn(reason_code, codes)
        self.assertIn("NEGADO", out)
        return row, out

    def manifest_with(self, change):
        manifest = fakes.example_manifest()
        change(manifest)
        fakes.write_batch(self.workdir, manifest)

    def test_F_PUBPROD_R006_niega_sin_conectarse_una_tanda_no_publicada(self):
        fakes.write_batch(self.workdir, status="CLOSED")
        self.assertRefused("NOT_PUBLISHED")

    def test_F_PUBPROD_R006_niega_sin_conectarse_si_la_huella_no_coincide(self):
        fakes.write_batch(self.workdir, digest="f" * 64)
        self.assertRefused("DIGEST_MISMATCH")

    def test_F_PUBPROD_R005_niega_sin_conectarse_si_el_veredicto_no_es_pasa(self):
        fakes.write_batch(self.workdir, publication={"verdictKind": "UNDECIDABLE"})
        self.assertRefused("VERDICT_NOT_PASSES")

    def test_F_PUBPROD_R005_niega_sin_conectarse_si_la_compuerta_no_paso(self):
        fakes.write_batch(self.workdir, publication={"gatePassed": False})
        self.assertRefused("GATE_NOT_PASSED")

    def test_F_PUBPROD_R005_niega_sin_conectarse_con_pisos_sin_decidir(self):
        fakes.write_batch(self.workdir, publication={"undecidableFloors": {"count": 1, "analyzers": ["x"]}})
        self.assertRefused("UNDECIDABLE_FLOORS")

    def test_F_PUBPROD_R006_niega_sin_conectarse_un_ejercicio_rechazado(self):
        self.manifest_with(lambda m: m["documents"][0].update(reviewStatus="REJECTED"))
        self.assertRefused("REJECTED_DOCUMENT")

    def test_F_PUBPROD_R002_niega_sin_conectarse_una_ruta_prohibida(self):
        def change(m):
            m["documents"][0]["fields"].append({"path": "form.kind", "before": "FILL_GAPS", "after": "REWRITE"})
            m["counts"]["fields"] += 1
            m["counts"]["byPath"]["form.kind"] = 1
        self.manifest_with(change)
        self.assertRefused("FORBIDDEN_PATH")

    def test_F_PUBPROD_R002_niega_sin_conectarse_un_texto_nulo(self):
        self.manifest_with(lambda m: m["documents"][0]["fields"][0]["after"][2].update(text=None))
        self.assertRefused("NULL_TEXT")

    def test_F_PUBPROD_R006_niega_sin_conectarse_otra_coleccion(self):
        self.manifest_with(lambda m: m["documents"][3].update(collection="quizzes"))
        self.assertRefused("COLLECTION_NOT_ALLOWED")

    def test_F_PUBPROD_R006_niega_sin_conectarse_una_tanda_sin_lista(self):
        fakes.write_batch(self.workdir, publication={"manifestDigest": None}, write_manifest=False)
        row, out = self.assertRefused("NO_MANIFEST")
        self.assertIn("antes de este paso", out)

    def test_F_PUBPROD_R006_lo_sin_lugar_en_produccion_se_niega_salvo_pedido(self):
        unsupported = [{"changeId": "c-009", "nodeId": "A1.01.02.09", "part": "FORM_KIND", "reason": "tipo"}]
        self.manifest_with(lambda m: m.update(unsupported=unsupported))
        self.assertRefused("UNSUPPORTED")
        code, out, _ = self.run_cli("--skip-unsupported")
        self.assertEqual(code, 0, out)
        self.assertEqual(self.rows()[-1]["skippedUnsupported"], ["c-009"])
        code, out, _ = self.run_cli(*(APPLY_ARGS + ("--skip-unsupported",)))
        self.assertEqual(code, 0, out)
        self.assertEqual(self.rows()[-1]["result"], APPLIED_VERIFIED)
        self.assertEqual(self.rows()[-1]["skippedUnsupported"], ["c-009"])

    def test_F_PUBPROD_R008_un_uri_que_no_se_puede_leer_se_niega_sin_mostrarlo(self):
        fakes.write_batch(self.workdir)
        bad = "http://%s:%s@host/" % (fakes.FAKE_USER, fakes.FAKE_PASSWORD)
        row, out = self.assertRefused("BAD_URI", "--uri", bad)
        self.assertIsNone(row["destination"])
        self.assertNotIn(fakes.FAKE_PASSWORD, out + json.dumps(self.rows()))

    def test_F_PUBPROD_R008_si_otra_corrida_tiene_el_registro_se_niega(self):
        fakes.write_batch(self.workdir)
        self.run_cli()  # un ensayo limpio: lo único que frena la aplicación es el registro tomado
        with registry.Registry(self.workdir).lock():  # otra corrida, con el candado de verdad
            with self.assertRaises(registry.RegistryBusy):
                with registry.Registry(self.workdir).lock():
                    pass
            self.assertRefused("REGISTRY_BUSY", *APPLY_ARGS)
        # Soltado el candado, la misma aplicación sigue.
        code, out, _ = self.run_cli(*APPLY_ARGS)
        self.assertEqual(code, 0, out)
        self.assertEqual(self.results(), [DRY_RUN_CLEAN, REFUSED, STARTED, APPLIED_VERIFIED])


class DriftTest(CliTestCase):
    def setUp(self):
        super().setUp()
        fakes.write_batch(self.workdir)
        self.store.data["quizTemplates"][fakes.QUIZ_2]["translation"] = "Ella es muy alta."

    def test_F_PUBPROD_R006_con_deriva_no_se_escribe_nada(self):
        code, out, _ = self.run_cli()
        self.assertEqual(code, 1)
        self.assertIn("DERIVA", out)
        self.assertEqual(self.rows()[-1]["result"], DRY_RUN_WITH_PROBLEMS)
        detail = fakes.run_detail(self.workdir, self.rows()[-1])
        drift = next(d for d in detail["documents"] if d["id"] == fakes.QUIZ_2)
        self.assertEqual(drift["fields"][0]["current"], "Ella es muy alta.")
        code, out, _ = self.run_cli(*APPLY_ARGS)
        self.assertEqual(code, 2)
        self.assertEqual(self.store.writes, [])

    def test_F_PUBPROD_R006_con_skip_drift_sigue_sin_esos_documentos_y_los_registra(self):
        code, out, _ = self.dry_run_then_apply("--skip-drift")
        self.assertEqual(code, 0, out)
        self.assertEqual(self.results(), [DRY_RUN_CLEAN, STARTED, APPLIED_VERIFIED])
        row = self.rows()[-1]
        self.assertEqual(row["skippedDrift"], ["quizTemplates/" + fakes.QUIZ_2])
        self.assertNotIn("quizTemplates/" + fakes.QUIZ_2, row["writtenIds"])
        self.assertEqual(self.store.data["quizTemplates"][fakes.QUIZ_2],
                         self.original["quizTemplates"][fakes.QUIZ_2] | {"translation": "Ella es muy alta."})
        self.assertIn("knowledges/" + fakes.KNOWLEDGE, row["writtenIds"])

    def test_F_PUBPROD_R006_la_deriva_que_aparece_despues_del_ensayo_aborta_sin_escribir(self):
        self.store.data["quizTemplates"][fakes.QUIZ_2]["translation"] = "Ella es alta."
        self.run_cli()
        self.store.data["knowledges"][fakes.KNOWLEDGE]["instructions"] = "Cambiada a mano."
        code, out, _ = self.run_cli(*APPLY_ARGS)
        self.assertEqual(code, 1, out)
        self.assertEqual(self.results(), [DRY_RUN_CLEAN, ABORTED])
        self.assertEqual(self.store.attempts, [])

    def test_F_PUBPROD_R006_si_el_documento_cambia_entre_leer_y_escribir_deja_de_escribir(self):
        self.store.data["quizTemplates"][fakes.QUIZ_2]["translation"] = "Ella es alta."
        self.run_cli()
        self.store.lose_race_on.add("quizTemplates/" + fakes.QUIZ_1)
        code, out, _ = self.run_cli(*APPLY_ARGS)
        self.assertEqual(code, 1, out)
        row = self.rows()[-1]
        self.assertEqual(row["result"], ABORTED)
        self.assertEqual(row["stoppedReason"], "CONCURRENT_CHANGE")
        self.assertEqual(row["stoppedAt"], "quizTemplates/" + fakes.QUIZ_1)
        self.assertEqual(row["writtenIds"], ["knowledges/" + fakes.KNOWLEDGE])
        self.assertEqual(self.store.attempts.count("quizTemplates/" + fakes.QUIZ_1), 1)  # sin reintento


class MultipleChoiceTest(CliTestCase):
    """De opción múltiple solo viaja la etiqueta: lo demás frena antes de escribir (F-PUBPROD-R002)."""

    def write_manifest(self, change):
        manifest = fakes.example_manifest()
        change(manifest)
        fakes.write_batch(self.workdir, manifest)

    def test_F_PUBPROD_R002_un_ejercicio_de_opcion_multiple_con_mas_que_etiquetas_frena_el_ensayo(self):
        def change(m):
            quiz = next(d for d in m["documents"] if d["id"] == fakes.QUIZ_3)
            quiz["fields"].append({"path": "form.sentenceParts", "before": [{"kind": "TEXT", "text": "I ___."}],
                                   "after": [{"kind": "TEXT", "text": "I ___ happy."}]})
            m["counts"]["fields"] += 1
            m["counts"]["byPath"]["form.sentenceParts"] += 1
        self.write_manifest(change)
        for flags in ((), ("--skip-drift",)):
            with self.subTest(flags=flags):
                code, out, _ = self.run_cli(*flags)
                self.assertEqual(code, 1, out)
                row = self.rows()[-1]
                self.assertEqual(row["result"], DRY_RUN_WITH_PROBLEMS)
                self.assertEqual(row["reason"]["code"], "MULTIPLE_CHOICE_BEYOND_LABEL")
                self.assertIn("BLOQUEADO quizTemplates/%s" % fakes.QUIZ_3, out)
                self.assertIn("no se saltea con --skip-drift", out)
                code, out, factory = self.run_cli(*(APPLY_ARGS + flags))
                self.assertEqual(code, 2, out)
                self.assertEqual(factory.calls, [])
        self.assertEqual(self.store.attempts, [])

    def test_F_PUBPROD_R002_si_el_ejercicio_deja_de_ser_de_opcion_multiple_despues_del_ensayo_aborta(self):
        fakes.write_batch(self.workdir)
        code, out, _ = self.run_cli()
        self.assertEqual(code, 0, out)
        self.store.data["quizTemplates"][fakes.QUIZ_3]["form"]["kind"] = "FILL_GAPS"
        self.store.data["quizTemplates"][fakes.QUIZ_3]["kind"] = "FILL_GAPS"
        code, out, _ = self.run_cli(*APPLY_ARGS)
        self.assertEqual(code, 1, out)
        row = self.rows()[-1]
        self.assertEqual(row["result"], ABORTED)
        self.assertEqual(row["reason"]["code"], "ITEMS_WITHOUT_MULTIPLE_CHOICE")
        self.assertEqual(self.store.attempts, [])

    def test_F_PUBPROD_R002_un_cambio_de_seleccion_que_llega_como_sin_lugar_no_rompe_nada(self):
        # El lado Java manda a unsupported[] lo de opción múltiple que no es etiqueta (la
        # selección, la correcta, las opciones), con su parte y un motivo en texto.
        selection = {"changeId": "c-seleccion", "nodeId": "A1.01.02.05", "part": "OPTIONS",
                     "reason": "opción múltiple: sólo se publica el texto de cada opción; cambiar la forma "
                               "de elegir no se publica por script"}
        self.write_manifest(lambda m: m.update(unsupported=[selection]))
        code, out, _ = self.run_cli("--skip-unsupported")
        self.assertEqual(code, 0, out)
        code, out, _ = self.run_cli(*(APPLY_ARGS + ("--skip-unsupported",)))
        self.assertEqual(code, 0, out)
        row = self.rows()[-1]
        self.assertEqual(row["result"], APPLIED_VERIFIED)
        self.assertEqual(row["skippedUnsupported"], ["c-seleccion"])
        self.assertEqual(self.store.data["quizTemplates"][fakes.QUIZ_3]["form"]["selection"], "SINGLE")


class VerifyTest(CliTestCase):
    def setUp(self):
        super().setUp()
        fakes.write_batch(self.workdir)

    def test_F_PUBPROD_R007_si_cambio_algo_fuera_de_la_lista_falla_la_verificacion_sin_reintentar(self):
        def corrupt(doc):
            doc["form"]["kind"] = "OTRA_COSA"
        self.store.after_write["quizTemplates/" + fakes.QUIZ_1] = corrupt
        code, out, _ = self.dry_run_then_apply()
        self.assertEqual(code, 1, out)
        row = self.rows()[-1]
        self.assertEqual(row["result"], VERIFY_FAILED)
        self.assertIs(row["verified"], False)
        self.assertIn("NO VERIFICA quizTemplates/" + fakes.QUIZ_1, out)
        self.assertEqual(self.store.attempts.count("quizTemplates/" + fakes.QUIZ_1), 1)
        self.assertEqual(self.store.data["quizTemplates"][fakes.QUIZ_1]["form"]["kind"], "OTRA_COSA")
        detail = fakes.run_detail(self.workdir, row)
        failure = next(v for v in detail["verification"] if not v["ok"])
        self.assertNotEqual(failure["restFingerprintBefore"], failure["restFingerprintAfter"])

    def test_F_PUBPROD_R007_si_un_campo_no_quedo_con_su_despues_falla_la_verificacion(self):
        def overwrite(doc):
            doc["translation"] = "Otra cosa."
        self.store.after_write["quizTemplates/" + fakes.QUIZ_2] = overwrite
        code, out, _ = self.dry_run_then_apply()
        self.assertEqual(code, 1, out)
        self.assertEqual(self.rows()[-1]["result"], VERIFY_FAILED)
        self.assertIn("translation quedó con", out)

    def test_F_PUBPROD_R007_si_la_base_falla_al_escribir_aborta_y_verifica_lo_escrito(self):
        self.run_cli()
        self.store.fail_write_on["quizTemplates/" + fakes.QUIZ_2] = RuntimeError(
            "mongodb+srv://%s:%s@x fallo" % (fakes.FAKE_USER, fakes.FAKE_PASSWORD))
        code, out, _ = self.run_cli(*APPLY_ARGS)
        self.assertEqual(code, 1, out)
        row = self.rows()[-1]
        self.assertEqual(row["result"], ABORTED)
        self.assertEqual(row["errorClass"], "RuntimeError")
        self.assertIs(row["verified"], False)
        self.assertNotIn(fakes.FAKE_PASSWORD, out)
        self.assertEqual(row["indeterminate"], [])  # se releyó: sigue con lo de antes
        self.assertEqual(row["writtenIds"], ["knowledges/" + fakes.KNOWLEDGE, "quizTemplates/" + fakes.QUIZ_1])
        detail = fakes.run_detail(self.workdir, row)
        self.assertTrue(all(v["ok"] for v in detail["verification"]))


class InterruptedOrLostWriteTest(CliTestCase):
    """Un Ctrl-C o una falla de la base a mitad de camino no se llevan el resultado (F-PUBPROD-R007)."""

    ALL_WRITTEN = ["knowledges/" + fakes.KNOWLEDGE, "quizTemplates/" + fakes.QUIZ_1,
                   "quizTemplates/" + fakes.QUIZ_2, "quizTemplates/" + fakes.QUIZ_3,
                   "quizzes/" + fakes.INSTANCE_IN_SYNC]

    def setUp(self):
        super().setUp()
        fakes.write_batch(self.workdir)
        code, out, _ = self.run_cli()
        self.assertEqual(code, 0, out)

    def written_to(self, doc_id):
        return any(w[1] == doc_id for w in self.store.writes)

    def test_F_PUBPROD_R007_un_ctrl_c_al_verificar_deja_el_renglon_final_con_lo_escrito(self):
        reads = []

        def interrupt(store, collection, doc_id):
            if len(store.writes) == 5:
                reads.append(doc_id)
                if len(reads) == 2:
                    raise KeyboardInterrupt

        self.store.on_find = interrupt
        code, out, _ = self.run_cli(*APPLY_ARGS)
        self.assertEqual(code, 1, out)
        self.assertEqual(self.results(), [DRY_RUN_CLEAN, STARTED, VERIFY_FAILED])
        row = self.rows()[-1]
        self.assertEqual(row["writtenIds"], self.ALL_WRITTEN)
        self.assertEqual(row["stoppedReason"], "INTERRUPTED")
        self.assertIs(row["verified"], False)
        detail = fakes.run_detail(self.workdir, row)
        self.assertEqual(detail["result"], VERIFY_FAILED)
        self.assertEqual(len(detail["verification"]), 5)
        self.assertTrue(detail["verification"][0]["ok"])
        for entry in detail["verification"][1:]:
            self.assertEqual(entry["problems"], ["no se llegó a verificar: se interrumpió"])
        self.assertIn("NO VERIFICA quizTemplates/%s: no se llegó a verificar" % fakes.QUIZ_1, out)

    def test_F_PUBPROD_R007_un_ctrl_c_al_escribir_deja_el_renglon_final_con_lo_escrito(self):
        def interrupt(store, collection, doc_id):
            if doc_id == fakes.QUIZ_2:
                raise KeyboardInterrupt

        self.store.on_write = interrupt
        code, out, _ = self.run_cli(*APPLY_ARGS)
        self.assertEqual(code, 1, out)
        row = self.rows()[-1]
        self.assertEqual(row["result"], ABORTED)
        self.assertEqual(row["stoppedReason"], "INTERRUPTED")
        self.assertEqual(row["stoppedAt"], "quizTemplates/" + fakes.QUIZ_2)
        self.assertEqual(row["writtenIds"], self.ALL_WRITTEN[:2])
        self.assertEqual(row["indeterminate"], [])  # se releyó: no llegó a escribirse
        self.assertTrue(all(v["ok"] for v in fakes.run_detail(self.workdir, row)["verification"]))

    def test_F_PUBPROD_R007_si_la_red_se_cae_despues_del_set_lo_escrito_cuenta_y_se_verifica(self):
        self.store.raise_after_write_on["quizTemplates/" + fakes.QUIZ_2] = ConnectionError("se cortó")
        code, out, _ = self.run_cli(*APPLY_ARGS)
        self.assertEqual(code, 1, out)
        row = self.rows()[-1]
        self.assertEqual(row["result"], ABORTED)
        self.assertEqual(row["stoppedReason"], "STORE_ERROR")
        self.assertEqual(row["errorClass"], "ConnectionError")
        self.assertEqual(row["stoppedAt"], "quizTemplates/" + fakes.QUIZ_2)
        self.assertEqual(row["writtenIds"], self.ALL_WRITTEN[:3])
        self.assertEqual(row["indeterminate"], [])
        detail = fakes.run_detail(self.workdir, row)
        self.assertEqual([(v["id"], v["ok"]) for v in detail["verification"]],
                         [(fakes.KNOWLEDGE, True), (fakes.QUIZ_1, True), (fakes.QUIZ_2, True)])
        self.assertEqual(self.store.attempts.count("quizTemplates/" + fakes.QUIZ_2), 1)  # sin reintento

    def test_F_PUBPROD_R007_si_no_se_puede_saber_si_quedo_escrito_lo_dice_y_lo_registra(self):
        self.store.raise_after_write_on["quizTemplates/" + fakes.QUIZ_2] = ConnectionError("se cortó")

        def unreadable(store, collection, doc_id):
            if doc_id == fakes.QUIZ_2 and self.written_to(fakes.QUIZ_2):
                raise ConnectionError("sigue cortada")

        self.store.on_find = unreadable
        code, out, _ = self.run_cli(*APPLY_ARGS)
        self.assertEqual(code, 1, out)
        row = self.rows()[-1]
        self.assertEqual(row["result"], ABORTED)
        self.assertEqual(row["indeterminate"], ["quizTemplates/" + fakes.QUIZ_2])
        self.assertEqual(row["writtenIds"], self.ALL_WRITTEN[:2])
        self.assertIn("NO SE SABE si quedó escrito quizTemplates/%s" % fakes.QUIZ_2, out)
        self.assertIn("ConnectionError", out)
        detail = fakes.run_detail(self.workdir, row)
        self.assertEqual(detail["indeterminate"][0]["problem"], "no se pudo releer (ConnectionError)")


class RegistryFailureTest(CliTestCase):
    def setUp(self):
        super().setUp()
        fakes.write_batch(self.workdir)

    def test_F_PUBPROD_R008_si_no_se_puede_dejar_el_renglon_de_inicio_no_se_escribe(self):
        self.run_cli()
        real_append = registry.Registry.append

        def append(reg, row):
            if row["result"] == STARTED:
                raise RegistryError("disco lleno (OSError)")
            return real_append(reg, row)

        with mock.patch.object(registry.Registry, "append", append):
            code, out, _ = self.run_cli(*APPLY_ARGS)
        self.assertEqual(code, 1, out)
        self.assertEqual(self.store.attempts, [])
        self.assertEqual(self.store.data, self.original)
        self.assertIn("no se escribió nada", out)

    def test_F_PUBPROD_R008_si_no_se_puede_escribir_el_respaldo_no_se_escribe(self):
        self.run_cli()
        with mock.patch.object(registry.Registry, "write_run", side_effect=RegistryError("sin lugar")):
            code, out, _ = self.run_cli(*APPLY_ARGS)
        self.assertEqual(code, 1, out)
        self.assertEqual(self.store.attempts, [])
        self.assertEqual(self.rows()[-1]["result"], ABORTED)

    def test_F_PUBPROD_R008_un_registro_ilegible_niega_la_aplicacion(self):
        self.run_cli()
        with open(os.path.join(self.workdir, ".content-audit", "production", "registry.jsonl"), "a") as fh:
            fh.write("esto no es json\n")
        code, out, factory = self.run_cli(*APPLY_ARGS)
        self.assertEqual(code, 2, out)
        self.assertEqual(factory.calls, [])


class ConnectionTest(CliTestCase):
    def setUp(self):
        super().setUp()
        fakes.write_batch(self.workdir)

    def test_F_PUBPROD_R008_si_no_se_puede_conectar_aborta_mostrando_solo_la_clase(self):
        class ServerSelectionTimeoutError(Exception):
            pass

        error = ServerSelectionTimeoutError("%s:%s@%s timeout" % (fakes.FAKE_USER, fakes.FAKE_PASSWORD, fakes.HOST))
        code, out, _ = self.run_cli(factory=self.factory(fail_open=error))
        self.assertEqual(code, 1, out)
        row = self.rows()[-1]
        self.assertEqual(row["result"], ABORTED)
        self.assertEqual(row["errorClass"], "ServerSelectionTimeoutError")
        self.assertIn("ServerSelectionTimeoutError", out)
        self.assertNotIn("timeout", out)
        self.assertNotIn(fakes.FAKE_PASSWORD, out + json.dumps(self.rows()))

    def test_F_PUBPROD_R008_si_la_base_no_tiene_las_colecciones_aborta(self):
        del self.store.data["knowledges"]
        code, out, _ = self.run_cli()
        self.assertEqual(code, 1, out)
        self.assertEqual(self.rows()[-1]["result"], ABORTED)
        self.assertEqual(self.rows()[-1]["reason"]["code"], "WRONG_DATABASE")

    def test_F_PUBPROD_R008_cierra_la_conexion_al_terminar(self):
        self.run_cli()
        self.assertTrue(self.store.closed)


class CredentialsTest(CliTestCase):
    def test_F_PUBPROD_R008_el_destino_nunca_lleva_usuario_clave_ni_el_uri(self):
        fakes.write_batch(self.workdir)
        self.store.after_write["knowledges/" + fakes.KNOWLEDGE] = lambda d: d.update(order=4)
        outputs = []
        for extra in ((), APPLY_ARGS, ("--expect-destination", "otro/learney"), (), APPLY_ARGS):
            code, out, _ = self.run_cli(*extra)
            outputs.append(out)
        everything = "\n".join(outputs) + fakes.all_text_under(self.workdir)
        for secret in (fakes.FAKE_USER, fakes.FAKE_PASSWORD, fakes.URI, "authSource", "retryWrites"):
            self.assertNotIn(secret, everything)
        for row in self.rows():
            self.assertIn(row["destination"], ({"host": fakes.HOST, "db": fakes.DB},))
        self.assertIn(VERIFY_FAILED, self.results())

    def test_F_PUBPROD_R008_los_errores_de_argumentos_tapan_el_uri(self):
        err = io.StringIO()
        with mock.patch("sys.stderr", err):
            with self.assertRaises(SystemExit):
                cli.main(["--batch", "x", "--uri", fakes.URI, "--sobra", fakes.URI], store_factory=self.factory())
        self.assertNotIn(fakes.FAKE_PASSWORD, err.getvalue())
        self.assertIn("<URI oculto>", err.getvalue())


if __name__ == "__main__":
    unittest.main()
