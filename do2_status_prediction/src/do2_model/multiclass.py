from __future__ import annotations

import json
from pathlib import Path

import pandas as pd

from do2_model.features import normalize_status


def filter_multiclass_status_rows(frame: pd.DataFrame) -> pd.DataFrame:
    if "status" not in frame:
        raise KeyError("status is required for multiclass training/evaluation")
    normalized = normalize_status(frame["status"])
    return frame.loc[normalized.ne("")].reset_index(drop=True)


def make_multiclass_status_target(frame: pd.DataFrame) -> pd.Series:
    if "status" not in frame:
        raise KeyError("status is required to create the multiclass target")
    target = normalize_status(frame["status"])
    if target.eq("").any():
        raise ValueError("Blank statuses must be filtered before multiclass target creation")
    return target.astype(str)


def dropped_multiclass_status_report(frame: pd.DataFrame) -> pd.DataFrame:
    if "status" not in frame:
        raise KeyError("status is required for multiclass status reporting")
    normalized = normalize_status(frame["status"]).replace("", "__BLANK__")
    report = normalized.value_counts(dropna=False).rename_axis("status").reset_index(name="rows")
    report["used_for_multiclass_training"] = report["status"].ne("__BLANK__")
    report["used_for_multiclass_training"] = report["used_for_multiclass_training"].astype(object)
    return report.sort_values(["used_for_multiclass_training", "rows"], ascending=[True, False])


def write_json(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2, default=str)
