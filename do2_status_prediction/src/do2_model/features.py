from __future__ import annotations

from dataclasses import dataclass

import numpy as np
import pandas as pd

from do2_model.config import LEAKAGE_COLUMNS, load_feature_config

MISSING_CATEGORY = "__MISSING__"
VALID_STATUS = "V"
NON_VALID_TERMINAL_STATUSES = frozenset({"I", "MR", "M", "N", "B"})
EXCLUDED_NON_TERMINAL_STATUSES = frozenset({"", "P", "D", "C"})
KNOWN_ARMY_NO_PLACEHOLDERS = frozenset({"JC490821", "JC490969"})
PLACEHOLDER_DISTINCT_EMPLOYEE_FLOOR = 100
PRIOR_RATE_MIN_COUNT = 5
DATE_PLAUSIBILITY_START = pd.Timestamp("1980-01-01")


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
    unsupported = sorted(
        set(normalized.unique()) - ({VALID_STATUS} | NON_VALID_TERMINAL_STATUSES)
    )
    if unsupported:
        raise ValueError(
            "Only terminal supervised statuses can be used for target construction. "
            f"Filter unsupported statuses first: {unsupported}"
        )
    return normalized.isin(NON_VALID_TERMINAL_STATUSES).astype("int8")


def supervised_status_mask(frame: pd.DataFrame) -> pd.Series:
    if "status" not in frame:
        raise KeyError("status is required for supervised training/evaluation")
    return normalize_status(frame["status"]).isin({VALID_STATUS} | NON_VALID_TERMINAL_STATUSES)


def filter_supervised_rows(frame: pd.DataFrame) -> pd.DataFrame:
    return frame.loc[supervised_status_mask(frame)].reset_index(drop=True)


def actual_status_group(frame: pd.DataFrame) -> pd.Series:
    normalized = normalize_status(frame["status"])
    return normalized.where(normalized.eq(VALID_STATUS), "NON_VALID")


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


def _date_with_plausibility(
    frame: pd.DataFrame,
    column: str,
    *,
    upper_bound: pd.Timestamp | None = None,
) -> tuple[pd.Series, pd.Series]:
    parsed = _date(frame, column)
    if upper_bound is None:
        upper_bound = pd.Timestamp.today().normalize() + pd.Timedelta(days=30)
    present = column in frame and frame[column].notna() & frame[column].astype("string").str.strip().ne("")
    plausible = present & parsed.between(DATE_PLAUSIBILITY_START, upper_bound)
    return parsed.where(plausible), plausible.astype("int8")


def _army_no_placeholder_flag(frame: pd.DataFrame) -> pd.Series:
    if "army_no" not in frame:
        return pd.Series(np.int8(0), index=frame.index)
    army_no = _clean_category(frame["army_no"])
    placeholder = army_no.isin(KNOWN_ARMY_NO_PLACEHOLDERS)
    if "fk_employee" in frame:
        employee = _clean_category(frame["fk_employee"])
        distinct_employee_counts = employee.groupby(army_no, sort=False).transform("nunique")
        placeholder = placeholder | distinct_employee_counts.ge(PLACEHOLDER_DISTINCT_EMPLOYEE_FLOOR)
    return placeholder.fillna(False).astype("int8")


def _asof_prior_rate(
    *,
    key: pd.Series,
    event_date: pd.Series,
    target: pd.Series,
    eligible: pd.Series,
    global_rate: float,
    min_count: int = PRIOR_RATE_MIN_COUNT,
) -> pd.Series:
    output = pd.Series(np.float32(global_rate), index=target.index)
    valid = eligible & key.ne(MISSING_CATEGORY) & event_date.notna()
    if not valid.any():
        return output.astype("float32")

    working = pd.DataFrame(
        {
            "key": key[valid],
            "event_date": event_date[valid],
            "target": target[valid].astype("float32"),
            "_original_index": target.index[valid],
        }
    ).sort_values(["key", "event_date", "_original_index"])
    grouped = working.groupby("key", sort=False)
    prior_count = grouped.cumcount()
    prior_non_valid = grouped["target"].cumsum().sub(working["target"])
    prior_rate = prior_non_valid.div(prior_count.replace(0, np.nan))
    prior_rate = prior_rate.where(prior_count.ge(min_count), global_rate).astype("float32")
    output.loc[working["_original_index"]] = prior_rate.to_numpy()
    return output.astype("float32")


