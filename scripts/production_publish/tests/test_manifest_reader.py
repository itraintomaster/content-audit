"""Lo que se niega antes de conectarse: la tanda, la huella, el veredicto y la lista."""

import copy
import os
import unittest

from production_publish import manifest_reader
from production_publish.manifest_reader import Refusal, load, validate_manifest
from production_publish.tests import fakes


def doc(manifest, doc_id):
    return next(d for d in manifest["documents"] if d["id"] == doc_id)


def recount(manifest):
    """Rehace las cuentas para que una prueba falle solo por lo que quiere probar."""
    by_path = {}
    total = 0
    for d in manifest["documents"]:
        for f in d.get("fields", []):
            if isinstance(f, dict) and isinstance(f.get("path"), str):
                by_path[f["path"]] = by_path.get(f["path"], 0) + 1
                total += 1
    manifest["counts"] = {"documents": len(manifest["documents"]), "fields": total, "byPath": by_path}
    return manifest


class LoadTest(unittest.TestCase):
    def setUp(self):
        self.workdir = fakes.temp_workdir(self)

    def assertRefused(self, code, fn, *args, **kwargs):
        with self.assertRaises(Refusal) as ctx:
            fn(*args, **kwargs)
        codes = [c for c, _ in ctx.exception.problems]
        self.assertIn(code, codes, "esperaba %s y vino %s: %s" % (code, codes, ctx.exception.message))
        return ctx.exception

    def test_F_PUBPROD_R006_lee_la_tanda_publicada_y_su_lista_de_ejemplo(self):
        digest = fakes.write_batch(self.workdir)
        loaded = load(self.workdir, fakes.BATCH_ID)
        self.assertEqual(loaded.digest, digest)
        self.assertEqual(loaded.plan_id, fakes.PLAN_ID)
        self.assertEqual(len(loaded.manifest["documents"]), 4)

    def test_F_PUBPROD_R006_niega_una_tanda_que_no_esta_publicada(self):
        fakes.write_batch(self.workdir, status="CLOSED")
        self.assertRefused("NOT_PUBLISHED", load, self.workdir, fakes.BATCH_ID)

    def test_F_PUBPROD_R006_niega_una_tanda_que_no_existe(self):
        self.assertRefused("BATCH_NOT_FOUND", load, self.workdir, "otra-tanda")

    def test_F_PUBPROD_R006_niega_una_identidad_de_tanda_que_sale_de_la_carpeta(self):
        for bad in ("../tanda", "tanda/../../x", "", ".oculta", "con espacio"):
            with self.subTest(bad=bad):
                self.assertRefused("BAD_BATCH_ID", load, self.workdir, bad)

    def test_F_PUBPROD_R006_niega_si_la_huella_no_coincide(self):
        fakes.write_batch(self.workdir, digest="0" * 64)
        self.assertRefused("DIGEST_MISMATCH", load, self.workdir, fakes.BATCH_ID)

    def test_F_PUBPROD_R006_niega_si_la_lista_cambio_despues_de_publicar(self):
        fakes.write_batch(self.workdir)
        path = manifest_reader.manifest_file_path(self.workdir, fakes.BATCH_ID)
        with open(path, "ab") as fh:
            fh.write(b" ")
        self.assertRefused("DIGEST_MISMATCH", load, self.workdir, fakes.BATCH_ID)

    def test_F_PUBPROD_R006_niega_una_tanda_publicada_antes_de_que_hubiera_lista(self):
        fakes.write_batch(self.workdir, publication={"manifestDigest": None}, write_manifest=False)
        refusal = self.assertRefused("NO_MANIFEST", load, self.workdir, fakes.BATCH_ID)
        self.assertIn("sin lista de publicación", refusal.message)

    def test_F_PUBPROD_R006_niega_si_falta_el_archivo_de_la_lista(self):
        fakes.write_batch(self.workdir, write_manifest=False)
        self.assertRefused("MANIFEST_MISSING", load, self.workdir, fakes.BATCH_ID)

    def test_F_PUBPROD_R005_niega_si_el_veredicto_registrado_no_es_pasa(self):
        for kind in ("FAILS", "UNDECIDABLE", None):
            with self.subTest(kind=kind):
                fakes.write_batch(self.workdir, publication={"verdictKind": kind})
                self.assertRefused("VERDICT_NOT_PASSES", load, self.workdir, fakes.BATCH_ID)

    def test_F_PUBPROD_R005_niega_si_la_compuerta_no_paso(self):
        for gate in (False, None, "true", 1):
            with self.subTest(gate=gate):
                fakes.write_batch(self.workdir, publication={"gatePassed": gate})
                self.assertRefused("GATE_NOT_PASSED", load, self.workdir, fakes.BATCH_ID)

    def test_F_PUBPROD_R005_niega_si_la_publicacion_registra_pisos_sin_decidir(self):
        for floors in ({"count": 2, "analyzers": ["quiz-instruction"]}, ["piso-1"], 3, True):
            with self.subTest(floors=floors):
                fakes.write_batch(self.workdir, publication={"undecidableFloors": floors})
                self.assertRefused("UNDECIDABLE_FLOORS", load, self.workdir, fakes.BATCH_ID)

    def test_F_PUBPROD_R005_niega_si_la_publicacion_no_dice_los_pisos_sin_decidir(self):
        fakes.write_batch(self.workdir)
        path = manifest_reader.batch_record_path(self.workdir, fakes.BATCH_ID)
        import json
        with open(path, encoding="utf-8") as fh:
            record = json.load(fh)
        del record["publication"]["undecidableFloors"]
        with open(path, "w", encoding="utf-8") as fh:
            json.dump(record, fh)
        self.assertRefused("UNDECIDABLE_FLOORS", load, self.workdir, fakes.BATCH_ID)

    def test_F_PUBPROD_R005_acepta_la_publicacion_con_todos_los_pisos_decididos(self):
        for floors in (None, [], 0, {"count": 0, "analyzers": []}):
            with self.subTest(floors=floors):
                fakes.write_batch(self.workdir, publication={"undecidableFloors": floors})
                load(self.workdir, fakes.BATCH_ID)

    def test_F_PUBPROD_R006_niega_una_lista_con_claves_repetidas(self):
        raw = fakes.manifest_bytes(fakes.example_manifest()).replace(
            b'"schema": 1,', b'"schema": 1, "schema": 1,', 1)
        fakes.write_batch(self.workdir, raw_manifest=raw)
        self.assertRefused("UNREADABLE", load, self.workdir, fakes.BATCH_ID)

    def test_F_PUBPROD_R006_niega_una_lista_de_otra_tanda_o_de_otro_plan(self):
        manifest = fakes.example_manifest()
        manifest["batchId"] = "otra"
        fakes.write_batch(self.workdir, manifest)
        self.assertRefused("BATCH_ID_MISMATCH", load, self.workdir, fakes.BATCH_ID)
        manifest = fakes.example_manifest()
        fakes.write_batch(self.workdir, manifest, plan_id="otro-plan")
        self.assertRefused("PLAN_ID_MISMATCH", load, self.workdir, fakes.BATCH_ID)


