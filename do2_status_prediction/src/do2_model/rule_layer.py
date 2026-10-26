from __future__ import annotations

from dataclasses import dataclass

import numpy as np
import pandas as pd

MISSING_CATEGORY = "__MISSING__"
RULE_FEATURE_PREFIXES = (
    "rule_",
    "n_",
    "i_",
    "mr_",
)

SEVENTH_CPC_ALLOWANCE_DATE = pd.Timestamp("2017-07-01")
ABOLISHED_AFTER_7CPC = frozenset(
    {
        "DAA",
    }
)
SYSTEM_GENERATED_ONLY_DESCRIPTIONS = frozenset()
NOT_EFFECTIVE_STATUS_FLOOR = 5
EFFECTIVE_STATUS_ID = 1
UID_ALLOWED_RELATIONSHIPS = frozenset(
    {"BROTHER", "SISTER", "SPOUSE", "SON", "DAUGHTER", "FATHER", "MOTHER", "SELF"}
)
BIOMET_ALLOWED_RELATIONSHIPS = frozenset({"SELF", "SPOUSE", "SON", "DAUGHTER"})
CEA_VALID_CLASSES = frozenset(
    {"NUR", "LKG", "UKG", "CL-01", "CL-02", "CL-03", "CL-04", "CL-05", "CL-06",
     "CL-07", "CL-08", "CL-09", "CL-10", "CL-11", "CL-12"}
)
CEA_VALID_QUARTERS = frozenset({"Q1", "Q2", "Q3", "Q4"})


@dataclass(frozen=True)
class RuleDecision:
    status: str
    reason: str
    confidence: float
    source: str


def _blank_series(frame: pd.DataFrame, value: object) -> pd.Series:
    return pd.Series(value, index=frame.index)


def _category(frame: pd.DataFrame, column: str) -> pd.Series:
    if column not in frame:
        return _blank_series(frame, MISSING_CATEGORY).astype("string")
    return (
        frame[column]
        .astype("string")
        .str.replace('"', "", regex=False)
        .str.strip()
        .str.upper()
        .fillna(MISSING_CATEGORY)
        .replace("", MISSING_CATEGORY)
    )


def _text_present(frame: pd.DataFrame, column: str) -> pd.Series:
    if column not in frame:
        return _blank_series(frame, False).astype(bool)
    return frame[column].notna() & frame[column].astype("string").str.strip().ne("")


def _date(frame: pd.DataFrame, column: str) -> pd.Series:
    if column not in frame:
        return pd.Series(pd.NaT, index=frame.index, dtype="datetime64[ns]")
    return pd.to_datetime(frame[column], errors="coerce")


def _number(frame: pd.DataFrame, column: str) -> pd.Series:
    if column not in frame:
        return pd.Series(np.nan, index=frame.index, dtype="float32")
    return pd.to_numeric(frame[column], errors="coerce").astype("float32")


def _flag(frame: pd.DataFrame, column: str) -> pd.Series:
    if column not in frame:
        return pd.Series(np.int8(0), index=frame.index)
    value = frame[column]
    if pd.api.types.is_bool_dtype(value):
        return value.fillna(False).astype("int8")
    numeric = pd.to_numeric(value, errors="coerce")
    text = value.astype("string").str.strip().str.upper()
    flagged = numeric.fillna(0).gt(0) | text.isin({"TRUE", "T", "YES", "Y"})
    return flagged.fillna(False).astype("int8")


def _count(frame: pd.DataFrame, column: str) -> pd.Series:
    if column not in frame:
        return pd.Series(np.float32(0.0), index=frame.index)
    return pd.to_numeric(frame[column], errors="coerce").fillna(0).astype("float32")


def _compact_text(series: pd.Series) -> pd.Series:
    return series.astype("string").str.replace(r"\s+", "", regex=True).str.strip()


