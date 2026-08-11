import pandas as pd
import pytest

from do2_model.identity_check import check_cross_split_overlap


def test_check_cross_split_overlap_counts_duplicate_business_keys(tmp_path):
    data_root = tmp_path
    for split, rows in {
        "train": [{"unit_code": "A", "do2_date": "2024-01-01"}, {"unit_code": "B", "do2_date": "2024-01-02"}],
        "validation": [{"unit_code": "A", "do2_date": "2024-01-01"}],
        "test": [{"unit_code": "B", "do2_date": "2024-01-02"}],
    }.items():
        split_dir = data_root / split
        split_dir.mkdir()
        pd.DataFrame(rows).to_parquet(split_dir / "part-00000.parquet", index=False)

    result = check_cross_split_overlap(data_root, ["unit_code", "do2_date"])

    assert dict(zip(result["overlap"], result["count"], strict=True)) == {
        "train_vs_validation": 1,
        "train_vs_test": 1,
        "validation_vs_test": 0,
    }


def test_check_cross_split_overlap_requires_key_columns(tmp_path):
    with pytest.raises(ValueError):
        check_cross_split_overlap(tmp_path, [])
