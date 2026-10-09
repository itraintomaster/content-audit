import tempfile
import unittest
import json
from argparse import Namespace
from datetime import datetime, timezone
from pathlib import Path

import monitor_analysis_activity


class AnalysisActivityMonitorTest(unittest.TestCase):
    def test_distractor_quality_has_an_independent_ledger_and_label(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            evaluations = Path(directory)
            quality = evaluations / "choice-quality-judge.jsonl"
            validity = evaluations / "choice-alternative-judge.jsonl"
            quality.write_text("old\n", encoding="utf-8")
            baseline = quality.stat().st_size
            quality.write_text("old\nnew-1\nnew-2\npartial", encoding="utf-8")
            validity.write_text("unrelated-1\nunrelated-2\nunrelated-3\n", encoding="utf-8")
            current = monitor_analysis_activity.metrics(evaluations,
                    {"multiple-choice-quality": 5}, {quality.name: baseline})
            self.assertEqual(current.completed, 2)
            self.assertEqual(current.total, 5)
            self.assertEqual(current.current.label, "Calidad de distractores")

    def test_finished_process_requires_a_new_persisted_closure(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            workdir = Path(directory)
            run = workdir / ".content-audit/activity/runs/analysis.json"
            run.parent.mkdir(parents=True)
            run.write_text(json.dumps({"batchId": "batch", "startedAt": "2026-10-08T12:00:00Z"}))
            batch = workdir / ".content-audit/batches/batch.json"
            batch.parent.mkdir(parents=True)
            args = Namespace(workdir=workdir, campaign_workdir=workdir, activity_id="analysis")
            batch.write_text(json.dumps({"closings": [{"closedAt": "2026-10-08T11:00:00Z"}]}))
            self.assertFalse(monitor_analysis_activity._has_persisted_closure(args))
            batch.write_text(json.dumps({"closings": [{"closedAt": "2026-10-08T13:00:00Z"}]}))
            self.assertTrue(monitor_analysis_activity._has_persisted_closure(args))

    def test_counts_only_records_appended_after_the_baseline(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            evaluations = Path(directory)
            instruction = evaluations / "quiz-instruction-validator.jsonl"
            translation = evaluations / "translation-sense-from-english.jsonl"
            instruction.write_text("old-1\nold-2\n", encoding="utf-8")
            translation.write_text("old-translation\n", encoding="utf-8")
            baselines = {
                instruction.name: instruction.stat().st_size,
                translation.name: translation.stat().st_size,
            }

            with instruction.open("a", encoding="utf-8") as target:
                target.write("new-1\nnew-2\n")
            with translation.open("a", encoding="utf-8") as target:
                target.write("new-1\nnew-2\nnew-3\n")

            current = monitor_analysis_activity.metrics(
                evaluations,
                {"quiz-instruction": 5, "translation-sense": 10},
                baselines,
            )

            self.assertEqual(current.completed, 5)
            self.assertEqual(current.total, 15)
            self.assertEqual(current.current.judge, "translation-sense")
            self.assertIn("5 consultas nuevas", monitor_analysis_activity.summary(current))
            self.assertIn("Traducción que no corresponde", monitor_analysis_activity.summary(current))

            quiet = monitor_analysis_activity.summary(
                current,
                now=current.last_result_at + 61,
                process_alive=True,
                settle_after=20,
            )
            self.assertIn("El proceso sigue activo", quiet)
            self.assertIn("máximo estimado, no una meta obligatoria", quiet)
            self.assertIn("hace 1 min", quiet)

    def test_does_not_count_an_unfinished_jsonl_record(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            evaluations = Path(directory)
            ledger = evaluations / "mini-theory-judge.jsonl"
            ledger.write_bytes(b'{"old":true}\n')
            baseline = ledger.stat().st_size
            with ledger.open("ab") as target:
                target.write(b'{"partial":true}')

            current = monitor_analysis_activity.metrics(
                evaluations,
                {"mini-theory": 3},
                {ledger.name: baseline},
            )

            self.assertEqual(current.completed, 0)
            self.assertIn("Preparando Miniteoría", monitor_analysis_activity.summary(current))

    def test_recovers_the_baseline_from_the_activity_start(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            ledger = Path(directory) / "single-answer-judge.jsonl"
            ledger.write_text(
                '{"recordedAt":"2026-10-07T23:59:00Z"}\n'
                '{"recordedAt":"2026-10-08T00:02:19Z"}\n'
                '{"recordedAt":"2026-10-08T00:03:00Z"}\n',
                encoding="utf-8",
            )

            baseline = monitor_analysis_activity.baseline_byte_at(
                ledger,
                datetime(2026, 10, 8, 0, 2, 18, tzinfo=timezone.utc),
            )
            records, _ = monitor_analysis_activity._appended_records(
                ledger,
                baseline,
            )

            self.assertEqual(records, 2)

    def test_reads_the_latest_attribution_marker_and_explains_real_local_progress(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "analysis.log"
            output.write_text(
                "ordinary output\n"
                '[stderr] @@CONTENT_AUDIT_PROGRESS {"phase":"ATTRIBUTING_REGRESSIONS","completed":0,"total":965}\n'
                '[stderr] @@CONTENT_AUDIT_PROGRESS {"phase":"ATTRIBUTING_REGRESSIONS","completed":73,"total":965}\n',
                encoding="utf-8",
            )

            attribution = monitor_analysis_activity.attribution_progress(output)

            self.assertIsNotNone(attribution)
            self.assertEqual(attribution.completed, 73)
            self.assertEqual(attribution.total, 965)
            current = monitor_analysis_activity.AnalysisMetrics(1984, 1988, None, ())
            explanation = monitor_analysis_activity.summary(
                current,
                process_alive=True,
                attribution=attribution,
            )
            self.assertIn("73 de 965 cambios", explanation)
            self.assertIn("dejando cada uno afuera una vez", explanation)


if __name__ == "__main__":
    unittest.main()
