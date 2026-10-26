import numpy as np
import pandas as pd
import pytest

from do2_model.features import (
    build_features,
    filter_supervised_rows,
    leakage_columns_present,
    normalize_status,
    validate_prediction_columns,
)


def test_status_normalization_keeps_actual_status_classes():
    status = pd.Series(["V", " v ", '"V"', "I", "P", "C", "", None])
    assert normalize_status(status).tolist() == ["V", "V", "V", "I", "P", "C", "", ""]
    frame = filter_supervised_rows(pd.DataFrame({"status": status}))
    assert frame["status"].tolist() == ["V", " v ", '"V"', "I", "P", "C"]


def test_prediction_rejects_missing_required_entry_columns():
    with pytest.raises(ValueError, match="description, unit_code"):
        validate_prediction_columns(pd.DataFrame({"do2_type": ["0"]}), ["unit_code", "description"])


def test_leakage_columns_do_not_enter_features():
    frame = pd.DataFrame(
        {
            "status": ["V"],
            "approved": [1],
            "reason": ["bad"],
            "input_qe": ["reviewer"],
            "column_a": ["unknown"],
            "audit_cage": [True],
            "do2_type": ["1"],
        }
    )
    result = build_features(frame)
    assert "status" not in result.frame.columns
    assert "approved" not in result.frame.columns
    assert "reason" not in result.frame.columns
    assert result.frame.loc[0, "column_a"] == "UNKNOWN"
    assert "audit_cage" not in result.frame.columns
    assert "audit_cage_flag" not in result.frame.columns
    assert "input_qe" not in result.frame.columns
    assert "input_qe_quarter_of_year" in result.frame.columns
    assert "has_column_a" in result.frame.columns
    assert leakage_columns_present(frame.columns) == [
        "approved",
        "audit_cage",
        "reason",
        "status",
    ]


def test_features_do_not_depend_on_status_label():
    frame = pd.DataFrame(
        {
            "army_no": ["A1", "A1", "A1", "B2"],
            "status": ["V", "I", "MR", "V"],
            "do2_date": ["2020-01-01", "2021-02-01", "2021-03-01", "2021-01-15"],
        }
    )

    with_status = build_features(frame).frame
    without_status = build_features(frame.drop(columns="status")).frame

    assert "army_no" not in with_status.columns
    pd.testing.assert_frame_equal(with_status, without_status)


def test_feature_engineering_dates_and_missingness():
    frame = pd.DataFrame(
        {
            "status": ["V", "I"],
            "do2_type": ["5", None],
            "do2_date": ["2024-01-10", "2024-02-10"],
            "created_at": ["2024-01-12", "2024-02-09"],
            "from_date": ["2024-01-01", "2024-03-10"],
            "to_date": ["2024-01-05", "2024-03-01"],
            "rate": [None, 10],
            "army_no": ["A1", ""],
            "check_digit": ["X", ""],
            "fk_employee": ["E1", ""],
            "unit_code": ["U1", ""],
            "fk_unit": ["FU1", ""],
            "do2_group": ["IA", ""],
            "do2_no": ["D1", ""],
            "do2_item_no": [1, np.nan],
            "description": ["DESC", ""],
            "transcription_type": ["D", ""],
            "qe": ["2024Q1", ""],
            "input_qe": ["", ""],
            "column_a": ["A", ""],
            "cfa_sanction_letter_no": ["", "L1"],
            "audit_cage": ["true", "0"],
        }
    )
    result = build_features(frame)
    assert result.frame["duration_days"].tolist() == [4.0, -9.0]
    assert result.frame["submission_delay_days"].tolist() == [2.0, -1.0]
    assert result.frame["has_rate"].tolist() == [0, 1]
    assert result.frame["rate_is_missing"].tolist() == [1, 0]
    assert result.frame["rate_is_negative"].tolist() == [0, 0]
    assert result.frame["from_after_to"].tolist() == [0, 1]
    assert result.frame.loc[1, "do2_type"] == "__MISSING__"
    assert np.isnan(result.frame.loc[0, "rate"])
    assert result.frame["description_char_count"].tolist() == [4.0, 0.0]
    assert result.frame["description_word_count"].tolist() == [1.0, 0.0]
    assert result.frame["created_before_do2"].tolist() == [0, 1]
    assert result.frame["negative_duration"].tolist() == [0, 1]
    assert result.frame["same_day_created_do2"].tolist() == [0, 0]
    assert result.frame["missing_required_field_count"].tolist() == [0.0, 12.0]
    assert result.frame["has_column_a"].tolist() == [1, 0]
    assert result.frame["has_cfa_sanction_letter_no"].tolist() == [0, 1]
    assert "audit_cage_flag" not in result.frame.columns


