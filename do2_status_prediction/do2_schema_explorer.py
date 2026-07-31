#!/usr/bin/env python3
"""
Export confirmed PostgreSQL schema relationships and build an offline explorer.

Install one PostgreSQL driver:
    python -m pip install psycopg2-binary

Create a fresh metadata snapshot and HTML explorer:
    python do2_schema_explorer.py export --database do2 --user postgres

If PostgreSQL asks for a password:
    python do2_schema_explorer.py export --database do2 --user postgres --ask-password

Find one table from the saved JSON without connecting to PostgreSQL:
    python do2_schema_explorer.py find do2_schema_metadata.json do2

Rebuild the HTML later from the saved JSON:
    python do2_schema_explorer.py build do2_schema_metadata.json

Only declared PostgreSQL primary keys and foreign keys are treated as confirmed.
The script does not infer relationships from similar column names.
"""

from __future__ import annotations

import argparse
import getpass
import html
import json
import os
import re
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable


ROW_COUNTS_SQL = """
SELECT
    schemaname AS table_schema,
    relname AS table_name,
    n_live_tup::bigint AS estimated_rows
FROM pg_stat_user_tables
WHERE schemaname = %s
ORDER BY n_live_tup DESC, relname
"""


COLUMNS_SQL = """
SELECT
    table_schema,
    table_name,
    ordinal_position,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = %s
ORDER BY table_schema, table_name, ordinal_position
"""


PRIMARY_KEYS_SQL = """
SELECT
    ns.nspname AS table_schema,
    tbl.relname AS table_name,
    att.attname AS primary_key_column,
    cols.ordinality::integer AS key_position,
    con.conname AS constraint_name
FROM pg_constraint con
JOIN pg_class tbl
    ON tbl.oid = con.conrelid
JOIN pg_namespace ns
    ON ns.oid = tbl.relnamespace
CROSS JOIN LATERAL
    unnest(con.conkey) WITH ORDINALITY AS cols(attnum, ordinality)
JOIN pg_attribute att
    ON att.attrelid = tbl.oid
   AND att.attnum = cols.attnum
WHERE con.contype = 'p'
  AND ns.nspname = %s
ORDER BY table_schema, table_name, key_position
"""


FOREIGN_KEYS_SQL = """
SELECT
    child_ns.nspname AS child_schema,
    child.relname AS child_table,
    child_col.attname AS child_column,
    parent_ns.nspname AS parent_schema,
    parent.relname AS parent_table,
    parent_col.attname AS parent_column,
    fk.conname AS foreign_key_name,
    CASE fk.confupdtype
        WHEN 'a' THEN 'NO ACTION'
        WHEN 'r' THEN 'RESTRICT'
        WHEN 'c' THEN 'CASCADE'
        WHEN 'n' THEN 'SET NULL'
        WHEN 'd' THEN 'SET DEFAULT'
        ELSE fk.confupdtype::text
    END AS update_rule,
    CASE fk.confdeltype
        WHEN 'a' THEN 'NO ACTION'
        WHEN 'r' THEN 'RESTRICT'
        WHEN 'c' THEN 'CASCADE'
        WHEN 'n' THEN 'SET NULL'
        WHEN 'd' THEN 'SET DEFAULT'
        ELSE fk.confdeltype::text
    END AS delete_rule,
    child_keys.position::integer AS key_position
FROM pg_constraint fk
JOIN pg_class child
    ON child.oid = fk.conrelid
JOIN pg_namespace child_ns
    ON child_ns.oid = child.relnamespace
JOIN pg_class parent
    ON parent.oid = fk.confrelid
JOIN pg_namespace parent_ns
    ON parent_ns.oid = parent.relnamespace
CROSS JOIN LATERAL
    unnest(fk.conkey) WITH ORDINALITY AS child_keys(attnum, position)
JOIN LATERAL
    unnest(fk.confkey) WITH ORDINALITY AS parent_keys(attnum, position)
    ON parent_keys.position = child_keys.position
JOIN pg_attribute child_col
    ON child_col.attrelid = child.oid
   AND child_col.attnum = child_keys.attnum
JOIN pg_attribute parent_col
    ON parent_col.attrelid = parent.oid
   AND parent_col.attnum = parent_keys.attnum
WHERE fk.contype = 'f'
  AND child_ns.nspname = %s
ORDER BY
    child_schema,
    child_table,
    foreign_key_name,
    child_keys.position
"""


