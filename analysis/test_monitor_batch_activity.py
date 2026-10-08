import json
import os
import tempfile
import unittest
from argparse import Namespace
from pathlib import Path
from unittest.mock import patch

import activity_run
import monitor_batch_activity


class MonitorBatchActivityTest(unittest.TestCase):
    def test_old_open_checkpoint_does_not_make_a_failed_launch_successful(self):
        with tempfile.TemporaryDirectory() as directory:
            workdir = Path(directory)
            batch = workdir / ".content-audit/batches/batch.json"
            batch.parent.mkdir(parents=True)
            batch.write_text(json.dumps({"status": "OPEN", "taskIds": ["t1"]}))
            os.utime(batch, (1, 1))
            path = activity_run.start(Namespace(
                workdir=workdir, id="failed-launch", kind="COURSE_CORRECTION", title="Launch",
                scope="A1", provider="codex-cli", model="gpt-6.1-sol", reasoning_effort="medium",
                version="v11", total=10, unit="consultas", summary=None, artifact=[],
            ))
            args = Namespace(workdir=workdir, campaign_workdir=workdir, batch_id="batch",
                activity_id="failed-launch", process_pid=1234, baseline_queries=0, total=10,
                baseline_tasks_attempted=0, baseline_proposals=0, baseline_resolved=0,
                baseline_no_winner=0, interval=0, heartbeat=0, max_missing_ticks=1)
            with patch.object(activity_run, "process_is_alive", return_value=False):
                monitor_batch_activity.monitor(args)
            self.assertEqual(json.loads(path.read_text())["status"], "FAILED")

    def test_dead_process_unlocks_recovery_without_claiming_success(self):
        with tempfile.TemporaryDirectory() as directory:
            workdir = Path(directory)
            path = activity_run.start(Namespace(
                workdir=workdir, id="dead", kind="COURSE_CORRECTION", title="Dead",
                scope="A1", provider="codex-cli", model="gpt-6.1-sol", reasoning_effort="medium",
                version="v11", total=10, unit="consultas", summary=None, artifact=[],
            ))
            batch = workdir / ".content-audit/batches/batch.json"
            batch.parent.mkdir(parents=True)
            batch.write_text(json.dumps({"status": "RUNNING", "taskIds": ["t1"],
                                         "outcomes": [{"kind": "NOT_ATTEMPTED"}]}))
            args = Namespace(workdir=workdir, campaign_workdir=workdir, batch_id="batch",
                activity_id="dead", process_pid=1234, baseline_queries=0, total=10,
                baseline_tasks_attempted=0, baseline_proposals=0, baseline_resolved=0,
                baseline_no_winner=0, interval=0, heartbeat=0, max_missing_ticks=1)
            with patch.object(activity_run, "process_is_alive", return_value=False):
                self.assertEqual(monitor_batch_activity.monitor(args), 0)
            record = json.loads(path.read_text())
            self.assertEqual(record["status"], "FAILED")
            self.assertIn("no está activo", record["summary"])
            self.assertIn("Reanudar la misma", record["next"])
            self.assertIn("finishedAt", record)

    def test_metrics_y_resumen_explican_el_avance_desde_el_inicio(self):
        batch = {
            "status": "RUNNING",
            "taskIds": ["t1", "t2", "t3", "t4", "t5"],
            "outcomes": [
                {"taskId": "t1", "kind": "RESOLVED_BY_RULE"},
                {"taskId": "t2", "kind": "RESOLVED_BY_PANEL"},
                {"taskId": "t3", "kind": "NO_WINNER"},
                {"taskId": "t4", "kind": "NOT_ATTEMPTED"},
                {"taskId": "t5", "kind": "NOT_ATTEMPTED"},
            ],
            "quizProposals": [{"proposalId": "p1"}, {"proposalId": "p2"}],
            "panelDecisions": [{"queriesSpent": 4}, {"queriesSpent": 3}],
        }

        current = monitor_batch_activity.metrics(batch)
        message = monitor_batch_activity.summary(
            current,
            Namespace(
                baseline_queries=5,
                baseline_tasks_attempted=1,
                baseline_proposals=1,
                baseline_resolved=1,
                baseline_no_winner=0,
            ),
        )

        self.assertEqual(current.tasks_attempted, 3)
        self.assertEqual(current.queries, 7)
        self.assertIn("+2 tareas trabajadas", message)
        self.assertIn("+1 quizzes con propuesta", message)
        self.assertIn("2 consultas nuevas", message)
        self.assertIn("Resueltas: +1", message)
        self.assertIn("sin ganador: +1", message)

    def test_el_monitor_cierra_la_actividad_aunque_la_pantalla_ya_no_exista(self):
        with tempfile.TemporaryDirectory() as temporary:
            workdir = Path(temporary)
            activity_run.start(
                Namespace(
                    workdir=workdir,
                    id="viva",
                    kind="COURSE_CORRECTION",
                    title="Corrección viva",
                    scope="A1",
                    provider="codex-cli",
                    model="gpt-6.1-sol",
                    reasoning_effort="medium",
                    version="v11",
                    total=10,
                    unit="consultas",
                    summary=None,
                    artifact=[],
                )
            )
            args = Namespace(
                workdir=workdir,
                activity_id="viva",
                baseline_queries=5,
                baseline_tasks_attempted=1,
                baseline_proposals=1,
                baseline_resolved=1,
                baseline_no_winner=0,
            )

            monitor_batch_activity._finish(
                monitor_batch_activity.BatchMetrics(
                    status="OPEN",
                    tasks=5,
                    tasks_attempted=3,
                    proposals=2,
                    queries=7,
                    resolved=2,
                    no_winner=1,
                ),
                {"status": "OPEN"},
                args,
            )

            record = json.loads(
                (
                    workdir
                    / ".content-audit/activity/runs/viva.json"
                ).read_text()
            )
            self.assertEqual(record["status"], "NEEDS_REVIEW")
            self.assertEqual(record["progress"]["completed"], 2)
            self.assertIn("terminó técnicamente", record["summary"])
            self.assertIn("finishedAt", record)


if __name__ == "__main__":
    unittest.main()
