from __future__ import annotations

import argparse
import importlib.metadata
from datetime import datetime, timezone
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix, f1_score
from sklearn.preprocessing import LabelEncoder

from do2_model.config import SPLITS, Settings, load_feature_config
from do2_model.dataio import load_split, sampling_profile
from do2_model.features import build_features
from do2_model.multiclass import (
    dropped_multiclass_status_report,
    filter_multiclass_status_rows,
    make_multiclass_status_target,
    write_json,
)


def _load_multiclass_split(
    settings: Settings,
    split_name: str,
    *,
    report_dir: Path,
    max_rows: int = 0,
) -> pd.DataFrame:
    raw = load_split(
        settings.data_root / split_name,
        max_rows=max_rows,
        random_seed=settings.random_seed,
        supervised_only=False,
        stratify_sample=False,
    )
    dropped_multiclass_status_report(raw).to_csv(
        report_dir / f"{split_name}_status_filter_report.csv", index=False
    )
    return filter_multiclass_status_rows(raw)


def _classification_outputs(
    raw: pd.DataFrame,
    y_true: np.ndarray,
    predicted: np.ndarray,
    probabilities: np.ndarray,
    classes: list[str],
) -> pd.DataFrame:
    output = pd.DataFrame(index=raw.index)
    for column in ("id", "created_at", "do2_date", "do2_type", "do2_group", "unit_code"):
        if column in raw:
            output[column] = raw[column]
    output["actual_status"] = y_true
    output["predicted_status"] = predicted
    output["predicted_status_probability"] = probabilities.max(axis=1)
    output["is_correct"] = output["actual_status"].eq(output["predicted_status"]).astype("int8")
    for index, status_class in enumerate(classes):
        output[f"probability_status_{status_class}"] = probabilities[:, index]
    return output.reset_index(drop=True)


def _metrics(y_true: np.ndarray, predicted: np.ndarray) -> dict[str, float | int]:
    return {
        "rows": len(y_true),
        "accuracy": float(accuracy_score(y_true, predicted)),
        "macro_f1": float(f1_score(y_true, predicted, average="macro", zero_division=0)),
        "weighted_f1": float(f1_score(y_true, predicted, average="weighted", zero_division=0)),
    }


