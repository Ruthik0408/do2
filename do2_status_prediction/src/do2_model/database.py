from __future__ import annotations

import re
from collections.abc import Iterator
from datetime import date
from pathlib import Path

import pandas as pd
from sqlalchemy import Engine, create_engine, inspect, text

from do2_model.config import LEAKAGE_COLUMNS, Settings, load_feature_config

_IDENTIFIER = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")
_PRESENCE_ONLY_COLUMNS = {
    "attachment": "has_attachment_source",
    "cfa_sanction_letter_no": "has_cfa_sanction_letter_no_source",
    "column9_remarks": "has_column9_remarks_source",
    "pan": "has_pan_source",
    "pto_remarks": "has_pto_remarks_source",
    "ref_do2_no": "has_reference_do2_source",
}
_DATE_FEATURE_COLUMNS = {
    "cfa_sanction_date",
    "created_at",
    "do2_date",
    "from_date",
    "to_date",
}
_EMPLOYEE_CONTEXT_COLUMNS = {
    "fk_status_code": "employee_status_code",
    "fs_due_date": "employee_fs_due_date",
    "birth_date": "employee_birth_date",
    "enrolment_date": "employee_enrolment_date",
}
_RULE_HISTORY_REQUIRED_COLUMNS = {
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
_APPENDIX_J_REQUIRED_COLUMNS = {
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
_FAMILY_DETAIL_REQUIRED_COLUMNS = {
    "id",
    "fk_employee",
    "relationship",
    "name_of_member",
    "record_status",
    "uid_no",
    "date_of_birth",
}
_IDENTITY_EMPLOYEE_COLUMNS = {
    "id",
    "army_no",
    "uid_no",
    "current_record",
    "qe",
}


def _validate_identifier(value: str, label: str) -> str:
    if not _IDENTIFIER.fullmatch(value):
        raise ValueError(f"Unsafe {label}: {value!r}")
    return value


def get_engine(settings: Settings) -> Engine:
    if not settings.database_url:
        raise RuntimeError(
            "DATABASE_URL is missing. Copy .env.example to .env and enter PostgreSQL details."
        )
    return create_engine(settings.database_url, pool_pre_ping=True)


def available_columns(engine: Engine, settings: Settings) -> list[str]:
    _validate_identifier(settings.db_schema, "schema")
    _validate_identifier(settings.db_table, "table")
    return [
        column["name"]
        for column in inspect(engine).get_columns(settings.db_table, schema=settings.db_schema)
    ]


def available_table_columns(engine: Engine, settings: Settings, table: str) -> list[str]:
    _validate_identifier(settings.db_schema, "schema")
    _validate_identifier(table, "table")
    try:
        return [
            column["name"]
            for column in inspect(engine).get_columns(table, schema=settings.db_schema)
        ]
    except Exception:
        return []


def selected_columns(engine: Engine, settings: Settings, require_status: bool) -> list[str]:
    config = load_feature_config()
    existing = set(available_columns(engine, settings))
    wanted = list(config["candidate_columns"])

    required = {settings.db_date_column}
    if require_status:
        required.add("status")
    missing_required = sorted(required - existing)
    if missing_required:
        raise RuntimeError(f"Required DO2 columns are missing: {missing_required}")

    selected = [
        column
        for column in wanted
        if column in existing and (column not in LEAKAGE_COLUMNS or column == "status")
    ]
    if settings.db_date_column not in selected:
        selected.append(settings.db_date_column)
    if require_status and "status" not in selected:
        selected.append("status")

    missing_optional = [column for column in wanted if column not in existing]
    if missing_optional:
        print("Skipping optional columns not found in do2:", ", ".join(missing_optional))
    return selected


def _select_expression(column: str, quote, table_alias: str = "") -> str:
    prefix = f"{table_alias}." if table_alias else ""
    if column in _PRESENCE_ONLY_COLUMNS:
        alias = _PRESENCE_ONLY_COLUMNS[column]
        return (
            "CASE WHEN NULLIF(BTRIM("
            f"{prefix}{quote(column)}::text), '') IS NULL THEN 0 ELSE 1 END AS {quote(alias)}"
        )
    if column in _DATE_FEATURE_COLUMNS:
        return f"{prefix}{quote(column)}::text AS {quote(column)}"
    return f"{prefix}{quote(column)}"


def _employee_context_sql(engine: Engine, settings: Settings, do2_columns: set[str]) -> tuple[list[str], str]:
    if "fk_employee" not in do2_columns:
        return [], ""
    employee_columns = set(available_table_columns(engine, settings, "employee"))
    if "id" not in employee_columns:
        return [], ""

    quote = engine.dialect.identifier_preparer.quote_identifier
    selected = [
        f"e.{quote(column)} AS {quote(alias)}"
        for column, alias in _EMPLOYEE_CONTEXT_COLUMNS.items()
        if column in employee_columns
    ]
    if not selected:
        return [], ""
    schema = _validate_identifier(settings.db_schema, "schema")
    employee_table = f"{quote(schema)}.{quote('employee')}"
    join_sql = f"LEFT JOIN {employee_table} e ON e.{quote('id')} = d.{quote('fk_employee')}"
    return selected, join_sql


def _rule_history_sql(engine: Engine, settings: Settings, do2_columns: set[str]) -> tuple[list[str], str]:
    if not _RULE_HISTORY_REQUIRED_COLUMNS.issubset(do2_columns):
        return [], ""

    quote = engine.dialect.identifier_preparer.quote_identifier
    schema = _validate_identifier(settings.db_schema, "schema")
    table = _validate_identifier(settings.db_table, "table")
    qualified_table = f"{quote(schema)}.{quote(table)}"
    selected = [
        "COALESCE(rule_hist.i_prior_duplicate_count, 0)::integer AS "
        f"{quote('i_prior_duplicate_count')}",
        "(COALESCE(rule_hist.i_prior_duplicate_count, 0) > 0)::integer AS "
        f"{quote('i_has_prior_duplicate')}",
        "COALESCE(rule_hist.i_prior_overlap_count, 0)::integer AS "
        f"{quote('i_prior_overlap_count')}",
        "(COALESCE(rule_hist.i_prior_overlap_count, 0) > 0)::integer AS "
        f"{quote('i_has_prior_overlap')}",
        "COALESCE(rule_hist.i_ref_do2_match_count, 0)::integer AS "
        f"{quote('i_ref_do2_match_count')}",
        "COALESCE(rule_hist.i_ref_do2_already_cancelled, 0)::integer AS "
        f"{quote('i_ref_do2_already_cancelled')}",
        "COALESCE(rule_hist.i_prior_optcpc_conflict, 0)::integer AS "
        f"{quote('i_prior_optcpc_conflict')}",
    ]
    valid_status = "'V', 'P'"
    cancelled_status = "'C'"
    join_sql = f"""
        LEFT JOIN LATERAL (
          SELECT
            COUNT(*) FILTER (
              WHERE hist.description = d.description
                AND COALESCE(hist.from_date::date, DATE '0001-01-01') =
                    COALESCE(d.from_date::date, DATE '0001-01-01')
                AND (
                  CASE
                    WHEN d.description IN ('CEA', 'CEAHC', 'HOSSUB', 'HSBTWN', 'CHNOK', 'CHSRNM', 'CHFMNM')
                      THEN COALESCE(hist.column_a::text, '') = COALESCE(d.column_a::text, '')
                       AND COALESCE(hist.column_b::text, '') = COALESCE(d.column_b::text, '')
                       AND COALESCE(hist.column_c::text, '') = COALESCE(d.column_c::text, '')
                    WHEN d.description = 'CANCEL'
                      THEN COALESCE(hist.column_a::text, '') = COALESCE(d.column_a::text, '')
                       AND COALESCE(hist.ref_do2_no::text, '') = COALESCE(d.ref_do2_no::text, '')
                    ELSE COALESCE(hist.column_a::text, '') = COALESCE(d.column_a::text, '')
                  END
                )
            ) AS i_prior_duplicate_count,
            COUNT(*) FILTER (
              WHERE hist.status IN ({valid_status})
                AND hist.from_date IS NOT NULL
                AND COALESCE(hist.to_date, hist.from_date)::date >= d.from_date::date
                AND COALESCE(d.to_date, d.from_date)::date >= hist.from_date::date
            ) AS i_prior_overlap_count,
            COUNT(*) FILTER (
              WHERE d.ref_do2_no IS NOT NULL
                AND NULLIF(BTRIM(d.ref_do2_no::text), '') IS NOT NULL
                AND (
                  COALESCE(hist.do2_no::text, '') = COALESCE(d.ref_do2_no::text, '')
                  OR COALESCE(hist.do2_item_no::text, '') = COALESCE(d.ref_do2_no::text, '')
                )
            ) AS i_ref_do2_match_count,
            MAX(
              CASE
                WHEN d.ref_do2_no IS NOT NULL
                 AND NULLIF(BTRIM(d.ref_do2_no::text), '') IS NOT NULL
                 AND hist.status IN ({cancelled_status})
                 AND (
                   COALESCE(hist.do2_no::text, '') = COALESCE(d.ref_do2_no::text, '')
                   OR COALESCE(hist.do2_item_no::text, '') = COALESCE(d.ref_do2_no::text, '')
                 )
                THEN 1 ELSE 0
              END
            ) AS i_ref_do2_already_cancelled,
            MAX(
              CASE
                WHEN hist.description = 'OPTCPC'
                 AND hist.status = 'V'
                 AND COALESCE(hist.column_b::text, '') = 'SEVENPC'
                 AND COALESCE(hist.column_a::text, '') <> 'INCR'
                THEN 1 ELSE 0
              END
            ) AS i_prior_optcpc_conflict
          FROM {qualified_table} hist
          WHERE hist.id <> d.id
            AND hist.created_at < d.created_at
            AND (
              (hist.fk_employee IS NOT NULL AND d.fk_employee IS NOT NULL AND hist.fk_employee = d.fk_employee)
              OR (
                NULLIF(BTRIM(hist.army_no::text), '') IS NOT NULL
                AND NULLIF(BTRIM(d.army_no::text), '') IS NOT NULL
                AND hist.army_no = d.army_no
              )
            )
        ) rule_hist ON TRUE
    """
    return selected, join_sql


def _appendix_j_sql(
    engine: Engine,
    settings: Settings,
    do2_columns: set[str],
) -> tuple[list[str], str]:
    needed_do2 = {
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
    if not needed_do2.issubset(do2_columns):
        return [], ""

    appendix_columns = set(available_table_columns(engine, settings, "appendix_j"))
    if not _APPENDIX_J_REQUIRED_COLUMNS.issubset(appendix_columns):
        return [], ""

    quote = engine.dialect.identifier_preparer.quote_identifier
    schema = _validate_identifier(settings.db_schema, "schema")
    appendix_table = f"{quote(schema)}.{quote('appendix_j')}"
    selected = [
        f"appendix_j_rule.{quote('id')} AS {quote('rule_appendix_j_id')}",
        f"appendix_j_rule.{quote('audit_account_cage')}::text AS {quote('rule_appendix_audit_account_cage')}",
        "CASE WHEN appendix_j_rule.id IS NULL THEN 0 ELSE 1 END AS "
        f"{quote('i_appendix_j_rule_found')}",
        "CASE WHEN appendix_j_rule.id IS NULL THEN 1 ELSE 0 END AS "
        f"{quote('i_appendix_description_not_found')}",
        "CASE WHEN appendix_j_rule.id IS NULL THEN 0 WHEN appendix_j_rule.from_date_req IS TRUE "
        "AND d.from_date IS NULL THEN 1 ELSE 0 END AS "
        f"{quote('i_appendix_required_from_date_missing')}",
        "CASE WHEN appendix_j_rule.id IS NULL THEN 0 WHEN appendix_j_rule.to_date_req IS TRUE "
        "AND d.to_date IS NULL THEN 1 ELSE 0 END AS "
        f"{quote('i_appendix_required_to_date_missing')}",
        "CASE WHEN appendix_j_rule.id IS NULL THEN 0 WHEN appendix_j_rule.rate_req IS TRUE "
        "AND d.rate IS NULL THEN 1 ELSE 0 END AS "
        f"{quote('i_appendix_required_rate_missing')}",
        "CASE WHEN appendix_j_rule.id IS NULL THEN 0 WHEN appendix_j_rule.tjra_req IS TRUE "
        "AND d.tjra IS NULL THEN 1 ELSE 0 END AS "
        f"{quote('i_appendix_required_tjra_missing')}",
        "CASE WHEN appendix_j_rule.id IS NULL THEN 0 WHEN appendix_j_rule.lra_req IS TRUE "
        "AND d.lra IS NULL THEN 1 ELSE 0 END AS "
        f"{quote('i_appendix_required_lra_missing')}",
        "CASE WHEN appendix_j_rule.id IS NULL THEN 0 WHEN appendix_j_rule.ref_do2_no_req IS TRUE "
        "AND NULLIF(BTRIM(d.ref_do2_no::text), '') IS NULL THEN 1 ELSE 0 END AS "
        f"{quote('i_appendix_required_ref_do2_no_missing')}",
        "CASE WHEN appendix_j_rule.id IS NULL THEN 0 WHEN appendix_j_rule.from_date_req IS FALSE "
        "AND d.from_date IS NOT NULL THEN 1 ELSE 0 END AS "
        f"{quote('i_appendix_from_date_should_be_blank')}",
        "CASE WHEN appendix_j_rule.id IS NULL THEN 0 WHEN appendix_j_rule.to_date_req IS FALSE "
        "AND d.to_date IS NOT NULL THEN 1 ELSE 0 END AS "
        f"{quote('i_appendix_to_date_should_be_blank')}",
        "CASE WHEN appendix_j_rule.id IS NULL THEN 0 ELSE ("
        "CASE WHEN appendix_j_rule.from_date_req IS TRUE AND d.from_date IS NULL THEN 1 ELSE 0 END + "
        "CASE WHEN appendix_j_rule.to_date_req IS TRUE AND d.to_date IS NULL THEN 1 ELSE 0 END + "
        "CASE WHEN appendix_j_rule.rate_req IS TRUE AND d.rate IS NULL THEN 1 ELSE 0 END + "
        "CASE WHEN appendix_j_rule.tjra_req IS TRUE AND d.tjra IS NULL THEN 1 ELSE 0 END + "
        "CASE WHEN appendix_j_rule.lra_req IS TRUE AND d.lra IS NULL THEN 1 ELSE 0 END + "
        "CASE WHEN appendix_j_rule.ref_do2_no_req IS TRUE AND NULLIF(BTRIM(d.ref_do2_no::text), '') IS NULL THEN 1 ELSE 0 END"
        ") END AS "
        f"{quote('i_appendix_required_missing_count')}",
        "CASE WHEN appendix_j_rule.id IS NULL THEN 0 ELSE ("
        "CASE WHEN appendix_j_rule.from_date_req IS FALSE AND d.from_date IS NOT NULL THEN 1 ELSE 0 END + "
        "CASE WHEN appendix_j_rule.to_date_req IS FALSE AND d.to_date IS NOT NULL THEN 1 ELSE 0 END"
        ") END AS "
        f"{quote('i_appendix_should_be_blank_count')}",
    ]
    join_sql = f"""
        LEFT JOIN LATERAL (
          SELECT appendix.*
          FROM {appendix_table} appendix
          WHERE appendix.description = d.description
            AND (
              (
                appendix.do2_group = d.do2_group
                AND COALESCE(appendix.column_a::text, '') = COALESCE(NULLIF(BTRIM(d.column_a::text), ''), '')
                AND COALESCE(appendix.column_b::text, '') = COALESCE(NULLIF(BTRIM(d.column_b::text), ''), '')
                AND COALESCE(appendix.column_c::text, '') = COALESCE(NULLIF(BTRIM(d.column_c::text), ''), '')
              )
              OR appendix.special IS TRUE
            )
          ORDER BY
            CASE
              WHEN appendix.do2_group = d.do2_group
               AND COALESCE(appendix.column_a::text, '') = COALESCE(NULLIF(BTRIM(d.column_a::text), ''), '')
               AND COALESCE(appendix.column_b::text, '') = COALESCE(NULLIF(BTRIM(d.column_b::text), ''), '')
               AND COALESCE(appendix.column_c::text, '') = COALESCE(NULLIF(BTRIM(d.column_c::text), ''), '')
              THEN 0 ELSE 1
            END,
            COALESCE(appendix.sort_priority, 2147483647),
            appendix.id
          LIMIT 1
        ) appendix_j_rule ON TRUE
    """
    return selected, join_sql


def _family_identity_sql(
    engine: Engine,
    settings: Settings,
    do2_columns: set[str],
) -> tuple[list[str], str]:
    needed_do2 = {
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
    if not needed_do2.issubset(do2_columns):
        return [], ""

    family_columns = set(available_table_columns(engine, settings, "family_detail"))
    employee_columns = set(available_table_columns(engine, settings, "employee"))
    if not _FAMILY_DETAIL_REQUIRED_COLUMNS.issubset(family_columns):
        return [], ""

    quote = engine.dialect.identifier_preparer.quote_identifier
    schema = _validate_identifier(settings.db_schema, "schema")
    family_table = f"{quote(schema)}.{quote('family_detail')}"
    employee_table = f"{quote(schema)}.{quote('employee')}"
    has_employee_identity = _IDENTITY_EMPLOYEE_COLUMNS.issubset(employee_columns)
    employee_duplicate_count = (
        "COALESCE(identity_rule.i_employee_uid_duplicate_count, 0)::integer AS "
        f"{quote('i_employee_uid_duplicate_count')}"
        if has_employee_identity
        else f"0::integer AS {quote('i_employee_uid_duplicate_count')}"
    )
    employee_identity_join = (
        f"""
          LEFT JOIN LATERAL (
            SELECT COUNT(*) AS i_employee_uid_duplicate_count
            FROM {employee_table} emp_uid
            WHERE d.description IN ('UID', 'BIOMET')
              AND NULLIF(BTRIM(d.column_c::text), '') IS NOT NULL
              AND emp_uid.uid_no = regexp_replace(d.column_c::text, '\\s+', '', 'g')
              AND emp_uid.current_record IS TRUE
              AND (
                d.fk_employee IS NULL
                OR emp_uid.id <> d.fk_employee
              )
          ) identity_rule ON TRUE
        """
        if has_employee_identity
        else ""
    )
    selected = [
        "COALESCE(family_rule.i_family_relation_name_match_count, 0)::integer AS "
        f"{quote('i_family_relation_name_match_count')}",
        "COALESCE(family_rule.i_family_active_relation_count, 0)::integer AS "
        f"{quote('i_family_active_relation_count')}",
        "COALESCE(family_rule.i_family_uid_duplicate_count, 0)::integer AS "
        f"{quote('i_family_uid_duplicate_count')}",
        "COALESCE(family_rule.i_family_uid_conflict_count, 0)::integer AS "
        f"{quote('i_family_uid_conflict_count')}",
        "COALESCE(family_rule.i_cea_prior_child_claim_count, 0)::integer AS "
        f"{quote('i_cea_prior_child_claim_count')}",
        "COALESCE(family_rule.i_cea_prior_child_overlap_count, 0)::integer AS "
        f"{quote('i_cea_prior_child_overlap_count')}",
        "COALESCE(family_rule.i_cea_children_claimed_same_period_count, 0)::integer AS "
        f"{quote('i_cea_children_claimed_same_period_count')}",
        employee_duplicate_count,
    ]
    join_sql = f"""
        LEFT JOIN LATERAL (
          SELECT
            COUNT(*) FILTER (
              WHERE fd.fk_employee = d.fk_employee
                AND fd.record_status = 'V'
                AND UPPER(COALESCE(fd.relationship, '')) = UPPER(COALESCE(d.column_a::text, ''))
                AND UPPER(COALESCE(fd.name_of_member, '')) = UPPER(COALESCE(d.column_b::text, ''))
            ) AS i_family_relation_name_match_count,
            COUNT(*) FILTER (
              WHERE fd.fk_employee = d.fk_employee
                AND fd.record_status = 'V'
                AND UPPER(COALESCE(fd.relationship, '')) = UPPER(COALESCE(d.column_a::text, ''))
            ) AS i_family_active_relation_count,
            COUNT(*) FILTER (
              WHERE d.description IN ('UID', 'BIOMET')
                AND NULLIF(BTRIM(d.column_c::text), '') IS NOT NULL
                AND fd.record_status = 'V'
                AND fd.uid_no = regexp_replace(d.column_c::text, '\\s+', '', 'g')
            ) AS i_family_uid_duplicate_count,
            COUNT(*) FILTER (
              WHERE d.description IN ('UID', 'BIOMET')
                AND NULLIF(BTRIM(d.column_c::text), '') IS NOT NULL
                AND fd.record_status = 'V'
                AND fd.uid_no = regexp_replace(d.column_c::text, '\\s+', '', 'g')
                AND (
                  fd.fk_employee IS DISTINCT FROM d.fk_employee
                  OR NOT (
                    UPPER(COALESCE(fd.relationship, '')) = UPPER(COALESCE(d.column_a::text, ''))
                    OR (
                      UPPER(COALESCE(d.column_a::text, '')) = 'WIFE'
                      AND UPPER(COALESCE(fd.relationship, '')) = 'SPOUSE'
                    )
                    OR (
                      UPPER(COALESCE(d.column_a::text, '')) = 'SPOUSE'
                      AND UPPER(COALESCE(fd.relationship, '')) = 'WIFE'
                    )
                  )
                )
            ) AS i_family_uid_conflict_count,
            COUNT(*) FILTER (
              WHERE d.description IN ('CEA', 'CEAHC', 'CEATWN', 'HOSSUB')
                AND fd.fk_employee = d.fk_employee
                AND fd.record_status = 'V'
                AND fd.date_of_birth IS NOT NULL
                AND fd.date_of_birth::text = d.column_b::text
            ) AS i_cea_prior_child_claim_count,
            0 AS i_cea_prior_child_overlap_count,
            0 AS i_cea_children_claimed_same_period_count
          FROM {family_table} fd
          WHERE fd.fk_employee = d.fk_employee
             OR (
               d.description IN ('UID', 'BIOMET')
               AND NULLIF(BTRIM(d.column_c::text), '') IS NOT NULL
               AND fd.uid_no = regexp_replace(d.column_c::text, '\\s+', '', 'g')
             )
        ) family_rule ON TRUE
        {employee_identity_join}
    """
    return selected, join_sql


def iter_date_range(
    engine: Engine,
    settings: Settings,
    start_date: str,
    end_date: str,
    *,
    require_status: bool,
    chunk_size: int | None = None,
    maturity_cutoff_date: str | None = None,
) -> Iterator[pd.DataFrame]:
    schema = _validate_identifier(settings.db_schema, "schema")
    table = _validate_identifier(settings.db_table, "table")
    date_column = _validate_identifier(settings.db_date_column, "date column")
    columns = selected_columns(engine, settings, require_status=require_status)
    do2_columns = set(available_columns(engine, settings))

    quote = engine.dialect.identifier_preparer.quote_identifier
    employee_selects, employee_join = _employee_context_sql(engine, settings, do2_columns)
    rule_selects, rule_join = (
        _rule_history_sql(engine, settings, do2_columns)
        if settings.enable_rule_history_sql
        else ([], "")
    )
    appendix_selects, appendix_join = (
        _appendix_j_sql(engine, settings, do2_columns)
        if settings.enable_appendix_j_sql
        else ([], "")
    )
    family_selects, family_join = (
        _family_identity_sql(engine, settings, do2_columns)
        if settings.enable_family_identity_sql
        else ([], "")
    )
    selected_sql = ", ".join(
        [_select_expression(column, quote, "d") for column in columns]
        + employee_selects
        + rule_selects
        + appendix_selects
        + family_selects
    )
    qualified_table = f"{quote(schema)}.{quote(table)}"
    status_filter = (
        f"AND NULLIF(BTRIM(d.{quote('status')}::text), '') IS NOT NULL" if require_status else ""
    )
    maturity_filter = (
        f"AND d.{quote(date_column)} <= :maturity_cutoff_date" if maturity_cutoff_date else ""
    )
    query = text(
        f"""
        SELECT {selected_sql}
        FROM {qualified_table} d
        {employee_join}
        {rule_join}
        {appendix_join}
        {family_join}
        WHERE d.{quote(date_column)} >= :start_date
          AND d.{quote(date_column)} < :end_date
          {status_filter}
          {maturity_filter}
        """
    )
    params = {"start_date": start_date, "end_date": end_date}
    if maturity_cutoff_date:
        params["maturity_cutoff_date"] = maturity_cutoff_date

    yield from pd.read_sql_query(
        query,
        engine,
        params=params,
        chunksize=chunk_size or settings.chunk_size,
    )


def _monthly_windows(start_date: str, end_date: str) -> Iterator[tuple[str, str]]:
    start = date.fromisoformat(start_date)
    end = date.fromisoformat(end_date)
    current = start
    while current < end:
        if current.month == 12:
            next_month = date(current.year + 1, 1, 1)
        else:
            next_month = date(current.year, current.month + 1, 1)
        window_end = min(next_month, end)
        yield current.isoformat(), window_end.isoformat()
        current = window_end


def extract_split(
    engine: Engine,
    settings: Settings,
    split_name: str,
    start_date: str,
    end_date: str,
    *,
    overwrite: bool = False,
    chunk_size: int | None = None,
    maturity_cutoff_date: str | None = None,
) -> dict[str, int | str | dict[str, int]]:
    output_dir = settings.data_root / split_name
    output_dir.mkdir(parents=True, exist_ok=True)
    existing = sorted(output_dir.glob("part-*.parquet"))

    if existing and not overwrite:
        raise FileExistsError(
            f"{output_dir} already contains extracted data. Use --overwrite to replace it."
        )
    if overwrite:
        for path in existing:
            path.unlink()

    total_rows = 0
    status_counts: dict[str, int] = {}
    part_count = 0

    for window_start, window_end in _monthly_windows(start_date, end_date):
        for chunk in iter_date_range(
            engine,
            settings,
            window_start,
            window_end,
            require_status=True,
            chunk_size=chunk_size,
            maturity_cutoff_date=maturity_cutoff_date,
        ):
            normalized_status = (
                chunk["status"]
                .fillna("")
                .astype("string")
                .str.replace('"', "", regex=False)
                .str.strip()
                .str.upper()
            )
            rows = len(chunk)
            chunk.to_parquet(output_dir / f"part-{part_count:05d}.parquet", index=False)
            part_count += 1
            total_rows += rows
            for status, count in normalized_status.value_counts().items():
                status_counts[str(status)] = status_counts.get(str(status), 0) + int(count)
            print(f"{split_name}: saved {total_rows:,} rows through {window_end}")

    if part_count == 0:
        raise RuntimeError(f"No rows found for {split_name}: {start_date} to {end_date}")

    return {
        "split": split_name,
        "start_date": start_date,
        "end_date_exclusive": end_date,
        "maturity_cutoff_date": maturity_cutoff_date or "",
        "rows": total_rows,
        "status_counts": status_counts,
        "parts": part_count,
    }


def read_input_file(path: Path) -> pd.DataFrame:
    suffix = path.suffix.lower()
    if suffix == ".csv":
        return pd.read_csv(path, low_memory=False)
    if suffix in {".parquet", ".pq"}:
        return pd.read_parquet(path)
    raise ValueError("Input must be CSV or Parquet.")
