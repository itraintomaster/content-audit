import unittest

from progress_estimate import ProgressEstimator


class ProgressEstimatorTest(unittest.TestCase):
    def test_waits_for_two_real_advances_before_publishing_an_eta(self) -> None:
        estimator = ProgressEstimator(minimum_elapsed=0)

        self.assertEqual(estimator.observe(0, 100, now=0).status, "CALCULATING")
        self.assertEqual(estimator.observe(10, 100, now=10).status, "CALCULATING")
        estimate = estimator.observe(20, 100, now=20)

        self.assertEqual(estimate.status, "AVAILABLE")
        self.assertAlmostEqual(estimate.rate_per_minute, 60.0)
        self.assertEqual(estimate.remaining_seconds_low, 52)
        self.assertEqual(estimate.remaining_seconds_high, 124)

    def test_marks_a_rate_stale_instead_of_counting_down_without_progress(self) -> None:
        estimator = ProgressEstimator(minimum_elapsed=0, stale_after=30)
        estimator.observe(0, 100, now=0)
        estimator.observe(10, 100, now=10)
        estimator.observe(20, 100, now=20)

        estimate = estimator.observe(20, 100, now=51)

        self.assertEqual(estimate.status, "STALE")
        self.assertIsNone(estimate.remaining_seconds_low)
        self.assertIsNone(estimate.remaining_seconds_high)

    def test_marks_exact_completion_without_needing_rate_samples(self) -> None:
        estimator = ProgressEstimator()

        estimate = estimator.observe(12, 12, now=3)

        self.assertEqual(estimate.status, "COMPLETE")
        self.assertEqual(estimate.remaining_seconds_low, 0)
        self.assertEqual(estimate.remaining_seconds_high, 0)


if __name__ == "__main__":
    unittest.main()
