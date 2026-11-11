"""La regrabación sale de la preparación de audios y anota lo grabado en el registro (F-DATOS-R007)."""

import json
import os
import shutil
import tempfile
import unittest

import regenerate_quiz_audios as regen


def preparacion(*entradas):
    """Lo que devuelve `content-audit batch audio --request-json '{"action":"read"}'`."""
    return {"batchId": "tanda-A1-c2", "contentVersion": 115, "stage": "PREPUBLICATION",
            "total": len(entradas), "entries": list(entradas)}


def entrada(quiz_id, clave, estado, *textos):
    return {"quizId": quiz_id, "audioKey": clave, "finalTexts": list(textos), "status": estado}


def ejercicio(clave, oracion, modo="FILL"):
    return {"key": clave, "mode": modo, "sentences": [oracion], "parts": [], "title": None}


def publicacion(documentos=(), sin_lugar=(), tanda="tanda-A1-c2"):
    """La lista que dejó `tray publish` (batch-publications/<tanda>/manifest.json)."""
    return {"batchId": tanda, "documents": list(documentos), "unsupported": list(sin_lugar)}


def documento(quiz_id, antes, despues):
    return {"collection": "quizTemplates", "id": quiz_id, "nodeId": quiz_id,
            "fields": [{"path": "form.sentences", "before": antes, "after": despues}]}


class DesdeLaPreparacionTest(unittest.TestCase):
    def setUp(self):
        self.workdir = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, self.workdir)

    def test_F_DATOS_R007_toma_solo_lo_que_falta_grabar_con_el_primer_texto_de_hoy(self):
        regen.desde_preparacion(preparacion(
            entrada("q1", "A1.04.13.09", "PENDING_GENERATION", "I heard a funny noise.", "I heard a noise."),
            entrada("q2", "A1.01.12.03", "TEXT_RECORD_MATCHES", "You aren't at the school."),
            entrada("q3", "A1.01.01.01", "TEXT_RECORD_PROBLEM", "She is English."),
            entrada("q4", "", "PENDING_GENERATION", "No key."),
        ), publicacion(), self.workdir)

        with open(os.path.join(self.workdir, "changed_spoken_sentences.json")) as f:
            self.assertEqual([{"id": "q1", "key": "A1.04.13.09"}], json.load(f))
        with open(os.path.join(self.workdir, "texto_final.json")) as f:
            self.assertEqual({"A1.04.13.09": "I heard a funny noise."}, json.load(f))
        self.assertEqual(["q1"], regen.load_changed_ids(self.workdir))

    def test_F_DATOS_R007_una_preparacion_sin_entradas_no_es_una_lista_vacia_para_grabar(self):
        with self.assertRaises(SystemExit):
            regen.desde_preparacion({"batchId": "tanda-A1-c2"}, publicacion(), self.workdir)
        self.assertFalse(os.path.exists(os.path.join(self.workdir, "changed_spoken_sentences.json")))

    def test_F_DATOS_R007_no_graba_lo_que_la_publicacion_no_llevo_a_produccion(self):
        # inv. 5: la C2 de A1 daba 458 por grabar; 233 eran de opción múltiple con un cambio que publicar no lleva
        # («water / oranges uncountable»). Grabarlos pondría en producción un audio de un ejercicio que no está ahí.
        regen.desde_preparacion(preparacion(
            entrada("q1", "A1.02.01.04", "PENDING_GENERATION", "You'll play with us."),
            entrada("q2", "A1.06.03.06", "PENDING_GENERATION", "water / oranges uncountable", "uncountable"),
            entrada("q3", "A1.01.01.14", "PENDING_GENERATION", "My camera is on the train."),
        ), publicacion(
            documentos=[documento("q1", ["Will you play with us?"], ["You'll play with us."])],
            sin_lugar=[{"changeId": "regla-multiple-choice-validity-q2", "nodeId": "q2", "part": "STATEMENT",
                        "reason": "opción múltiple"}],
        ), self.workdir)

        with open(os.path.join(self.workdir, "changed_spoken_sentences.json")) as f:
            self.assertEqual([{"id": "q1", "key": "A1.02.01.04", "oracion": "You'll play with us."},
                              {"id": "q3", "key": "A1.01.01.14"}], json.load(f))
        with open(os.path.join(self.workdir, "texto_final.json")) as f:
            self.assertNotIn("A1.06.03.06", json.load(f))
        with open(os.path.join(self.workdir, "sin_publicar.json")) as f:
            self.assertEqual([{"id": "q2", "key": "A1.06.03.06", "partes": ["STATEMENT"]}], json.load(f))

    def test_F_DATOS_R007_la_lista_de_otra_tanda_no_sirve(self):
        with self.assertRaises(SystemExit):
            regen.desde_preparacion(preparacion(entrada("q1", "A1.04.13.09", "PENDING_GENERATION", "Hi.")),
                                    publicacion(tanda="tanda-A1-c1"), self.workdir)
        self.assertFalse(os.path.exists(os.path.join(self.workdir, "changed_spoken_sentences.json")))


