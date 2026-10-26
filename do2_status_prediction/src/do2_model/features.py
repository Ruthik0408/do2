from __future__ import annotations

from dataclasses import dataclass

import numpy as np
import pandas as pd

from do2_model.config import LEAKAGE_COLUMNS, load_feature_config
from do2_model.rule_layer import build_rule_features

MISSING_CATEGORY = "__MISSING__"
KNOWN_ARMY_NO_PLACEHOLDERS = frozenset({"JC490821", "JC490969"})
PLACEHOLDER_DISTINCT_EMPLOYEE_FLOOR = 100
DATE_PLAUSIBILITY_START = pd.Timestamp("1980-01-01")
LONG_DURATION_DAYS = 365
CORE_REQUIRED_COLUMNS = (
    "army_no",
    "check_digit",
    "fk_employee",
    "unit_code",
    "fk_unit",
    "do2_group",
    "do2_type",
    "do2_no",
    "do2_item_no",
    "do2_date",
    "description",
    "from_date",
    "transcription_type",
)
CONDITIONAL_REQUIRED_COLUMNS = (
    "to_date",
    "column_a",
    "column_b",
    "column_c",
    "rate",
    "tjra",
    "lra",
    "punishment",
    "ref_do2_no",
    "attachment",
    "cfa_sanction_received",
    "cfa_sanction_letter_no",
    "cfa_sanction_date",
    "time_bar_sanction",
)


@dataclass
class FeatureResult:
    frame: pd.DataFrame
    categorical_columns: list[str]
    text_columns: list[str]


def normalize_status(series: pd.Series) -> pd.Series:
    return (
        series.fillna("")
        .astype("string")
        .str.replace('"', "", regex=False)
        .str.strip()
        .str.upper()
    )


def supervised_status_mask(frame: pd.DataFrame) -> pd.Series:
    if "status" not in frame:
        raise KeyError("status is required for supervised training/evaluation")
    return normalize_status(frame["status"]).ne("")


def filter_supervised_rows(frame: pd.DataFrame) -> pd.DataFrame:
    return frame.loc[supervised_status_mask(frame)].reset_index(drop=True)


def leakage_columns_present(columns: list[str] | pd.Index) -> list[str]:
    present = set(columns)
    return sorted(column for column in LEAKAGE_COLUMNS if column in present)


def validate_prediction_columns(frame: pd.DataFrame, required_columns: list[str]) -> None:
    missing = sorted(set(required_columns) - set(frame.columns))
    if missing:
        raise ValueError(
            "Prediction input is missing required entry-time columns: " + ", ".join(missing)
        )


def _clean_category(series: pd.Series) -> pd.Series:
    cleaned = series.astype("string").str.strip().str.upper()
    return cleaned.fillna(MISSING_CATEGORY).replace("", MISSING_CATEGORY).astype(str)


def _clean_text(series: pd.Series) -> pd.Series:
    return series.fillna("").astype("string").str.strip().str.upper().astype(str)


def _text_shape_features(frame: pd.DataFrame, column: str, prefix: str) -> pd.DataFrame:
    if column not in frame:
        return pd.DataFrame(
            {
                f"{prefix}_char_count": pd.Series(np.float32(0.0), index=frame.index),
                f"{prefix}_word_count": pd.Series(np.float32(0.0), index=frame.index),
                f"{prefix}_has_digit": pd.Series(np.int8(0), index=frame.index),
            }
        )
    text = _clean_text(frame[column]).astype("string")
    return pd.DataFrame(
        {
            f"{prefix}_char_count": text.str.len().fillna(0).astype("float32"),
            f"{prefix}_word_count": text.str.split().str.len().fillna(0).astype("float32"),
            f"{prefix}_has_digit": text.str.contains(r"\d", regex=True).fillna(False).astype("int8"),
        },
        index=frame.index,
    )


def _present(series: pd.Series) -> pd.Series:
    return (
        series.notna()
        & series.astype("string").str.strip().fillna("").ne("")
    ).astype("int8")


def _column_present(frame: pd.DataFrame, column: str) -> pd.Series:
    if column not in frame:
        return pd.Series(np.int8(0), index=frame.index)
    return _present(frame[column])


def _any_column_present(frame: pd.DataFrame, columns: tuple[str, ...]) -> pd.Series:
    present = pd.Series(False, index=frame.index)
    for column in columns:
        if column in frame:
            present = present | _present(frame[column]).astype(bool)
    return present.astype("int8")


