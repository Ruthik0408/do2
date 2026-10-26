from __future__ import annotations

import json
import os
from dataclasses import dataclass
from pathlib import Path
from typing import Any

try:
    from dotenv import load_dotenv
except ImportError:  # Allows feature-only checks before project dependencies are installed.
    def load_dotenv(*_args, **_kwargs) -> bool:
        return False


PROJECT_ROOT = Path(__file__).resolve().parents[2]
load_dotenv(PROJECT_ROOT / ".env")


@dataclass(frozen=True)
class Settings:
    database_url: str | None = os.getenv("DATABASE_URL")
    db_schema: str = os.getenv("DB_SCHEMA", "public")
    db_table: str = os.getenv("DB_TABLE", "do2")
    db_date_column: str = os.getenv("DB_DATE_COLUMN", "created_at")
    chunk_size: int = int(os.getenv("DB_CHUNK_SIZE", "100000"))
    enable_rule_history_sql: bool = os.getenv("DB_ENABLE_RULE_HISTORY_SQL", "0") == "1"
    enable_family_identity_sql: bool = os.getenv("DB_ENABLE_FAMILY_IDENTITY_SQL", "0") == "1"
    enable_appendix_j_sql: bool = os.getenv("DB_ENABLE_APPENDIX_J_SQL", "1") == "1"
    random_seed: int = 42

    @property
    def data_root(self) -> Path:
        return PROJECT_ROOT / "data" / "raw"

    @property
    def model_root(self) -> Path:
        return PROJECT_ROOT / "models"

    @property
    def report_root(self) -> Path:
        return PROJECT_ROOT / "reports"

    @property
    def feature_config_path(self) -> Path:
        return PROJECT_ROOT / "config" / "feature_config.json"

    def ensure_output_directories(self) -> None:
        self.data_root.mkdir(parents=True, exist_ok=True)
        self.model_root.mkdir(parents=True, exist_ok=True)
        self.report_root.mkdir(parents=True, exist_ok=True)


SPLITS = {
    "train": ("2022-01-01", "2024-01-01"),
    "validation": ("2024-01-01", "2025-01-01"),
    "test": ("2025-01-01", "2026-01-01"),
}


# Columns unavailable at initial DO2 entry because they encode the final outcome,
# human review/approval decisions, rejection reasons, or post-processing results.
LEAKAGE_COLUMNS: dict[str, str] = {
    "status": "supervised label, never a model feature",
    "reason": "final rejection/decision reason",
    "sys_reason": "system-populated final decision reason",
    "change_reason": "post-decision correction reason",
    "rejection_type": "final rejection category",
    "approved": "approval outcome",
    "reviewed": "review outcome",
    "accepted_qe": "acceptance/review identifier",
    "review_qe": "review identifier",
    "fk_auditor": "auditor decision actor",
    "fk_aao": "AAO decision actor",
    "fk_ao": "AO decision actor",
    "fk_hoo": "HOO decision actor",
    "auditor_date": "auditor decision date",
    "aao_date": "AAO decision date",
    "ao_date": "AO decision date",
    "hoo_date": "HOO decision date",
    "processed_date": "post-processing date",
    "bulk_approval": "approval workflow result",
    "attachment_verified": "verification result",
    "udise_aud_verified": "verification result",
    "udise_aao_verified": "verification result",
    "udise_sao_verified": "verification result",
    "fk_remove_mr_ao": "MR removal decision actor",
    "fk_remove_mr_ao_date": "MR removal decision date",
    "mr_removal_reason": "MR removal decision reason",
    "fk_put_mr_ao": "MR placement decision actor",
    "fk_put_mr_ao_date": "MR placement decision date",
    "mr_put_reason": "MR placement decision reason",
    "fk_task": "unusable: never populated in current schema profile",
    "sms_sent_at": "unusable/workflow notification timestamp",
    "pan": "unusable: never populated in current schema profile",
    "sms_sent": "workflow notification likely triggered post-decision",
    "fk_usr": "possibly processing actor rather than submitter; treat as leakage until confirmed safe",
    "remarks": "free-text remarks may be reviewer-authored; treat as leakage until confirmed safe",
    "audit_cage": "audit/workflow flag; treat as leakage until confirmed safe at initial DO2 entry",
}


def load_feature_config(path: Path | None = None) -> dict[str, Any]:
    config_path = path or Settings().feature_config_path
    with config_path.open("r", encoding="utf-8") as handle:
        config = json.load(handle)

    candidates = config["candidate_columns"]
    if len(candidates) != len(set(candidates)):
        raise ValueError("candidate_columns contains duplicates")

    availability = config["column_availability"]
    allowed_sources = set(availability["entry_payload"]) | set(
        availability["system_at_entry"]
    ) | set(availability["target_only"])
    unexpected_candidates = sorted(set(candidates) - allowed_sources)
    if unexpected_candidates:
        raise ValueError(f"Candidate columns are unavailable at prediction time: {unexpected_candidates}")
    unknown_required = sorted(set(config["required_input_columns"]) - set(candidates))
    if unknown_required:
        raise ValueError(f"Required input columns are not extracted: {unknown_required}")

    forbidden = set(availability["outcome_or_workflow_forbidden"])
    configured_forbidden = set(config["excluded_leakage_columns"]) - {"status"}
    if forbidden != configured_forbidden:
        raise ValueError("outcome_or_workflow_forbidden and excluded_leakage_columns disagree")
    code_forbidden = set(LEAKAGE_COLUMNS) - {"status"}
    if forbidden != code_forbidden:
        raise ValueError("Feature configuration and Python leakage policy disagree")
    leaked_candidates = sorted((set(candidates) & forbidden) - {"status"})
    if leaked_candidates:
        raise ValueError(f"Post-entry columns configured as candidates: {leaked_candidates}")
    return config