def load_driver() -> Any:
    try:
        import psycopg  # type: ignore

        return psycopg
    except ImportError:
        try:
            import psycopg2  # type: ignore

            return psycopg2
        except ImportError as exc:
            raise RuntimeError(
                "No PostgreSQL driver is installed. Run: "
                "python -m pip install psycopg2-binary"
            ) from exc


def connect(args: argparse.Namespace) -> Any:
    driver = load_driver()
    connection_options: dict[str, Any] = {
        "dbname": args.database,
        "host": args.host,
        "port": args.port,
        "user": args.user,
    }
    if args.ask_password:
        connection_options["password"] = getpass.getpass("PostgreSQL password: ")
    return driver.connect(**connection_options)


def fetch_rows(connection: Any, query: str, parameter: str) -> list[dict[str, Any]]:
    with connection.cursor() as cursor:
        cursor.execute(query, (parameter,))
        column_names = [description[0] for description in cursor.description]
        return [
            dict(zip(column_names, row, strict=True))
            for row in cursor.fetchall()
        ]


def safe_basename(value: str) -> str:
    cleaned = re.sub(r"[^A-Za-z0-9_.-]+", "_", value.strip())
    return cleaned.strip("._") or "database"


def all_table_names(metadata: dict[str, Any]) -> list[str]:
    names: set[str] = set()
    for row in metadata.get("tables", []):
        names.add(row["table_name"])
    for row in metadata.get("columns", []):
        names.add(row["table_name"])
    for row in metadata.get("primary_keys", []):
        names.add(row["table_name"])
    for row in metadata.get("foreign_keys", []):
        names.add(row["parent_table"])
        names.add(row["child_table"])
    return sorted(names)


def export_metadata(args: argparse.Namespace) -> tuple[Path, Path]:
    with connect(args) as connection:
        metadata = {
            "format_version": 1,
            "generated_at_utc": datetime.now(timezone.utc).isoformat(),
            "database": args.database,
            "schema": args.schema,
            "relationship_policy": (
                "Only declared PostgreSQL foreign-key constraints are confirmed."
            ),
            "tables": fetch_rows(connection, ROW_COUNTS_SQL, args.schema),
            "columns": fetch_rows(connection, COLUMNS_SQL, args.schema),
            "primary_keys": fetch_rows(connection, PRIMARY_KEYS_SQL, args.schema),
            "foreign_keys": fetch_rows(connection, FOREIGN_KEYS_SQL, args.schema),
        }

    metadata["summary"] = {
        "tables": len(all_table_names(metadata)),
        "columns": len(metadata["columns"]),
        "primary_key_columns": len(metadata["primary_keys"]),
        "foreign_key_column_relationships": len(metadata["foreign_keys"]),
    }

    output_dir = Path(args.output_dir).expanduser().resolve()
    output_dir.mkdir(parents=True, exist_ok=True)
    database_name = safe_basename(args.database)
    json_path = output_dir / f"{database_name}_schema_metadata.json"
    html_path = output_dir / f"{database_name}_relationship_explorer.html"

    json_path.write_text(
        json.dumps(metadata, indent=2, ensure_ascii=False, default=str),
        encoding="utf-8",
    )
    html_path.write_text(build_html(metadata), encoding="utf-8")
    return json_path, html_path


def load_metadata(path: str | Path) -> dict[str, Any]:
    metadata_path = Path(path).expanduser().resolve()
    with metadata_path.open("r", encoding="utf-8") as file:
        data = json.load(file)
    required = {"columns", "primary_keys", "foreign_keys"}
    missing = sorted(required.difference(data))
    if missing:
        raise ValueError(
            f"{metadata_path} is missing required fields: {', '.join(missing)}"
        )
    return data


