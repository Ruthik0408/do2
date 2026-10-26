from typing import Any, Iterable, Mapping

from .models import BlockingCheck, CheckState, Decision


class BlockingCheckEngine:
    def __init__(self, checks: Iterable[BlockingCheck]):
        self.checks = tuple(checks)

    def evaluate(self, cursor: Any, bill: Mapping[str, Any]) -> Decision:
        applicable = [check for check in self.checks if check.is_applicable(bill)]
        if not applicable:
            return Decision(predicted_status="REVIEW", results=())

        results = tuple(check.evaluator(cursor, bill) for check in applicable)

        for result in results:
            if result.state is CheckState.BLOCKED:
                return Decision(
                    predicted_status=result.target_status or "REVIEW",
                    results=results,
                    blocking_check_id=result.check_id,
                    reason_family=result.reason_family,
                )

        if any(result.state is CheckState.UNAVAILABLE for result in results):
            return Decision(predicted_status="REVIEW", results=results)

        return Decision(predicted_status="V", results=results)

