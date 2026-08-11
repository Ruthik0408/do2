import numpy as np
import pandas as pd
import pytest

from do2_model.features import (
    build_features,
    filter_supervised_rows,
    leakage_columns_present,
    make_target_non_valid,
    normalize_status,
)


def test_status_mapping_v_only_is_valid():
    status = pd.Series(["V", " v ", '"V"', "I", "P", "C", "", None])
    assert normalize_status(status).tolist() == ["V", "V", "V", "I", "P", "C", "", ""]
    frame = filter_supervised_rows(pd.DataFrame({"status": status}))
    assert frame["status"].tolist() == ["V", " v ", '"V"', "I"]
    assert make_target_non_valid(frame).tolist() == [0, 0, 0, 1]


def test_non_terminal_status_rejected_for_target_construction():
    frame = pd.DataFrame({"status": ["V", "P", "C", "", None]})
    with pytest.raises(ValueError):
        make_target_non_valid(frame)


def test_leakage_columns_do_not_enter_features():
    frame = pd.DataFrame(
        {
            "status": ["V"],
            "approved": [1],
            "reason": ["bad"],
            "input_qe": ["reviewer"],
            "column_a": ["unknown"],
            "do2_type": ["1"],
        }
    )
    result = build_features(frame)
    assert "status" not in result.frame.columns
    assert "approved" not in result.frame.columns
    assert "reason" not in result.frame.columns
    assert "input_qe" not in result.frame.columns
    assert "column_a" not in result.frame.columns
    assert leakage_columns_present(frame.columns) == [
        "approved",
        "column_a",
        "input_qe",
        "reason",
        "status",
    ]


def test_army_history_features_do_not_expose_raw_identity():
    frame = pd.DataFrame(
        {
            "army_no": ["A1", "A1", "A1", "B2"],
            "status": ["V", "I", "MR", "V"],
            "do2_date": ["2021-01-01", "2021-02-01", "2021-03-01", "2021-01-15"],
        }
    )

    result = build_features(frame).frame

    assert "army_no" not in result.columns
    assert result["army_prior_record_count"].tolist() == [0.0, 1.0, 2.0, 0.0]
    assert result["army_prior_non_valid_count"].tolist() == [0.0, 0.0, 1.0, 0.0]
    assert result["army_prior_non_valid_rate"].round(3).tolist() == [0.0, 0.0, 0.5, 0.0]


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
        }
    )
    result = build_features(frame)
    assert result.frame["duration_days"].tolist() == [4.0, -9.0]
    assert result.frame["submission_delay_days"].tolist() == [2.0, -1.0]
    assert result.frame["has_rate"].tolist() == [0, 1]
    assert result.frame["from_after_to"].tolist() == [0, 1]
    assert result.frame.loc[1, "do2_type"] == "__MISSING__"
    assert np.isnan(result.frame.loc[0, "rate"])


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


def test_duplicate_open_do2_uses_only_prior_v_or_p_records():
    frame = pd.DataFrame(
        {
            "army_no": ["A1", "A1", "A1", "A1", "A1"],
            "do2_type": ["T1", "T1", "T1", "T1", "T2"],
            "status": ["V", "I", "P", "MR", "I"],
            "created_at": [
                "2024-01-01",
                "2024-01-02",
                "2024-01-03",
                "2024-01-04",
                "2024-01-05",
            ],
            "from_date": [
                "2024-02-01",
                "2024-02-05",
                "2024-03-01",
                "2024-03-10",
                "2024-02-05",
            ],
            "to_date": [
                "2024-02-10",
                "2024-02-07",
                "2024-03-20",
                "2024-03-15",
                "2024-02-07",
            ],
        }
    )

    result = build_features(frame).frame

    assert result["has_duplicate_open_do2"].tolist() == [0, 1, 0, 1, 0]


def test_prior_rate_features_are_as_of_and_have_count_floor():
    statuses = ["V", "I", "V", "MR", "V", "I", "V"]
    frame = pd.DataFrame(
        {
            "army_no": ["A1"] * len(statuses),
            "unit_code": ["U1"] * len(statuses),
            "do2_type": ["T1"] * len(statuses),
            "status": statuses,
            "do2_date": pd.date_range("2024-01-01", periods=len(statuses), freq="D"),
        }
    )

    result = build_features(frame).frame

    global_rate = make_target_non_valid(frame).mean()
    assert result.loc[0, "army_no_prior_non_valid_rate"] == pytest.approx(global_rate)
    assert result.loc[4, "army_no_prior_non_valid_rate"] == pytest.approx(global_rate)
    assert result.loc[5, "army_no_prior_non_valid_rate"] == pytest.approx(2 / 5)
    assert result.loc[6, "unit_code_prior_non_valid_rate"] == pytest.approx(3 / 6)


def test_army_no_placeholder_excluded_from_army_history():
    frame = pd.DataFrame(
        {
            "army_no": ["JC490821", "JC490821", "A1", "A1"],
            "fk_employee": ["E1", "E2", "E3", "E3"],
            "status": ["V", "I", "V", "I"],
            "do2_date": ["2024-01-01", "2024-01-02", "2024-01-01", "2024-01-02"],
        }
    )

    result = build_features(frame).frame

    assert result["army_no_is_placeholder"].tolist() == [1, 1, 0, 0]
    assert result["army_prior_record_count"].tolist() == [0.0, 0.0, 0.0, 1.0]