def run_training(
    *,
    max_train_rows: int,
    max_validation_rows: int,
    max_test_rows: int,
    iterations: int,
    depth: int,
    learning_rate: float,
    l2_leaf_reg: float,
    min_data_in_leaf: int,
    task_type: str,
    devices: str,
    thread_count: int,
    auto_class_weights: str | None,
    model_dir: Path,
    report_dir: Path,
) -> dict:
    from catboost import CatBoostClassifier

    settings = Settings()
    model_dir.mkdir(parents=True, exist_ok=True)
    report_dir.mkdir(parents=True, exist_ok=True)

    print(f"Loading multiclass train data: {SPLITS['train'][0]} to {SPLITS['train'][1]}...")
    train_raw = _load_multiclass_split(
        settings,
        "train",
        report_dir=report_dir,
        max_rows=max_train_rows,
    )
    print(f"Loading multiclass validation data: {SPLITS['validation'][0]} to {SPLITS['validation'][1]}...")
    validation_raw = _load_multiclass_split(
        settings,
        "validation",
        report_dir=report_dir,
        max_rows=max_validation_rows,
    )
    print(f"Loading multiclass test data: {SPLITS['test'][0]} to {SPLITS['test'][1]}...")
    test_raw = _load_multiclass_split(
        settings,
        "test",
        report_dir=report_dir,
        max_rows=max_test_rows,
    )

    sampling_profile(train_raw).to_csv(report_dir / "train_status_distribution.csv", index=False)
    sampling_profile(validation_raw).to_csv(report_dir / "validation_status_distribution.csv", index=False)
    sampling_profile(test_raw).to_csv(report_dir / "test_status_distribution.csv", index=False)

    train_features = build_features(train_raw)
    validation_features = build_features(
        validation_raw,
        expected_columns=list(train_features.frame.columns),
        expected_categorical=train_features.categorical_columns,
        expected_text=train_features.text_columns,
    )
    test_features = build_features(
        test_raw,
        expected_columns=list(train_features.frame.columns),
        expected_categorical=train_features.categorical_columns,
        expected_text=train_features.text_columns,
    )

    y_train_status = make_multiclass_status_target(train_raw).to_numpy()
    y_validation_status = make_multiclass_status_target(validation_raw).to_numpy()
    y_test_status = make_multiclass_status_target(test_raw).to_numpy()

    encoder = LabelEncoder()
    y_train = encoder.fit_transform(y_train_status)
    classes = encoder.classes_.astype(str).tolist()
    validation_seen_mask = pd.Series(y_validation_status).isin(classes).to_numpy()
    unseen_validation_classes = sorted(set(y_validation_status) - set(classes))
    unseen_test_classes = sorted(set(y_test_status) - set(classes))
    if unseen_validation_classes:
        print(
            "Validation contains statuses not present in training and excluded from CatBoost eval_set: "
            f"{unseen_validation_classes}"
        )
    if unseen_test_classes:
        print(
            "Test contains statuses not present in training; model cannot predict them: "
            f"{unseen_test_classes}"
        )

    print(
        f"Training rows: {len(y_train):,}; classes: {classes}; "
        f"features: {train_features.frame.shape[1]}"
    )
    model_params = {
        "iterations": iterations,
        "depth": depth,
        "learning_rate": learning_rate,
        "l2_leaf_reg": l2_leaf_reg,
        "min_data_in_leaf": min_data_in_leaf,
        "loss_function": "MultiClass",
        "eval_metric": "MultiClass",
        "random_seed": settings.random_seed,
        "task_type": task_type,
        "thread_count": thread_count,
        "allow_writing_files": False,
        "verbose": 100,
        "od_type": "Iter",
    }
    if auto_class_weights:
        model_params["auto_class_weights"] = auto_class_weights
    if task_type == "GPU":
        model_params["devices"] = devices

    model = CatBoostClassifier(**model_params)
    model.fit(
        train_features.frame,
        y_train,
        cat_features=train_features.categorical_columns,
        text_features=train_features.text_columns,
        eval_set=(
            validation_features.frame.loc[validation_seen_mask],
            encoder.transform(y_validation_status[validation_seen_mask]),
        ),
        early_stopping_rounds=100,
        use_best_model=True,
    )

    validation_probability = model.predict_proba(validation_features.frame)
    test_probability = model.predict_proba(test_features.frame)
    validation_predicted = encoder.inverse_transform(validation_probability.argmax(axis=1))
    test_predicted = encoder.inverse_transform(test_probability.argmax(axis=1))

    validation_metrics = _metrics(y_validation_status, validation_predicted)
    test_metrics = _metrics(y_test_status, test_predicted)
    validation_labels = sorted(set(classes) | set(y_validation_status))
    test_labels = sorted(set(classes) | set(y_test_status))
    validation_report = pd.DataFrame(
        classification_report(
            y_validation_status,
            validation_predicted,
            labels=validation_labels,
            output_dict=True,
            zero_division=0,
        )
    ).transpose()
    test_report = pd.DataFrame(
        classification_report(
            y_test_status,
            test_predicted,
            labels=test_labels,
            output_dict=True,
            zero_division=0,
        )
    ).transpose()
    validation_report.to_csv(report_dir / "validation_classification_report.csv")
    test_report.to_csv(report_dir / "test_classification_report.csv")

    pd.DataFrame(
        confusion_matrix(y_validation_status, validation_predicted, labels=validation_labels),
        index=pd.Index(validation_labels, name="actual"),
        columns=pd.Index(validation_labels, name="predicted"),
    ).to_csv(report_dir / "validation_confusion_matrix.csv")
    pd.DataFrame(
        confusion_matrix(y_test_status, test_predicted, labels=test_labels),
        index=pd.Index(test_labels, name="actual"),
        columns=pd.Index(test_labels, name="predicted"),
    ).to_csv(report_dir / "test_confusion_matrix.csv")

    _classification_outputs(
        validation_raw,
        y_validation_status,
        validation_predicted,
        validation_probability,
        classes,
    ).to_parquet(report_dir / "validation_predictions.parquet", index=False)
    _classification_outputs(
        test_raw,
        y_test_status,
        test_predicted,
        test_probability,
        classes,
    ).to_parquet(report_dir / "test_predictions.parquet", index=False)

    importance = pd.DataFrame(
        {
            "feature": train_features.frame.columns,
            "importance": model.get_feature_importance(),
        }
    ).sort_values("importance", ascending=False)
    importance.to_csv(report_dir / "feature_importance.csv", index=False)

    model_path = model_dir / "do2_catboost_multiclass.cbm"
    model.save_model(model_path)
    feature_config = load_feature_config()
    metadata = {
        "created_at_utc": datetime.now(timezone.utc).isoformat(),
        "model_version": datetime.now(timezone.utc).strftime("do2-catboost-multiclass-%Y%m%dT%H%M%SZ"),
        "model_type": "multiclass_status",
        "model_target": "every nonblank normalized status class observed in training",
        "prediction_strategy": "layer1_rule_decision_then_layer2_multiclass_ml",
        "rule_layer": {
            "enabled": True,
            "description": (
                "Deterministic Dolphin-derived blocking signals are evaluated before "
                "the CatBoost status model; ML is used when no rule emits a decision."
            ),
        },
        "classes": classes,
        "unseen_validation_classes": unseen_validation_classes,
        "unseen_test_classes": unseen_test_classes,
        "feature_columns": list(train_features.frame.columns),
        "categorical_columns": train_features.categorical_columns,
        "text_columns": train_features.text_columns,
        "splits": SPLITS,
        "split_date_column": feature_config["split_date_column"],
        "training_rows": len(train_raw),
        "validation_rows": len(validation_raw),
        "test_rows": len(test_raw),
        "max_train_rows": max_train_rows,
        "max_validation_rows": max_validation_rows,
        "max_test_rows": max_test_rows,
        "best_iteration": int(model.get_best_iteration()),
        "model_parameters": model.get_params(),
        "candidate_columns": feature_config["candidate_columns"],
        "required_input_columns": feature_config["required_input_columns"],
        "dependency_versions": {
            name: importlib.metadata.version(name)
            for name in ("catboost", "pandas", "numpy", "scikit-learn", "pyarrow")
        },
    }
    write_json(model_dir / "model_metadata_multiclass.json", metadata)
    metrics = {"validation": validation_metrics, "test": test_metrics}
    write_json(report_dir / "metrics_multiclass.json", metrics)

    print("Validation multiclass metrics:", validation_metrics)
    print("Test multiclass metrics:", test_metrics)
    print(f"Multiclass model saved to {model_path}")
    return metrics