def json_for_script(value: Any) -> str:
    result = json.dumps(value, ensure_ascii=False, separators=(",", ":"), default=str)
    return (
        result.replace("</", "<\\/")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")
    )


def build_html(metadata: dict[str, Any]) -> str:
    database = html.escape(str(metadata.get("database", "database")))
    schema = html.escape(str(metadata.get("schema", "public")))
    payload = {
        "database": metadata.get("database", "database"),
        "schema": metadata.get("schema", "public"),
        "generated_at_utc": metadata.get("generated_at_utc"),
        "tables": all_table_names(metadata),
        "row_counts": {
            row["table_name"]: row.get("estimated_rows")
            for row in metadata.get("tables", [])
        },
        "columns": metadata.get("columns", []),
        "primary_keys": metadata.get("primary_keys", []),
        "foreign_keys": metadata.get("foreign_keys", []),
    }

    template = """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>__DATABASE__ relationship explorer</title>
  <style>
    :root {
      color-scheme: light dark;
      --bg: #f6f7f9;
      --panel: #ffffff;
      --text: #172033;
      --muted: #647087;
      --border: #d9dee8;
      --accent: #2563eb;
      --accent-soft: #dbeafe;
      --selected-text: #ffffff;
    }
    @media (prefers-color-scheme: dark) {
      :root {
        --bg: #111827;
        --panel: #1f2937;
        --text: #f3f4f6;
        --muted: #aeb7c7;
        --border: #465165;
        --accent: #60a5fa;
        --accent-soft: #203d67;
        --selected-text: #0b1220;
      }
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      background: var(--bg);
      color: var(--text);
      font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    }
    main { width: min(1180px, calc(100% - 28px)); margin: 24px auto 48px; }
    h1 { margin: 0 0 6px; font-size: clamp(1.4rem, 3vw, 2rem); }
    h2 { margin: 26px 0 10px; font-size: 1.15rem; }
    .muted { color: var(--muted); }
    .controls {
      display: flex;
      flex-wrap: wrap;
      align-items: end;
      gap: 12px;
      margin: 20px 0 12px;
    }
    label { display: grid; gap: 6px; font-weight: 600; }
    select {
      min-width: min(360px, 90vw);
      padding: 9px 11px;
      border: 1px solid var(--border);
      border-radius: 8px;
      background: var(--panel);
      color: var(--text);
      font: inherit;
    }
    .summary {
      padding: 12px 14px;
      border: 1px solid var(--border);
      border-radius: 9px;
      background: var(--panel);
    }
    svg { width: 100%; display: block; overflow: visible; margin-top: 10px; }
    .link { fill: none; stroke: var(--accent); stroke-width: 1.5; opacity: .7; }
    .node { fill: var(--accent-soft); stroke: var(--border); }
    .focus { fill: var(--accent); stroke: var(--accent); }
    .node-label { fill: var(--text); font-size: 13px; font-weight: 600; }
    .node-sub { fill: var(--muted); font-size: 11px; }
    .focus-label, .focus-sub { fill: var(--selected-text); }
    .focus-label { font-size: 14px; font-weight: 700; }
    .focus-sub { font-size: 11px; }
    .section-label { fill: var(--muted); font-size: 12px; font-weight: 600; }
    .table-wrap { overflow-x: auto; background: var(--panel); border-radius: 9px; }
    table { width: 100%; border-collapse: collapse; }
    th, td {
      padding: 9px 11px;
      border-bottom: 1px solid var(--border);
      text-align: left;
      vertical-align: top;
    }
    th { color: var(--muted); font-size: .82rem; }
    td { font-size: .9rem; }
    code { overflow-wrap: anywhere; }
    .badge {
      display: inline-block;
      margin-left: 6px;
      padding: 1px 6px;
      border-radius: 999px;
      background: var(--accent-soft);
      font-size: .72rem;
      font-weight: 700;
    }
  </style>
</head>
<body>
<main>
  <h1>__DATABASE__ database relationships</h1>
  <div class="muted">Schema: <code>__SCHEMA__</code>. Only declared PostgreSQL constraints are shown as relationships.</div>

  <div class="controls">
    <label for="table-select">Find table
      <select id="table-select"></select>
    </label>
  </div>

  <div id="summary" class="summary" aria-live="polite"></div>

  <svg id="chart" role="img" aria-labelledby="chart-title chart-desc">
    <title id="chart-title">Foreign-key relationships for the selected table</title>
    <desc id="chart-desc">Parent tables appear above the selected table and referencing child tables below it.</desc>
    <defs>
      <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto">
        <path d="M 0 0 L 10 5 L 0 10 z" fill="var(--accent)"></path>
      </marker>
    </defs>
    <g id="links"></g>
    <g id="nodes"></g>
  </svg>

  <h2>Confirmed foreign-key column relationships</h2>
  <div class="table-wrap">
    <table>
      <thead>
        <tr>
          <th>Referenced parent</th>
          <th>Foreign-key child</th>
          <th>Constraint</th>
          <th>Update/delete</th>
        </tr>
      </thead>
      <tbody id="relationship-rows"></tbody>
    </table>
  </div>

  <h2>Selected table columns</h2>
  <div class="table-wrap">
    <table>
      <thead>
        <tr>
          <th>Position</th>
          <th>Column</th>
          <th>Type</th>
          <th>Nullable</th>
          <th>Default</th>
        </tr>
      </thead>
      <tbody id="column-rows"></tbody>
    </table>
  </div>
</main>

<script>
(() => {
  const data = __SCHEMA_DATA__;
  const select = document.getElementById("table-select");
  const summary = document.getElementById("summary");
  const svg = document.getElementById("chart");
  const linksLayer = document.getElementById("links");
  const nodesLayer = document.getElementById("nodes");
  const relationshipRows = document.getElementById("relationship-rows");
  const columnRows = document.getElementById("column-rows");
  const svgNS = "http://www.w3.org/2000/svg";

  data.tables.forEach(table => {
    const option = document.createElement("option");
    option.value = table;
    option.textContent = table;
    select.appendChild(option);
  });
  select.value = data.tables.includes("do2") ? "do2" : data.tables[0];

  function element(name, attributes = {}) {
    const item = document.createElementNS(svgNS, name);
    Object.entries(attributes).forEach(([key, value]) => item.setAttribute(key, String(value)));
    return item;
  }

  function group(items, key) {
    const result = new Map();
    items.forEach(item => {
      const value = item[key];
      if (!result.has(value)) result.set(value, []);
      result.get(value).push(item);
    });
    return Array.from(result, ([table, edges]) => ({ table, edges }))
      .sort((a, b) => a.table.localeCompare(b.table));
  }

  function layout(count, startY, width) {
    const columns = width < 560 ? 2 : 4;
    const gapX = 12;
    const side = 10;
    const nodeWidth = Math.max(125, (width - side * 2 - gapX * (columns - 1)) / columns);
    const nodeHeight = 62;
    const gapY = 14;
    const items = [];
    for (let index = 0; index < count; index += 1) {
      const row = Math.floor(index / columns);
      const countInRow = Math.min(columns, count - row * columns);
      const rowWidth = countInRow * nodeWidth + (countInRow - 1) * gapX;
      const rowStart = (width - rowWidth) / 2;
      const column = index % columns;
      items.push({
        x: rowStart + column * (nodeWidth + gapX),
        y: startY + row * (nodeHeight + gapY),
        width: nodeWidth,
        height: nodeHeight
      });
    }
    const rows = Math.ceil(count / columns);
    return {
      items,
      height: rows ? rows * nodeHeight + (rows - 1) * gapY : 0
    };
  }

  function wrappedName(name, maxCharacters) {
    if (name.length <= maxCharacters) return [name];
    const pieces = name.split("_");
    const lines = [""];
    pieces.forEach(piece => {
      const current = lines[lines.length - 1];
      const candidate = current ? current + "_" + piece : piece;
      if (candidate.length <= maxCharacters || lines.length === 2) {
        lines[lines.length - 1] = candidate;
      } else {
        lines.push(piece);
      }
    });
    return lines.slice(0, 2);
  }

  function drawNode(position, label, subtitle, selected = false) {
    const groupElement = element("g");
    groupElement.appendChild(element("rect", {
      x: position.x,
      y: position.y,
      width: position.width,
      height: position.height,
      rx: 8,
      class: selected ? "node focus" : "node"
    }));
    const labelClass = selected ? "focus-label" : "node-label";
    const subtitleClass = selected ? "focus-sub" : "node-sub";
    wrappedName(label, Math.max(12, Math.floor(position.width / 8))).forEach((line, index) => {
      const text = element("text", {
        x: position.x + position.width / 2,
        y: position.y + 20 + index * 15,
        "text-anchor": "middle",
        class: labelClass
      });
      text.textContent = line;
      groupElement.appendChild(text);
    });
    const sub = element("text", {
      x: position.x + position.width / 2,
      y: position.y + position.height - 9,
      "text-anchor": "middle",
      class: subtitleClass
    });
    sub.textContent = subtitle;
    groupElement.appendChild(sub);
    nodesLayer.appendChild(groupElement);
  }

  function drawSectionLabel(x, y, value) {
    const text = element("text", {
      x,
      y,
      "text-anchor": "middle",
      class: "section-label"
    });
    text.textContent = value;
    nodesLayer.appendChild(text);
  }

  function drawLink(from, to) {
    const startX = from.x + from.width / 2;
    const startY = from.y + from.height;
    const endX = to.x + to.width / 2;
    const endY = to.y;
    const middleY = (startY + endY) / 2;
    linksLayer.appendChild(element("path", {
      d: `M ${startX} ${startY} C ${startX} ${middleY}, ${endX} ${middleY}, ${endX} ${endY}`,
      class: "link",
      "marker-end": "url(#arrow)"
    }));
  }

  function addTextCell(row, value, code = false) {
    const cell = document.createElement("td");
    if (code) {
      const codeElement = document.createElement("code");
      codeElement.textContent = value ?? "";
      cell.appendChild(codeElement);
    } else {
      cell.textContent = value ?? "";
    }
    row.appendChild(cell);
  }

  function render() {
    const selectedTable = select.value;
    const parentEdges = data.foreign_keys.filter(edge => edge.child_table === selectedTable);
    const childEdges = data.foreign_keys.filter(edge => edge.parent_table === selectedTable);
    const selectedEdges = data.foreign_keys
      .filter(edge => edge.parent_table === selectedTable || edge.child_table === selectedTable)
      .sort((a, b) =>
        (a.parent_table + a.child_table + a.foreign_key_name + a.key_position)
          .localeCompare(b.parent_table + b.child_table + b.foreign_key_name + b.key_position)
      );
    const parents = group(parentEdges, "parent_table");
    const children = group(childEdges, "child_table");
    const estimatedRows = data.row_counts[selectedTable];
    const rowText = estimatedRows === undefined || estimatedRows === null
      ? "row estimate unavailable"
      : Number(estimatedRows).toLocaleString() + " estimated rows";

    summary.textContent =
      selectedTable + ": " + rowText + "; " +
      parents.length + " referenced parent table(s); " +
      children.length + " referencing child table(s); " +
      selectedEdges.length + " confirmed foreign-key column relationship(s).";

    linksLayer.replaceChildren();
    nodesLayer.replaceChildren();
    relationshipRows.replaceChildren();
    columnRows.replaceChildren();

    const width = Math.max(320, Math.floor(svg.getBoundingClientRect().width || 900));
    const parentLayout = layout(parents.length, 34, width);
    const focusWidth = Math.min(230, width - 30);
    const focus = {
      x: (width - focusWidth) / 2,
      y: 34 + parentLayout.height + (parents.length ? 44 : 24),
      width: focusWidth,
      height: 68
    };
    const childLabelY = focus.y + focus.height + 30;
    const childLayout = layout(children.length, childLabelY + 14, width);
    const height = Math.max(220, childLabelY + 14 + childLayout.height + 24);
    svg.setAttribute("viewBox", `0 0 ${width} ${height}`);
    svg.setAttribute("height", String(height));

    drawSectionLabel(width / 2, 18, "Referenced parents");
    const parentPositions = parents.map((item, index) => {
      const position = parentLayout.items[index];
      drawNode(position, item.table, item.edges.length + " column link(s)");
      return position;
    });
    drawNode(focus, selectedTable, "selected table", true);
    parentPositions.forEach(position => drawLink(position, focus));

    drawSectionLabel(width / 2, childLabelY, "Referencing children");
    children.forEach((item, index) => {
      const position = childLayout.items[index];
      drawNode(position, item.table, item.edges.length + " column link(s)");
      drawLink(focus, position);
    });

    selectedEdges.forEach(edge => {
      const row = document.createElement("tr");
      addTextCell(row, edge.parent_schema + "." + edge.parent_table + "." + edge.parent_column, true);
      addTextCell(row, edge.child_schema + "." + edge.child_table + "." + edge.child_column, true);
      addTextCell(row, edge.foreign_key_name, true);
      addTextCell(row, edge.update_rule + " / " + edge.delete_rule);
      relationshipRows.appendChild(row);
    });
    if (!selectedEdges.length) {
      const row = document.createElement("tr");
      const cell = document.createElement("td");
      cell.colSpan = 4;
      cell.textContent = "No declared foreign-key relationship was found for this table.";
      row.appendChild(cell);
      relationshipRows.appendChild(row);
    }

    const primaryKeyColumns = new Set(
      data.primary_keys
        .filter(item => item.table_name === selectedTable)
        .map(item => item.primary_key_column)
    );
    const columns = data.columns
      .filter(column => column.table_name === selectedTable)
      .sort((a, b) => Number(a.ordinal_position) - Number(b.ordinal_position));
    columns.forEach(column => {
      const row = document.createElement("tr");
      addTextCell(row, String(column.ordinal_position));
      const nameCell = document.createElement("td");
      const code = document.createElement("code");
      code.textContent = column.column_name;
      nameCell.appendChild(code);
      if (primaryKeyColumns.has(column.column_name)) {
        const badge = document.createElement("span");
        badge.className = "badge";
        badge.textContent = "PK";
        nameCell.appendChild(badge);
      }
      row.appendChild(nameCell);
      addTextCell(row, column.data_type);
      addTextCell(row, column.is_nullable);
      addTextCell(row, column.column_default ?? "", true);
      columnRows.appendChild(row);
    });
    if (!columns.length) {
      const row = document.createElement("tr");
      const cell = document.createElement("td");
      cell.colSpan = 5;
      cell.textContent = "No column metadata was saved for this table.";
      row.appendChild(cell);
      columnRows.appendChild(row);
    }
  }

  select.addEventListener("change", render);
  let resizeTimer;
  window.addEventListener("resize", () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(render, 120);
  });
  render();
})();
</script>
</body>
</html>
"""
    return (
        template.replace("__DATABASE__", database)
        .replace("__SCHEMA__", schema)
        .replace("__SCHEMA_DATA__", json_for_script(payload))
    )