def test_rule_layer_features_capture_known_dolphin_blockers():
    frame = pd.DataFrame(
        {
            "army_no": ["A1", "A2"],
            "description": ["ALLOWANCE", "DAA"],
            "from_date": ["2024-02-01", "2018-01-01"],
            "to_date": ["2024-01-01", "2018-01-31"],
            "do2_date": ["2024-02-02", "2018-01-02"],
            "fk_employee": ["E1", "E2"],
            "fk_unit": ["U1", "U2"],
            "unit_code": ["UC1", "UC2"],
            "employee_status_code": [6, 1],
            "employee_fs_due_date": ["2024-01-15", "2030-01-01"],
            "employee_enrolment_date": ["2020-01-01", "2010-01-01"],
        }
    )

    result = build_features(frame).frame

    assert result["rule_from_after_to"].tolist() == [1, 0]
    assert result["n_employee_not_effective_on_from_date"].tolist() == [1, 0]
    assert result["n_from_date_after_fs_due_date"].tolist() == [1, 0]
    assert result["i_description_abolished_on_from_date"].tolist() == [0, 1]
    assert result["rule_blocking_signal_count"].tolist() == [3.0, 1.0]


def test_qe_features_are_relative_not_absolute_calendar_labels():
    frame = pd.DataFrame(
        {
            "status": ["V", "V"],
            "do2_date": ["2024-01-15", "2025-01-15"],
            "qe": ["2024Q2", "2025Q2"],
            "input_qe": ["2024Q1", "2025Q1"],
            "do2_group": ["A", "A"],
        }
    )

    result = build_features(frame).frame

    assert "qe" not in result.columns
    assert "input_qe" not in result.columns
    assert "qe_do2_group" not in result.columns
    assert result["qe_quarter_of_year"].tolist() == [2.0, 2.0]
    assert result["input_qe_quarter_of_year"].tolist() == [1.0, 1.0]
    assert result["qe_quarters_since_do2_date"].tolist() == [1.0, 1.0]
    assert result["input_qe_quarters_since_do2_date"].tolist() == [0.0, 0.0]


def test_qe_features_parse_month_year_labels_as_quarters():
    frame = pd.DataFrame(
        {
            "status": ["V", "V", "V"],
            "do2_date": ["2025-06-15", "2025-11-15", "2025-01-15"],
            "qe": ["06/2025", "2025-11", "Q1-2025"],
            "input_qe": ["07/2025", "11/2025", "2025Q1"],
        }
    )

    result = build_features(frame).frame

    assert result["qe_missing"].tolist() == [0, 0, 0]
    assert result["input_qe_missing"].tolist() == [0, 0, 0]
    assert result["qe_quarter_of_year"].tolist() == [2.0, 4.0, 1.0]
    assert result["input_qe_quarter_of_year"].tolist() == [3.0, 4.0, 1.0]
    assert result["qe_quarters_since_do2_date"].tolist() == [0.0, 0.0, 0.0]
    assert result["input_qe_quarters_since_do2_date"].tolist() == [1.0, 0.0, 0.0]


def test_direct_calendar_shortcuts_do_not_enter_features():
    frame = pd.DataFrame(
        {
            "status": ["V"],
            "do2_date": ["2024-01-31"],
            "created_at": ["2024-01-27"],
            "from_date": ["2024-01-01"],
            "to_date": ["2024-01-10"],
        }
    )

    result = build_features(frame).frame

    assert "duration_days" in result.columns
    assert "submission_delay_days" in result.columns
    assert "do2_date_plausible" in result.columns
    assert "created_at_plausible" in result.columns
    forbidden_calendar_features = {
        "do2_year",
        "created_year",
        "do2_month",
        "created_month",
        "do2_quarter",
        "created_quarter",
        "do2_day",
        "created_day",
        "do2_day_of_week",
        "created_day_of_week",
        "do2_is_month_end",
        "created_is_month_end",
        "do2_is_weekend",
        "created_is_weekend",
    }
    assert forbidden_calendar_features.isdisjoint(result.columns)


def test_feature_engineering_coerces_out_of_range_dates():
    frame = pd.DataFrame(
        {
            "status": ["V"],
            "do2_date": ["20202-01-10"],
            "created_at": ["2024-01-12"],
        }
    )

    result = build_features(frame)

    assert result.frame["do2_date_missing"].tolist() == [1]
    assert result.frame["do2_date_plausible"].tolist() == [0]
    assert np.isnan(result.frame.loc[0, "submission_delay_days"])


