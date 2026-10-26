from .aadhaar import check_duplicate_aadhaar
from .models import BlockingCheck


def build_default_registry() -> tuple[BlockingCheck, ...]:
    return (
        BlockingCheck(
            check_id="I_DUPLICATE_AADHAAR",
            target_status="I",
            reason_family="aadhaar already used by another army number or relation",
            applicable_do2_types=frozenset({"UID", "BIOMET"}),
            required_sources=("do2", "employee", "family_detail"),
            required_columns=(
                "do2.fk_employee",
                "do2.description",
                "do2.column_a",
                "do2.column_c",
                "do2.qe",
                "employee.id",
                "employee.army_no",
                "employee.uid_no",
                "employee.current_record",
                "employee.qe",
                "family_detail.fk_employee",
                "family_detail.uid_no",
                "family_detail.record_status",
                "family_detail.relationship",
            ),
            evaluator=check_duplicate_aadhaar,
        ),
    )