def _prior_rate_features(
    frame: pd.DataFrame,
    do2_date: pd.Series,
    army_no_is_placeholder: pd.Series,
) -> pd.DataFrame:
    result = pd.DataFrame(index=frame.index)
    for column in (
        "army_no_prior_non_valid_rate",
        "unit_code_prior_non_valid_rate",
        "do2_type_prior_non_valid_rate",
    ):
        result[column] = np.float32(0.0)
    if "status" not in frame:
        return result

    eligible_supervised = supervised_status_mask(frame)
    if not eligible_supervised.any():
        return result

    target = make_target_non_valid(frame.loc[eligible_supervised]).reindex(frame.index)
    global_rate = float(target.loc[eligible_supervised].mean())
    target = target.fillna(0).astype("float32")

    if "army_no" in frame:
        result["army_no_prior_non_valid_rate"] = _asof_prior_rate(
            key=_clean_category(frame["army_no"]),
            event_date=do2_date,
            target=target,
            eligible=eligible_supervised & army_no_is_placeholder.eq(0),
            global_rate=global_rate,
        )
    if "unit_code" in frame:
        result["unit_code_prior_non_valid_rate"] = _asof_prior_rate(
            key=_clean_category(frame["unit_code"]),
            event_date=do2_date,
            target=target,
            eligible=eligible_supervised,
            global_rate=global_rate,
        )
    if "do2_type" in frame:
        result["do2_type_prior_non_valid_rate"] = _asof_prior_rate(
            key=_clean_category(frame["do2_type"]),
            event_date=do2_date,
            target=target,
            eligible=eligible_supervised,
            global_rate=global_rate,
        )
    return result.astype("float32")


def _duplicate_open_do2_feature(
    frame: pd.DataFrame,
    created_at: pd.Series,
    from_date: pd.Series,
    to_date: pd.Series,
) -> pd.Series:
    if not {"army_no", "do2_type", "status"}.issubset(frame.columns):
        return pd.Series(np.int8(0), index=frame.index)

    working = pd.DataFrame(
        {
            "army_no": _clean_category(frame["army_no"]),
            "do2_type": _clean_category(frame["do2_type"]),
            "status": normalize_status(frame["status"]),
            "created_at": created_at,
            "from_date": from_date,
            "to_date": to_date,
            "_original_index": frame.index,
        }
    )
    valid_key = (
        working["army_no"].ne(MISSING_CATEGORY)
        & working["do2_type"].ne(MISSING_CATEGORY)
        & working["created_at"].notna()
    )
    output = pd.Series(np.int8(0), index=frame.index)
    if not valid_key.any():
        return output

    for _, group in working.loc[valid_key].sort_values(
        ["army_no", "do2_type", "created_at", "_original_index"]
    ).groupby(["army_no", "do2_type"], sort=False):
        prior_latest_to = pd.NaT
        prior_open_without_range = False
        pending_updates = []
        current_created_at = None

        for (
            _army_no,
            _do2_type,
            status,
            row_created_at,
            row_from_date,
            row_to_date,
            original_index,
        ) in group.itertuples(index=False, name=None):
            if current_created_at is None or row_created_at != current_created_at:
                for prior_to_date, prior_has_range in pending_updates:
                    if not prior_has_range:
                        prior_open_without_range = True
                    elif pd.isna(prior_latest_to) or prior_to_date > prior_latest_to:
                        prior_latest_to = prior_to_date
                pending_updates = []
                current_created_at = row_created_at

            row_has_range = pd.notna(row_from_date) and pd.notna(row_to_date)
            overlaps_prior = False
            if prior_open_without_range:
                overlaps_prior = True
            elif row_has_range and pd.notna(prior_latest_to):
                overlaps_prior = prior_latest_to >= row_from_date
            elif pd.notna(prior_latest_to):
                overlaps_prior = True
            output.loc[original_index] = np.int8(overlaps_prior)

            if status in {VALID_STATUS, "P"}:
                pending_updates.append((row_to_date, row_has_range))

    return output.astype("int8")


