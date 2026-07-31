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
    status = pd.Series(["V", " v ", '"V"', "I", "P", "", None])
    assert normalize_status(status).tolist() == ["V", "V", "V", "I", "P", "", ""]
    frame = filter_supervised_rows(pd.DataFrame({"status": status}))
    assert make_target_non_valid(frame).tolist() == [0, 0, 0, 1, 1]


def test_blank_status_rejected_for_target_construction():
    frame = pd.DataFrame({"status": ["V", "", None]})
    with pytest.raises(ValueError):
        make_target_non_valid(frame)


def test_leakage_columns_do_not_enter_features():
    frame = pd.DataFrame({"status": ["V"], "approved": [1], "reason": ["bad"], "do2_type": ["1"]})
    result = build_features(frame)
    assert "status" not in result.frame.columns
    assert "approved" not in result.frame.columns
    assert "reason" not in result.frame.columns
    assert leakage_columns_present(frame.columns) == ["approved", "reason", "status"]


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
    assert np.isnan(result.frame.loc[0, "submission_delay_days"])
