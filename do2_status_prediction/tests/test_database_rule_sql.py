from do2_model.config import Settings
from do2_model.database import _appendix_j_sql, _family_identity_sql, _rule_history_sql


class _FakePreparer:
    def quote_identifier(self, value):
        return f'"{value}"'


class _FakeDialect:
    identifier_preparer = _FakePreparer()


class _FakeEngine:
    dialect = _FakeDialect()


def test_rule_history_sql_is_generated_only_when_required_columns_exist():
    settings = Settings(db_schema="public", db_table="do2")
    required_columns = {
        "id",
        "fk_employee",
        "army_no",
        "description",
        "from_date",
        "to_date",
        "column_a",
        "column_b",
        "column_c",
        "ref_do2_no",
        "do2_no",
        "do2_item_no",
        "created_at",
        "status",
    }

    selected, join_sql = _rule_history_sql(_FakeEngine(), settings, required_columns)

    assert "i_prior_duplicate_count" in selected[0]
    assert "i_prior_overlap_count" in " ".join(selected)
    assert "i_ref_do2_already_cancelled" in " ".join(selected)
    assert "LEFT JOIN LATERAL" in join_sql
    assert "hist.created_at < d.created_at" in join_sql


def test_rule_history_sql_is_skipped_for_partial_schema():
    settings = Settings(db_schema="public", db_table="do2")

    selected, join_sql = _rule_history_sql(_FakeEngine(), settings, {"id", "created_at"})

    assert selected == []
    assert join_sql == ""


def test_appendix_j_sql_generates_required_field_features(monkeypatch):
    settings = Settings(db_schema="public", db_table="do2")
    do2_columns = {
        "description",
        "do2_group",
        "column_a",
        "column_b",
        "column_c",
        "from_date",
        "to_date",
        "rate",
        "tjra",
        "lra",
        "ref_do2_no",
    }
    appendix_columns = {
        "id",
        "description",
        "do2_group",
        "column_a",
        "column_b",
        "column_c",
        "from_date_req",
        "to_date_req",
        "rate_req",
        "tjra_req",
        "lra_req",
        "ref_do2_no_req",
        "audit_account_cage",
        "special",
        "sort_priority",
    }
    monkeypatch.setattr(
        "do2_model.database.available_table_columns",
        lambda engine, settings, table: list(appendix_columns),
    )

    selected, join_sql = _appendix_j_sql(_FakeEngine(), settings, do2_columns)

    selected_sql = " ".join(selected)
    assert "rule_appendix_j_id" in selected_sql
    assert "i_appendix_description_not_found" in selected_sql
    assert "i_appendix_required_from_date_missing" in selected_sql
    assert "i_appendix_required_missing_count" in selected_sql
    assert "LEFT JOIN LATERAL" in join_sql
    assert "appendix.special IS TRUE" in join_sql
    assert "appendix.sort_priority" in join_sql


def test_appendix_j_sql_is_skipped_without_appendix_schema(monkeypatch):
    settings = Settings(db_schema="public", db_table="do2")
    monkeypatch.setattr(
        "do2_model.database.available_table_columns",
        lambda engine, settings, table: [],
    )

    selected, join_sql = _appendix_j_sql(
        _FakeEngine(),
        settings,
        {"description", "do2_group", "column_a", "from_date"},
    )

    assert selected == []
    assert join_sql == ""


def test_family_identity_sql_generates_identity_and_cea_features(monkeypatch):
    settings = Settings(db_schema="public", db_table="do2")
    do2_columns = {
        "id",
        "fk_employee",
        "army_no",
        "description",
        "column_a",
        "column_b",
        "column_c",
        "from_date",
        "created_at",
    }
    family_columns = {
        "id",
        "fk_employee",
        "relationship",
        "name_of_member",
        "record_status",
        "uid_no",
        "date_of_birth",
    }
    employee_columns = {"id", "army_no", "uid_no", "current_record", "qe"}

    def fake_available_columns(engine, settings, table):
        return list(family_columns if table == "family_detail" else employee_columns)

    monkeypatch.setattr("do2_model.database.available_table_columns", fake_available_columns)

    selected, join_sql = _family_identity_sql(_FakeEngine(), settings, do2_columns)

    selected_sql = " ".join(selected)
    assert "i_family_relation_name_match_count" in selected_sql
    assert "i_family_uid_conflict_count" in selected_sql
    assert "i_employee_uid_duplicate_count" in selected_sql
    assert "i_cea_prior_child_claim_count" in selected_sql
    assert "family_detail" in join_sql
    assert "employee" in join_sql
    assert "regexp_replace" in join_sql


def test_family_identity_sql_is_skipped_without_family_schema(monkeypatch):
    settings = Settings(db_schema="public", db_table="do2")
    monkeypatch.setattr(
        "do2_model.database.available_table_columns",
        lambda engine, settings, table: [],
    )

    selected, join_sql = _family_identity_sql(
        _FakeEngine(),
        settings,
        {"id", "fk_employee", "description", "column_a", "created_at"},
    )

    assert selected == []
    assert join_sql == ""
