from __future__ import annotations

import argparse
import json
from pathlib import Path

import numpy as np
import pandas as pd
from catboost import CatBoostClassifier

from do2_model.config import Settings
from do2_model.dataio import load_split
from do2_model.evaluation import (
    classification_metrics,
    group_metrics,
    threshold_policy_table,
    threshold_table,
)
from do2_model.features import actual_status_group, build_features

GRID_THRESHOLDS = (0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 0.50)
DIAGNOSTIC_COLUMNS = ("id", "created_at", "do2_date", "do2_type", "do2_group", "unit_code", "status")


def _read_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def _confusion_matrix(metrics: dict) -> pd.DataFrame:
    return pd.DataFrame(
        [
            [metrics["true_valid_predicted_valid"], metrics["valid_flagged_non_valid"]],
            [metrics["non_valid_missed_as_valid"], metrics["non_valid_detected"]],
        ],
        index=pd.Index(["Actual V", "Actual NON_VALID"], name="actual"),
        columns=pd.Index(["Predicted V", "Predicted NON_VALID"], name="predicted"),
    )


def _row_normalized(matrix: pd.DataFrame) -> pd.DataFrame:
    return matrix.div(matrix.sum(axis=1), axis=0).mul(100).round(2)


def _load_existing_predictions(path: Path) -> pd.DataFrame | None:
    if not path.exists():
        return None
    predictions = pd.read_parquet(path)
    required = {"actual_status_group", "non_valid_probability"}
    if not required.issubset(predictions.columns):
        return None
    return predictions


def _score_split(settings: Settings, split: str, metadata: dict) -> pd.DataFrame:
    existing = _load_existing_predictions(settings.report_root / f"{split}_predictions.parquet")
    if existing is not None:
        return existing

    raw = load_split(settings.data_root / split, supervised_only=True)
    model = CatBoostClassifier()
    model.load_model(settings.model_root / "do2_catboost.cbm")
    features = build_features(
        raw,
        expected_columns=metadata["feature_columns"],
        expected_categorical=metadata["categorical_columns"],
    )
    probability = model.predict_proba(features.frame)[:, 1]

    output = pd.DataFrame(index=raw.index)
    for column in DIAGNOSTIC_COLUMNS:
        if column in raw:
            output[column] = raw[column]
    output["actual_status_group"] = actual_status_group(raw)
    output["non_valid_probability"] = probability
    return output.reset_index(drop=True)


def _y_true(predictions: pd.DataFrame) -> np.ndarray:
    return predictions["actual_status_group"].eq("NON_VALID").astype("int8").to_numpy()


def _metrics_at_thresholds(
    split: str,
    predictions: pd.DataFrame,
    thresholds: list[float],
) -> pd.DataFrame:
    y_true = _y_true(predictions)
    probability = predictions["non_valid_probability"].to_numpy()
    rows = []
    for threshold in thresholds:
        rows.append(
            {
                "split": split,
                **classification_metrics(y_true, probability, threshold),
            }
        )
    return pd.DataFrame(rows)


def _monthly_metrics(predictions: pd.DataFrame, threshold: float) -> pd.DataFrame:
    if "do2_date" not in predictions:
        return pd.DataFrame()
    month = pd.to_datetime(predictions["do2_date"], errors="coerce").dt.to_period("M")
    return group_metrics(
        month.astype("string"),
        _y_true(predictions),
        predictions["non_valid_probability"].to_numpy(),
        threshold,
    )


def _attach_raw_test_columns(settings: Settings, predictions: pd.DataFrame) -> pd.DataFrame:
    needed = {"do2_type", "do2_group", "unit_code"}
    if needed.issubset(predictions.columns):
        return predictions
    if "id" not in predictions.columns:
        return predictions

    raw = load_split(settings.data_root / "test", supervised_only=False)
    keep = [column for column in DIAGNOSTIC_COLUMNS if column in raw.columns]
    if "id" not in keep:
        return predictions
    raw = raw[keep].drop_duplicates("id")
    return predictions.merge(raw, on="id", how="left", suffixes=("", "_raw"))