def main() -> None:
    parser = argparse.ArgumentParser(description="Train multiclass CatBoost for raw DO2 status.")
    parser.add_argument("--max-train-rows", type=int, default=0)
    parser.add_argument("--max-validation-rows", type=int, default=0)
    parser.add_argument("--max-test-rows", type=int, default=0)
    parser.add_argument("--iterations", type=int, default=2000)
    parser.add_argument("--depth", type=int, default=6)
    parser.add_argument("--learning-rate", type=float, default=0.03)
    parser.add_argument("--l2-leaf-reg", type=float, default=10)
    parser.add_argument("--min-data-in-leaf", type=int, default=100)
    parser.add_argument("--task-type", choices=["CPU", "GPU"], default="CPU")
    parser.add_argument("--devices", default="0")
    parser.add_argument("--thread-count", type=int, default=-1)
    parser.add_argument(
        "--auto-class-weights",
        choices=["Balanced", "SqrtBalanced", "None"],
        default="SqrtBalanced",
        help="CatBoost class weighting for imbalanced status classes. Use None to disable.",
    )
    parser.add_argument("--model-dir", type=Path, default=Path("models_multiclass"))
    parser.add_argument("--report-dir", type=Path, default=Path("reports_multiclass"))
    args = parser.parse_args()

    run_training(
        max_train_rows=args.max_train_rows,
        max_validation_rows=args.max_validation_rows,
        max_test_rows=args.max_test_rows,
        iterations=args.iterations,
        depth=args.depth,
        learning_rate=args.learning_rate,
        l2_leaf_reg=args.l2_leaf_reg,
        min_data_in_leaf=args.min_data_in_leaf,
        task_type=args.task_type,
        devices=args.devices,
        thread_count=args.thread_count,
        auto_class_weights=None if args.auto_class_weights == "None" else args.auto_class_weights,
        model_dir=args.model_dir,
        report_dir=args.report_dir,
    )


if __name__ == "__main__":
    main()
