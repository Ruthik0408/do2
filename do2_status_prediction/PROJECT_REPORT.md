# DO2 Status Prediction Report

## Project Overview

This project predicts whether a DO2 record will be `valid` or `non-valid` at the time of initial entry. The model is a binary CatBoost classifier.

Target definition:

| Original status | Model target | Meaning |
|---|---:|---|
| `V` | `0` | Valid |
| Any non-`V` value | `1` | Non-valid |

The main business goal is to detect non-valid DO2 records early. Because valid records are the majority class, accuracy alone is not a reliable success metric. The important metrics are recall, precision, F1/F2 score, and the confusion matrix for the non-valid class.

## Data Split

| Dataset | Period | Purpose |
|---|---|---|
| Training | 2021-01-01 to 2023-12-31 | Model fitting |
| Validation | 2024-01-01 to 2024-06-30 | Threshold selection |
| Test | 2024-07-01 to 2024-12-31 | Final evaluation |

The selected decision threshold is `0.26`. A row is predicted as non-valid when:

```text
probability_non_valid >= 0.26
```

## Final Test Confusion Matrix

Test period: July 2024 to December 2024  
Total test rows: `341,719`

| Actual \ Predicted | Predicted Valid | Predicted Non-Valid |
|---|---:|---:|
| Actual Valid | 92,945 | 217,064 |
| Actual Non-Valid | 3,494 | 28,216 |

## Confusion Matrix Explanation

| Term | Count | Meaning |
|---|---:|---|
| True Negative | 92,945 | Valid records correctly predicted as valid |
| False Positive | 217,064 | Valid records incorrectly flagged as non-valid |
| False Negative | 3,494 | Non-valid records missed and predicted as valid |
| True Positive | 28,216 | Non-valid records correctly detected |

The model detects most non-valid records, but it also flags many valid records for review. This means the model is tuned for high recall, not high precision.

## Test Metrics

| Metric | Value |
|---|---:|
| Accuracy | 35.46% |
| Non-valid precision | 11.50% |
| Non-valid recall | 88.98% |
| Non-valid F1 score | 20.37% |
| Non-valid F2 score | 37.91% |
| ROC-AUC | 70.81% |
| PR-AUC for non-valid class | 23.55% |
| Actual non-valid rate | 9.28% |
| Predicted non-valid rate | 71.78% |
| False valid rate | 11.02% |
| Valid review rate | 70.02% |

## Interpretation

The model is strong at catching non-valid records. It correctly identifies `28,216` out of `31,710` actual non-valid records, giving a recall of `88.98%`.

The main weakness is precision. Out of all rows flagged as non-valid, only `11.50%` are actually non-valid. This creates a high review workload because `217,064` valid records are incorrectly flagged.

The selected threshold of `0.26` prioritizes reducing missed non-valid records. This is useful if missing a bad DO2 record is expensive, but it is not ideal if review capacity is limited.

## Validation vs Test Performance

| Metric | Validation | Test |
|---|---:|---:|
| Rows | 345,251 | 341,719 |
| Accuracy | 71.63% | 35.46% |
| Precision | 27.83% | 11.50% |
| Recall | 81.44% | 88.98% |
| F1 | 41.48% | 20.37% |
| ROC-AUC | 85.62% | 70.81% |
| PR-AUC | 57.95% | 23.55% |
| Valid review rate | 29.75% | 70.02% |

The test-period performance is much worse than validation, especially precision, accuracy, PR-AUC, and valid review rate. This suggests distribution shift between January-June 2024 and July-December 2024. The model still catches non-valid records, but it becomes overly aggressive and marks too many valid records as non-valid.

## Monthly Test Behavior

The review rate changes significantly by month:

| Month | Rows | Recall | Precision | Valid Review Rate |
|---|---:|---:|---:|---:|
| 2024-07 | 50,376 | 82.39% | 27.75% | 40.85% |
| 2024-08 | 56,318 | 77.10% | 13.84% | 70.02% |
| 2024-09 | 63,847 | 98.67% | 7.76% | 97.27% |
| 2024-10 | 93,134 | 96.30% | 8.44% | 48.47% |
| 2024-11 | 38,885 | 96.87% | 11.38% | 91.69% |
| 2024-12 | 38,808 | 97.46% | 8.72% | 92.39% |

September, November, and December have extremely high valid review rates. This is the clearest sign that the model threshold or feature distribution is not stable across time.

## Important Features

Top model features:

| Rank | Feature | Importance |
|---:|---|---:|
| 1 | `description` | 29.53 |
| 2 | `unit_code` | 12.16 |
| 3 | `input_qe` | 9.18 |
| 4 | `transcription_type` | 8.64 |
| 5 | `submission_delay_days` | 5.98 |
| 6 | `column_a` | 4.91 |
| 7 | `rate` | 4.03 |
| 8 | `qe` | 3.40 |
| 9 | `created_month` | 2.53 |
| 10 | `audit_cage` | 2.47 |

The model relies heavily on text and operational fields such as `description`, `unit_code`, `input_qe`, and `transcription_type`. If the meaning, format, or data-entry pattern of these fields changed in late 2024, model performance would degrade.

## Conclusion

The model is useful as a high-recall screening tool for non-valid DO2 records. It catches most non-valid records, but it is not ready as an efficient automatic review-routing system because it creates too many false positives.

The model should not be judged by accuracy. A simple baseline that predicts every row as valid gets `90.72%` accuracy on the test set, but it misses every non-valid record. This model sacrifices accuracy to detect `88.98%` of non-valid records.

## Recommendations

1. Recalibrate the threshold using later 2024 data or rolling monthly validation.
2. Report two operating modes: high-recall mode for audit safety and balanced mode for review-capacity control.
3. Investigate September, November, and December 2024 for data drift or process changes.
4. Add drift checks for top features such as `description`, `unit_code`, `input_qe`, and `transcription_type`.
5. Review the target definition before production use. If statuses like pending or in-progress are not truly invalid, they should be excluded or remapped.
6. Add a monthly monitoring dashboard with recall, precision, review rate, and false-negative count.
7. Consider probability calibration before using scores for operational ranking.

