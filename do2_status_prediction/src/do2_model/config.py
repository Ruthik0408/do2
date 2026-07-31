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
    db_date_column: str = os.getenv("DB_DATE_COLUMN", "do2_date")
    chunk_size: int = int(os.getenv("DB_CHUNK_SIZE", "100000"))
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
    "train": ("2021-01-01", "2025-01-01"),
    "validation": ("2025-01-01", "2025-07-01"),
    "test": ("2025-07-01", "2025-12-31"),
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
}


def load_feature_config(path: Path | None = None) -> dict[str, Any]:
    config_path = path or Settings().feature_config_path
    with config_path.open("r", encoding="utf-8") as handle:
        return json.load(handle)
