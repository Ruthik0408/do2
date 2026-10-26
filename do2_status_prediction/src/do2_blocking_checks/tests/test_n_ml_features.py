import unittest

from do2_checks.n_ml_features import (
    DEFAULT_NE_CUTOFF,
    fetch_n_ml_features,
    ne_cutoff_for_pao,
)


class NFeatureTests(unittest.TestCase):
    def test_known_pao_cutoffs(self):
        self.assertEqual("2014-01-01", ne_cutoff_for_pao("94"))
        self.assertEqual("2016-07-01", ne_cutoff_for_pao("86"))
        self.assertEqual("2020-01-01", ne_cutoff_for_pao("53"))

    def test_unknown_pao_uses_java_default(self):
        self.assertEqual(DEFAULT_NE_CUTOFF, ne_cutoff_for_pao("unknown"))

    def test_empty_ids_do_not_open_cursor(self):
        self.assertEqual([], fetch_n_ml_features(None, [], mode="historical", pao_code="94"))

    def test_realtime_requires_quarter(self):
        with self.assertRaises(ValueError):
            fetch_n_ml_features(None, [1], mode="realtime", pao_code="94")


if __name__ == "__main__":
    unittest.main()
