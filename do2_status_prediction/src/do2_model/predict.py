from __future__ import annotations

import argparse
import json
from pathlib import Path

import numpy as np
import pandas as pd
from catboost import CatBoostClassifier

from do2_model.config import Settings
from do2_model.database import get_engine, iter_date_range, read_input_file
from do2_model.features import actual_status_group, build_features, normalize_status


def load_model_assets() -> tuple[CatBoostClassifier, dict]:
    settings = Settings()
    metadata_path = settings.model_root / "model_metadata.json"
    model_path = settings.model_root / "do2_catboost.cbm"
    if not metadata_path.exists() or not model_path.exists():
        raise FileNotFoundError("Train the model first; model files were not found.")

    with metadata_path.open("r", encoding="utf-8") as handle:
        metadata = json.load(handle)
    for key in ("feature_columns", "categorical_columns", "selected_probability_non_valid_threshold"):
        if key not in metadata:
            raise KeyError(f"Model metadata is missing required key: {key}")
    model = CatBoostClassifier()
    model.load_model(model_path)
    return model, metadata


def predict_frame(frame: pd.DataFrame) -> pd.DataFrame:
    model, metadata = load_model_assets()
    features = build_features(
        frame,
        expected_columns=metadata["feature_columns"],
        expected_categorical=metadata["categorical_columns"],
    )
    probability_non_valid = model.predict_proba(features.frame)[:, 1]
    threshold = float(metadata["selected_probability_non_valid_threshold"])
    predicted_non_valid = probability_non_valid >= threshold

    output = pd.DataFrame(index=frame.index)
    for column in ("id", "created_at"):
        if column in frame:
            output[column] = frame[column]
    if "status" in frame:
        output["actual_status"] = normalize_status(frame["status"])
        output["actual_status_group"] = actual_status_group(frame)
    output["non_valid_probability"] = probability_non_valid
    output["predicted_is_non_valid"] = predicted_non_valid.astype("int8")
    output["predicted_status_group"] = np.where(predicted_non_valid, "NON_VALID", "V")
    output["threshold_used"] = threshold
    output["threshold_policy"] = metadata.get("threshold_policy", "unknown")
    output["model_version"] = metadata.get("model_version", "unknown")
    output["hard_rule_failure"] = 0
    output["review_rule_signal"] = 0
    output["recommended_action"] = np.where(
        predicted_non_valid,
        "HUMAN_REVIEW",
        "AUTO_VALID_CANDIDATE",
    )
    return output.reset_index(drop=True)


def main() -> None:
    parser = argparse.ArgumentParser(description="Predict DO2 status for a CSV/Parquet file.")
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--input", type=Path)
    source.add_argument(
        "--start-date",
        help="Read directly from PostgreSQL starting on this date (inclusive).",
    )
    parser.add_argument(
        "--end-date",
        help="Required with --start-date; PostgreSQL end date is exclusive.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("reports/predictions/new_predictions.parquet"),
    )
    args = parser.parse_args()

    if args.input:
        frame = read_input_file(args.input)
    else:
        if not args.end_date:
            parser.error("--end-date is required with --start-date")
        settings = Settings()
        engine = get_engine(settings)
        try:
            chunks = list(
                iter_date_range(
                    engine,
                    settings,
                    args.start_date,
                    args.end_date,
                    require_status=False,
                )
            )
        finally:
            engine.dispose()
        if not chunks:
            raise RuntimeError("No PostgreSQL rows were found in the requested date range.")
        frame = pd.concat(chunks, ignore_index=True)
    output = predict_frame(frame)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.suffix.lower() == ".csv":
        output.to_csv(args.output, index=False)
    else:
        output.to_parquet(args.output, index=False)
    print(f"Saved {len(output):,} predictions to {args.output}")


if __name__ == "__main__":
    main()
