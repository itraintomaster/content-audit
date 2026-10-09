import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location("cycle", Path(__file__).with_name("run_choice_correction_cycle.py"))
cycle = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cycle)


class CycleGuardTest(unittest.TestCase):
    def test_authorization_and_uncertain_launches_do_not_replenish_budget(self):
        with tempfile.TemporaryDirectory() as tmp:
            directory = Path(tmp)
            auth = json.loads((cycle.EXPERIMENT / "authorization.json").read_text())
            cycle.save(directory / "authorization.json", auth)
            self.assertEqual(50, cycle.validate_auth(directory)["physicalCallCap"])
            (directory / "physical-calls.jsonl").write_text("{}\n" * 49 + '{"uncertain":')
            self.assertEqual(50, cycle.physical_count(directory))
            with self.assertRaises(ValueError):
                cycle.validate_auth(directory)

    def test_different_provider_or_implicit_budget_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            directory = Path(tmp)
            auth = json.loads((cycle.EXPERIMENT / "authorization.json").read_text())
            for key, value in [("provider", "claude"), ("physicalCallCap", 51), ("userReply", "")]:
                wrong = dict(auth, **{key: value})
                cycle.save(directory / "authorization.json", wrong)
                with self.assertRaises(ValueError):
                    cycle.validate_auth(directory)


if __name__ == "__main__":
    unittest.main()
