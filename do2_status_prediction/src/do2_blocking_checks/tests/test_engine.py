import unittest

from do2_checks.engine import BlockingCheckEngine
from do2_checks.registry import build_default_registry


class FakeCursor:
    def __init__(self, rows):
        self.rows = iter(rows)

    def execute(self, _sql, _params):
        pass

    def fetchone(self):
        return next(self.rows)


class EngineTests(unittest.TestCase):
    def setUp(self):
        self.engine = BlockingCheckEngine(build_default_registry())
        self.bill = {
            "fk_employee": 10,
            "description": "UID",
            "column_a": "wife",
            "column_c": "123412341234",
            "qe": "01/2026",
        }

    def test_employee_duplicate_is_invalid(self):
        result = self.engine.evaluate(FakeCursor([(99, "JC999")]), self.bill)
        self.assertEqual("I", result.predicted_status)

    def test_family_duplicate_is_invalid(self):
        result = self.engine.evaluate(FakeCursor([None, (77, "JC777")]), self.bill)
        self.assertEqual("I", result.predicted_status)

    def test_all_available_checks_passed_is_valid(self):
        result = self.engine.evaluate(FakeCursor([None, None]), self.bill)
        self.assertEqual("V", result.predicted_status)

    def test_missing_required_value_is_review(self):
        bill = dict(self.bill, column_c="")
        result = self.engine.evaluate(FakeCursor([]), bill)
        self.assertEqual("REVIEW", result.predicted_status)

    def test_unknown_do2_type_is_review(self):
        bill = dict(self.bill, description="INCR")
        result = self.engine.evaluate(FakeCursor([]), bill)
        self.assertEqual("REVIEW", result.predicted_status)


if __name__ == "__main__":
    unittest.main()
