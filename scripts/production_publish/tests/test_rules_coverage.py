"""Guarda: cada regla de publicar tiene al menos una prueba que la nombra.

Cada `### Rule[F-PUBPROD-Rnnn]` del requisito de publicar
(requirements/2026-10-11.01_publicar-a-produccion/REQUIREMENT.md) necesita una prueba que la nombre,
del lado que sea:

  - en Java, un @Tag con el id de la regla en cualquier src/test/**/*.java del repo;
  - en Python, un `def test_F_PUBPROD_Rnnn_<qué se prueba>` en cualquier .py del repo.

Que la cubra "de rebote" la prueba de otra regla no cuenta: la tiene que nombrar. Se recorre el repo
entero salteando lo oculto (.git, .content-audit, .claude…), target, node_modules y __pycache__.
"""

import os
import re
import tempfile
import unittest

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
REQUIREMENT = os.path.join(REPO, "requirements", "2026-10-11.01_publicar-a-produccion", "REQUIREMENT.md")

SKIPPED_DIRS = {"target", "node_modules", "__pycache__"}
RULE_HEADING = re.compile(r"^### Rule\[(F-PUBPROD-R\d{3})\]", re.MULTILINE)
JAVA_TAG = re.compile(r'@Tag\(\s*"(F-PUBPROD-R\d{3})"\s*\)')
PYTHON_TEST = re.compile(r"^\s*def test_F_PUBPROD_(R\d{3})_", re.MULTILINE)


def rules(requirement_text):
    """Los ids de las reglas, en el orden del requisito."""
    return RULE_HEADING.findall(requirement_text)


def named_rules(root):
    """Las reglas que nombra alguna prueba del árbol `root` (Java por @Tag, Python por el nombre)."""
    named = set()
    for directory, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if not d.startswith(".") and d not in SKIPPED_DIRS]
        for name in filenames:
            path = os.path.join(directory, name)
            if name.endswith(".java") and "/src/test/" in path.replace(os.sep, "/"):
                named.update(JAVA_TAG.findall(_read(path)))
            elif name.endswith(".py"):
                named.update("F-PUBPROD-" + rule for rule in PYTHON_TEST.findall(_read(path)))
    return named


def unnamed_message(missing):
    lines = ["estas reglas de publicar no tienen ninguna prueba que las nombre:"]
    for rule in missing:
        lines.append('  - %s: en Java @Tag("%s"), en Python def test_%s_<qué se prueba>(self)'
                     % (rule, rule, rule.replace("-", "_")))
    lines.append("La prueba de otra regla no cuenta aunque la ejercite: la tiene que nombrar.")
    return "\n".join(lines)


def _read(path):
    with open(path, encoding="utf-8", errors="replace") as fh:
        return fh.read()


class RulesCoverageTest(unittest.TestCase):
    def test_cada_regla_de_publicar_tiene_una_prueba_que_la_nombra(self):
        self.assertTrue(os.path.isfile(REQUIREMENT), "no está el requisito de publicar en %s" % REQUIREMENT)
        declared = rules(_read(REQUIREMENT))
        self.assertTrue(declared, "el requisito de publicar no tiene ningún «### Rule[F-PUBPROD-Rnnn]»: "
                                  "¿cambió el formato del encabezado?")
        named = named_rules(REPO)
        missing = [rule for rule in declared if rule not in named]
        if missing:
            self.fail(unnamed_message(missing))

    def test_la_guarda_cuenta_el_tag_de_java_y_el_nombre_de_python_y_saltea_lo_oculto_y_lo_compilado(self):
        with tempfile.TemporaryDirectory() as root:
            def put(relative, text):
                path = os.path.join(root, *relative.split("/"))
                os.makedirs(os.path.dirname(path), exist_ok=True)
                with open(path, "w", encoding="utf-8") as fh:
                    fh.write(text)

            put("mod/src/test/java/a/ATest.java", '@Test\n@Tag("FEAT-PUBPROD")\n@Tag("F-PUBPROD-R001")\nvoid a() {}\n')
            put("mod/src/main/java/a/A.java", '@Tag("F-PUBPROD-R002")\n')
            put("mod/target/test-classes/a/BTest.java", '@Tag("F-PUBPROD-R003")\n')
            put(".otra/src/test/java/a/CTest.java", '@Tag("F-PUBPROD-R004")\n')
            put("scripts/x/tests/test_x.py", "class T:\n    def test_F_PUBPROD_R005_algo(self):\n        pass\n")
            put("scripts/x/__pycache__/test_y.py", "def test_F_PUBPROD_R006_algo(self):\n    pass\n")
            put("scripts/x/tests/test_z.py", "# test_F_PUBPROD_R007 nombrada en un comentario no cuenta\n")

            self.assertEqual(named_rules(root), {"F-PUBPROD-R001", "F-PUBPROD-R005"})

    def test_la_negativa_dice_que_regla_falta_y_como_se_nombra_su_prueba(self):
        message = unnamed_message(["F-PUBPROD-R011"])
        self.assertIn('@Tag("F-PUBPROD-R011")', message)
        self.assertIn("def test_F_PUBPROD_R011_", message)


if __name__ == "__main__":
    unittest.main()
