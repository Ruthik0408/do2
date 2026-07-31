from __future__ import annotations

import argparse

import pandas as pd

from do2_model.config import SPLITS, Settings
from do2_model.database import extract_split, get_engine


def run_extraction(*, overwrite: bool, chunk_size: int) -> pd.DataFrame:
    settings = Settings()
    settings.ensure_output_directories()
    engine = get_engine(settings)
    summaries = []
    try:
        for split_name, (start_date, end_date) in SPLITS.items():
            print(f"Extracting {split_name}: {start_date} to {end_date}...")
            summaries.append(
                extract_split(
                    engine,
                    settings,
                    split_name,
                    start_date,
                    end_date,
                    overwrite=overwrite,
                    chunk_size=chunk_size,
                )
            )
    finally:
        engine.dispose()

    profile = pd.DataFrame(summaries)
    profile["non_valid_pct"] = 100.0 * profile["non_valid_rows"] / profile["rows"]
    profile.to_csv(settings.report_root / "extract_profile.csv", index=False)
    print(profile.to_string(index=False))
    return profile


def main() -> None:
    parser = argparse.ArgumentParser(description="Extract DO2 train/validation/test data.")
    parser.add_argument("--overwrite", action="store_true")
    parser.add_argument("--chunk-size", type=int, default=100000)
    args = parser.parse_args()
    run_extraction(overwrite=args.overwrite, chunk_size=args.chunk_size)


if __name__ == "__main__":
    main()

