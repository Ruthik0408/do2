from __future__ import annotations

from pathlib import Path

import pandas as pd

from do2_model.dataio import load_split


def _load_keys(data_root: Path, split_name: str, key_columns: list[str]) -> pd.DataFrame:
    frame = load_split(data_root / split_name, supervised_only=False)
    missing = [column for column in key_columns if column not in frame.columns]
    if missing:
        raise KeyError(f"{split_name} split is missing key columns: {missing}")
    return frame[key_columns].dropna(how="all").drop_duplicates()


def check_cross_split_overlap(data_root: Path, key_columns: list[str]) -> pd.DataFrame:
    if not key_columns:
        raise ValueError("At least one key column is required for overlap checks.")

    train = _load_keys(data_root, "train", key_columns)
    validation = _load_keys(data_root, "validation", key_columns)
    test = _load_keys(data_root, "test", key_columns)

    train_keys = set(map(tuple, train.to_numpy()))
    validation_keys = set(map(tuple, validation.to_numpy()))
    test_keys = set(map(tuple, test.to_numpy()))

    return pd.DataFrame(
        [
            {"overlap": "train_vs_validation", "count": len(train_keys & validation_keys)},
            {"overlap": "train_vs_test", "count": len(train_keys & test_keys)},
            {"overlap": "validation_vs_test", "count": len(validation_keys & test_keys)},
        ]
    )