class AntesDeGrabarTest(unittest.TestCase):
    def test_F_DATOS_R007_produccion_tiene_que_decir_lo_que_dejo_la_publicacion(self):
        # inv. 5: comparar con la copia de antes de publicar dejaba afuera, después de publicar, los 224 que sí
        # cambiaron. Producción tiene que tener la oración que dejó la publicación o, si no la tocó, la de antes.
        head = {"q1": ejercicio("A1.02.01.04", "Will you play with us?"),
                "q3": ejercicio("A1.01.01.14", "My camera is on the train."),
                "q4": ejercicio("A1.01.01.15", "Your bag is on the bus.")}
        cambiadas = [{"id": "q1", "key": "A1.02.01.04", "oracion": "You'll play with us."},
                     {"id": "q3", "key": "A1.01.01.14"},
                     {"id": "q4", "key": "A1.01.01.15", "oracion": "Your bag isn't on the bus."},
                     {"id": "q5", "key": "A1.01.01.16"}]
        prod = {"q1": "You'll play with us.", "q3": "My camera is on the train.", "q4": "Your bag is on the bus."}

        reporte = regen.comparar_con_produccion(cambiadas, head, prod)

        self.assertEqual(["q1", "q3"], reporte["ok"])
        self.assertEqual([{"id": "q4", "key": "A1.01.01.15", "prod": "Your bag is on the bus.",
                           "esperada": "Your bag isn't on the bus."}], reporte["divergent"])
        self.assertEqual(["q5"], reporte["missing"])


class ManifiestoTest(unittest.TestCase):
    def test_F_DATOS_R007_graba_el_texto_de_hoy_y_manda_a_revision_lo_que_no_se_puede_grabar_asi(self):
        head = {
            "q1": ejercicio("A1.04.13.09", "I heard a funny noise"),
            "q2": ejercicio("A1.06.03.06", "water / oranges uncountable"),
            "q3": ejercicio("A1.09.04.05", "She wrote from Monday to."),
            "q4": ejercicio("A1.02.06.05", "Where are we going to?"),
            "q5": ejercicio("A1.01.01.20", "We are not ready."),
            "q6": ejercicio("A1.02.01.04", "like liking"),
        }
        finales = {
            "A1.04.13.09": "I heard a funny noise.",
            "A1.06.03.06": "water / oranges uncountable",
            "A1.09.04.05": "She began writing on Monday and finished on Saturday. She wrote from Monday to.",
            "A1.02.06.05": "Where are we going to?",
            "A1.02.01.04": "like liking",
        }
        overrides = {"A1.02.01.04": "liking"}

        manifest, manual, lint = regen.armar_manifiesto(head, ["q1", "q2", "q3", "q4", "q5", "q6"], overrides,
                                                        finales)

        por_clave = {r["key"]: r for r in manifest}
        self.assertEqual("I heard a funny noise.", por_clave["A1.04.13.09"]["text"])
        self.assertEqual("contenido", por_clave["A1.04.13.09"]["source"])
        self.assertEqual("Where are we going to?", por_clave["A1.02.06.05"]["text"], "una pregunta puede terminar así")
        self.assertEqual("liking", por_clave["A1.02.01.04"]["text"], "lo que José escribe a mano gana")
        self.assertEqual("override", por_clave["A1.02.01.04"]["source"])
        self.assertEqual("We are not ready.", por_clave["A1.01.01.20"]["text"], "sin texto de hoy, como siempre")
        a_mano = {r["key"]: r["issue"] for r in manual}
        self.assertIn("A1.06.03.06", a_mano)
        self.assertIn("«/»", a_mano["A1.06.03.06"])
        self.assertIn("A1.09.04.05", a_mano)
        self.assertIn("«to»", a_mano["A1.09.04.05"])
        self.assertNotIn("A1.06.03.06", por_clave)
        self.assertNotIn("A1.09.04.05", por_clave)
        self.assertEqual([], lint)


