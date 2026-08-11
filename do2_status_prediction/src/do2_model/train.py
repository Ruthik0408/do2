from __future__ import annotations

import argparse
import importlib.metadata
from datetime import datetime, timezone

import numpy as np
import pandas as pd
from catboost import CatBoostClassifier

from do2_model.config import LEAKAGE_COLUMNS, SPLITS, Settings, load_feature_config
from do2_model.dataio import load_split, load_split_profile, sampling_profile
from do2_model.evaluation import (
    classification_metrics,
    group_metrics,
    select_threshold,
    status_recall_report,
    threshold_policy_table,
    threshold_table,
    write_json,
)
from do2_model.features import (
    actual_status_group,
    build_features,
    leakage_columns_present,
    make_target_non_valid,
    normalize_status,
)


def _prediction_output(
    raw: pd.DataFrame,
    target: np.ndarray,
    probability_non_valid: np.ndarray,
    threshold: float,
) -> pd.DataFrame:
    predicted_non_valid = probability_non_valid >= threshold
    output = pd.DataFrame(index=raw.index)
    if "id" in raw:
        output["id"] = raw["id"]
    if "created_at" in raw:
        output["created_at"] = raw["created_at"]
    if "do2_date" in raw:
        output["do2_date"] = raw["do2_date"]
    for column in ("do2_type", "do2_group", "unit_code"):
        if column in raw:
            output[column] = raw[column]
    output["actual_status"] = normalize_status(raw["status"])
    output["actual_status_group"] = actual_status_group(raw)
    output["non_valid_probability"] = probability_non_valid
    output["predicted_is_non_valid"] = predicted_non_valid.astype("int8")
    output["predicted_status_group"] = np.where(predicted_non_valid, "NON_VALID", "V")
    output["threshold_used"] = threshold
    output["is_correct"] = output["actual_status_group"].eq(output["predicted_status_group"]).astype("int8")
    output["recommended_action"] = np.where(
        predicted_non_valid,
        "HUMAN_REVIEW",
        "AUTO_VALID_CANDIDATE",
    )
    return output.reset_index(drop=True)


