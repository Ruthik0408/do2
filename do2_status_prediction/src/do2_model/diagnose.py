from __future__ import annotations

import argparse
import json
from pathlib import Path

import numpy as np
import pandas as pd
from catboost import CatBoostClassifier

from do2_model.config import Settings
from do2_model.dataio import load_split
from do2_model.evaluation import classification_metrics, group_metrics
from do2_model.features import (
    actual_status_group,
    build_features,
    filter_supervised_rows,
    make_target_non_valid,
    normalize_status,
)


def prediction_output(
    raw: pd.DataFrame,
    target: np.ndarray,
    probability_non_valid: np.ndarray,
    threshold: float,
) -> pd.DataFrame:
    predicted_non_valid = probability_non_valid >= threshold

    output: dict[str, np.ndarray] = {}

    # Keep useful columns for investigating errors.
    for column in (
        "id",
        "created_at",
        "do2_type",
        "do2_group",
        "unit_code",
        "description",
    ):
        if column in raw.columns:
            output[column] = raw[column].to_numpy()

    output["actual_status"] = normalize_status(raw["status"]).to_numpy()
    output["actual_status_group"] = actual_status_group(raw).to_numpy()
    output["non_valid_probability"] = probability_non_valid
    output["predicted_is_non_valid"] = predicted_non_valid.astype("int8")
    output["predicted_status_group"] = np.where(predicted_non_valid, "NON_VALID", "V")
    output["threshold_used"] = threshold

    result = pd.DataFrame(output)

    if "created_at" in result.columns:
        created = pd.to_datetime(result["created_at"], errors="coerce")
        result["created_month"] = (
            created.dt.to_period("M").astype("string")
        )

    result["is_correct"] = (
        result["actual_status_group"] == result["predicted_status_group"]
    ).astype("int8")

    return result


def save_group_report(
    groups: pd.Series,
    target: np.ndarray,
    probability: np.ndarray,
    threshold: float,
    path: Path,
) -> None:
    try:
        report = group_metrics(
            groups,
            target,
            probability,
            threshold,
            minimum_rows=100,
        )
    except KeyError:
        print(f"No groups with at least 100 rows for {path.name}")
        return

    if report.empty:
        print(f"No groups with at least 100 rows for {path.name}")
        return

    report.to_csv(path, index=False)
    print(f"Saved {path}")


def diagnose_split(
    *,
    split_name: str,
    data_directory: Path,
    model: CatBoostClassifier,
    metadata: dict,
    threshold: float,
    report_directory: Path,
) -> dict:
    print(f"Loading {split_name} data...")

    raw = filter_supervised_rows(load_split(data_directory))

    features = build_features(
        raw,
        expected_columns=metadata["feature_columns"],
        expected_categorical=metadata["categorical_columns"],
    )

    target = make_target_non_valid(raw).to_numpy()
    probability = model.predict_proba(features.frame)[:, 1]

    metrics = classification_metrics(
        target,
        probability,
        threshold,
    )

    predictions = prediction_output(
        raw,
        target,
        probability,
        threshold,
    )

    predictions.to_parquet(
        report_directory / f"{split_name}_predictions.parquet",
        index=False,
    )

    false_positives = predictions[
        (predictions["actual_status_group"] == "V")
        & (predictions["predicted_status_group"] == "NON_VALID")
    ]

    false_negatives = predictions[
        (predictions["actual_status_group"] == "NON_VALID")
        & (predictions["predicted_status_group"] == "V")
    ]

    false_positives.to_csv(
        report_directory / f"{split_name}_false_positives.csv",
        index=False,
    )
    false_negatives.to_csv(
        report_directory / f"{split_name}_false_negatives.csv",
        index=False,
    )

    if "do2_type" in raw.columns:
        save_group_report(
            raw["do2_type"],
            target,
            probability,
            threshold,
            report_directory
            / f"{split_name}_metrics_by_do2_type.csv",
        )

    if "do2_group" in raw.columns:
        save_group_report(
            raw["do2_group"],
            target,
            probability,
            threshold,
            report_directory
            / f"{split_name}_metrics_by_do2_group.csv",
        )

    if "created_at" in raw.columns:
        month = (
            pd.to_datetime(raw["created_at"], errors="coerce")
            .dt.to_period("M")
            .astype("string")
            .fillna("__MISSING__")
        )

        save_group_report(
            month,
            target,
            probability,
            threshold,
            report_directory
            / f"{split_name}_metrics_by_month.csv",
        )

    return metrics


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Generate detailed reports for a saved DO2 model."
    )
    parser.add_argument(
        "--model-dir",
        default="models_sqrt_baseline",
    )
    parser.add_argument(
        "--report-dir",
        default="reports_sqrt_baseline",
    )
    args = parser.parse_args()

    settings = Settings()
    model_directory = Path(args.model_dir)
    report_directory = Path(args.report_dir)
    report_directory.mkdir(parents=True, exist_ok=True)

    metadata_path = model_directory / "model_metadata.json"
    model_path = model_directory / "do2_catboost.cbm"

    if not metadata_path.exists():
        raise FileNotFoundError(
            f"Metadata file not found: {metadata_path}"
        )

    if not model_path.exists():
        raise FileNotFoundError(
            f"Model file not found: {model_path}"
        )

    with metadata_path.open("r", encoding="utf-8") as handle:
        metadata = json.load(handle)

    threshold = float(
        metadata["selected_probability_non_valid_threshold"]
    )

    model = CatBoostClassifier()
    model.load_model(model_path)

    validation_metrics = diagnose_split(
        split_name="validation",
        data_directory=settings.data_root / "validation",
        model=model,
        metadata=metadata,
        threshold=threshold,
        report_directory=report_directory,
    )

    test_metrics = diagnose_split(
        split_name="test",
        data_directory=settings.data_root / "test",
        model=model,
        metadata=metadata,
        threshold=threshold,
        report_directory=report_directory,
    )

    diagnostic_metrics = {
        "threshold": threshold,
        "validation": validation_metrics,
        "test": test_metrics,
    }

    output_path = report_directory / "diagnostic_metrics.json"
    with output_path.open("w", encoding="utf-8") as handle:
        json.dump(diagnostic_metrics, handle, indent=2)

    print("\nSelected threshold:", threshold)
    print("Validation metrics:", validation_metrics)
    print("Test metrics:", test_metrics)
    print(f"Reports saved in {report_directory}")


if __name__ == "__main__":
    main()
