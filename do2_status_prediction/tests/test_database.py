from do2_model.database import _DATE_FEATURE_COLUMNS, _select_expression


def _quote(identifier: str) -> str:
    return f'"{identifier}"'


def test_date_feature_columns_are_selected_as_text():
    for column in _DATE_FEATURE_COLUMNS:
        assert _select_expression(column, _quote) == f'"{column}"::text AS "{column}"'


def test_no_database_status_or_approval_update_code_exists():
    import re

    from do2_model import database

    source = database.Path(database.__file__).read_text(encoding="utf-8").lower()
    assert not re.search(r"\bupdate\b", source)
    assert not re.search(r"\bset\s+[^;]*(status|approved)\s*=", source)