def run_training(
    *,
    max_train_rows: int,
    iterations: int,
    depth: int,
    learning_rate: float,
    l2_leaf_reg: float,
    min_data_in_leaf: int,
    task_type: str,
    thread_count: int,
    minimum_non_valid_recall: float,
    training_mode: str,
    threshold_policy: str,
    fixed_threshold: float | None,
) -> dict:
    settings = Settings()
    settings.ensure_output_directories()

    train_dir = settings.data_root / "train"
    train_start, train_end = SPLITS["train"]
    print(f"Loading training data: {train_start} to {train_end}...")
    original_train_profile = load_split_profile(train_dir)
    original_train_profile.to_csv(settings.report_root / "train_original_counts.csv", index=False)
    if training_mode == "sampled" and max_train_rows > 0:
        train_raw = load_split(
            train_dir,
            max_rows=max_train_rows,
            random_seed=settings.random_seed,
            supervised_only=True,
            stratify_sample=True,
        )
    else:
        train_raw = load_split(
            train_dir,
            max_rows=0,
            random_seed=settings.random_seed,
            supervised_only=True,
        )
        if training_mode == "full":
            estimated_mb = train_raw.memory_usage(deep=True).sum() / (1024 * 1024)
            print(f"Full training rows: {len(train_raw):,}; dataframe memory: {estimated_mb:,.1f} MB")
    sampling_profile(train_raw).to_csv(settings.report_root / "train_sampled_counts.csv", index=False)
    validation_start, validation_end = SPLITS["validation"]
    test_start, test_end = SPLITS["test"]

    print(f"Loading validation data: {validation_start} to {validation_end}...")
    validation_raw = load_split(settings.data_root / "validation", supervised_only=True)
    print(f"Loading test data: {test_start} to {test_end}...")
    test_raw = load_split(settings.data_root / "test", supervised_only=True)

    train_features = build_features(train_raw)
    validation_features = build_features(
        validation_raw,
        expected_columns=list(train_features.frame.columns),
        expected_categorical=train_features.categorical_columns,
    )
    test_features = build_features(
        test_raw,
        expected_columns=list(train_features.frame.columns),
        expected_categorical=train_features.categorical_columns,
    )

    y_train = make_target_non_valid(train_raw).to_numpy()
    y_validation = make_target_non_valid(validation_raw).to_numpy()
    y_test = make_target_non_valid(test_raw).to_numpy()
    for split_name, target in (
        ("train", y_train),
        ("validation", y_validation),
        ("test", y_test),
    ):
        if len(np.unique(target)) != 2:
            raise RuntimeError(
                f"{split_name} contains only one target class; binary metrics/training are invalid."
            )

    print(
        f"Training rows: {len(y_train):,}; non-valid: {y_train.mean():.2%}. "
        f"Features: {train_features.frame.shape[1]}"
    )
    model = CatBoostClassifier(
        iterations=iterations,
        depth=depth,
        learning_rate=learning_rate,
        l2_leaf_reg=l2_leaf_reg,
        min_data_in_leaf=min_data_in_leaf,
        loss_function="Logloss",
        eval_metric="Logloss",
        custom_metric=["PRAUC:type=Classic", "AUC"],
        auto_class_weights="SqrtBalanced",
        random_seed=settings.random_seed,
        task_type=task_type,
        thread_count=thread_count,
        allow_writing_files=False,
        verbose=100,
    )
    model.fit(
        train_features.frame,
        y_train,
        cat_features=train_features.categorical_columns,
        eval_set=(validation_features.frame, y_validation),
        early_stopping_rounds=200,
        use_best_model=True,
    )
    print("Resolved class_weights:", model.get_all_params().get("class_weights"))
    validation_probability = model.predict_proba(validation_features.frame)[:, 1]
    thresholds = threshold_table(y_validation, validation_probability)
    threshold = select_threshold(
        thresholds,
        minimum_non_valid_recall=minimum_non_valid_recall,
        policy=threshold_policy,
        fixed_threshold=fixed_threshold,
    )
    selected_row = thresholds.iloc[(thresholds["threshold"] - threshold).abs().argsort()[:1]].iloc[0]
    recall_constraint_met = bool(selected_row["recall_non_valid"] >= minimum_non_valid_recall)

    test_probability = model.predict_proba(test_features.frame)[:, 1]
    validation_metrics = classification_metrics(
        y_validation, validation_probability, threshold
    )
    test_metrics = classification_metrics(y_test, test_probability, threshold)

    model_path = settings.model_root / "do2_catboost.cbm"
    model.save_model(model_path)
    thresholds.to_csv(settings.report_root / "validation_thresholds.csv", index=False)
    threshold_policy_table(thresholds).to_csv(
        settings.report_root / "threshold_comparison.csv", index=False
    )

    importance = pd.DataFrame(
        {
            "feature": train_features.frame.columns,
            "importance": model.get_feature_importance(),
        }
    ).sort_values("importance", ascending=False)
    importance.to_csv(settings.report_root / "feature_importance.csv", index=False)

    if "do2_type" in test_raw:
        group_metrics(
            test_raw["do2_type"],
            y_test,
            test_probability,
            threshold,
        ).to_csv(settings.report_root / "test_metrics_by_do2_type.csv", index=False)
    if "do2_type" in validation_raw:
        group_metrics(
            validation_raw["do2_type"],
            y_validation,
            validation_probability,
            threshold,
        ).to_csv(settings.report_root / "validation_metrics_by_do2_type.csv", index=False)
    if "do2_date" in validation_raw:
        validation_month = pd.to_datetime(validation_raw["do2_date"], errors="coerce").dt.to_period("M")
        group_metrics(
            validation_month.astype("string"),
            y_validation,
            validation_probability,
            threshold,
        ).to_csv(settings.report_root / "validation_metrics_by_month.csv", index=False)
    if "do2_date" in test_raw:
        test_month = pd.to_datetime(test_raw["do2_date"], errors="coerce").dt.to_period("M")
        group_metrics(
            test_month.astype("string"),
            y_test,
            test_probability,
            threshold,
        ).to_csv(settings.report_root / "test_metrics_by_month.csv", index=False)

    test_predictions = _prediction_output(
        test_raw, y_test, test_probability, threshold
    )
    test_predictions.to_parquet(settings.report_root / "test_predictions.parquet", index=False)
    test_predictions[test_predictions["predicted_status_group"] == "NON_VALID"].to_csv(
        settings.report_root / "test_predicted_non_valid.csv", index=False
    )
    test_predictions.head(5000).to_csv(
        settings.report_root / "test_predictions_sample.csv", index=False
    )

    test_predictions.sort_values("non_valid_probability", ascending=False).head(1000).to_csv(
        settings.report_root / "test_top_risk_records.csv", index=False
    )
    test_predictions[
        (test_predictions["actual_status_group"] == "V")
        & (test_predictions["predicted_status_group"] == "NON_VALID")
    ].to_csv(settings.report_root / "test_false_positives.csv", index=False)
    test_predictions[
        (test_predictions["actual_status_group"] == "NON_VALID")
        & (test_predictions["predicted_status_group"] == "V")
    ].to_csv(settings.report_root / "test_false_negatives.csv", index=False)
    status_recall_report(
        normalize_status(test_raw["status"]),
        y_test,
        test_probability,
        threshold,
    ).to_csv(settings.report_root / "test_recall_by_original_status.csv", index=False)
    for split_name, raw in (
        ("train", train_raw),
        ("validation", validation_raw),
        ("test", test_raw),
    ):
        normalize_status(raw["status"]).value_counts(dropna=False).rename_axis("status").reset_index(
            name="rows"
        ).to_csv(settings.report_root / f"{split_name}_status_distribution.csv", index=False)
    pd.DataFrame(
        {
            "feature": train_features.frame.columns,
            "null_rate": train_features.frame.isna().mean().to_numpy(),
        }
    ).to_csv(settings.report_root / "selected_feature_null_rates.csv", index=False)

    config = load_feature_config()
    excluded_present = leakage_columns_present(train_raw.columns)
    dropped_report = pd.DataFrame(
        [
            {"column": column, "reason": reason, "present_in_training_data": column in train_raw.columns}
            for column, reason in LEAKAGE_COLUMNS.items()
        ]
    )
    dropped_report.to_csv(settings.report_root / "dropped_leakage_columns.csv", index=False)

    class_counts = pd.Series(y_train).value_counts().sort_index().to_dict()
    class_weights = model.get_all_params().get("class_weights")
    metadata = {
        "created_at_utc": datetime.now(timezone.utc).isoformat(),
        "model_version": datetime.now(timezone.utc).strftime("do2-catboost-%Y%m%dT%H%M%SZ"),
        "model_target": "is_non_valid: normalized nonblank status V=0, every other nonblank status=1",
        "label_mapping": {"V": 0, "NON_VALID": 1},
        "output_mapping": "predicted_is_non_valid: V=0, NON_VALID=1",
        "splits": SPLITS,
        "split_date_column": "do2_date",
        "training_rows": len(train_raw),
        "validation_rows": len(validation_raw),
        "test_rows": len(test_raw),
        "feature_columns": list(train_features.frame.columns),
        "categorical_columns": train_features.categorical_columns,
        "included_feature_columns": list(train_features.frame.columns),
        "excluded_leakage_columns": {column: LEAKAGE_COLUMNS[column] for column in excluded_present},
        "sparse_column_policy": "missing values retained for CatBoost; categorical blanks mapped to __MISSING__; no sparse-column drop fitted in this baseline",
        "selected_probability_non_valid_threshold": threshold,
        "threshold_policy": "fixed" if fixed_threshold is not None else threshold_policy,
        "recall_constraint_met": recall_constraint_met,
        "minimum_requested_non_valid_recall": minimum_non_valid_recall,
        "training_mode": training_mode,
        "regularization": {
            "l2_leaf_reg": l2_leaf_reg,
            "min_data_in_leaf": min_data_in_leaf,
        },
        "sampling_config": {
            "max_train_rows": max_train_rows,
            "sampled_mode": "deterministic stratified by do2 month and original status",
            "random_seed": settings.random_seed,
        },
        "class_counts": {str(key): int(value) for key, value in class_counts.items()},
        "class_weights": class_weights,
        "dependency_versions": {
            name: importlib.metadata.version(name)
            for name in ("catboost", "pandas", "numpy", "scikit-learn", "pyarrow")
        },
        "best_iteration": int(model.get_best_iteration()),
        "model_parameters": model.get_params(),
        "candidate_columns": config["candidate_columns"],
    }
    write_json(settings.model_root / "model_metadata.json", metadata)
    metrics = {
        "validation": validation_metrics,
        "test": test_metrics,
        "all_v_accuracy_baseline_test": float((y_test == 0).mean()),
    }
    write_json(settings.report_root / "metrics.json", metrics)

    print(f"Selected non-valid threshold: {threshold:.2f}")
    print(f"Threshold policy: {metadata['threshold_policy']}")
    print("Validation metrics:", validation_metrics)
    print("Final test metrics:", test_metrics)
    print(f"Model saved to {model_path}")
    return metrics


