from __future__ import annotations

import re
from collections.abc import Iterator
from pathlib import Path

import pandas as pd
from sqlalchemy import Engine, create_engine, inspect, text

from do2_model.config import LEAKAGE_COLUMNS, Settings, load_feature_config

_IDENTIFIER = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")
_PRESENCE_ONLY_COLUMNS = {
    "attachment": "has_attachment_source",
    "cfa_sanction_letter_no": "has_cfa_letter_source",
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


def _select_expression(column: str, quote) -> str:
    if column in _PRESENCE_ONLY_COLUMNS:
        alias = _PRESENCE_ONLY_COLUMNS[column]
        return (
            "CASE WHEN NULLIF(BTRIM("
            f"{quote(column)}::text), '') IS NULL THEN 0 ELSE 1 END AS {quote(alias)}"
        )
    if column in _DATE_FEATURE_COLUMNS:
        return f"{quote(column)}::text AS {quote(column)}"
    return quote(column)


def iter_date_range(
    engine: Engine,
    settings: Settings,
    start_date: str,
    end_date: str,
    *,
    require_status: bool,
    chunk_size: int | None = None,
) -> Iterator[pd.DataFrame]:
    schema = _validate_identifier(settings.db_schema, "schema")
    table = _validate_identifier(settings.db_table, "table")
    date_column = _validate_identifier(settings.db_date_column, "date column")
    columns = selected_columns(engine, settings, require_status=require_status)

    quote = engine.dialect.identifier_preparer.quote_identifier
    selected_sql = ", ".join(_select_expression(column, quote) for column in columns)
    qualified_table = f"{quote(schema)}.{quote(table)}"
    status_filter = (
        f"AND NULLIF(BTRIM({quote('status')}::text), '') IS NOT NULL" if require_status else ""
    )
    query = text(
        f"""
        SELECT {selected_sql}
        FROM {qualified_table}
        WHERE {quote(date_column)} >= :start_date
          AND {quote(date_column)} < :end_date
          {status_filter}
        """
    )

    yield from pd.read_sql_query(
        query,
        engine,
        params={"start_date": start_date, "end_date": end_date},
        chunksize=chunk_size or settings.chunk_size,
    )


def extract_split(
    engine: Engine,
    settings: Settings,
    split_name: str,
    start_date: str,
    end_date: str,
    *,
    overwrite: bool = False,
    chunk_size: int | None = None,
) -> dict[str, int | str]:
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
    valid_rows = 0
    non_valid_rows = 0
    part_count = 0

    for part_count, chunk in enumerate(
        iter_date_range(
            engine,
            settings,
            start_date,
            end_date,
            require_status=True,
            chunk_size=chunk_size,
        ),
        start=1,
    ):
        normalized_status = (
            chunk["status"]
            .fillna("")
            .astype("string")
            .str.replace('"', "", regex=False)
            .str.strip()
            .str.upper()
        )
        chunk_valid = int(normalized_status.eq("V").sum())
        rows = len(chunk)
        chunk.to_parquet(output_dir / f"part-{part_count - 1:05d}.parquet", index=False)
        total_rows += rows
        valid_rows += chunk_valid
        non_valid_rows += rows - chunk_valid
        print(f"{split_name}: saved {total_rows:,} rows")

    if part_count == 0:
        raise RuntimeError(f"No rows found for {split_name}: {start_date} to {end_date}")

    return {
        "split": split_name,
        "start_date": start_date,
        "end_date_exclusive": end_date,
        "rows": total_rows,
        "valid_rows": valid_rows,
        "non_valid_rows": non_valid_rows,
        "parts": part_count,
    }


def read_input_file(path: Path) -> pd.DataFrame:
    suffix = path.suffix.lower()
    if suffix == ".csv":
        return pd.read_csv(path, low_memory=False)
    if suffix in {".parquet", ".pq"}:
        return pd.read_parquet(path)
    raise ValueError("Input must be CSV or Parquet.")
