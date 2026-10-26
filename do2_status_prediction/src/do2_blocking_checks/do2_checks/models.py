from dataclasses import dataclass, field
from enum import Enum
from typing import Any, Callable, Mapping, Sequence


class CheckState(str, Enum):
    BLOCKED = "BLOCKED"
    PASSED = "PASSED"
    UNAVAILABLE = "UNAVAILABLE"


@dataclass(frozen=True)
class CheckResult:
    check_id: str
    state: CheckState
    target_status: str | None = None
    reason_family: str | None = None
    evidence: Mapping[str, Any] = field(default_factory=dict)
    unavailable_reason: str | None = None


Evaluator = Callable[[Any, Mapping[str, Any]], CheckResult]


@dataclass(frozen=True)
class BlockingCheck:
    check_id: str
    target_status: str
    reason_family: str
    applicable_do2_types: frozenset[str]
    required_sources: tuple[str, ...]
    required_columns: tuple[str, ...]
    evaluator: Evaluator

    def is_applicable(self, bill: Mapping[str, Any]) -> bool:
        return str(bill.get("description") or "").strip().upper() in self.applicable_do2_types


@dataclass(frozen=True)
class Decision:
    predicted_status: str
    results: Sequence[CheckResult]
    blocking_check_id: str | None = None
    reason_family: str | None = None