def test_context_and_interaction_features_are_prior_created_only():
    frame = pd.DataFrame(
        {
            "army_no": ["A1", "A1", "A1", "A2"],
            "batch_no": ["B1", "B1", "B1", "B2"],
            "do2_no": ["D1", "D1", "D1", "D2"],
            "do2_item_no": [1, 1, 2, 2],
            "unit_code": ["U1", "U1", "U1", "U2"],
            "do2_group": ["IA", "IA", "IA", "PAY"],
            "do2_type": ["0", "0", "0", "3"],
            "description": ["ALLOWANCE", "ALLOWANCE", "ALLOWANCE", "DEDUCT"],
            "created_at": [
                "2024-01-01 10:00:00",
                "2024-01-01 10:00:00",
                "2024-01-01 11:00:00",
                "2024-01-01 12:00:00",
            ],
        }
    )

    result = build_features(frame)

    assert result.text_columns == []
    assert "description" in result.categorical_columns
    assert "do2_group_type" in result.categorical_columns
    assert result.frame["do2_group_type"].tolist() == ["IA__0", "IA__0", "IA__0", "PAY__3"]
    assert result.frame["prior_batch_row_count"].tolist() == [0.0, 0.0, 2.0, 0.0]
    assert result.frame["prior_do2_no_row_count"].tolist() == [0.0, 0.0, 2.0, 0.0]
    assert result.frame["prior_army_do2_type_row_count"].tolist() == [0.0, 0.0, 2.0, 0.0]
    assert result.frame["prior_unit_do2_type_row_count"].tolist() == [0.0, 0.0, 2.0, 0.0]
    assert np.isnan(result.frame.loc[0, "prior_batch_do2_item_share"])
    assert result.frame.loc[2, "prior_batch_do2_item_share"] == pytest.approx(0.0)


def test_prior_row_count_features_are_strictly_point_in_time():
    frame = pd.DataFrame(
        {
            "army_no": ["A1", "A1", "A1", "A1"],
            "batch_no": ["B1", "B1", "B1", "B1"],
            "do2_no": ["D1", "D1", "D1", "D1"],
            "do2_item_no": [1, 1, 2, 1],
            "unit_code": ["U1", "U1", "U1", "U1"],
            "do2_type": ["T1", "T1", "T1", "T1"],
            "created_at": [
                "2024-01-01 10:00:00",
                "2024-01-01 10:00:00",
                "2024-01-01 11:00:00",
                "2024-01-01 12:00:00",
            ],
        }
    )

    result = build_features(frame).frame

    assert result["prior_batch_row_count"].tolist() == [0.0, 0.0, 2.0, 3.0]
    assert result["prior_do2_no_row_count"].tolist() == [0.0, 0.0, 2.0, 3.0]
    assert result["prior_army_do2_type_row_count"].tolist() == [0.0, 0.0, 2.0, 3.0]
    assert result["prior_unit_do2_type_row_count"].tolist() == [0.0, 0.0, 2.0, 3.0]
    assert np.isnan(result.loc[0, "prior_batch_do2_item_share"])
    assert np.isnan(result.loc[1, "prior_batch_do2_item_share"])
    assert result.loc[2, "prior_batch_do2_item_share"] == pytest.approx(0.0)
    assert result.loc[3, "prior_batch_do2_item_share"] == pytest.approx(2 / 3)


def test_each_prior_context_feature_excludes_future_rows():
    frame = pd.DataFrame(
        {
            "army_no": ["A1", "A1", "A1"],
            "batch_no": ["B1", "B1", "B1"],
            "do2_no": ["D1", "D1", "D1"],
            "do2_item_no": [1, 2, 1],
            "unit_code": ["U1", "U1", "U1"],
            "do2_type": ["T1", "T1", "T1"],
            "created_at": [
                "2024-01-02 10:00:00",
                "2024-01-03 10:00:00",
                "2024-01-01 10:00:00",
            ],
        }
    )

    result = build_features(frame).frame

    assert result.loc[2, "prior_batch_row_count"] == 0.0
    assert result.loc[2, "prior_do2_no_row_count"] == 0.0
    assert result.loc[2, "prior_army_do2_type_row_count"] == 0.0
    assert result.loc[2, "prior_unit_do2_type_row_count"] == 0.0
    assert np.isnan(result.loc[2, "prior_batch_do2_item_share"])
    assert result.loc[0, "prior_batch_row_count"] == 1.0
    assert result.loc[1, "prior_batch_row_count"] == 2.0


def test_presence_features_accept_database_presence_aliases():
    frame = pd.DataFrame(
        {
            "status": ["V"],
            "has_reference_do2_source": [1],
            "has_attachment_source": [1],
            "has_pto_remarks_source": [1],
            "has_column9_remarks_source": [1],
            "has_cfa_sanction_letter_no_source": [1],
        }
    )

    result = build_features(frame).frame

    assert result["has_reference_do2"].tolist() == [1]
    assert result["has_attachment"].tolist() == [1]
    assert result["has_pto_remarks"].tolist() == [1]
    assert result["has_column9_remarks"].tolist() == [1]
    assert result["has_cfa_sanction_letter_no"].tolist() == [1]
    assert result["conditional_required_present_count"].tolist() == [3.0]


def test_expected_categorical_columns_are_preserved_for_scoring():
    training = build_features(
        pd.DataFrame(
            {
                "status": ["V"],
                "description": ["PAY FIXATION 2024"],
                "do2_type": ["T1"],
            }
        )
    )

    scoring = build_features(
        pd.DataFrame({"status": ["V"], "do2_type": ["T1"]}),
        expected_columns=list(training.frame.columns),
        expected_categorical=training.categorical_columns,
        expected_text=training.text_columns,
    )

    assert scoring.text_columns == []
    assert scoring.categorical_columns == training.categorical_columns
    assert scoring.frame["description"].tolist() == ["__MISSING__"]
