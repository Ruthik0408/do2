import numpy as np

from do2_model.evaluation import (
    classification_metrics,
    select_threshold,
    threshold_policy_table,
    threshold_table,
)


def test_threshold_and_confusion_metrics():
    y_true = np.array([0, 0, 0, 1, 1, 1], dtype="int8")
    probability = np.array([0.05, 0.20, 0.40, 0.55, 0.80, 0.95])
    table = threshold_table(y_true, probability)
    threshold = select_threshold(table, minimum_non_valid_recall=0.80, policy="recall_80")
    metrics = classification_metrics(y_true, probability, threshold)
    assert 0.05 <= threshold <= 0.95
    assert metrics["recall_non_valid"] >= 0.80
    assert metrics["non_valid_missed_as_valid"] <= 1


def test_threshold_policy_table_uses_validation_metrics():
    y_true = np.array([0, 0, 1, 1], dtype="int8")
    probability = np.array([0.1, 0.7, 0.6, 0.9])
    table = threshold_table(y_true, probability)
    policies = threshold_policy_table(table)
    assert set(policies["policy"]) == {"max_f1", "max_f2", "recall_80", "precision_50"}
    assert select_threshold(table, policy="max_f1") in set(table["threshold"])
    assert select_threshold(table, fixed_threshold=0.33) == 0.33
