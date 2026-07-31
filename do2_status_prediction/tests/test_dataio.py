import pandas as pd

from do2_model.dataio import load_split, load_split_profile


def test_deterministic_stratified_sampling_retains_rare_statuses(tmp_path):
    rows = []
    for month in ("2021-01-01", "2021-02-01"):
        for status, count in (("V", 20), ("I", 3), ("RR", 1)):
            rows.extend(
                {"do2_date": month, "status": status, "id": f"{month}-{status}-{i}"}
                for i in range(count)
            )
    frame = pd.DataFrame(rows)
    frame.iloc[: len(frame) // 2].to_parquet(tmp_path / "part-00000.parquet", index=False)
    frame.iloc[len(frame) // 2 :].to_parquet(tmp_path / "part-00001.parquet", index=False)

    first = load_split(tmp_path, max_rows=12, random_seed=7, stratify_sample=True)
    second = load_split(tmp_path, max_rows=12, random_seed=7, stratify_sample=True)
    profile = load_split_profile(tmp_path)

    assert first["id"].tolist() == second["id"].tolist()
    assert {"2021-01", "2021-02"} <= set(
        pd.to_datetime(first["do2_date"]).dt.to_period("M").astype(str)
    )
    assert {"I", "RR"} <= set(first["status"])
    assert profile["part"].nunique() == 2
    assert profile["rows"].sum() == len(frame)