class RegistroTest(unittest.TestCase):
    def setUp(self):
        self.workdir = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, self.workdir)
        self.registro = os.path.join(self.workdir, "grabaciones.json")
        with open(self.registro, "w") as f:
            json.dump({"version": "2026-09-30 · regrabación del 24/8 (2958) + generador de marzo de 2025 (8529)",
                       "descripcion": "Por cada clave de audio, el texto con que se grabó por última vez.",
                       "grabaciones": {
                           "A1.04.13.09": {"texto": "I heard a funny noise", "fuente": "generador-2025-03",
                                           "fecha": "2025-03-15"},
                           "A1.01.01.02": {"texto": "They are very short.", "fuente": "generador-2025-03",
                                           "fecha": "2025-03-15"},
                           "A1.06.03.06": {"texto": "Water.", "fuente": "regrabacion-2026-08-24",
                                           "fecha": "2026-08-24"},
                       }}, f, ensure_ascii=False, indent=1)
        manifest = [{"id": "q1", "key": "A1.04.13.09", "text": "I heard a funny noise."},
                    {"id": "q2", "key": "A1.06.03.06", "text": "Water is uncountable."},
                    {"id": "q3", "key": "A1.09.04.05", "text": "She wrote from Monday to Saturday."}]
        with open(os.path.join(self.workdir, "regen_manifest.json"), "w") as f:
            json.dump(manifest, f)
        mp3 = os.path.join(self.workdir, "regen", "mp3")
        os.makedirs(mp3)
        for clave, tam in (("A1.04.13.09", 100), ("A1.06.03.06", 200), ("A1.09.04.05", 300)):
            with open(os.path.join(mp3, clave + ".mp3"), "wb") as f:
                f.write(b"x" * tam)

    def test_F_DATOS_R007_anota_solo_lo_subido_hoy_con_su_tamano_y_deja_igual_el_resto(self):
        listing = {"A1.04.13.09.mp3": {"date": "2026-11-12", "size": 100},
                   "A1.06.03.06.mp3": {"date": "2026-11-12", "size": 999},
                   "A1.09.04.05.mp3": {"date": "2026-08-24", "size": 300}}

        anotadas, sin_verificar = regen.registrar_en(self.workdir, self.registro, listing, "2026-11-12")

        self.assertEqual(["A1.04.13.09"], anotadas)
        self.assertEqual(["A1.06.03.06", "A1.09.04.05"], sin_verificar)
        with open(self.registro) as f:
            registro = json.load(f)
        g = registro["grabaciones"]
        self.assertEqual({"texto": "I heard a funny noise.", "fuente": "regrabacion-2026-11-12",
                          "fecha": "2026-11-12"}, g["A1.04.13.09"])
        self.assertEqual("Water.", g["A1.06.03.06"]["texto"], "lo que no se verificó no se anota")
        self.assertNotIn("A1.09.04.05", g)
        self.assertEqual("They are very short.", g["A1.01.01.02"]["texto"], "el resto no cambia")
        self.assertEqual("2026-11-12 · generador-2025-03 (1), regrabacion-2026-08-24 (1), regrabacion-2026-11-12 (1)",
                         registro["version"])
        self.assertEqual("Por cada clave de audio, el texto con que se grabó por última vez.", registro["descripcion"])

    def test_F_DATOS_R007_sin_nada_verificado_el_registro_no_se_toca(self):
        with open(self.registro, "rb") as f:
            antes = f.read()

        anotadas, sin_verificar = regen.registrar_en(self.workdir, self.registro, {}, "2026-11-12")

        self.assertEqual([], anotadas)
        self.assertEqual(3, len(sin_verificar))
        with open(self.registro, "rb") as f:
            self.assertEqual(antes, f.read())


if __name__ == "__main__":
    unittest.main()