def build_from_json(args: argparse.Namespace) -> Path:
    metadata_path = Path(args.metadata).expanduser().resolve()
    metadata = load_metadata(metadata_path)
    if args.output:
        output_path = Path(args.output).expanduser().resolve()
    else:
        output_path = metadata_path.with_name(
            f"{safe_basename(str(metadata.get('database', 'database')))}"
            "_relationship_explorer.html"
        )
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(build_html(metadata), encoding="utf-8")
    return output_path


def print_table(metadata: dict[str, Any], requested_table: str) -> int:
    table_lookup = {name.casefold(): name for name in all_table_names(metadata)}
    table = table_lookup.get(requested_table.casefold())
    if table is None:
        print(f"Table not found: {requested_table}", file=sys.stderr)
        suggestions = [
            name for name in all_table_names(metadata)
            if requested_table.casefold() in name.casefold()
        ][:20]
        if suggestions:
            print("Possible matches:", file=sys.stderr)
            for suggestion in suggestions:
                print(f"  {suggestion}", file=sys.stderr)
        return 1

    columns = sorted(
        (
            row for row in metadata["columns"]
            if row["table_name"] == table
        ),
        key=lambda row: int(row["ordinal_position"]),
    )
    primary_keys = {
        row["primary_key_column"]
        for row in metadata["primary_keys"]
        if row["table_name"] == table
    }
    relations = sorted(
        (
            row for row in metadata["foreign_keys"]
            if row["parent_table"] == table or row["child_table"] == table
        ),
        key=lambda row: (
            row["parent_table"],
            row["child_table"],
            row["foreign_key_name"],
            int(row["key_position"]),
        ),
    )

    print(f"TABLE: {table}")
    print("\nCOLUMNS")
    if not columns:
        print("  No saved columns.")
    for column in columns:
        marker = " [PK]" if column["column_name"] in primary_keys else ""
        nullable = "NULL" if column["is_nullable"] == "YES" else "NOT NULL"
        print(
            f"  {column['ordinal_position']:>3}  "
            f"{column['column_name']}  {column['data_type']}  {nullable}{marker}"
        )

    print("\nCONFIRMED FOREIGN KEYS")
    if not relations:
        print("  No declared foreign-key relationships.")
    for relation in relations:
        print(
            "  "
            f"{relation['parent_schema']}.{relation['parent_table']}."
            f"{relation['parent_column']} -> "
            f"{relation['child_schema']}.{relation['child_table']}."
            f"{relation['child_column']} "
            f"[{relation['foreign_key_name']}]"
        )
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Save PostgreSQL schema metadata and create a searchable, "
            "offline relationship explorer."
        )
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    export_parser = subparsers.add_parser(
        "export",
        help="Connect to PostgreSQL and save JSON plus HTML.",
    )
    export_parser.add_argument(
        "--database",
        default=os.getenv("PGDATABASE", "do2"),
        help="Database name; default: PGDATABASE or do2.",
    )
    export_parser.add_argument(
        "--host",
        default=os.getenv("PGHOST", "localhost"),
        help="PostgreSQL host; default: PGHOST or localhost.",
    )
    export_parser.add_argument(
        "--port",
        type=int,
        default=int(os.getenv("PGPORT", "5432")),
        help="PostgreSQL port; default: PGPORT or 5432.",
    )
    export_parser.add_argument(
        "--user",
        default=os.getenv("PGUSER", "postgres"),
        help="PostgreSQL user; default: PGUSER or postgres.",
    )
    export_parser.add_argument(
        "--schema",
        default="public",
        help="Schema to export; default: public.",
    )
    export_parser.add_argument(
        "--output-dir",
        default=".",
        help="Directory for the JSON and HTML files; default: current directory.",
    )
    export_parser.add_argument(
        "--ask-password",
        action="store_true",
        help="Prompt securely for the PostgreSQL password.",
    )

    build_parser_ = subparsers.add_parser(
        "build",
        help="Rebuild the HTML explorer from previously saved JSON.",
    )
    build_parser_.add_argument("metadata", help="Saved schema metadata JSON file.")
    build_parser_.add_argument(
        "--output",
        help="Output HTML path; default: <database>_relationship_explorer.html.",
    )

    find_parser = subparsers.add_parser(
        "find",
        help="Print one table's saved columns and confirmed relationships.",
    )
    find_parser.add_argument("metadata", help="Saved schema metadata JSON file.")
    find_parser.add_argument("table", help="Exact table name, case-insensitive.")
    return parser


def main(argv: Iterable[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    try:
        if args.command == "export":
            json_path, html_path = export_metadata(args)
            print(f"Saved metadata: {json_path}")
            print(f"Saved explorer: {html_path}")
            print("Open the HTML file in a browser. Re-run export after schema changes.")
            return 0
        if args.command == "build":
            output_path = build_from_json(args)
            print(f"Saved explorer: {output_path}")
            return 0
        if args.command == "find":
            return print_table(load_metadata(args.metadata), args.table)
    except Exception as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 1
    parser.error("Unknown command")
    return 2


if __name__ == "__main__":
    raise SystemExit(main())