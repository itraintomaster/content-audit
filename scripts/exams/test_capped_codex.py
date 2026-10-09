import concurrent.futures
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


class CappedCodexTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="choice-quality-cap-test-")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.fake = self.root / "fake"
        self.fake.write_text("#!/bin/sh\nprintf 'fake-runtime\\n'\n")
        self.fake.chmod(0o755)
        self.env = dict(os.environ,
                        CONTENT_AUDIT_CODEX_REAL_BINARY=str(self.fake),
                        CONTENT_AUDIT_EXAM_RUNTIME_WRAPPER=str(self.fake),
                        CONTENT_AUDIT_EXAM_CAP_DIR=str(self.root),
                        CONTENT_AUDIT_EXAM_PHYSICAL_CAP="2")
        self.script = Path(__file__).with_name("capped-codex")

    def call(self, action="exec"):
        return subprocess.run([str(self.script), action], env=self.env,
                              capture_output=True, text=True)

    def test_durable_ceiling_is_shared_by_concurrent_calls(self):
        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
            results = list(pool.map(lambda _: self.call(), range(12)))
        self.assertEqual(2, sum(r.returncode == 0 for r in results))
        rows = [json.loads(line) for line in
                (self.root / "physical-calls.jsonl").read_text().splitlines()]
        self.assertEqual([1, 2], [row["ordinal"] for row in rows])
        self.assertNotEqual(0, self.call().returncode)

    def test_login_readiness_does_not_spend_and_partial_launch_does(self):
        self.assertEqual(0, self.call("login").returncode)
        self.assertFalse((self.root / "physical-calls.jsonl").exists())
        (self.root / "physical-calls.jsonl").write_text('{"uncertain":')
        self.assertEqual(0, self.call().returncode)
        self.assertNotEqual(0, self.call().returncode)

    def test_case_metadata_is_durable_but_does_not_replenish_the_ceiling(self):
        self.env.update(CONTENT_AUDIT_EXAM_EXPERIMENT_ID="cycle-test", CONTENT_AUDIT_EXAM_CASE_ID="1")
        self.assertEqual(0, self.call().returncode)
        self.env["CONTENT_AUDIT_EXAM_CASE_ID"] = "2"
        self.assertEqual(0, self.call().returncode)
        self.assertNotEqual(0, self.call().returncode)
        rows = [json.loads(line) for line in (self.root / "physical-calls.jsonl").read_text().splitlines()]
        self.assertEqual(["1", "2"], [row["caseId"] for row in rows])
        self.assertTrue(all(row["experimentId"] == "cycle-test" for row in rows))


if __name__ == "__main__":
    unittest.main()
