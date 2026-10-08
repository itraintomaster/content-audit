import json
import tempfile
import unittest
from pathlib import Path

from activity_feed import build_feed, write_feed


class ActivityFeedTest(unittest.TestCase):
    def test_consolidates_the_latest_coverage_for_each_judge(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            digests = root / ".content-audit" / "audit-digests"
            digests.mkdir(parents=True)
            self._digest(digests / "audit-2026-10-06T10-00-00.json", "a", 2)
            self._digest(digests / "audit-2026-10-06T11-00-00.json", "b", 5)
            self._digest(digests / "audit-2026-10-06T12-00-00.json", "a", 7)

            feed = build_feed(root, [])

            self.assertEqual(
                [(item["analyzer"], item["evaluated"]) for item in feed["coverage"]],
                [("a", 7), ("b", 5)],
            )
            self.assertEqual(feed["coverage"][0]["sourceAuditId"], "2026-10-06T12-00-00")

    def test_local_running_record_wins_and_becomes_current(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            versioned = root / "versioned"
            local = root / ".content-audit" / "activity" / "runs"
            versioned.mkdir(parents=True)
            local.mkdir(parents=True)
            (versioned / "run.json").write_text(
                json.dumps({"id": "r1", "status": "FAILED", "updatedAt": "2026-10-06T10:00:00Z"}),
                encoding="utf-8",
            )
            (local / "run.json").write_text(
                json.dumps({"id": "r1", "status": "RUNNING", "updatedAt": "2026-10-06T11:00:00Z"}),
                encoding="utf-8",
            )

            feed = build_feed(root, [versioned])

            self.assertEqual(feed["currentRunId"], "r1")
            self.assertEqual(feed["runs"][0]["status"], "RUNNING")

    def test_write_is_valid_and_atomic_shape(self):
        with tempfile.TemporaryDirectory() as raw:
            output = Path(raw) / "nested" / "activity.json"
            write_feed({"schemaVersion": 1, "runs": []}, output)
            self.assertEqual(json.loads(output.read_text())["schemaVersion"], 1)
            self.assertFalse(output.with_suffix(".json.tmp").exists())

    @staticmethod
    def _digest(path: Path, analyzer: str, evaluated: int):
        path.write_text(
            json.dumps(
                {
                    "auditId": path.stem.removeprefix("audit-"),
                    "judgeCoverages": [
                        {
                            "judgeName": analyzer,
                            "unit": "QUIZ",
                            "reached": 10,
                            "evaluated": evaluated,
                            "notEvaluated": 10 - evaluated,
                            "standing": {"status": "APPROVED"},
                        }
                    ],
                }
            ),
            encoding="utf-8",
        )


if __name__ == "__main__":
    unittest.main()