class ValidateManifestTest(unittest.TestCase):
    def setUp(self):
        self.manifest = fakes.example_manifest()

    def assertRefused(self, code, manifest):
        with self.assertRaises(Refusal) as ctx:
            validate_manifest(manifest, fakes.BATCH_ID, fakes.PLAN_ID)
        codes = [c for c, _ in ctx.exception.problems]
        self.assertIn(code, codes, "esperaba %s y vino %s" % (code, codes))
        return codes

    def test_F_PUBPROD_R001_la_lista_de_ejemplo_respeta_el_esquema_1(self):
        validate_manifest(self.manifest, fakes.BATCH_ID, fakes.PLAN_ID)

    def test_F_PUBPROD_R006_niega_un_esquema_desconocido(self):
        for schema in (2, "1", True, None):
            with self.subTest(schema=schema):
                m = copy.deepcopy(self.manifest)
                m["schema"] = schema
                self.assertRefused("UNKNOWN_SCHEMA", m)

    def test_F_PUBPROD_R005_niega_una_lista_cuyo_veredicto_no_es_pasa(self):
        self.manifest["verdict"]["kind"] = "UNDECIDABLE"
        self.assertRefused("VERDICT_NOT_PASSES", self.manifest)

    def test_F_PUBPROD_R005_niega_una_lista_sin_compuerta_pasada(self):
        self.manifest["verdict"]["gatePassed"] = False
        self.assertRefused("GATE_NOT_PASSED", self.manifest)

    def test_F_PUBPROD_R005_niega_una_lista_con_pisos_sin_decidir_o_sin_decirlos(self):
        for floors in (["quiz-instruction"], None, {"count": 1, "analyzers": ["x"]}):
            with self.subTest(floors=floors):
                m = copy.deepcopy(self.manifest)
                m["verdict"]["undecidableFloors"] = floors
                self.assertRefused("UNDECIDABLE_FLOORS", m)
        m = copy.deepcopy(self.manifest)
        del m["verdict"]["undecidableFloors"]
        self.assertRefused("UNDECIDABLE_FLOORS", m)

    def test_F_PUBPROD_R005_niega_una_lista_con_pisos_rotos(self):
        self.manifest["verdict"]["brokenFloorGoalIds"] = ["meta-1"]
        self.assertRefused("BROKEN_FLOORS", self.manifest)

    def test_F_PUBPROD_R006_niega_un_ejercicio_rechazado(self):
        doc(self.manifest, fakes.QUIZ_1)["reviewStatus"] = "REJECTED"
        self.assertRefused("REJECTED_DOCUMENT", self.manifest)

    def test_F_PUBPROD_R003_niega_un_documento_originado_por_un_cambio_que_quedo_afuera(self):
        for left_out in ("c-rechazado", "c-encadenado"):
            with self.subTest(left_out=left_out):
                m = copy.deepcopy(self.manifest)
                doc(m, fakes.QUIZ_2)["changeIds"].append(left_out)
                self.assertRefused("EXCLUDED_CHANGE_TRAVELS", m)

    def test_F_PUBPROD_R004_cada_ejercicio_dice_si_quedo_algo_sin_revisar(self):
        for status in (None, "REVIEWED", "approved"):
            with self.subTest(status=status):
                m = copy.deepcopy(self.manifest)
                doc(m, fakes.QUIZ_1)["reviewStatus"] = status
                self.assertRefused("BAD_REVIEW_STATUS", m)

    def test_F_PUBPROD_R004_la_lista_trae_las_cuentas_de_la_revision(self):
        for key in ("approved", "pending", "rejected"):
            for value in (None, -1, "3", True):
                with self.subTest(key=key, value=value):
                    m = copy.deepcopy(self.manifest)
                    m["reviews"][key] = value
                    self.assertRefused("BAD_REVIEWS", m)

    def test_F_PUBPROD_R002_niega_las_rutas_prohibidas(self):
        for path in manifest_reader.FORBIDDEN_PATHS + ("form.kind.x", "_id.oid"):
            with self.subTest(path=path):
                m = copy.deepcopy(self.manifest)
                doc(m, fakes.QUIZ_1)["fields"].append({"path": path, "before": "a", "after": "b"})
                self.assertRefused("FORBIDDEN_PATH", recount(m))

    def test_F_PUBPROD_R002_niega_las_rutas_que_no_estan_en_la_tabla(self):
        cases = ((fakes.QUIZ_1, "title"), (fakes.QUIZ_1, "audio"), (fakes.QUIZ_1, "form"),
                 (fakes.KNOWLEDGE, "label"), (fakes.KNOWLEDGE, "miniTheory"))
        for doc_id, path in cases:
            with self.subTest(path=path):
                m = copy.deepcopy(self.manifest)
                doc(m, doc_id)["fields"].append({"path": path, "before": "a", "after": "b"})
                self.assertRefused("PATH_NOT_ALLOWED", recount(m))

    def test_F_PUBPROD_R002_niega_un_texto_nulo_en_el_antes_o_en_el_despues(self):
        for side in ("before", "after"):
            with self.subTest(side=side):
                m = copy.deepcopy(self.manifest)
                doc(m, fakes.QUIZ_1)["fields"][0][side][0]["text"] = None
                self.assertRefused("NULL_TEXT", m)

    def test_F_PUBPROD_R002_niega_un_despues_nulo(self):
        doc(self.manifest, fakes.QUIZ_2)["fields"][0]["after"] = None
        self.assertRefused("NULL_AFTER", self.manifest)

    def test_F_PUBPROD_R002_niega_json_extendido_sin_convertir(self):
        items = doc(self.manifest, fakes.QUIZ_3)["fields"][0]["after"]
        items[0]["incidence"] = {"$numberDouble": "1.0"}
        self.assertRefused("EXTENDED_JSON", self.manifest)

    def test_F_PUBPROD_R002_niega_un_valor_del_tipo_equivocado(self):
        cases = (("translation", 3), ("form.sentences", "She is tall."), ("form.sentences", [1]))
        for path, after in cases:
            with self.subTest(path=path, after=after):
                m = copy.deepcopy(self.manifest)
                field = next(f for f in doc(m, fakes.QUIZ_2)["fields"] if f["path"] == path)
                field["after"] = after
                self.assertRefused("BAD_VALUE", m)

    def test_F_PUBPROD_R002_en_opcion_multiple_solo_viaja_la_etiqueta(self):
        m = copy.deepcopy(self.manifest)
        doc(m, fakes.QUIZ_3)["fields"][0]["after"][1]["incidence"] = 1.0
        self.assertRefused("MULTIPLE_CHOICE_BEYOND_LABEL", m)
        m = copy.deepcopy(self.manifest)
        doc(m, fakes.QUIZ_3)["fields"][0]["after"][1]["id"] = "is."
        self.assertRefused("MULTIPLE_CHOICE_BEYOND_LABEL", m)
        m = copy.deepcopy(self.manifest)
        doc(m, fakes.QUIZ_3)["fields"][0]["after"].append({"id": "are", "label": "are", "incidence": 0.0})
        self.assertRefused("MULTIPLE_CHOICE_BEYOND_LABEL", m)
        m = copy.deepcopy(self.manifest)
        doc(m, fakes.QUIZ_3)["fields"].append({"path": "form.selection", "before": "SINGLE", "after": "MULTI"})
        self.assertRefused("MULTIPLE_CHOICE_BEYOND_LABEL", recount(m))

    def test_F_PUBPROD_R006_niega_colecciones_que_no_son_quizTemplates_ni_knowledges(self):
        for collection in ("quizzes", "users", "QuizTemplates", None):
            with self.subTest(collection=collection):
                m = copy.deepcopy(self.manifest)
                doc(m, fakes.KNOWLEDGE)["collection"] = collection
                self.assertRefused("COLLECTION_NOT_ALLOWED", m)

    def test_F_PUBPROD_R006_niega_ids_que_no_son_de_produccion_y_documentos_repetidos(self):
        m = copy.deepcopy(self.manifest)
        doc(m, fakes.KNOWLEDGE)["id"] = "A1.01.02"
        self.assertRefused("BAD_ID", m)
        m = copy.deepcopy(self.manifest)
        m["documents"].append(copy.deepcopy(doc(m, fakes.KNOWLEDGE)))
        self.assertRefused("DUPLICATE_DOCUMENT", recount(m))

    def test_F_PUBPROD_R001_lo_que_no_cambio_no_esta_en_la_lista(self):
        field = doc(self.manifest, fakes.QUIZ_2)["fields"][0]
        field["after"] = field["before"]
        self.assertRefused("UNCHANGED_FIELD", self.manifest)

    def test_F_PUBPROD_R001_las_cuentas_de_la_lista_coinciden_con_sus_campos(self):
        m = copy.deepcopy(self.manifest)
        m["counts"]["fields"] = 99
        self.assertRefused("BAD_COUNTS", m)
        m = copy.deepcopy(self.manifest)
        m["counts"]["documents"] = 1
        self.assertRefused("BAD_COUNTS", m)
        m = copy.deepcopy(self.manifest)
        m["counts"]["byPath"]["miniTheory"] = 5
        self.assertRefused("BAD_COUNTS", m)

    def test_F_PUBPROD_R006_junta_todos_los_problemas_en_una_sola_negativa(self):
        doc(self.manifest, fakes.QUIZ_1)["reviewStatus"] = "REJECTED"
        doc(self.manifest, fakes.KNOWLEDGE)["collection"] = "users"
        codes = self.assertRefused("REJECTED_DOCUMENT", self.manifest)
        self.assertIn("COLLECTION_NOT_ALLOWED", codes)


class FindBatchesForPlanTest(unittest.TestCase):
    def test_F_PUBPROD_R006_encuentra_las_tandas_de_un_plan_en_el_repo_y_en_las_campanas(self):
        repo = fakes.temp_workdir(self)
        fakes.write_batch(repo, batch_id="tanda-raiz", plan_id="plan-x")
        campaign = os.path.join(repo, ".content-audit", "campaigns", "a1-v11")
        fakes.write_batch(campaign, batch_id="tanda-campana", plan_id="plan-x")
        fakes.write_batch(campaign, batch_id="tanda-otra", plan_id="plan-y")
        matches, unreadable = manifest_reader.find_batches_for_plan([repo], "plan-x")
        self.assertEqual(sorted(i for _, i in matches), ["tanda-campana", "tanda-raiz"])
        self.assertEqual(unreadable, [])


if __name__ == "__main__":
    unittest.main()
