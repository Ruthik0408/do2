from pathlib import Path

from do2_model.config import LEAKAGE_COLUMNS, SPLITS, load_feature_config


def test_exact_temporal_split_boundaries_and_default_date_column():
    config_source = Path("src/do2_model/config.py").read_text(encoding="utf-8")
    assert 'os.getenv("DB_DATE_COLUMN", "created_at")' in config_source
    assert SPLITS["train"] == ("2022-01-01", "2024-01-01")
    assert SPLITS["validation"] == ("2024-01-01", "2025-01-01")
    assert SPLITS["test"] == ("2025-01-01", "2026-01-01")


def test_feature_sources_are_available_before_review_or_permission():
    config = load_feature_config()
    availability = config["column_availability"]
    forbidden = set(availability["outcome_or_workflow_forbidden"])

    assert config["split_date_column"] == "created_at"
    assert "description" in config["candidate_columns"]
    assert set(config["required_input_columns"]).issubset(config["candidate_columns"])
    assert forbidden.isdisjoint(config["candidate_columns"])
    assert forbidden == set(config["excluded_leakage_columns"]) - {"status"}
    assert forbidden == set(LEAKAGE_COLUMNS) - {"status"}
