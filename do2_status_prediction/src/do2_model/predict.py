from __future__ import annotations

import json
from pathlib import Path

import numpy as np
import pandas as pd

from do2_model.database import read_input_file
from do2_model.features import build_features, normalize_status, validate_prediction_columns
from do2_model.rule_layer import rule_decisions

__all__ = ["load_model_assets", "main", "predict_frame"]


def load_model_assets(model_dir: Path = Path("models_multiclass")):
    from catboost import CatBoostClassifier

    metadata_path = model_dir / "model_metadata_multiclass.json"
    model_path = model_dir / "do2_catboost_multiclass.cbm"
    if not metadata_path.exists() or not model_path.exists():
        raise FileNotFoundError("Train the multiclass model first; model files were not found.")

    with metadata_path.open("r", encoding="utf-8") as handle:
        metadata = json.load(handle)
    for key in ("feature_columns", "categorical_columns", "classes"):
        if key not in metadata:
            raise KeyError(f"Multiclass model metadata is missing required key: {key}")
    model = CatBoostClassifier()
    model.load_model(model_path)
    return model, metadata


def predict_frame(frame: pd.DataFrame, *, model_dir: Path = Path("models_multiclass")) -> pd.DataFrame:
    model, metadata = load_model_assets(model_dir)
    validate_prediction_columns(frame, metadata.get("required_input_columns", []))
    features = build_features(
        frame,
        expected_columns=metadata["feature_columns"],
        expected_categorical=metadata["categorical_columns"],
        expected_text=metadata.get("text_columns", []),
    )
    probabilities = model.predict_proba(features.frame)
    classes = [str(value) for value in metadata["classes"]]
    predicted_index = probabilities.argmax(axis=1)
    predicted_status = [classes[index] for index in predicted_index]

    output = pd.DataFrame(index=frame.index)
    for column in ("id", "created_at", "do2_date", "do2_type", "do2_group", "unit_code"):
        if column in frame:
            output[column] = frame[column]
    if "status" in frame:
        output["actual_status"] = normalize_status(frame["status"])
    output["layer2_ml_status"] = predicted_status
    output["layer2_ml_probability"] = probabilities.max(axis=1)
    for index, status_class in enumerate(classes):
        output[f"probability_status_{status_class}"] = probabilities[:, index]
    layer1 = rule_decisions(frame)
    for column in layer1.columns:
        output[column] = layer1[column].to_numpy()
    layer1_has_decision = output["layer1_status"].ne("")
    output["predicted_status"] = output["layer2_ml_status"].where(
        ~layer1_has_decision, output["layer1_status"]
    )
    output["predicted_status_probability"] = output["layer2_ml_probability"].where(
        ~layer1_has_decision, output["layer1_confidence"]
    )
    output["prediction_layer"] = np.where(layer1_has_decision, "layer1_rules", "layer2_ml")
    output["model_version"] = metadata.get("model_version", "unknown")
    return output.reset_index(drop=True)


def main() -> None:
    import argparse

    parser = argparse.ArgumentParser(description="Predict raw DO2 status using multiclass model.")
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--model-dir", type=Path, default=Path("models_multiclass"))
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("reports_multiclass/new_predictions.parquet"),
    )
    args = parser.parse_args()

    frame = read_input_file(args.input)
    output = predict_frame(frame, model_dir=args.model_dir)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.suffix.lower() == ".csv":
        output.to_csv(args.output, index=False)
    else:
        output.to_parquet(args.output, index=False)
    print(f"Saved {len(output):,} multiclass predictions to {args.output}")


if __name__ == "__main__":
    main()
