from __future__ import annotations

import math
from pathlib import Path

import pandas as pd
import pyarrow.parquet as pq

from do2_model.features import filter_supervised_rows, normalize_status


def parquet_parts(directory: Path) -> list[Path]:
    files = sorted(directory.glob("part-*.parquet"))
    if not files:
        raise FileNotFoundError(f"No extracted Parquet parts found in {directory}")
    return files


def load_split(
    directory: Path,
    *,
    max_rows: int = 0,
    random_seed: int = 42,
    supervised_only: bool = True,
    stratify_sample: bool = False,
) -> pd.DataFrame:
    files = parquet_parts(directory)
    row_counts = [pq.ParquetFile(path).metadata.num_rows for path in files]
    total_rows = sum(row_counts)

    if max_rows <= 0 or max_rows >= total_rows:
        result = pd.concat((pd.read_parquet(path) for path in files), ignore_index=True)
        return filter_supervised_rows(result) if supervised_only else result

    if stratify_sample:
        result = _load_stratified_sample(files, max_rows=max_rows, random_seed=random_seed)
        return filter_supervised_rows(result) if supervised_only else result

    exact_allocations = [max_rows * count / total_rows for count in row_counts]
    allocations = [math.floor(value) for value in exact_allocations]
    remainder = max_rows - sum(allocations)
    order = sorted(
        range(len(files)),
        key=lambda index: exact_allocations[index] - allocations[index],
        reverse=True,
    )
    for index in order[:remainder]:
        allocations[index] += 1

    sampled = []
    for index, (path, take) in enumerate(zip(files, allocations, strict=True)):
        if take == 0:
            continue
        frame = pd.read_parquet(path)
        if supervised_only:
            frame = filter_supervised_rows(frame)
        if take < len(frame):
            frame = frame.sample(n=take, random_state=random_seed + index)
        sampled.append(frame)

    result = pd.concat(sampled, ignore_index=True)
    return result.sample(frac=1, random_state=random_seed).reset_index(drop=True)


def _strata_frame(frame: pd.DataFrame) -> pd.DataFrame:
    do2_month = pd.to_datetime(frame.get("do2_date"), errors="coerce").dt.to_period("M")
    status = normalize_status(frame["status"])
    return pd.DataFrame(
        {
            "month": do2_month.astype("string").fillna("__MISSING_MONTH__"),
            "status": status.replace("", "__MISSING_STATUS__"),
        },
        index=frame.index,
    )


def _allocate_sample_counts(counts: pd.DataFrame, *, max_rows: int) -> pd.DataFrame:
    counts = counts.copy()
    counts["exact"] = max_rows * counts["rows"] / counts["rows"].sum()
    counts["take"] = counts["exact"].apply(math.floor).astype(int)
    counts.loc[counts["take"].eq(0) & counts["rows"].gt(0), "take"] = 1
    while counts["take"].sum() > max_rows:
        candidates = counts[counts["take"].gt(1)].sort_values(
            ["take", "rows"], ascending=[False, False]
        )
        if candidates.empty:
            break
        counts.loc[candidates.index[0], "take"] -= 1
    while counts["take"].sum() < max_rows:
        counts["remaining"] = counts["rows"] - counts["take"]
        candidates = counts[counts["remaining"].gt(0)].sort_values(
            ["exact", "remaining"], ascending=[False, False]
        )
        if candidates.empty:
            break
        counts.loc[candidates.index[0], "take"] += 1
    return counts.drop(columns=[column for column in ("remaining",) if column in counts])


def _load_stratified_sample(
    files: list[Path],
    *,
    max_rows: int,
    random_seed: int,
) -> pd.DataFrame:
    profile = parquet_sampling_profile(files)
    counts = _allocate_sample_counts(
        profile.groupby(["month", "status"], dropna=False)["rows"].sum().reset_index(),
        max_rows=max_rows,
    )
    targets = {
        (row["month"], row["status"]): int(row["take"])
        for _, row in counts.iterrows()
        if int(row["take"]) > 0
    }
    if not targets:
        raise RuntimeError("No eligible supervised rows found for stratified sampling.")

    remaining = targets.copy()
    sampled = []
    for file_index, path in enumerate(files):
        frame = filter_supervised_rows(pd.read_parquet(path))
        if frame.empty:
            continue
        strata = _strata_frame(frame)
        for stratum_index, key in enumerate(sorted(remaining)):
            target = remaining[key]
            if target <= 0:
                continue
            mask = strata["month"].eq(key[0]) & strata["status"].eq(key[1])
            part = frame.loc[mask]
            if part.empty:
                continue
            take = min(target, len(part))
            sampled.append(
                part.sample(
                    n=take,
                    random_state=random_seed + (file_index * 1009) + stratum_index,
                )
            )
            remaining[key] -= take
    if not sampled:
        raise RuntimeError("No rows were sampled from the extracted Parquet cache.")
    return pd.concat(sampled, ignore_index=True).sample(
        frac=1, random_state=random_seed
    ).reset_index(drop=True)


def sampling_profile(frame: pd.DataFrame) -> pd.DataFrame:
    status = normalize_status(frame["status"])
    binary = status.ne("V").astype("int8")
    dates = pd.to_datetime(frame.get("do2_date"), errors="coerce")
    return (
        pd.DataFrame(
            {
                "year": dates.dt.year.astype("Int64").astype("string").fillna("__MISSING__"),
                "month": dates.dt.to_period("M").astype("string").fillna("__MISSING__"),
                "status": status,
                "is_non_valid": binary,
            }
        )
        .groupby(["year", "month", "status", "is_non_valid"], dropna=False)
        .size()
        .rename("rows")
        .reset_index()
        .sort_values(["year", "month", "status", "is_non_valid"])
    )


def parquet_sampling_profile(files: list[Path]) -> pd.DataFrame:
    profiles = []
    for part_index, path in enumerate(files):
        frame = filter_supervised_rows(pd.read_parquet(path, columns=["do2_date", "status"]))
        if frame.empty:
            continue
        profile = sampling_profile(frame)
        profile["part"] = part_index
        profile["path"] = str(path)
        profiles.append(profile)
    if not profiles:
        return pd.DataFrame(
            columns=["year", "month", "status", "is_non_valid", "rows", "part", "path"]
        )
    return pd.concat(profiles, ignore_index=True).sort_values(
        ["year", "month", "status", "is_non_valid", "part"]
    )


def load_split_profile(directory: Path) -> pd.DataFrame:
    return parquet_sampling_profile(parquet_parts(directory))
