from __future__ import annotations

import json
import warnings
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.metrics import (
    accuracy_score,
    average_precision_score,
    brier_score_loss,
    confusion_matrix,
    f1_score,
    fbeta_score,
    log_loss,
    precision_score,
    recall_score,
    roc_auc_score,
)


def threshold_table(y_true: np.ndarray, probability_non_valid: np.ndarray) -> pd.DataFrame:
    rows = []
    for threshold in np.round(np.arange(0.05, 0.951, 0.01), 2):
        predicted = (probability_non_valid >= threshold).astype("int8")
        rows.append(
            {
                "threshold": float(threshold),
                "precision_non_valid": precision_score(
                    y_true, predicted, pos_label=1, zero_division=0
                ),
                "recall_non_valid": recall_score(
                    y_true, predicted, pos_label=1, zero_division=0
                ),
                "f1_non_valid": f1_score(y_true, predicted, pos_label=1, zero_division=0),
                "f2_non_valid": fbeta_score(
                    y_true, predicted, beta=2, pos_label=1, zero_division=0
                ),
                "review_rate": float(predicted.mean()),
            }
        )
    return pd.DataFrame(rows)


def select_threshold(
    table: pd.DataFrame,
    *,
    minimum_non_valid_recall: float = 0.80,
    policy: str = "max_f1",
    fixed_threshold: float | None = None,
) -> float:
    if fixed_threshold is not None:
        return float(fixed_threshold)
    if policy == "max_f1":
        selected = table.sort_values(
            ["f1_non_valid", "precision_non_valid", "threshold"],
            ascending=[False, False, False],
        ).iloc[0]
    elif policy == "max_f2":
        selected = table.sort_values(
            ["f2_non_valid", "recall_non_valid", "threshold"],
            ascending=[False, False, False],
        ).iloc[0]
    elif policy == "recall_80":
        eligible = table[table["recall_non_valid"] >= minimum_non_valid_recall]
        if not eligible.empty:
            selected = eligible.sort_values(
                ["precision_non_valid", "f1_non_valid", "threshold"],
                ascending=[False, False, False],
            ).iloc[0]
        else:
            warnings.warn(
                f"No threshold achieved minimum_non_valid_recall={minimum_non_valid_recall}; "
                "falling back to best F2 threshold. Recall SLA NOT met.",
                stacklevel=2,
            )
            selected = table.sort_values(
                ["f2_non_valid", "recall_non_valid"], ascending=[False, False]
            ).iloc[0]
    elif policy == "precision_50":
        eligible = table[table["precision_non_valid"] >= 0.50]
        if not eligible.empty:
            selected = eligible.sort_values(
                ["recall_non_valid", "f1_non_valid", "threshold"],
                ascending=[False, False, False],
            ).iloc[0]
        else:
            selected = table.sort_values(
                ["precision_non_valid", "f1_non_valid"], ascending=[False, False]
            ).iloc[0]
    else:
        raise ValueError(f"Unknown threshold policy: {policy}")
    return float(selected["threshold"])


def threshold_policy_table(table: pd.DataFrame) -> pd.DataFrame:
    rows = []
    for policy in ("max_f1", "max_f2", "recall_80", "precision_50"):
        threshold = select_threshold(table, policy=policy)
        nearest = table.iloc[(table["threshold"] - threshold).abs().argsort()[:1]].iloc[0]
        rows.append({"policy": policy, **nearest.to_dict()})
    return pd.DataFrame(rows)


def classification_metrics(
    y_true: np.ndarray,
    probability_non_valid: np.ndarray,
    threshold: float,
) -> dict[str, float | int]:
    predicted = (probability_non_valid >= threshold).astype("int8")
    tn, fp, fn, tp = confusion_matrix(y_true, predicted, labels=[0, 1]).ravel()

    metrics: dict[str, float | int] = {
        "rows": len(y_true),
        "threshold": float(threshold),
        "actual_non_valid_rate": float(np.mean(y_true)),
        "actual_valid_count": int((y_true == 0).sum()),
        "actual_non_valid_count": int((y_true == 1).sum()),
        "predicted_non_valid_rate": float(np.mean(predicted)),
        "predicted_valid_rate": float(1.0 - np.mean(predicted)),
        "accuracy": float(accuracy_score(y_true, predicted)),
        "precision_non_valid": float(
            precision_score(y_true, predicted, pos_label=1, zero_division=0)
        ),
        "recall_non_valid": float(
            recall_score(y_true, predicted, pos_label=1, zero_division=0)
        ),
        "f1_non_valid": float(f1_score(y_true, predicted, pos_label=1, zero_division=0)),
        "f2_non_valid": float(
            fbeta_score(y_true, predicted, beta=2, pos_label=1, zero_division=0)
        ),
        "true_valid_predicted_valid": int(tn),
        "valid_flagged_non_valid": int(fp),
        "non_valid_missed_as_valid": int(fn),
        "non_valid_detected": int(tp),
        "true_positives": int(tp),
        "false_positives": int(fp),
        "false_negatives": int(fn),
        "true_negatives": int(tn),
        "false_valid_rate": float(fn / (fn + tp)) if fn + tp else 0.0,
        "valid_review_rate": float(fp / (tn + fp)) if tn + fp else 0.0,
        "human_review_rate": float(np.mean(predicted)),
        "auto_valid_candidate_rate": float(1.0 - np.mean(predicted)),
        "brier_score": float(brier_score_loss(y_true, probability_non_valid)),
        "log_loss": float(log_loss(y_true, probability_non_valid, labels=[0, 1])),
    }
    if len(np.unique(y_true)) == 2:
        metrics["roc_auc"] = float(roc_auc_score(y_true, probability_non_valid))
        metrics["pr_auc_non_valid"] = float(
            average_precision_score(y_true, probability_non_valid)
        )
    return metrics


def status_recall_report(
    original_status: pd.Series,
    y_true: np.ndarray,
    probability_non_valid: np.ndarray,
    threshold: float,
) -> pd.DataFrame:
    predicted = probability_non_valid >= threshold
    working = pd.DataFrame(
        {
            "status": original_status.fillna("__MISSING__").astype(str),
            "target": y_true,
            "predicted": predicted.astype("int8"),
        }
    )
    rows = []
    for status, part in working.groupby("status", dropna=False):
        if status == "V":
            continue
        actual_non_valid = part[part["target"].eq(1)]
        rows.append(
            {
                "status": status,
                "rows": len(part),
                "actual_non_valid_rows": len(actual_non_valid),
                "detected_non_valid_rows": int(actual_non_valid["predicted"].sum()),
                "recall_non_valid": float(actual_non_valid["predicted"].mean())
                if len(actual_non_valid)
                else 0.0,
            }
        )
    return pd.DataFrame(rows).sort_values("rows", ascending=False)


def group_metrics(
    groups: pd.Series,
    y_true: np.ndarray,
    probability_non_valid: np.ndarray,
    threshold: float,
    *,
    minimum_rows: int = 100,
) -> pd.DataFrame:
    working = pd.DataFrame(
        {
            "group": groups.fillna("__MISSING__").astype(str).to_numpy(),
            "target": y_true,
            "probability": probability_non_valid,
        }
    )
    rows = []
    for group, part in working.groupby("group", dropna=False):
        if len(part) < minimum_rows:
            continue
        metrics = classification_metrics(
            part["target"].to_numpy(),
            part["probability"].to_numpy(),
            threshold,
        )
        rows.append({"group": group, **metrics})
    if not rows:
        return pd.DataFrame()
    return pd.DataFrame(rows).sort_values("rows", ascending=False)


def write_json(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2, default=str)
