from pathlib import Path

from do2_model.config import SPLITS


def test_exact_temporal_split_boundaries_and_default_date_column():
    config_source = Path("src/do2_model/config.py").read_text(encoding="utf-8")
    assert 'os.getenv("DB_DATE_COLUMN", "do2_date")' in config_source
    assert SPLITS["train"] == ("2021-01-01", "2025-01-01")
    assert SPLITS["validation"] == ("2025-01-01", "2025-07-01")
    assert SPLITS["test"] == ("2025-07-01", "2025-12-31")