def _presence_feature(
    frame: pd.DataFrame,
    source_column: str,
    precomputed_columns: tuple[str, ...] = (),
) -> pd.Series:
    for precomputed_column in precomputed_columns:
        if precomputed_column in frame:
            return (
                pd.to_numeric(frame[precomputed_column], errors="coerce")
                .fillna(0)
                .gt(0)
                .astype("int8")
            )
    return _column_present(frame, source_column)


def _safe_divide(numerator: pd.Series, denominator: pd.Series) -> pd.Series:
    return numerator.div(denominator.replace(0, np.nan)).astype("float32")


def _quarter_period(frame: pd.DataFrame, column: str) -> pd.PeriodIndex:
    if column not in frame:
        return pd.PeriodIndex([pd.NaT] * len(frame), freq="Q")
    text = frame[column].astype("string").str.strip().str.upper()
    labels = pd.Series(pd.NA, index=frame.index, dtype="string")

    year_quarter = text.str.extract(r"(?P<year>\d{4})\D*Q(?P<quarter>[1-4])")
    valid = year_quarter["year"].notna()
    labels.loc[valid] = year_quarter.loc[valid, "year"] + "Q" + year_quarter.loc[valid, "quarter"]

    quarter_year = text.str.extract(r"Q(?P<quarter>[1-4])\D*(?P<year>\d{4})")
    valid = labels.isna() & quarter_year["year"].notna()
    labels.loc[valid] = quarter_year.loc[valid, "year"] + "Q" + quarter_year.loc[valid, "quarter"]

    month_year = text.str.extract(r"^(?P<month>0?[1-9]|1[0-2])\D+(?P<year>\d{4})$")
    valid = labels.isna() & month_year["year"].notna()
    if valid.any():
        quarter = ((month_year.loc[valid, "month"].astype(int) - 1) // 3 + 1).astype(str)
        labels.loc[valid] = month_year.loc[valid, "year"] + "Q" + quarter

    year_month = text.str.extract(r"^(?P<year>\d{4})\D+(?P<month>0?[1-9]|1[0-2])$")
    valid = labels.isna() & year_month["year"].notna()
    if valid.any():
        quarter = ((year_month.loc[valid, "month"].astype(int) - 1) // 3 + 1).astype(str)
        labels.loc[valid] = year_month.loc[valid, "year"] + "Q" + quarter

    return pd.PeriodIndex(labels, freq="Q")


def _quarter_features(
    frame: pd.DataFrame,
    column: str,
    prefix: str,
    reference_date: pd.Series,
) -> pd.DataFrame:
    quarter = _quarter_period(frame, column)
    reference_quarter = reference_date.dt.to_period("Q")
    quarter_number = pd.Series(quarter.quarter, index=frame.index, dtype="float32")
    quarter_missing = pd.Series(quarter.isna(), index=frame.index).astype("int8")
    quarters_since_reference = pd.Series(
        quarter.astype("int64") - reference_quarter.astype("int64"),
        index=frame.index,
        dtype="float32",
    )
    quarters_since_reference = quarters_since_reference.where(
        ~quarter_missing.astype(bool) & reference_quarter.notna()
    )
    return pd.DataFrame(
        {
            f"{prefix}_quarter_of_year": quarter_number.where(~quarter_missing.astype(bool)),
            f"{prefix}_missing": quarter_missing,
            f"{prefix}_quarters_since_do2_date": quarters_since_reference,
        },
        index=frame.index,
    ).astype(
        {
            f"{prefix}_quarter_of_year": "float32",
            f"{prefix}_missing": "int8",
            f"{prefix}_quarters_since_do2_date": "float32",
        }
    )


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


def _category_pair(frame: pd.DataFrame, left: str, right: str) -> pd.Series:
    if left not in frame or right not in frame:
        return pd.Series(MISSING_CATEGORY, index=frame.index, dtype="object")
    return _clean_category(frame[left]).str.cat(_clean_category(frame[right]), sep="__")


def _prior_row_count_by_key(
    frame: pd.DataFrame,
    columns: list[str],
    event_date: pd.Series,
) -> pd.Series:
    if not set(columns).issubset(frame.columns):
        return pd.Series(np.float32(0.0), index=frame.index)
    working = pd.DataFrame(
        {
            **{column: _clean_category(frame[column]) for column in columns},
            "event_date": event_date,
            "_original_index": frame.index,
        }
    )
    valid = working[columns].ne(MISSING_CATEGORY).all(axis=1) & working["event_date"].notna()
    output = pd.Series(np.float32(0.0), index=frame.index)
    if not valid.any():
        return output

    grouped_at_date = (
        working.loc[valid]
        .groupby([*columns, "event_date"], sort=False)
        .agg(
            rows_at_event=("_original_index", "size"),
            original_indices=("_original_index", list),
        )
        .reset_index()
        .sort_values([*columns, "event_date"], kind="mergesort")
    )
    grouped_at_date["prior_rows"] = (
        grouped_at_date.groupby(columns, sort=False)["rows_at_event"]
        .cumsum()
        .sub(grouped_at_date["rows_at_event"])
        .astype("float32")
    )
    exploded = grouped_at_date[["original_indices", "prior_rows"]].explode(
        "original_indices", ignore_index=True
    )
    output.loc[exploded["original_indices"].to_numpy()] = exploded["prior_rows"].to_numpy(
        dtype="float32"
    )
    return output.astype("float32")


def build_features(
    frame: pd.DataFrame,
    *,
    expected_columns: list[str] | None = None,
    expected_categorical: list[str] | None = None,
    expected_text: list[str] | None = None,
) -> FeatureResult:
    config = load_feature_config()
    result = pd.DataFrame(index=frame.index)
    categorical: list[str] = []
    text_columns: list[str] = []
    leakage_present = leakage_columns_present(frame.columns)
    feature_columns = set(config["categorical_base"]) | set(config["numeric_base"])
    leaking_features = sorted(feature_columns & set(leakage_present))
    if leaking_features:
        raise RuntimeError(f"Leakage columns configured as model features: {leaking_features}")

    text_base = set(config.get("text_base", []))
    for column in config["categorical_base"]:
        if column in text_base:
            continue
        if column in frame:
            result[column] = _clean_category(frame[column])
            categorical.append(column)

    for column in text_base:
        if column in frame:
            result[column] = _clean_text(frame[column])
            text_columns.append(column)

    engineered_categorical = {
        "do2_group_type": ("do2_group", "do2_type"),
        "unit_do2_type": ("unit_code", "do2_type"),
        "description_do2_type": ("description", "do2_type"),
    }
    for feature_name, (left, right) in engineered_categorical.items():
        if left in frame and right in frame:
            result[feature_name] = _category_pair(frame, left, right)
            categorical.append(feature_name)

    for column in config["numeric_base"]:
        if column in frame:
            result[column] = pd.to_numeric(frame[column], errors="coerce").astype("float32")

    amount_values: dict[str, pd.Series] = {}
    for column in ("rate", "tjra", "lra"):
        value = (
            pd.to_numeric(frame[column], errors="coerce")
            if column in frame
            else pd.Series(np.nan, index=frame.index)
        )
        amount_values[column] = value
        result[f"{column}_is_missing"] = value.isna().astype("int8")
        result[f"{column}_is_zero"] = value.eq(0).fillna(False).astype("int8")
        result[f"{column}_is_negative"] = value.lt(0).fillna(False).astype("int8")
        result[f"{column}_log_abs"] = np.log1p(value.abs()).astype("float32")
    result["rate_tjra_abs_diff"] = (amount_values["rate"] - amount_values["tjra"]).abs().astype("float32")
    result["rate_lra_abs_diff"] = (amount_values["rate"] - amount_values["lra"]).abs().astype("float32")
    result = pd.concat([result, _text_shape_features(frame, "description", "description")], axis=1)

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
    result["abs_duration_days"] = result["duration_days"].abs().astype("float32")
    result["log_abs_duration_days"] = np.log1p(result["abs_duration_days"]).astype(
        "float32"
    )
    result["abs_submission_delay_days"] = result["submission_delay_days"].abs().astype(
        "float32"
    )
    result["log_abs_submission_delay_days"] = np.log1p(
        result["abs_submission_delay_days"]
    ).astype("float32")
    result["created_before_do2"] = (
        created_at.notna()
        & do2_date.notna()
        & created_at.dt.normalize().lt(do2_date.dt.normalize())
    ).astype("int8")
    result["same_day_created_do2"] = (
        created_at.notna()
        & do2_date.notna()
        & created_at.dt.normalize().eq(do2_date.dt.normalize())
    ).astype("int8")
    result["negative_duration"] = result["duration_days"].lt(0).fillna(False).astype("int8")
    result["zero_duration"] = result["duration_days"].eq(0).fillna(False).astype("int8")
    result["long_duration"] = (
        result["duration_days"].gt(LONG_DURATION_DAYS).fillna(False).astype("int8")
    )
    result = pd.concat(
        [
            result,
            _quarter_features(frame, "qe", "qe", do2_date),
            _quarter_features(frame, "input_qe", "input_qe", do2_date),
        ],
        axis=1,
    )

    presence_map = {
        "has_to_date": ("to_date", ()),
        "has_rate": ("rate", ()),
        "has_reference_do2": ("ref_do2_no", ("has_reference_do2_source",)),
        "has_attachment": ("attachment", ("has_attachment_source",)),
        "has_punishment": ("punishment", ()),
        "has_column_a": ("column_a", ()),
        "has_column_b": ("column_b", ()),
        "has_column_c": ("column_c", ()),
        "has_pto_remarks": ("pto_remarks", ("has_pto_remarks_source",)),
        "has_column9_remarks": ("column9_remarks", ("has_column9_remarks_source",)),
        "has_cfa_sanction_received": ("cfa_sanction_received", ()),
        "has_cfa_sanction_letter_no": (
            "cfa_sanction_letter_no",
            ("has_cfa_sanction_letter_no_source", "has_cfa_letter_source"),
        ),
        "has_cfa_sanction_date": ("cfa_sanction_date", ()),
    }
    for feature_name, (source_column, precomputed_columns) in presence_map.items():
        result[feature_name] = _presence_feature(frame, source_column, precomputed_columns)

    core_presence = pd.DataFrame(
        {
            column: _column_present(frame, column)
            for column in CORE_REQUIRED_COLUMNS
            if column != "do2_date"
        },
        index=frame.index,
    )
    core_presence["do2_date"] = do2_date.notna().astype("int8")
    core_presence["qe_or_input_qe"] = _any_column_present(frame, ("qe", "input_qe"))
    result["core_required_present_count"] = core_presence.sum(axis=1).astype("float32")
    result["missing_required_field_count"] = (
        len(CORE_REQUIRED_COLUMNS) + 1 - result["core_required_present_count"]
    ).astype("float32")

    conditional_presence = pd.DataFrame(index=frame.index)
    for column in CONDITIONAL_REQUIRED_COLUMNS:
        feature_name = f"has_{column}"
        if feature_name in result:
            conditional_presence[column] = result[feature_name]
        elif column == "ref_do2_no":
            conditional_presence[column] = result["has_reference_do2"]
        else:
            conditional_presence[column] = _column_present(frame, column)
    result["conditional_required_present_count"] = conditional_presence.sum(axis=1).astype(
        "float32"
    )
    result["conditional_required_missing_count"] = (
        len(CONDITIONAL_REQUIRED_COLUMNS) - result["conditional_required_present_count"]
    ).astype("float32")

    result["from_after_to"] = (
        from_date.notna() & to_date.notna() & from_date.gt(to_date)
    ).astype("int8")
    result["do2_date_missing"] = do2_date.isna().astype("int8")
    result["from_date_missing"] = from_date.isna().astype("int8")
    result["to_date_missing"] = to_date.isna().astype("int8")
    result["created_at_missing"] = created_at.isna().astype("int8")
    prior_batch_rows = _prior_row_count_by_key(frame, ["batch_no"], created_at)
    result["prior_batch_row_count"] = prior_batch_rows
    result["prior_do2_no_row_count"] = _prior_row_count_by_key(frame, ["do2_no"], created_at)
    result["prior_army_do2_type_row_count"] = _prior_row_count_by_key(
        frame, ["army_no", "do2_type"], created_at
    )
    result["prior_unit_do2_type_row_count"] = _prior_row_count_by_key(
        frame, ["unit_code", "do2_type"], created_at
    )
    result["prior_batch_do2_item_share"] = _safe_divide(
        _prior_row_count_by_key(frame, ["batch_no", "do2_item_no"], created_at),
        prior_batch_rows,
    )
    result = pd.concat([result, build_rule_features(frame)], axis=1)
    if expected_columns is not None:
        expected_categorical_set = set(expected_categorical or [])
        expected_text_set = set(expected_text or [])
        for column in expected_columns:
            if column not in result:
                if column in expected_categorical_set:
                    result[column] = MISSING_CATEGORY
                elif column in expected_text_set:
                    result[column] = ""
                else:
                    result[column] = np.nan
        result = result[expected_columns]
        categorical = [column for column in expected_columns if column in expected_categorical_set]
        text_columns = [column for column in expected_columns if column in expected_text_set]

    if result.empty:
        raise RuntimeError("No model features could be built from the supplied columns.")

    return FeatureResult(frame=result, categorical_columns=categorical, text_columns=text_columns)