def main() -> None:
    parser = argparse.ArgumentParser(description="Train and evaluate the DO2 CatBoost model.")
    parser.add_argument(
        "--max-train-rows",
        type=int,
        default=0,
        help="0 uses all configured training rows. Use 1000000 for a laptop test.",
    )
    parser.add_argument("--iterations", type=int, default=1500)
    parser.add_argument("--depth", type=int, default=6)
    parser.add_argument("--learning-rate", type=float, default=0.02)
    parser.add_argument("--l2-leaf-reg", type=float, default=8)
    parser.add_argument("--min-data-in-leaf", type=int, default=50)
    parser.add_argument("--task-type", choices=["CPU", "GPU"], default="CPU")
    parser.add_argument("--thread-count", type=int, default=-1)
    parser.add_argument("--minimum-non-valid-recall", type=float, default=0.80)
    parser.add_argument("--training-mode", choices=["sampled", "full"], default="sampled")
    parser.add_argument(
        "--threshold-policy",
        choices=["max_f1", "max_f2", "recall_80", "precision_50"],
        default="max_f1",
    )
    parser.add_argument("--fixed-threshold", type=float)
    args = parser.parse_args()
    run_training(
        max_train_rows=args.max_train_rows,
        iterations=args.iterations,
        depth=args.depth,
        learning_rate=args.learning_rate,
        l2_leaf_reg=args.l2_leaf_reg,
        min_data_in_leaf=args.min_data_in_leaf,
        task_type=args.task_type,
        thread_count=args.thread_count,
        minimum_non_valid_recall=args.minimum_non_valid_recall,
        training_mode=args.training_mode,
        threshold_policy=args.threshold_policy,
        fixed_threshold=args.fixed_threshold,
    )


if __name__ == "__main__":
    main()
