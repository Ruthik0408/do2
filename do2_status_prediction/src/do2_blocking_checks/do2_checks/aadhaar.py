from typing import Any, Mapping

from .models import CheckResult, CheckState


CHECK_ID = "I_DUPLICATE_AADHAAR"
REASON_FAMILY = "aadhaar already used by another army number or relation"

EMPLOYEE_DUPLICATE_SQL = """
SELECT e.id, e.army_no
FROM employee e
WHERE e.current_record IS TRUE
  AND e.qe = %s
  AND BTRIM(e.uid_no) = %s
  AND e.id <> %s
LIMIT 1
"""

FAMILY_DUPLICATE_SQL = """
SELECT fd.id, owner.army_no
FROM family_detail fd
JOIN employee owner ON owner.id = fd.fk_employee
WHERE fd.record_status = 'V'
  AND fd.uid_no IS NOT NULL
  AND fd.uid_no = %s
  AND NOT (
      fd.fk_employee = %s
      AND %s IS NOT NULL
      AND fd.relationship IS NOT NULL
      AND (
          LOWER(%s) = LOWER(fd.relationship)
          OR (LOWER(%s) = 'wife' AND LOWER(fd.relationship) = 'spouse')
          OR (LOWER(%s) = 'spouse' AND LOWER(fd.relationship) = 'wife')
      )
  )
ORDER BY fd.uid_no
LIMIT 1
"""


def _unavailable(message: str) -> CheckResult:
    return CheckResult(
        check_id=CHECK_ID,
        state=CheckState.UNAVAILABLE,
        reason_family=REASON_FAMILY,
        unavailable_reason=message,
    )


def check_duplicate_aadhaar(cursor: Any, bill: Mapping[str, Any]) -> CheckResult:
    """Reproduce the UID/BIOMET cross-person duplicate-Aadhaar blocker."""
    employee_id = bill.get("fk_employee")
    uid_no = str(bill.get("column_c") or "").strip()
    qe = str(bill.get("qe") or "").strip()
    relationship = bill.get("column_a")

    missing = [
        name
        for name, value in (("fk_employee", employee_id), ("column_c", uid_no), ("qe", qe))
        if value in (None, "")
    ]
    if missing:
        return _unavailable("Missing required bill values: " + ", ".join(missing))

    try:
        cursor.execute(EMPLOYEE_DUPLICATE_SQL, (qe, uid_no, employee_id))
        duplicate_employee = cursor.fetchone()
        if duplicate_employee:
            return CheckResult(
                check_id=CHECK_ID,
                state=CheckState.BLOCKED,
                target_status="I",
                reason_family=REASON_FAMILY,
                evidence={
                    "match_source": "employee.uid_no",
                    "matched_employee_id": duplicate_employee[0],
                    "matched_army_no": duplicate_employee[1],
                },
            )

        cursor.execute(
            FAMILY_DUPLICATE_SQL,
            (
                uid_no,
                employee_id,
                relationship,
                relationship,
                relationship,
                relationship,
            ),
        )
        duplicate_relation = cursor.fetchone()
        if duplicate_relation:
            return CheckResult(
                check_id=CHECK_ID,
                state=CheckState.BLOCKED,
                target_status="I",
                reason_family=REASON_FAMILY,
                evidence={
                    "match_source": "family_detail.uid_no",
                    "matched_family_detail_id": duplicate_relation[0],
                    "matched_army_no": duplicate_relation[1],
                },
            )
    except Exception as exc:
        return _unavailable(f"Database check failed: {type(exc).__name__}: {exc}")

    return CheckResult(
        check_id=CHECK_ID,
        state=CheckState.PASSED,
        reason_family=REASON_FAMILY,
        evidence={"checked_qe": qe},
    )