def _army_history_features(frame: pd.DataFrame, do2_date: pd.Series) -> pd.DataFrame:
    history = pd.DataFrame(index=frame.index)
    history["army_prior_record_count"] = np.float32(0.0)
    history["army_prior_non_valid_count"] = np.float32(0.0)
    history["army_prior_non_valid_rate"] = np.float32(0.0)
    history["army_days_since_prior_record"] = np.nan
    if "army_no" not in frame or "status" not in frame:
        return history.astype(
            {
                "army_prior_record_count": "float32",
                "army_prior_non_valid_count": "float32",
                "army_prior_non_valid_rate": "float32",
                "army_days_since_prior_record": "float32",
            }
        )

    army_no = _clean_category(frame["army_no"])
    army_no_is_placeholder = _army_no_placeholder_flag(frame)
    valid_identity = (
        army_no.ne(MISSING_CATEGORY)
        & army_no_is_placeholder.eq(0)
        & do2_date.notna()
        & supervised_status_mask(frame)
    )
    if not valid_identity.any():
        return history.astype(
            {
                "army_prior_record_count": "float32",
                "army_prior_non_valid_count": "float32",
                "army_prior_non_valid_rate": "float32",
                "army_days_since_prior_record": "float32",
            }
        )

    working = pd.DataFrame(
        {
            "army_no": army_no[valid_identity],
            "do2_date": do2_date[valid_identity],
            "is_non_valid": make_target_non_valid(frame.loc[valid_identity]).astype("float32"),
            "_original_index": frame.index[valid_identity],
        }
    ).sort_values(["army_no", "do2_date", "_original_index"])

    grouped = working.groupby("army_no", sort=False)
    working["prior_record_count"] = grouped.cumcount().astype("float32")
    working["prior_non_valid_count"] = (
        grouped["is_non_valid"].cumsum().sub(working["is_non_valid"]).astype("float32")
    )
    working["prior_non_valid_rate"] = (
        working["prior_non_valid_count"]
        .div(working["prior_record_count"].replace(0, np.nan))
        .fillna(0)
        .astype("float32")
    )
    working["days_since_prior_record"] = (
        working["do2_date"].sub(grouped["do2_date"].shift(1)).dt.days.astype("float32")
    )

    history.loc[working["_original_index"], "army_prior_record_count"] = working[
        "prior_record_count"
    ].to_numpy()
    history.loc[working["_original_index"], "army_prior_non_valid_count"] = working[
        "prior_non_valid_count"
    ].to_numpy()
    history.loc[working["_original_index"], "army_prior_non_valid_rate"] = working[
        "prior_non_valid_rate"
    ].to_numpy()
    history.loc[working["_original_index"], "army_days_since_prior_record"] = working[
        "days_since_prior_record"
    ].to_numpy()
    return history.astype(
        {
            "army_prior_record_count": "float32",
            "army_prior_non_valid_count": "float32",
            "army_prior_non_valid_rate": "float32",
            "army_days_since_prior_record": "float32",
        }
    )


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

    date_upper_bound = pd.Timestamp.today().normalize() + pd.Timedelta(days=30)
    do2_date, do2_date_plausible = _date_with_plausibility(
        frame, "do2_date", upper_bound=date_upper_bound
    )
    from_date, from_date_plausible = _date_with_plausibility(
        frame, "from_date", upper_bound=date_upper_bound
    )
    to_date, to_date_plausible = _date_with_plausibility(
        frame, "to_date", upper_bound=date_upper_bound
    )
    created_at, created_at_plausible = _date_with_plausibility(
        frame, "created_at", upper_bound=date_upper_bound
    )
    army_no_is_placeholder = _army_no_placeholder_flag(frame)

    result["do2_date_plausible"] = do2_date_plausible
    result["from_date_plausible"] = from_date_plausible
    result["to_date_plausible"] = to_date_plausible
    result["created_at_plausible"] = created_at_plausible
    result["army_no_is_placeholder"] = army_no_is_placeholder

    result["duration_days"] = (to_date - from_date).dt.days.astype("float32")
    result["submission_delay_days"] = (
        created_at.dt.normalize() - do2_date.dt.normalize()
    ).dt.days.astype("float32")

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
    result["do2_date_missing"] = do2_date.isna().astype("int8")
    result["from_date_missing"] = from_date.isna().astype("int8")
    result["has_duplicate_open_do2"] = _duplicate_open_do2_feature(
        frame, created_at, from_date, to_date
    )
    result = pd.concat(
        [
            result,
            _army_history_features(frame, do2_date),
            _prior_rate_features(frame, do2_date, army_no_is_placeholder),
        ],
        axis=1,
    )

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
