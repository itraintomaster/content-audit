import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

SPEC = importlib.util.spec_from_file_location("prepare", Path(__file__).with_name("prepare_choice_quality_exam.py"))
exam = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(exam)


class ExamPreparationTest(unittest.TestCase):
    def exports(self):
        corpus, frozen, records = exam.recover(exam.SOURCE)
        return [{"ejercicio": case["id"], "juez": "multiple-choice-quality",
                 "proveedor": "codex-cli", "modelo": "gpt-6.1-sol", "esfuerzo": "medium",
                 "version": frozen["qualityVersion"],
                 "resultado": {"PASS": "PASSES", "FAIL": "FAILS"}[json.loads(json.loads(record)["payload"])["status"]],
                 "reglas": [issue["kind"].lower().replace("_", "-")
                            for issue in json.loads(json.loads(record)["payload"])["issues"]]}
                for case, record in zip(corpus["cases"], records)]

    def measured(self, rows):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "verdicts.jsonl"
            path.write_text("\n".join(json.dumps(row) for row in rows))
            return exam.measure(exam.SOURCE, path)

    def test_measures_all_groups_against_frozen_rubric(self):
        result = self.measured(self.exports())
        self.assertEqual("APROBADO", result["estado"])
        self.assertEqual([9, 7], [g["comoSeEspera"] for g in result["grupos"]])
        self.assertEqual(0, result["newModelQueries"])

    def test_incorrect_status_is_failure_not_approval(self):
        rows = self.exports()
        rows[0]["resultado"] = "PASSES"
        result = self.measured(rows)
        self.assertEqual("REPROBADO", result["estado"])
        self.assertEqual([rows[0]["ejercicio"]], result["noDieronLoEsperado"])

    def test_refuses_incomplete_duplicate_foreign_runtime_and_mixed_version(self):
        original = self.exports()
        for rows in [original[:-1], original[:-1] + [original[0]]]:
            with self.assertRaises(ValueError):
                self.measured(rows)
        for field, value in [("modelo", "other"), ("esfuerzo", "high"),
                             ("version", None), ("resultado", "NOT_JUDGED"),
                             ("reglas", ["irrelevant-distractors"])]:
            rows = self.exports()
            rows[0][field] = value
            with self.subTest(field=field), self.assertRaises(ValueError):
                self.measured(rows)
        rows = self.exports()
        for row in rows:
            row["version"] = "different-but-consistent-version"
        with self.assertRaises(ValueError):
            self.measured(rows)

    def test_recovery_is_exam_only_append_original_and_refuses_overwrite(self):
        _, _, original = exam.recover(exam.SOURCE)
        with tempfile.TemporaryDirectory() as directory:
            workdir = Path(directory)
            self.assertEqual(16, exam.recover_into(exam.SOURCE, workdir))
            target = workdir / ".content-audit/evaluations/exam_choice-quality-judge.jsonl"
            before = target.read_bytes()
            self.assertEqual(original, target.read_text().splitlines())
            with self.assertRaises(FileExistsError):
                exam.recover_into(exam.SOURCE, workdir)
            self.assertEqual(before, target.read_bytes())
            self.assertFalse((workdir / ".content-audit/judge-exams").exists())
            self.assertFalse((workdir / ".content-audit/evaluations/choice-quality-judge.jsonl").exists())

    def test_recovers_exact_initial_records_and_no_corrected_candidates(self):
        corpus, frozen, records = exam.recover(exam.SOURCE)
        self.assertEqual(16, len(records))
        for case, line in zip(corpus["cases"], records):
            row = json.loads(line)
            self.assertEqual(case["id"], row["subjectRef"])
            self.assertEqual(frozen["qualityVersion"], row["evaluatorVersion"])
            self.assertEqual(exam.load(exam.SOURCE / f'case-{case["caseNumber"]:02d}/initial.json'),
                             json.loads(row["payload"]))

    def test_photo_contains_only_frozen_visible_content_not_gold(self):
        corpus, _, _ = exam.recover(exam.SOURCE)
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "exam"
            self.assertEqual(16, exam.prepare(exam.SOURCE, output))
            self.assertEqual([{"ejercicio": case["id"]} for case in corpus["cases"]],
                             exam.load(output / "cases.json"))
            quizzes = {q["id"]: q for path in (output / "course").rglob("quizzes.json")
                       for q in exam.load(path)}
            self.assertEqual(16, len(quizzes))
            for case in corpus["cases"]:
                quiz = quizzes[case["id"]]
                self.assertEqual(case["instructions"], quiz["instructions"])
                self.assertEqual(case["options"], [i["label"] for i in quiz["form"]["items"]])
                self.assertEqual(case["exercise"], "___".join(
                    part["text"] for part in quiz["form"]["sentenceParts"] if part["kind"] == "TEXT"))
            for path in (output / "course").rglob("*.json"):
                text = path.read_text()
                for field in ("expected", "allowedIssueKinds", "expectationReason", "rubric"):
                    self.assertNotIn(f'"{field}"', text)
            self.assertFalse((output / "workdir/.content-audit/judge-exams").exists())

    def test_does_not_overwrite_existing_attempt_or_campaign(self):
        with tempfile.TemporaryDirectory() as directory:
            existing = Path(directory)
            with self.assertRaises(FileExistsError):
                exam.prepare(exam.SOURCE, existing)
            self.assertEqual([], list(existing.iterdir()))


if __name__ == "__main__":
    unittest.main()