def _prior_count_by_key(
    frame: pd.DataFrame,
    columns: list[str],
    event_date: pd.Series,
) -> pd.Series:
    if not set(columns).issubset(frame.columns):
        return pd.Series(np.float32(0.0), index=frame.index)
    working = pd.DataFrame(
        {
            **{column: _category(frame, column) for column in columns},
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
        .agg(rows_at_event=("_original_index", "size"), original_indices=("_original_index", list))
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


def build_rule_features(frame: pd.DataFrame) -> pd.DataFrame:
    result = pd.DataFrame(index=frame.index)
    description = _category(frame, "description")
    transcription_type = _category(frame, "transcription_type")
    from_date = _date(frame, "from_date")
    to_date = _date(frame, "to_date")
    do2_date = _date(frame, "do2_date")
    created_at = _date(frame, "created_at")
    fs_due_date = _date(frame, "employee_fs_due_date")
    birth_date = _date(frame, "employee_birth_date")
    enrolment_date = _date(frame, "employee_enrolment_date")
    status_code = _number(frame, "employee_status_code")

    result["rule_missing_army_no"] = (
        ("army_no" in frame) & (~_text_present(frame, "army_no"))
    ).astype("int8")
    result["rule_missing_description"] = (
        ("description" in frame) & (~_text_present(frame, "description"))
    ).astype("int8")
    result["rule_missing_employee"] = (
        (("fk_employee" in frame) & (~_text_present(frame, "fk_employee")))
        | _flag(frame, "employee_missing").astype(bool)
    ).astype("int8")
    result["rule_missing_unit"] = (
        (("unit_code" in frame) & (~_text_present(frame, "unit_code")))
        | (("fk_unit" in frame) & (~_text_present(frame, "fk_unit")))
    ).astype("int8")
    result["rule_from_after_to"] = (
        from_date.notna() & to_date.notna() & from_date.gt(to_date)
    ).astype("int8")
    result["rule_do2_date_before_enrolment"] = (
        do2_date.notna() & enrolment_date.notna() & do2_date.lt(enrolment_date)
    ).astype("int8")
    result["n_fs_due_date_missing"] = fs_due_date.isna().astype("int8")
    result["n_from_date_after_fs_due_date"] = (
        from_date.notna() & fs_due_date.notna() & from_date.gt(fs_due_date)
    ).astype("int8")
    result["n_days_after_fs_due_date"] = (
        (from_date - fs_due_date).dt.days.where(result["n_from_date_after_fs_due_date"].astype(bool))
    ).astype("float32")
    result["n_from_date_within_fs_month"] = (
        from_date.notna()
        & fs_due_date.notna()
        & from_date.dt.to_period("M").eq(fs_due_date.dt.to_period("M"))
    ).astype("int8")
    result["n_employee_status_missing"] = status_code.isna().astype("int8")
    result["n_employee_effective_on_from_date"] = status_code.eq(EFFECTIVE_STATUS_ID).fillna(False).astype("int8")
    result["n_employee_not_effective_on_from_date"] = (
        status_code.gt(NOT_EFFECTIVE_STATUS_FLOOR - 1).fillna(False).astype("int8")
    )
    result["i_description_abolished_on_from_date"] = (
        from_date.notna()
        & from_date.ge(SEVENTH_CPC_ALLOWANCE_DATE)
        & description.isin(ABOLISHED_AFTER_7CPC)
    ).astype("int8")
    result["i_system_generated_only_manual_feed"] = (
        description.isin(SYSTEM_GENERATED_ONLY_DESCRIPTIONS)
        & transcription_type.ne("S")
    ).astype("int8")
    result["i_ref_do2_match_count"] = _count(frame, "i_ref_do2_match_count")
    result["i_ref_do2_already_cancelled"] = _flag(frame, "i_ref_do2_already_cancelled")
    result["i_prior_duplicate_count"] = _count(frame, "i_prior_duplicate_count")
    if "i_prior_duplicate_count" not in frame:
        result["i_prior_duplicate_count"] = _prior_count_by_key(
            frame,
            ["fk_employee", "description", "from_date", "column_a"],
            created_at,
        )
    result["i_has_prior_duplicate"] = (
        _flag(frame, "i_has_prior_duplicate").astype(bool)
        | result["i_prior_duplicate_count"].gt(0)
    ).astype("int8")
    result["i_prior_overlap_count"] = _count(frame, "i_prior_overlap_count")
    result["i_has_prior_overlap"] = (
        _flag(frame, "i_has_prior_overlap").astype(bool)
        | result["i_prior_overlap_count"].gt(0)
    ).astype("int8")
    result["i_prior_optcpc_conflict"] = _flag(frame, "i_prior_optcpc_conflict")
    result["i_pay_fixation_not_involved"] = _flag(frame, "i_pay_fixation_not_involved")
    result["i_relationship_missing_or_invalid"] = _flag(frame, "i_relationship_missing_or_invalid")
    result["i_appendix_j_rule_found"] = _flag(frame, "i_appendix_j_rule_found")
    result["i_appendix_description_not_found"] = _flag(frame, "i_appendix_description_not_found")
    result["i_appendix_required_from_date_missing"] = _flag(
        frame, "i_appendix_required_from_date_missing"
    )
    result["i_appendix_required_to_date_missing"] = _flag(
        frame, "i_appendix_required_to_date_missing"
    )
    result["i_appendix_required_rate_missing"] = _flag(frame, "i_appendix_required_rate_missing")
    result["i_appendix_required_tjra_missing"] = _flag(frame, "i_appendix_required_tjra_missing")
    result["i_appendix_required_lra_missing"] = _flag(frame, "i_appendix_required_lra_missing")
    result["i_appendix_required_ref_do2_no_missing"] = _flag(
        frame, "i_appendix_required_ref_do2_no_missing"
    )
    result["i_appendix_from_date_should_be_blank"] = _flag(
        frame, "i_appendix_from_date_should_be_blank"
    )
    result["i_appendix_to_date_should_be_blank"] = _flag(
        frame, "i_appendix_to_date_should_be_blank"
    )
    result["i_appendix_required_missing_count"] = _count(
        frame, "i_appendix_required_missing_count"
    )
    result["i_appendix_should_be_blank_count"] = _count(
        frame, "i_appendix_should_be_blank_count"
    )
    result["i_appendix_format_invalid"] = (
        result["i_appendix_description_not_found"].astype(bool)
        | result["i_appendix_required_missing_count"].gt(0)
        | result["i_appendix_should_be_blank_count"].gt(0)
    ).astype("int8")
    result["i_phone_duplicate_count"] = _count(frame, "i_phone_duplicate_count")
    result["i_aadhaar_duplicate_count"] = _count(frame, "i_aadhaar_duplicate_count")
    column_a = _category(frame, "column_a")
    column_b_present = _text_present(frame, "column_b")
    column_c_present = _text_present(frame, "column_c")
    column_b_compact = _compact_text(_blank_series(frame, "") if "column_b" not in frame else frame["column_b"])
    column_c_compact = _compact_text(_blank_series(frame, "") if "column_c" not in frame else frame["column_c"])
    uid_description = description.eq("UID")
    biomet_description = description.eq("BIOMET")
    cea_description = description.isin({"CEA", "CEAHC", "CEATWN", "HOSSUB"})

    result["i_uid_relationship_invalid"] = (
        uid_description & ~column_a.isin(UID_ALLOWED_RELATIONSHIPS)
    ).astype("int8")
    result["i_uid_name_missing"] = (uid_description & ~column_b_present).astype("int8")
    result["i_uid_number_missing"] = (uid_description & ~column_c_present).astype("int8")
    result["i_biomet_relationship_invalid"] = (
        biomet_description & ~column_a.isin(BIOMET_ALLOWED_RELATIONSHIPS)
    ).astype("int8")
    result["i_biomet_name_missing"] = (biomet_description & ~column_b_present).astype("int8")
    result["i_biomet_uid_invalid"] = (
        biomet_description
        & (
            ~column_c_present
            | ~column_c_compact.str.fullmatch(r"\d{12}").fillna(False)
        )
    ).astype("int8")
    result["i_family_relation_name_missing"] = (
        description.isin({"UID", "BIOMET"})
        & ~column_a.eq("SELF")
        & _count(frame, "i_family_relation_name_match_count").eq(0)
        & _count(frame, "i_family_active_relation_count").gt(0)
    ).astype("int8")
    result["i_family_uid_duplicate_count"] = _count(frame, "i_family_uid_duplicate_count")
    result["i_family_uid_conflict_count"] = _count(frame, "i_family_uid_conflict_count")
    result["i_employee_uid_duplicate_count"] = _count(frame, "i_employee_uid_duplicate_count")
    result["i_identity_uid_conflict"] = (
        result["i_family_uid_conflict_count"].gt(0)
        | result["i_employee_uid_duplicate_count"].gt(0)
        | _count(frame, "i_aadhaar_duplicate_count").gt(0)
    ).astype("int8")
    result["i_cea_class_invalid"] = (cea_description & ~column_a.isin(CEA_VALID_CLASSES)).astype("int8")
    result["i_cea_dob_invalid"] = (
        cea_description & ~column_b_compact.str.fullmatch(r"\d{8}").fillna(False)
    ).astype("int8")
    result["i_cea_quarter_invalid"] = (
        description.isin({"CEA", "CEAHC", "CEATWN"}) & ~_category(frame, "column_c").isin(CEA_VALID_QUARTERS)
    ).astype("int8")
    result["i_cea_prior_child_claim_count"] = _count(frame, "i_cea_prior_child_claim_count")
    result["i_cea_prior_child_overlap_count"] = _count(frame, "i_cea_prior_child_overlap_count")
    result["i_cea_children_claimed_same_period_count"] = _count(
        frame, "i_cea_children_claimed_same_period_count"
    )
    result["i_service_months_at_from_date"] = (
        ((from_date - enrolment_date).dt.days / 30.4375)
        .where(from_date.notna() & enrolment_date.notna())
        .astype("float32")
    )
    result["i_age_months_at_from_date"] = (
        ((from_date - birth_date).dt.days / 30.4375)
        .where(from_date.notna() & birth_date.notna())
        .astype("float32")
    )
    result["mr_manual_review_signal_count"] = (
        _flag(frame, "mr_batch_verification_pending")
        + _flag(frame, "mr_orders_awaited")
        + _flag(frame, "mr_requires_human_review")
    ).astype("float32")
    result["rule_blocking_signal_count"] = result[
        [
            "rule_missing_army_no",
            "rule_missing_description",
            "rule_missing_employee",
            "rule_missing_unit",
            "rule_from_after_to",
            "rule_do2_date_before_enrolment",
            "n_from_date_after_fs_due_date",
            "n_employee_not_effective_on_from_date",
            "i_description_abolished_on_from_date",
            "i_ref_do2_already_cancelled",
            "i_has_prior_duplicate",
            "i_has_prior_overlap",
            "i_prior_optcpc_conflict",
            "i_pay_fixation_not_involved",
            "i_relationship_missing_or_invalid",
            "i_appendix_format_invalid",
            "i_uid_relationship_invalid",
            "i_uid_name_missing",
            "i_uid_number_missing",
            "i_biomet_relationship_invalid",
            "i_biomet_name_missing",
            "i_biomet_uid_invalid",
            "i_family_relation_name_missing",
            "i_identity_uid_conflict",
            "i_cea_class_invalid",
            "i_cea_dob_invalid",
            "i_cea_quarter_invalid",
        ]
    ].sum(axis=1).astype("float32")
    return result


def rule_decisions(frame: pd.DataFrame) -> pd.DataFrame:
    features = build_rule_features(frame)
    output = pd.DataFrame(index=frame.index)
    output["layer1_status"] = ""
    output["layer1_reason"] = ""
    output["layer1_confidence"] = np.float32(0.0)
    output["layer1_source"] = "rule_layer"

    rules: list[tuple[str, str, str, float]] = [
        ("rule_missing_army_no", "I", "Cannot save a DO2 without Army No.", 0.99),
        ("rule_missing_description", "I", "Cannot save a DO2 without Description.", 0.99),
        ("i_uid_relationship_invalid", "I", "UID relationship is missing or invalid.", 0.91),
        ("i_uid_name_missing", "I", "UID name is missing.", 0.90),
        ("i_uid_number_missing", "I", "UID number is missing.", 0.90),
        ("i_biomet_relationship_invalid", "I", "BIOMET relationship is missing or invalid.", 0.91),
        ("i_biomet_name_missing", "I", "BIOMET name is missing.", 0.90),
        ("i_biomet_uid_invalid", "I", "BIOMET UID must be a 12 digit number.", 0.91),
        ("i_identity_uid_conflict", "I", "UID/Aadhaar already exists for another employee or relation.", 0.90),
    ]
    undecided = output["layer1_status"].eq("")
    for feature, status, reason, confidence in rules:
        matched = undecided & features[feature].gt(0)
        output.loc[matched, "layer1_status"] = status
        output.loc[matched, "layer1_reason"] = reason
        output.loc[matched, "layer1_confidence"] = np.float32(confidence)
        undecided = output["layer1_status"].eq("")

    manual_review = undecided & features["mr_manual_review_signal_count"].gt(0)
    output.loc[manual_review, "layer1_status"] = "MR"
    output.loc[manual_review, "layer1_reason"] = "Manual review signal found."
    output.loc[manual_review, "layer1_confidence"] = np.float32(0.75)
    return output.reset_index(drop=True)


def rule_feature_columns(columns: pd.Index | list[str]) -> list[str]:
    return [
        column
        for column in columns
        if any(str(column).startswith(prefix) for prefix in RULE_FEATURE_PREFIXES)
    ]
