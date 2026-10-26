from __future__ import annotations

import argparse
from datetime import datetime, timedelta, timezone

import pandas as pd

from do2_model.config import SPLITS, Settings
from do2_model.database import extract_split, get_engine

DEFAULT_MATURITY_DAYS = 45


def run_extraction(*, overwrite: bool, chunk_size: int, maturity_days: int) -> pd.DataFrame:
    settings = Settings()
    settings.ensure_output_directories()
    engine = get_engine(settings)
    summaries = []
    try:
        for split_name, (start_date, end_date) in SPLITS.items():
            maturity_cutoff_date = None
            if split_name in {"validation", "test"} and maturity_days > 0:
                maturity_cutoff_date = (
                    datetime.now(timezone.utc).date() - timedelta(days=maturity_days)
                ).isoformat()
            print(f"Extracting {split_name}: {start_date} to {end_date}...")
            if maturity_cutoff_date:
                print(f"Applying label maturity cutoff for {split_name}: {maturity_cutoff_date}")
            summaries.append(
                extract_split(
                    engine,
                    settings,
                    split_name,
                    start_date,
                    end_date,
                    overwrite=overwrite,
                    chunk_size=chunk_size,
                    maturity_cutoff_date=maturity_cutoff_date,
                )
            )
    finally:
        engine.dispose()

    rows = []
    for summary in summaries:
        row = {key: value for key, value in summary.items() if key != "status_counts"}
        for status, count in summary["status_counts"].items():
            row[f"status_{status}_rows"] = count
            row[f"status_{status}_pct"] = 100.0 * count / summary["rows"]
        rows.append(row)
    profile = pd.DataFrame(rows).fillna(0)
    profile.to_csv(settings.report_root / "extract_profile.csv", index=False)
    print(profile.to_string(index=False))
    return profile


def main() -> None:
    parser = argparse.ArgumentParser(description="Extract DO2 train/validation/test data.")
    parser.add_argument("--overwrite", action="store_true")
    parser.add_argument("--chunk-size", type=int, default=100000)
    parser.add_argument(
        "--maturity-days",
        type=int,
        default=DEFAULT_MATURITY_DAYS,
        help=(
            "Apply today minus this many days as a label-maturity cutoff to validation/test "
            "extraction. Use 0 to disable."
        ),
    )
    args = parser.parse_args()
    run_extraction(
        overwrite=args.overwrite,
        chunk_size=args.chunk_size,
        maturity_days=args.maturity_days,
    )


if __name__ == "__main__":
    main()