def _november_false_negative_diagnostics(
    settings: Settings,
    test_predictions: pd.DataFrame,
    threshold: float,
) -> dict[str, pd.DataFrame]:
    predictions = _attach_raw_test_columns(settings, test_predictions.copy())
    predicted_non_valid = predictions["non_valid_probability"].ge(threshold)
    false_negative = predictions[
        predictions["actual_status_group"].eq("NON_VALID") & ~predicted_non_valid
    ].copy()
    date = pd.to_datetime(false_negative.get("do2_date"), errors="coerce")
    november = false_negative[date.dt.to_period("M").astype("string").eq("2025-11")]

    breakdowns: dict[str, pd.DataFrame] = {}
    for column in ("do2_type", "do2_group", "unit_code", "actual_status"):
        if column in november:
            breakdowns[column] = (
                november[column]
                .fillna("__MISSING__")
                .astype(str)
                .value_counts()
                .rename_axis(column)
                .reset_index(name="false_negative_rows")
                .head(20)
            )

    good_month = predictions[
        pd.to_datetime(predictions.get("do2_date"), errors="coerce")
        .dt.to_period("M")
        .astype("string")
        .eq("2025-10")
    ].copy()
    november_all = predictions[
        pd.to_datetime(predictions.get("do2_date"), errors="coerce")
        .dt.to_period("M")
        .astype("string")
        .eq("2025-11")
    ].copy()
    breakdowns["month_probability_summary"] = pd.DataFrame(
        [
            {
                "month": "2025-10",
                "rows": len(good_month),
                "mean_probability": good_month["non_valid_probability"].mean(),
                "p50_probability": good_month["non_valid_probability"].quantile(0.50),
                "p90_probability": good_month["non_valid_probability"].quantile(0.90),
            },
            {
                "month": "2025-11",
                "rows": len(november_all),
                "mean_probability": november_all["non_valid_probability"].mean(),
                "p50_probability": november_all["non_valid_probability"].quantile(0.50),
                "p90_probability": november_all["non_valid_probability"].quantile(0.90),
            },
        ]
    )
    return breakdowns


def run_diagnostics() -> None:
    settings = Settings()
    output_dir = settings.report_root / "threshold_diagnostics"
    output_dir.mkdir(parents=True, exist_ok=True)

    metadata = _read_json(settings.model_root / "model_metadata.json")
    threshold = float(metadata["selected_probability_non_valid_threshold"])
    print(f"Using metadata selected_probability_non_valid_threshold={threshold:.4f}")

    validation_predictions = _score_split(settings, "validation", metadata)
    test_predictions = _score_split(settings, "test", metadata)

    for split, predictions in (
        ("validation", validation_predictions),
        ("test", test_predictions),
    ):
        metrics = classification_metrics(
            _y_true(predictions),
            predictions["non_valid_probability"].to_numpy(),
            threshold,
        )
        matrix = _confusion_matrix(metrics)
        matrix.to_csv(output_dir / f"{split}_confusion_matrix.csv")
        _row_normalized(matrix).to_csv(output_dir / f"{split}_confusion_matrix_row_pct.csv")
        print(f"\n{split} confusion matrix at threshold={threshold:.4f}")
        print(matrix.to_string())
        print(f"\n{split} row-normalized confusion matrix (%)")
        print(_row_normalized(matrix).to_string())

    validation_thresholds = threshold_table(
        _y_true(validation_predictions),
        validation_predictions["non_valid_probability"].to_numpy(),
    )
    policy_comparison = threshold_policy_table(validation_thresholds)
    policy_comparison.to_csv(output_dir / "validation_threshold_policy_comparison.csv", index=False)

    grid = pd.concat(
        [
            _metrics_at_thresholds("validation", validation_predictions, list(GRID_THRESHOLDS)),
            _metrics_at_thresholds("test", test_predictions, list(GRID_THRESHOLDS)),
        ],
        ignore_index=True,
    )
    grid_columns = [
        "split",
        "threshold",
        "precision_non_valid",
        "recall_non_valid",
        "f1_non_valid",
        "f2_non_valid",
        "human_review_rate",
    ]
    grid[grid_columns].to_csv(output_dir / "threshold_grid_validation_test.csv", index=False)

    monthly = _monthly_metrics(test_predictions, threshold)
    monthly_columns = [
        "group",
        "rows",
        "actual_non_valid_rate",
        "predicted_non_valid_rate",
        "accuracy",
        "precision_non_valid",
        "recall_non_valid",
        "f1_non_valid",
        "non_valid_missed_as_valid",
        "valid_review_rate",
    ]
    monthly[[column for column in monthly_columns if column in monthly]].to_csv(
        output_dir / "test_metrics_by_month_at_metadata_threshold.csv",
        index=False,
    )

    max_f2_threshold = float(
        policy_comparison.loc[policy_comparison["policy"].eq("max_f2"), "threshold"].iloc[0]
    )
    before_after = pd.concat(
        [
            _metrics_at_thresholds("test_current_metadata_threshold", test_predictions, [threshold]),
            _metrics_at_thresholds("test_max_f2_threshold", test_predictions, [max_f2_threshold]),
        ],
        ignore_index=True,
    )
    before_after[grid_columns].to_csv(output_dir / "test_current_vs_max_f2.csv", index=False)

    for name, frame in _november_false_negative_diagnostics(
        settings, test_predictions, threshold
    ).items():
        frame.to_csv(output_dir / f"november_false_negative_{name}.csv", index=False)

    print("\nPolicy comparison on validation:")
    print(policy_comparison.to_string(index=False))
    print("\nTest threshold grid:")
    print(grid.loc[grid["split"].eq("test"), grid_columns].to_string(index=False))
    print(
        "\nRecommendation: prefer max_f2 or recall_80 over max_f1 when missed non-valid "
        "records cost materially more than extra human reviews. Validate the chosen review "
        "rate operationally before deployment."
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Run threshold-aware DO2 diagnostics.")
    parser.parse_args()
    run_diagnostics()


if __name__ == "__main__":
    main()
