"""Course JSON → the real judge-input adapter, without running any model."""

import copy
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


EVAL_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = Path(__file__).resolve().parents[4]
SPEC = importlib.util.spec_from_file_location("quiz_instruction_run_evals", EVAL_DIR / "run_evals.py")
RUNNER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RUNNER)


class StudentViewInputsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        source = PROJECT_ROOT / "db/english-course/a1/determiners/choosing-between-much-and-many/quizzes.json"
        cls.quiz = next(quiz for quiz in RUNNER.load_json(source)
                       if quiz["id"] == "67fab6d599301022953423c5")

    def case(self):
        return {"cefrLevel": "A1", "topic": "Determiners", "quiz": copy.deepcopy(self.quiz)}

    def view_after_save_and_reload(self, case):
        with tempfile.TemporaryDirectory() as directory:
            saved = Path(directory) / "quiz-case.json"
            saved.write_text(json.dumps(case, ensure_ascii=False), encoding="utf-8")
            return RUNNER.student_view_inputs(RUNNER.load_json(saved))

    def assert_many_and_much(self, view):
        self.assertEqual(view["exercise"], "Do you know ___ people?")
        self.assertEqual(view["acceptedAnswers"], "Hueco 1: many")
        self.assertEqual(view["options"], "- many (correcta)\n- much")

    def test_raw_production_quiz_keeps_options_and_correct_answer_after_json_round_trip(self):
        case = self.case()
        original = copy.deepcopy(case)
        form = case["quiz"]["form"]
        self.assertEqual(form["kind"], "MULTIPLE_CHOICE")
        self.assertEqual(form["selection"], "SINGLE")
        self.assertEqual(form["sentences"], ["Do you know many people?"])
        self.assertNotIn("multipleChoice", form)

        view = self.view_after_save_and_reload(case)

        self.assert_many_and_much(view)
        self.assertEqual(view["instructions"], "Elige 'much' o 'many'.")
        self.assertEqual(case, original, "reading the judge view must not mutate the course JSON")

    def test_numeric_and_extended_json_incidences_have_the_same_judge_view(self):
        for correct, wrong in [(1.0, 0.0),
                               ({"$numberDouble": "1.0"}, {"$numberDouble": "0.0"}),
                               ({"$numberDouble": 1.0}, {"$numberDouble": 0.0})]:
            with self.subTest(correct=correct):
                case = self.case()
                case["quiz"]["form"]["items"][0]["incidence"] = correct
                case["quiz"]["form"]["items"][1]["incidence"] = wrong
                self.assert_many_and_much(self.view_after_save_and_reload(case))

    def test_legacy_snapshot_options_are_read_when_canonical_items_are_absent(self):
        case = self.case()
        form = case["quiz"]["form"]
        form["multipleChoice"] = {"selection": form.pop("selection"), "items": form.pop("items")}

        self.assert_many_and_much(self.view_after_save_and_reload(case))

    def test_canonical_items_win_over_conflicting_legacy_snapshot(self):
        case = self.case()
        case["quiz"]["form"]["multipleChoice"] = {
            "selection": "SINGLE", "items": [{"label": "obsolete", "incidence": 1.0}]
        }

        self.assert_many_and_much(self.view_after_save_and_reload(case))

    def test_explicit_empty_canonical_items_do_not_revive_legacy_options(self):
        case = self.case()
        form = case["quiz"]["form"]
        form["multipleChoice"] = {"selection": "SINGLE", "items": form["items"]}
        form["items"] = []

        view = self.view_after_save_and_reload(case)

        self.assertEqual(view["options"], "")
        self.assertEqual(view["acceptedAnswers"], "Hueco 1: ")

    def test_cloze_ignores_saved_choice_items_and_keeps_its_accepted_answers(self):
        case = self.case()
        form = case["quiz"]["form"]
        form["kind"] = "CLOZE"
        form["sentenceParts"][1]["options"] = ["many"]
        form["multipleChoice"] = {"selection": "SINGLE", "items": form["items"]}
        # The form is authoritative even if the quiz's old top-level kind differs.
        self.assertEqual(case["quiz"]["kind"], "MULTIPLE_CHOICE")

        view = self.view_after_save_and_reload(case)

        self.assertEqual(view["options"], "")
        self.assertEqual(view["acceptedAnswers"], "Hueco 1: many")

    def test_form_kind_controls_multiple_choice_even_with_an_old_quiz_kind(self):
        case = self.case()
        case["quiz"]["kind"] = "CLOZE"

        self.assert_many_and_much(self.view_after_save_and_reload(case))


if __name__ == "__main__":
    unittest.main()
