from __future__ import annotations

from dataclasses import dataclass

import numpy as np
import pandas as pd

from do2_model.config import LEAKAGE_COLUMNS, load_feature_config

MISSING_CATEGORY = "__MISSING__"


@dataclass
class FeatureResult:
    frame: pd.DataFrame
    categorical_columns: list[str]


def normalize_status(series: pd.Series) -> pd.Series:
    return (
        series.fillna("")
        .astype("string")
        .str.replace('"', "", regex=False)
        .str.strip()
        .str.upper()
    )


def make_target_non_valid(frame: pd.DataFrame) -> pd.Series:
    if "status" not in frame:
        raise KeyError("status is required to create the training target")
    normalized = normalize_status(frame["status"])
    if normalized.eq("").any():
        raise ValueError("Blank/null status rows must be excluded before target construction.")
    return normalized.ne("V").astype("int8")


def supervised_status_mask(frame: pd.DataFrame) -> pd.Series:
    if "status" not in frame:
        raise KeyError("status is required for supervised training/evaluation")
    return normalize_status(frame["status"]).ne("")


def filter_supervised_rows(frame: pd.DataFrame) -> pd.DataFrame:
    return frame.loc[supervised_status_mask(frame)].reset_index(drop=True)


def actual_status_group(frame: pd.DataFrame) -> pd.Series:
    normalized = normalize_status(frame["status"])
    return normalized.where(normalized.eq("V"), "NON_VALID")


def leakage_columns_present(columns: list[str] | pd.Index) -> list[str]:
    present = set(columns)
    return sorted(column for column in LEAKAGE_COLUMNS if column in present)


def _clean_category(series: pd.Series) -> pd.Series:
    cleaned = series.astype("string").str.strip().str.upper()
    return cleaned.fillna(MISSING_CATEGORY).replace("", MISSING_CATEGORY).astype(str)


def _present(series: pd.Series) -> pd.Series:
    return (
        series.notna()
        & series.astype("string").str.strip().fillna("").ne("")
    ).astype("int8")


def _date(frame: pd.DataFrame, column: str) -> pd.Series:
    if column not in frame:
        return pd.Series(pd.NaT, index=frame.index, dtype="datetime64[ns]")
    try:
        return pd.to_datetime(frame[column], errors="coerce")
    except ValueError:
        return pd.to_datetime(frame[column].astype("string"), errors="coerce")


def build_features(
    frame: pd.DataFrame,
    *,
    expected_columns: list[str] | None = None,
    expected_categorical: list[str] | None = None,
) -> FeatureResult:
    config = load_feature_config()
    result = pd.DataFrame(index=frame.index)
    categorical: list[str] = []
    leakage_present = leakage_columns_present(frame.columns)
    feature_columns = set(config["categorical_base"]) | set(config["numeric_base"])
    leaking_features = sorted(feature_columns & set(leakage_present))
    if leaking_features:
        raise RuntimeError(f"Leakage columns configured as model features: {leaking_features}")

    for column in config["categorical_base"]:
        if column in frame:
            result[column] = _clean_category(frame[column])
            categorical.append(column)

    for column in config["numeric_base"]:
        if column in frame:
            result[column] = pd.to_numeric(frame[column], errors="coerce").astype("float32")

    do2_date = _date(frame, "do2_date")
    from_date = _date(frame, "from_date")
    to_date = _date(frame, "to_date")
    created_at = _date(frame, "created_at")
    cfa_date = _date(frame, "cfa_sanction_date")

    result["duration_days"] = (to_date - from_date).dt.days.astype("float32")
    result["submission_delay_days"] = (
        created_at.dt.normalize() - do2_date.dt.normalize()
    ).dt.days.astype("float32")
    result["cfa_to_do2_days"] = (do2_date - cfa_date).dt.days.astype("float32")

    result["do2_month"] = do2_date.dt.month.astype("float32")
    result["do2_day_of_week"] = do2_date.dt.dayofweek.astype("float32")
    result["do2_day"] = do2_date.dt.day.astype("float32")
    result["created_month"] = created_at.dt.month.astype("float32")
    result["created_day_of_week"] = created_at.dt.dayofweek.astype("float32")

    presence_map = {
        "has_to_date": ("to_date", None),
        "has_rate": ("rate", None),
        "has_reference_do2": ("ref_do2_no", "has_reference_do2_source"),
        "has_attachment": ("attachment", "has_attachment_source"),
        "has_cfa_letter": ("cfa_sanction_letter_no", "has_cfa_letter_source"),
        "has_pan": ("pan", "has_pan_source"),
        "has_pto_remarks": ("pto_remarks", "has_pto_remarks_source"),
        "has_column9_remarks": ("column9_remarks", "has_column9_remarks_source"),
        "has_punishment": ("punishment", None),
    }
    for feature_name, (source_column, precomputed_column) in presence_map.items():
        if precomputed_column and precomputed_column in frame:
            result[feature_name] = (
                pd.to_numeric(frame[precomputed_column], errors="coerce")
                .fillna(0)
                .gt(0)
                .astype("int8")
            )
        elif source_column in frame:
            result[feature_name] = _present(frame[source_column])
        else:
            result[feature_name] = np.int8(0)

    result["from_after_to"] = (
        from_date.notna() & to_date.notna() & from_date.gt(to_date)
    ).astype("int8")
    result["cfa_after_do2"] = (
        cfa_date.notna() & do2_date.notna() & cfa_date.gt(do2_date)
    ).astype("int8")
    result["do2_date_missing"] = do2_date.isna().astype("int8")
    result["from_date_missing"] = from_date.isna().astype("int8")

    if expected_columns is not None:
        expected_categorical_set = set(expected_categorical or [])
        for column in expected_columns:
            if column not in result:
                if column in expected_categorical_set:
                    result[column] = MISSING_CATEGORY
                else:
                    result[column] = np.nan
        result = result[expected_columns]
        categorical = [column for column in expected_columns if column in expected_categorical_set]

    if result.empty:
        raise RuntimeError("No model features could be built from the supplied columns.")

    return FeatureResult(frame=result, categorical_columns=categorical)
