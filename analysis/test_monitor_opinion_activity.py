import json
import tempfile
import unittest
from argparse import Namespace
from datetime import datetime, timezone
from pathlib import Path
from unittest.mock import patch

import monitor_opinion_activity as monitor


class OpinionMonitorTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.record = {"id":"opinion", "batchId":"batch", "kind":"CONTENT_CONTEXT_OPINION",
            "status":"RUNNING", "startedAt":"2026-10-14T16:00:00Z", "provider":"codex-cli",
            "model":"model", "reasoningEffort":"medium", "progress":{"completed":0,"total":20,"unit":"consultas"}}
        self.run = {"id":"opinion", "status":"RUNNING", "finishedAt":None, "spent":1, "reserved":20,
            "request":{"selection":{"a1-didactic":["a","b"], "naturalness":["a","b"]}},
            "judges":[{"analyzer":j,"quizzes":2,"judgeVersion":"v1"} for j in ["a1-didactic","naturalness"]],
            "results":[{"analyzer":"a1-didactic","outcome":{"evaluatedAnalyzers":["a1-didactic"]}},
                       {"analyzer":"a1-didactic","outcome":{"evaluatedAnalyzers":[]}}]}
        self.batch = {"status":"RUNNING", "opinionRuns":[self.run],"selectiveRuns":[]}
        self.write(".content-audit/activity/runs/opinion.json", self.record)
        self.save_batch()
        self.args = Namespace(workdir=self.root,campaign_workdir=self.root,activity_id="opinion",process_pid=12345)

    def write(self, name, value):
        p=self.root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value));return p

    def save_batch(self):
        return self.write(".content-audit/batches/batch.json",self.batch)

    def row(self, **kwargs):
        return {"evaluatorId":"native-usage-judge", "contentFingerprint":"fp", "subjectRef":"a",
            "evaluatorVersion":"v1", "recordedAt":"2026-10-14T16:01:00Z", "payload":"{}",
            "runtimeKind":"EXPLICIT", "providerName":"codex-cli", "modelName":"model",
            "reasoningEffort":"medium", **kwargs}

    def ledger(self, rows, filename="native-usage-judge.jsonl", partial=""):
        p=self.root/".content-audit/evaluations"/filename;p.parent.mkdir(parents=True,exist_ok=True)
        p.write_text("".join(json.dumps(r)+"\n" for r in rows)+partial)

    def test_only_new_valid_exact_runtime_scope_records_count(self):
        good=self.row()
        self.ledger([good,good,self.row(contentFingerprint="old",recordedAt="2026-10-14T15:00:00Z"),
            self.row(contentFingerprint="foreign",subjectRef="c"),self.row(contentFingerprint="wrong",evaluatorVersion="v2"),
            self.row(contentFingerprint="other-model",modelName="different"),self.row(payload="unusable")],
            partial=json.dumps(self.row(contentFingerprint="partial")))
        state=monitor.snapshot(self.root,self.record,now=datetime(2026,10,14,16,1,1,tzinfo=timezone.utc))
        self.assertEqual(state["completed"],1)
        self.assertEqual(state["stages"][0]["status"],"NEEDS_REVIEW")
        self.assertEqual(state["stages"][1]["status"],"RUNNING")

    def test_pair_counts_only_when_both_exercises_belong_to_selection(self):
        self.ledger([self.row(evaluatorId="near-duplicates-pair-judge",subjectRef="a~b"),
            self.row(evaluatorId="near-duplicates-pair-judge",contentFingerprint="foreign",subjectRef="a~c")],
            filename="near-duplicates-pair-judge.jsonl")
        count,_=monitor.saved_records(self.root/".content-audit/evaluations",("near-duplicates-pair-judge.jsonl",),
            monitor.instant(self.record["startedAt"]),{"a","b"},"v1")
        self.assertEqual(count,1)

    def test_partial_scope_reads_failure_without_changing_batch_or_verdicts(self):
        self.batch["selectiveRuns"]=[{"sourceRunId":"opinion","analyzer":"a1-didactic","auditId":"one"}]
        path=self.save_batch();before=path.read_bytes()
        self.write(".content-audit/audits/audit-one.json",{"root":{"diagnoses":{"judgeCoverages":[
            {"judgeName":"a1-didactic","unavailableReason":"límite de solicitudes informado por el proveedor"}]}}})
        self.ledger([self.row()]);ledger=self.root/".content-audit/evaluations/native-usage-judge.jsonl";raw=ledger.read_bytes()
        with patch.object(monitor.activity_run,"process_is_alive",return_value=True):
            self.assertTrue(monitor.tick(self.args))
        activity=monitor.read(self.root/".content-audit/activity/runs/opinion.json")
        self.assertIn("límite de solicitudes",activity["stages"][0]["result"])
        self.assertEqual(activity["progress"]["unit"],"opiniones nuevas")
        self.assertEqual(path.read_bytes(),before);self.assertEqual(ledger.read_bytes(),raw)
        self.assertNotIn("finishedAt",activity)

    def test_absent_process_requires_this_operations_finished_result(self):
        self.batch["opinionRuns"].append({"id":"old","status":"DONE","finishedAt":"2026-10-14T16:01:00Z"})
        self.save_batch()
        with patch.object(monitor.activity_run,"process_is_alive",return_value=False):monitor.tick(self.args)
        self.assertEqual(monitor.read(self.root/".content-audit/activity/runs/opinion.json")["status"],"FAILED")

    def test_confirmed_partial_finish_is_for_review_not_approved(self):
        self.run.update(status="PARTIAL",finishedAt="2026-10-14T16:02:00Z");self.save_batch()
        with patch.object(monitor.activity_run,"process_is_alive",return_value=False):monitor.tick(self.args)
        self.assertEqual(monitor.read(self.root/".content-audit/activity/runs/opinion.json")["status"],"NEEDS_REVIEW")

    def test_does_not_resurrect_cancelled_activity(self):
        self.record["status"]="CANCELLED";p=self.write(".content-audit/activity/runs/opinion.json",self.record);before=p.read_bytes()
        self.assertFalse(monitor.tick(self.args));self.assertEqual(p.read_bytes(),before)

    def test_quiet_alive_process_does_not_claim_it_is_progressing(self):
        self.ledger([self.row()])
        state=monitor.snapshot(self.root,self.record,now=datetime(2026,10,14,16,10,0,tzinfo=timezone.utc))
        self.assertIn("no confirma avance",state["summary"])

    def test_a_single_failing_variant_makes_one_bad_exercise_not_three(self):
        payload=json.dumps({"assessments":[{"status":"PASS"},{"status":"FAIL"},{"status":"FAIL"}]})
        self.ledger([self.row(payload=payload)])
        state=monitor.snapshot(self.root,self.record)
        self.assertEqual(state["outcomes"]["BAD"],1)
        self.assertEqual(state["outcomes"]["GOOD"],0)
        self.assertEqual(state["completed"],1)
        self.assertIn("Bien: 0 · Mal: 1",state["stages"][1]["result"])
        self.assertIn("Mal: 1",state["summary"])

    def test_reviews_non_applicable_and_unknown_are_never_good(self):
        for statuses,expected in [(["PASS","REVIEW"],"REVIEW"),(["NEEDS_CONTEXT"],"REVIEW"),
            (["NOT_APPLICABLE"],"NOT_APPLICABLE"),(["PASS","NOT_APPLICABLE"],"GOOD"),
            (["unexpected"],"UNKNOWN"),([{"invalid":True}],"UNKNOWN")]:
            with self.subTest(statuses=statuses):
                self.assertEqual(monitor.grade({"assessments":[{"status":s} for s in statuses]},"native-usage-judge"),expected)
        for status,expected in [("SUITABLE","GOOD"),("IMPROVE","BAD"),("REWRITE_REQUIRED","BAD"),
                               ("REVIEW","REVIEW"),("NOT_APPLICABLE","NOT_APPLICABLE")]:
            self.assertEqual(monitor.grade({"status":status},"a1-didactic-judge"),expected)
        self.assertEqual(monitor.grade({"sameExercise":True},"near-duplicates-pair-judge"),"BAD")
        self.assertEqual(monitor.grade({"sameExercise":False},"near-duplicates-pair-judge"),"GOOD")
        self.assertEqual(monitor.grade({"sameExercise":"false"},"near-duplicates-pair-judge"),"UNKNOWN")

    def test_f_opina_r010_every_judge_of_a_completar_counts_what_it_saves(self):
        files={"a1-didactic":"a1-didactic-judge","naturalness":"native-usage-judge","pragmatism":"pragmatism-judge",
            "near-duplicates":"near-duplicates-pair-judge","answer-giveaway":"answer-giveaway-judge",
            "quiz-instruction":"quiz-instruction-validator","multiple-choice-validity":"choice-alternative-judge",
            "multiple-choice-quality":"choice-quality-judge","translation-sense":"translation-sense-from-english",
            "single-answer":"single-answer-judge","mini-theory":"mini-theory-judge"}
        self.run.update(results=[],request={"selection":{j:["a","b"] for j in files}},
            judges=[{"analyzer":j,"quizzes":2,"pendingQueries":1,"judgeVersion":"v1"} for j in files])
        self.save_batch()
        for judge,evaluator in files.items():
            subject="a~b" if judge=="near-duplicates" else "a"
            self.ledger([self.row(evaluatorId=evaluator,subjectRef=subject)],filename=evaluator+".jsonl")
        state=monitor.snapshot(self.root,self.record,now=datetime(2026,10,14,16,1,10,tzinfo=timezone.utc))
        self.assertEqual({s["label"]:s["progress"]["completed"] for s in state["stages"]},
                         {monitor.NAMES[j]:1 for j in files})
        self.assertTrue(all(s["status"]=="RUNNING" for s in state["stages"]))
        self.assertFalse(any("Todavía no comenzó" in s["result"] for s in state["stages"]))
        self.assertEqual(state["completed"],len(files))
        self.assertNotIn("no confirma avance",state["summary"])
        self.assertIn("No regala la respuesta",[s["label"] for s in state["stages"]])

    def test_f_opina_r010_a_judge_whose_opinions_cannot_be_counted_says_so(self):
        self.run.update(results=[],request={"selection":{"a-new-judge":["a"]}},
            judges=[{"analyzer":"a-new-judge","quizzes":1,"judgeVersion":"v1"}])
        self.save_batch()
        stage=monitor.snapshot(self.root,self.record)["stages"][0]
        self.assertIn("no se cuentan acá",stage["result"])
        self.assertNotIn("Todavía no comenzó",stage["result"])

    def test_f_opina_r010_giveaway_and_distractor_opinions_are_graded(self):
        for evaluator,cases in [("answer-giveaway-judge",[("OK","GOOD"),("GIVES_AWAY","BAD"),("REVIEW","REVIEW"),
                                    ("NOT_APPLICABLE","NOT_APPLICABLE"),("other","UNKNOWN")]),
                                ("choice-quality-judge",[("PASS","GOOD"),("FAIL","BAD"),("NEEDS_CONTEXT","REVIEW"),
                                    ("other","UNKNOWN")])]:
            for status,expected in cases:
                with self.subTest(evaluator=evaluator,status=status):
                    self.assertEqual(monitor.grade({"status":status},evaluator),expected)

    def test_latest_duplicate_replaces_grade_without_inflating_count(self):
        good=json.dumps({"assessments":[{"status":"PASS"}]});bad=json.dumps({"assessments":[{"status":"FAIL"}]})
        self.ledger([self.row(payload=good),self.row(payload=bad,recordedAt="2026-10-14T16:02:00Z"),self.row(payload=good)])
        state=monitor.snapshot(self.root,self.record)
        self.assertEqual(state["outcomes"]["GOOD"],0)
        self.assertEqual(state["outcomes"]["BAD"],1)
        self.assertEqual(state["completed"],sum(state["outcomes"].values()))


if __name__=="__main__":unittest.main()
