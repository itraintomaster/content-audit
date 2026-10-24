import copy
import fcntl
import json
import os
import tempfile
import unittest
from argparse import Namespace
from pathlib import Path
from unittest.mock import patch

import monitor_recorrect_activity as monitor


class RecorrectMonitorTest(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.activity = {
            "id": "recorrect", "batchId": "batch", "kind": monitor.KIND,
            "status": "RUNNING", "startedAt": "2026-10-24T03:25:07Z",
            "updatedAt": "2026-10-24T03:25:07Z", "processPid": 12345,
            "progress": {"completed": 0, "total": 1000, "unit": "consultas", "totalKind": "CEILING"},
        }
        self.run = {
            "id": "recorrect", "action": "recorrect", "status": "RUNNING",
            "taskIds": ["t1", "t2", "t3"], "quizIds": ["q1", "q2", "q3"],
            "queryCap": 1000, "spent": 99999, "process": {"pid": 12345},
        }
        self.batch = {
            "selectiveRuns": [self.run], "queriesSpent": 999999,
            "panelDecisions": [self.decision(), self.decision("t2", "q2", "TIE", 19)],
        }
        self.record_path = self.write(".content-audit/activity/runs/recorrect.json", self.activity)
        self.batch_path = self.save_batch()
        self.args = Namespace(workdir=self.root, campaign_workdir=self.root,
                              activity_id="recorrect", process_pid=12345)

    def write(self, name, value):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(value), encoding="utf-8")
        return path

    def save_batch(self):
        return self.write(".content-audit/batches/batch.json", self.batch)

    def decision(self, task="t1", quiz="q1", outcome="ACCEPTED", spent=9, **changes):
        return {"knowledgeId": "knowledge", "taskIds": [task], "quizIds": [quiz],
                "outcome": outcome, "attempts": 1, "queriesSpent": spent,
                "resolvedTaskIds": [task] if outcome == "ACCEPTED" else [], **changes}

    def tick(self, args=None):
        with patch.object(monitor.activity_run, "process_is_alive", return_value=True):
            return monitor.tick(args or self.args)

    def record(self):
        return monitor.read(self.record_path)

    def test_reports_only_saved_attributed_partial_results_without_touching_sources(self):
        ledger = self.write(".content-audit/evaluations/foreign.jsonl", {"queries": 7000})
        before = self.batch_path.read_bytes(), ledger.read_bytes()
        real_read = monitor.read
        with patch.object(monitor, "read", wraps=real_read) as reads:
            self.assertTrue(self.tick())
        record = self.record()
        self.assertEqual(record["progress"]["completed"], 28)
        self.assertEqual(record["progress"]["total"], 1000)
        self.assertEqual(record["status"], "RUNNING")
        self.assertNotIn("finishedAt", record)
        self.assertIn("2 de 3 quizzes", record["summary"])
        self.assertIn("1 propuestas que pasaron la validación", record["summary"])
        self.assertIn("pendientes de tu revisión", record["summary"])
        self.assertIn("1 empate para revisar", record["summary"])
        self.assertIn("28 " + monitor.QUERY_LABEL, record["summary"])
        self.assertIn("aún sin resultado guardado no están incluidas", record["summary"])
        self.assertEqual(record["stages"][0]["progress"]["completed"], 28)
        self.assertEqual(before, (self.batch_path.read_bytes(), ledger.read_bytes()))
        self.assertNotIn(ledger, [call.args[0] for call in reads.call_args_list])

    def test_exact_run_id_and_action_are_required_not_source_run_id(self):
        for change in ({"id": "foreign", "sourceRunId": "recorrect"}, {"action": "analyze"}):
            with self.subTest(change=change):
                batch = copy.deepcopy(self.batch)
                batch["selectiveRuns"][0].update(change)
                self.assertIsNone(monitor.summarize(batch, "recorrect")["run"])

    def test_ambiguous_matching_runs_fail_closed(self):
        self.batch["selectiveRuns"].append(copy.deepcopy(self.run))
        self.assertIsNone(monitor.summarize(self.batch, "recorrect")["run"])

    def test_foreign_empty_mixed_and_malformed_task_or_quiz_selections_are_excluded(self):
        outsiders = [
            {"taskIds": ["foreign"]}, {"taskIds": ["t1", "foreign"]}, {"taskIds": []},
            {"taskIds": "t1"}, {"taskIds": ["t1", None]}, {"quizIds": ["foreign"]},
            {"quizIds": ["q1", "foreign"]}, {"quizIds": []}, {"quizIds": None},
            {"resolvedTaskIds": ["foreign"]}, {"outcome": "UNKNOWN"},
        ]
        self.batch["panelDecisions"].extend(self.decision(spent=999, **change) for change in outsiders)
        self.batch["panelDecisions"].extend([None, "bad"])
        state = monitor.summarize(self.batch, "recorrect")
        self.assertEqual((state["confirmed"], state["decisions"], state["accepted"], state["ties"]), (28, 2, 1, 1))

    def test_merged_task_decision_is_included_only_when_every_task_belongs(self):
        self.batch["panelDecisions"] = [self.decision(taskIds=["t1", "t2"], quizIds=["q1", "q2"])]
        state = monitor.summarize(self.batch, "recorrect")
        self.assertEqual((state["decided"], state["remaining"], state["confirmed"]), (2, 1, 9))

    def test_duplicate_decisions_never_double_charge_and_keep_largest_known_copy(self):
        self.batch["panelDecisions"] = [self.decision(spent=9), self.decision(spent=9),
                                        self.decision(spent=None), self.decision(spent=12)]
        state = monitor.summarize(self.batch, "recorrect")
        self.assertEqual((state["decisions"], state["accepted"], state["confirmed"], state["withoutCost"]), (1, 1, 12, 0))

    def test_unique_quizzes_not_decisions_determine_completed_selection(self):
        self.batch["panelDecisions"].append(self.decision("t3", "q1", "NO_WINNER", 4))
        state = monitor.summarize(self.batch, "recorrect")
        self.assertEqual((state["decisions"], state["decided"], state["remaining"]), (3, 2, 1))

    def test_missing_invalid_or_negative_spending_is_unknown_not_fabricated(self):
        for cost in [None, -1, True, "9", 1.5]:
            with self.subTest(cost=cost):
                self.batch["panelDecisions"] = [self.decision(spent=cost)]
                state = monitor.summarize(self.batch, "recorrect")
                self.assertEqual((state["confirmed"], state["withoutCost"]), (0, 1))
                self.assertTrue(any(f["label"] == "Gasto sin dato" for f in monitor.presentation(state)["facts"]))

    def test_no_longer_holds_zero_does_not_imply_no_other_queries_are_in_flight(self):
        self.batch["panelDecisions"] = [self.decision(outcome="NO_LONGER_HOLDS", spent=0)]
        shown = monitor.presentation(monitor.summarize(self.batch, "recorrect"))
        self.assertEqual(shown["completed"], 0)
        self.assertIn("consultas aún sin resultado guardado no están incluidas", shown["summary"])

    def test_candidate_and_verdict_payloads_are_never_inspected_or_retained(self):
        class Unreadable:
            def __getattribute__(self, _name):
                raise AssertionError("candidate payload must not be read")
        self.batch["panelDecisions"][0].update(accepted=Unreadable(), acceptedVerdict=Unreadable())
        state = monitor.summarize(self.batch, "recorrect")
        self.assertEqual(state["confirmed"], 28)
        json.dumps(state)

    def test_dead_process_does_not_read_batch_close_activity_or_guess_final_spending(self):
        before = self.record_path.read_bytes()
        with patch.object(monitor.activity_run, "process_is_alive", return_value=False), \
                patch.object(monitor, "snapshot") as snapshot:
            self.assertFalse(monitor.tick(self.args))
        snapshot.assert_not_called()
        self.assertEqual(self.record_path.read_bytes(), before)

    def test_terminal_activity_never_resurrects_even_with_live_process(self):
        for status, finished in [("CANCELLED", None), ("FAILED", None), ("NEEDS_REVIEW", None),
                                 ("PASSED", None), ("RUNNING", "2026-10-24T04:00:00Z")]:
            with self.subTest(status=status, finished=finished):
                self.activity.update(status=status, finishedAt=finished)
                self.write(".content-audit/activity/runs/recorrect.json", self.activity)
                before = self.record_path.read_bytes()
                self.assertFalse(self.tick())
                self.assertEqual(self.record_path.read_bytes(), before)

    def test_terminal_selective_run_is_left_to_launchers_final_reconciliation(self):
        for changes in [{"status": "DONE"}, {"finishedAt": "2026-10-24T04:00:00Z"}]:
            with self.subTest(changes=changes):
                batch = copy.deepcopy(self.batch)
                batch["selectiveRuns"][0].update(changes)
                self.write(".content-audit/batches/batch.json", batch)
                before = self.record_path.read_bytes()
                self.assertFalse(self.tick())
                self.assertEqual(self.record_path.read_bytes(), before)

    def test_foreign_process_does_not_attach_to_matching_operation(self):
        self.run["process"]["pid"] = 54321
        self.save_batch()
        before = self.record_path.read_bytes()
        self.assertFalse(self.tick())
        self.assertEqual(self.record_path.read_bytes(), before)

    def test_finalization_between_read_and_update_is_protected_by_shared_writer_lock(self):
        real_update = monitor.activity_run.update
        terminal = {**self.activity, "status": "CANCELLED", "finishedAt": "2026-10-24T04:00:00Z"}
        def finalize_then_update(args):
            self.write(".content-audit/activity/runs/recorrect.json", terminal)
            return real_update(args)
        with patch.object(monitor.activity_run, "update", side_effect=finalize_then_update):
            self.assertTrue(self.tick())
        self.assertEqual(self.record(), terminal)

    def test_batch_is_not_reparsed_until_its_stat_changes(self):
        cache = monitor.SnapshotCache()
        with patch.object(monitor, "read", wraps=monitor.read) as reads:
            first = cache.load(self.batch_path, "recorrect")
            self.assertIs(cache.load(self.batch_path, "recorrect"), first)
            self.assertEqual(reads.call_count, 1)
            self.batch["panelDecisions"].append(self.decision("t3", "q3", spent=4))
            self.save_batch()
            self.assertEqual(cache.load(self.batch_path, "recorrect")["confirmed"], 32)
            self.assertEqual(reads.call_count, 2)

    def test_cache_detects_same_size_revision_and_atomic_replacement(self):
        cache = monitor.SnapshotCache()
        first = cache.load(self.batch_path, "recorrect")
        stat = self.batch_path.stat()
        self.batch["panelDecisions"][0]["queriesSpent"] = 8
        self.save_batch()
        self.assertEqual(self.batch_path.stat().st_size, stat.st_size)
        os.utime(self.batch_path, ns=(stat.st_atime_ns, stat.st_mtime_ns + 1_000_000))
        self.assertEqual(cache.load(self.batch_path, "recorrect")["confirmed"], first["confirmed"] - 1)
        replacement = self.write("replacement.json", self.batch)
        before = cache.value
        with patch.object(monitor, "read", wraps=monitor.read) as reads:
            os.replace(replacement, self.batch_path)
            cache.load(self.batch_path, "recorrect")
            self.assertEqual(reads.call_count, 1)
        self.assertEqual(cache.value, before)

    def test_batch_changed_during_read_does_not_cache_or_publish_mixed_snapshot(self):
        cache = monitor.SnapshotCache()
        with patch.object(cache, "signature", side_effect=[(1, 1, 1, 1), (1, 1, 2, 1)]):
            with self.assertRaises(OSError):
                cache.load(self.batch_path, "recorrect")
        self.assertIsNone(cache.key)
        self.assertIsNone(cache.value)

    def test_foreign_batch_updates_and_reattach_do_not_create_heartbeat(self):
        self.assertTrue(self.tick())
        before = self.record_path.read_bytes()
        self.batch["panelDecisions"].append(self.decision("foreign", "q1", spent=888))
        self.save_batch()
        with patch.object(monitor.activity_run, "update", wraps=monitor.activity_run.update) as update:
            self.assertTrue(self.tick())
            # Fresh namespace represents a newly attached monitor with no in-memory cache.
            self.assertTrue(self.tick(Namespace(workdir=self.root, campaign_workdir=self.root,
                                               activity_id="recorrect", process_pid=12345)))
        update.assert_not_called()
        self.assertEqual(self.record_path.read_bytes(), before)

    def test_redaction_change_does_not_invent_last_progress_time(self):
        self.assertTrue(self.tick())
        record = self.record()
        record["summary"] = "An earlier wording for these same saved results."
        self.write(".content-audit/activity/runs/recorrect.json", record)
        self.assertTrue(self.tick())
        self.assertEqual(self.record()["lastProgressAt"], record["lastProgressAt"])

    def test_new_own_saved_result_updates_partial_progress(self):
        self.assertTrue(self.tick())
        self.batch["panelDecisions"].append(self.decision("t3", "q3", spent=4))
        self.save_batch()
        self.assertTrue(self.tick())
        record = self.record()
        self.assertEqual(record["progress"]["completed"], 32)
        self.assertIn("3 de 3 quizzes", record["summary"])
        self.assertEqual(record["status"], "RUNNING")
        self.assertNotIn("finishedAt", record)

    def test_over_cap_is_visible_unclipped_in_stage_facts_and_summary_without_changing_cap(self):
        self.batch["panelDecisions"][0]["queriesSpent"] = 1009
        self.save_batch()
        self.assertTrue(self.tick())
        record = self.record()
        self.assertEqual(record["progress"]["total"], 1000)
        self.assertEqual(record["progress"]["completed"], 1000)  # shared visual bar
        self.assertEqual(record["stages"][0]["progress"]["completed"], 1028)
        self.assertIn("1028 " + monitor.QUERY_LABEL, record["summary"])
        self.assertIn("superan el tope en 28 consultas", record["summary"])
        self.assertTrue(any(f["label"] == "Tope superado" and "1028" in f["value"] for f in record["facts"]))

    def test_missing_or_temporarily_invalid_batch_does_not_invent_progress(self):
        before = self.record_path.read_bytes()
        for content in ['{"selectiveRuns":', '{"selectiveRuns": null}', '{"selectiveRuns": [], "panelDecisions": null}']:
            self.batch_path.write_text(content)
            self.assertTrue(self.tick())
            self.assertEqual(self.record_path.read_bytes(), before)
        self.batch_path.unlink()
        self.assertTrue(self.tick())
        self.assertEqual(self.record_path.read_bytes(), before)

    def test_only_correct_activity_kind_and_safe_batch_ids_are_accepted(self):
        self.activity["kind"] = "CONTENT_SELECTIVE_ANALYSIS"
        self.write(".content-audit/activity/runs/recorrect.json", self.activity)
        with self.assertRaises(ValueError):
            self.tick()
        with self.assertRaises(ValueError):
            monitor.snapshot(self.root, {**self.activity, "batchId": "../other"})

    def test_duplicate_monitor_exits_without_truncating_existing_lock(self):
        path = self.root / ".content-audit/activity/monitors/recorrect-recorrect.lock"
        path.parent.mkdir(parents=True)
        path.write_text("original-owner")
        with path.open("a+") as held:
            fcntl.flock(held, fcntl.LOCK_EX | fcntl.LOCK_NB)
            with patch.object(monitor, "tick") as tick:
                result = monitor.main(["--workdir", str(self.root), "--campaign-workdir", str(self.root),
                                       "--activity-id", "recorrect", "--process-pid", "12345", "--once"])
            self.assertEqual(result, 0)
            tick.assert_not_called()
            self.assertEqual(path.read_text(), "original-owner")

    def test_once_runs_one_snapshot_and_does_not_sleep(self):
        with patch.object(monitor, "tick", return_value=True) as tick, patch.object(monitor.time, "sleep") as sleep:
            self.assertEqual(monitor.main(["--workdir", str(self.root), "--campaign-workdir", str(self.root),
                                           "--activity-id", "recorrect", "--process-pid", "12345", "--once"]), 0)
        tick.assert_called_once()
        sleep.assert_not_called()


if __name__ == "__main__":
    unittest.main()
