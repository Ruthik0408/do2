# DO2 Status Prediction

This project trains a binary CatBoost model for the rule:

```text
normalized status = V  -> valid
every other value      -> non-valid
```

Internally, `is_non_valid` is used:

```text
V     = 0
NON_VALID = 1
```

This makes recall, precision, F1, and PR-AUC directly describe detection of
non-valid records. Prediction files contain `predicted_is_non_valid` and
`predicted_status_group`.

## Exact time split

| Purpose | Period |
|---|---|
| Training | 2021-01-01 through 2024-12-31 |
| Validation and threshold selection | 2025-01-01 through 2025-06-30 |
| Final untouched test/results | 2025-07-01 through 2025-12-30 |

The test split end date is exclusive: `2025-12-31`.

## Leakage protection

The model predicts at initial DO2 entry. It does not use final status, rejection
reasons, approval/review flags, reviewer IDs, auditor/AAO/AO dates, or processed
dates as features. Review `config/feature_config.json` before training and remove
any candidate column that is not known at your real prediction time.

## 1. Open in VS Code

Open the `do2_status_prediction` folder, then run:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
python -m pip install -e ".[dev]"
```

## 2. Configure PostgreSQL

```bash
cp .env.example .env
```

Edit `.env`:

```dotenv
DATABASE_URL=postgresql+psycopg://postgres:YOUR_PASSWORD@localhost:5432/tulip2
DB_SCHEMA=public
DB_TABLE=do2
DB_DATE_COLUMN=do2_date
```

If a password contains `@`, `/`, `:` or another URL character, URL-encode it.

## 3. Confirm the data split

Run `sql/data_quality_checks.sql` in pgAdmin or VS Code's PostgreSQL extension.
Each 2025 evaluation half must contain both `V` and non-`V` rows.

## 4. Extract PostgreSQL data

```bash
python -m do2_model.extract
```

Extraction uses chunks, so it does not load every database row into memory at
once. It writes Parquet parts beneath `data/raw/`.

If you intentionally want to replace an earlier extraction:

```bash
python -m do2_model.extract --overwrite
```

## 5. First training run

First verify the full pipeline on a time-distributed one-million-row training
sample:

```bash
python -m do2_model.train --max-train-rows 1000000
```

When that finishes correctly, use every extracted training row:

```bash
python -m do2_model.train --max-train-rows 0
```

The default is CPU. Your RTX 3050 can be tested with:

```bash
python -m do2_model.train --max-train-rows 1000000 --task-type GPU
```

If GPU training reports a CUDA/CatBoost compatibility error, use CPU; the model
logic and outputs remain the same.

## One-command pipeline

After `.env` is ready:

```bash
python -m do2_model.pipeline --max-train-rows 1000000
```

Use `--overwrite` only when replacing an existing extraction.

## Outputs

```text
models/do2_catboost.cbm
models/model_metadata.json
reports/extract_profile.csv
reports/metrics.json
reports/validation_thresholds.csv
reports/feature_importance.csv
reports/test_metrics_by_do2_type.csv
reports/test_predictions.parquet
reports/test_predicted_non_valid.csv
reports/test_predictions_sample.csv
```

Important result columns:

```text
probability_valid
probability_non_valid
predicted_class          # V or OTHER
predicted_is_valid       # V=1, OTHER=0
is_correct               # available for the 2024 test set
```

The threshold is selected only from January-June 2024. The default requirement
is at least 80% recall for non-valid records. Change it with:

```bash
python -m do2_model.train --minimum-non-valid-recall 0.85
```

## Predict later CSV or Parquet data

The new file should contain the same initial-entry columns used during training.
It does not need a `status` column.

```bash
python -m do2_model.predict \
  --input data/new_do2_rows.parquet \
  --output reports/predictions/new_predictions.parquet
```

CSV is also supported:

```bash
python -m do2_model.predict \
  --input data/new_do2_rows.csv \
  --output reports/predictions/new_predictions.csv
```

Or fetch a later period directly from the configured PostgreSQL table:

```bash
python -m do2_model.predict \
  --start-date 2025-01-01 \
  --end-date 2025-02-01 \
  --output reports/predictions/2025_01_predictions.parquet
```

Use smaller date windows if the later period is too large for laptop memory.

## Run checks

```bash
pytest
ruff check .
```

## How to judge the model

Use `reports/metrics.json`, especially:

- `recall_non_valid`: percentage of actual non-valid rows detected.
- `precision_non_valid`: percentage of flagged rows that are truly non-valid.
- `pr_auc_non_valid`: probability-ranking quality for the minority class.
- `non_valid_missed_as_valid`: the most important audit error count.
- `valid_review_rate`: valid rows unnecessarily sent for review.

Do not select the model by accuracy alone. A model that predicts every row as
`V` already appears strong because `V` is the majority class.

## Business-label warning

This implementation follows the requested binary definition exactly: every
non-`V` value is labelled non-valid. If `P`, `I`, `MR`, `N`, `C`, or another
status actually means pending/in-progress rather than a finalized wrong result,
exclude those rows or remap them before using the model operationally.
