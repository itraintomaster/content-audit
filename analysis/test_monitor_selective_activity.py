import json
import tempfile
import unittest
from argparse import Namespace
from pathlib import Path
from unittest.mock import patch

import monitor_selective_activity as monitor


class SelectiveMonitorTest(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.activity = {"id": "selective", "batchId": "batch", "kind": "CONTENT_SELECTIVE_ANALYSIS",
                         "status": "RUNNING", "startedAt": "2026-10-19T23:30:00Z",
                         "provider": "codex-cli", "model": "selected", "reasoningEffort": "medium",
                         "progress": {"completed": 0, "total": 100, "unit": "consultas"}}
        self.run = {"id": "selective", "action": "analyze", "analyzer": "naturalness",
                    "judgeVersion": "v1", "createdAt": "2026-10-19T23:30:02Z",
                    "status": "RUNNING", "spent": 0, "evaluated": 0, "quizIds": []}
        self.record = self.write(".content-audit/activity/runs/selective.json", self.activity)
        self.batch = self.write(".content-audit/batches/batch.json", {"selectiveRuns": [self.run]})
        self.args = Namespace(workdir=self.root, campaign_workdir=self.root,
                              activity_id="selective", process_pid=12345)

    def write(self, name, value):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(value))
        return path

    def row(self, **changes):
        return {"evaluatorId": "native-usage-judge", "contentFingerprint": "fp", "subjectRef": "q1",
                "evaluatorVersion": "v1", "recordedAt": "2026-10-19T23:31:00Z", "payload": "{}",
                "runtimeKind": "EXPLICIT", "providerName": "codex-cli", "modelName": "selected",
                "reasoningEffort": "medium", **changes}

    def ledger(self, rows, partial=""):
        path = self.root / ".content-audit/evaluations/native-usage-judge.jsonl"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text("".join(json.dumps(row) + "\n" for row in rows) + partial)
        return path

    def test_visible_progress_before_selective_counters_are_consolidated(self):
        ledger = self.ledger([self.row()])
        before = (self.batch.read_bytes(), ledger.read_bytes())
        with patch.object(monitor.activity_run, "process_is_alive", return_value=True):
            self.assertTrue(monitor.tick(self.args))
        record = monitor.read(self.record)
        self.assertEqual(record["progress"]["completed"], 1)
        self.assertEqual(record["progress"]["unit"], "evaluaciones nuevas")
        self.assertEqual(record["lastProgressAt"], "2026-10-19T23:31:00+00:00")
        self.assertEqual(record["status"], "RUNNING")
        self.assertNotIn("finishedAt", record)
        self.assertIn("no es gasto confirmado", record["facts"][0]["value"])
        self.assertEqual(before, (self.batch.read_bytes(), ledger.read_bytes()))

    def test_excludes_old_foreign_malformed_partial_and_duplicate_records(self):
        self.ledger([self.row(), self.row(), self.row(contentFingerprint="before", recordedAt="2026-10-19T23:30:01Z"),
                     self.row(contentFingerprint="version", evaluatorVersion="v2"),
                     self.row(contentFingerprint="model", modelName="other"),
                     self.row(contentFingerprint="provider", providerName="other"),
                     self.row(contentFingerprint="effort", reasoningEffort="high"),
                     self.row(contentFingerprint="legacy", runtimeKind="LEGACY"),
                     self.row(contentFingerprint="evaluator", evaluatorId="other"),
                     self.row(contentFingerprint="invalid", payload="not json"),
                     self.row(contentFingerprint="list", payload="[]"),
                     self.row(contentFingerprint="invalid-time", recordedAt=None)],
                    partial=json.dumps(self.row(contentFingerprint="unfinished")))
        count, last = monitor.snapshot(self.root, self.activity)
        self.assertEqual(count, 1)
        self.assertEqual(last.isoformat(), "2026-10-19T23:31:00+00:00")

    def test_honors_explicit_exercise_selection(self):
        self.run["quizIds"] = ["q1"]
        self.write(".content-audit/batches/batch.json", {"selectiveRuns": [self.run]})
        self.ledger([self.row(), self.row(subjectRef="q2", contentFingerprint="outside")])
        self.assertEqual(monitor.snapshot(self.root, self.activity)[0], 1)

    def test_never_resurrects_finished_or_cancelled_activity(self):
        for status in ["CANCELLED", "NEEDS_REVIEW", "FAILED"]:
            self.activity["status"] = status
            path = self.write(".content-audit/activity/runs/selective.json", self.activity)
            before = path.read_bytes()
            self.assertFalse(monitor.tick(self.args))
            self.assertEqual(path.read_bytes(), before)

    def test_absent_process_does_not_infer_success_or_replace_official_closure(self):
        before = self.record.read_bytes()
        with patch.object(monitor.activity_run, "process_is_alive", return_value=False):
            self.assertFalse(monitor.tick(self.args))
        self.assertEqual(self.record.read_bytes(), before)

    def test_corrector_outputs_are_not_counted_as_analysis_progress(self):
        self.run["action"] = "correct"
        self.write(".content-audit/batches/batch.json", {"selectiveRuns": [self.run]})
        self.ledger([self.row()])
        self.assertEqual(monitor.snapshot(self.root, self.activity)[0], 0)


if __name__ == "__main__":
    unittest.main()
