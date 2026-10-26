from __future__ import annotations

import argparse
import json
import threading
import webbrowser
from datetime import date
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any

import pandas as pd

from do2_model.config import Settings
from do2_model.database import get_engine, iter_date_range
from do2_model.predict import predict_frame

DEFAULT_HOST = "127.0.0.1"
DEFAULT_PORT = 8501


INDEX_HTML = """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>DO2 Status Prediction</title>
  <style>
    :root {
      color-scheme: light;
      --bg: #f5f7fb;
      --panel: #ffffff;
      --text: #17202a;
      --muted: #617080;
      --line: #d9e1ec;
      --accent: #0f766e;
      --danger: #b42318;
      --ok: #137333;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: Arial, Helvetica, sans-serif;
      background: var(--bg);
      color: var(--text);
    }
    main { max-width: 1180px; margin: 0 auto; padding: 28px 20px 40px; }
    header { margin-bottom: 18px; }
    h1 { margin: 0 0 6px; font-size: 28px; line-height: 1.2; }
    .sub { margin: 0; color: var(--muted); }
    form, .summary, .table-wrap {
      background: var(--panel);
      border: 1px solid var(--line);
      border-radius: 8px;
      padding: 16px;
    }
    form {
      display: grid;
      grid-template-columns: repeat(3, minmax(150px, 1fr));
      gap: 14px;
      align-items: end;
    }
    label { display: grid; gap: 6px; font-size: 13px; color: var(--muted); }
    input, select {
      width: 100%;
      min-height: 38px;
      border: 1px solid var(--line);
      border-radius: 6px;
      padding: 8px 10px;
      font-size: 14px;
      color: var(--text);
      background: #fff;
    }
    button {
      min-height: 38px;
      border: 0;
      border-radius: 6px;
      background: var(--accent);
      color: #fff;
      font-weight: 700;
      cursor: pointer;
    }
    button:disabled { opacity: .62; cursor: progress; }
    .status-line { margin: 14px 0; color: var(--muted); min-height: 22px; }
    .error { color: var(--danger); font-weight: 700; }
    .summary {
      display: grid;
      grid-template-columns: repeat(4, minmax(120px, 1fr));
      gap: 12px;
      margin-bottom: 14px;
    }
    .metric { border-left: 3px solid var(--accent); padding-left: 10px; }
    .metric b { display: block; font-size: 22px; margin-bottom: 3px; }
    .metric span { color: var(--muted); font-size: 13px; }
    .table-wrap { overflow: auto; padding: 0; }
    table { width: 100%; border-collapse: collapse; font-size: 13px; }
    th, td { padding: 9px 10px; border-bottom: 1px solid var(--line); white-space: nowrap; text-align: left; }
    th { position: sticky; top: 0; background: #eef3f8; z-index: 1; }
    .badge { display: inline-block; border-radius: 999px; padding: 3px 8px; font-size: 12px; font-weight: 700; }
    .bad { background: #fee4e2; color: var(--danger); }
    .good { background: #dcfce7; color: var(--ok); }
    @media (max-width: 760px) {
      form, .summary { grid-template-columns: 1fr; }
      main { padding: 18px 12px 28px; }
    }
  </style>
</head>
<body>
  <main>
    <header>
      <h1>DO2 Status Prediction</h1>
      <p class="sub">Select a model and created-at date range, then inspect actual status against predicted status.</p>
    </header>
    <form id="predictForm">
      <label>Model
        <select id="modelDir">
          <option value="models_multiclass">train.py model</option>
          <option value="models_prac">prac.ipynb model</option>
        </select>
      </label>
      <label>Start date
        <input id="startDate" type="date" required>
      </label>
      <label>End date
        <input id="endDate" type="date" required>
      </label>
      <button id="runButton" type="submit">Run Prediction</button>
    </form>
    <div id="statusLine" class="status-line"></div>
    <section id="summary" class="summary" hidden></section>
    <section class="table-wrap" id="tableWrap" hidden>
      <table>
        <thead id="tableHead"></thead>
        <tbody id="tableBody"></tbody>
      </table>
    </section>
  </main>
  <script>
    const form = document.getElementById("predictForm");
    const statusLine = document.getElementById("statusLine");
    const summary = document.getElementById("summary");
    const tableWrap = document.getElementById("tableWrap");
    const tableHead = document.getElementById("tableHead");
    const tableBody = document.getElementById("tableBody");
    const button = document.getElementById("runButton");

    function escapeHtml(value) {
      return String(value ?? "").replace(/[&<>"']/g, ch => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;"
      }[ch]));
    }

    function badge(value) {
      if (value === "V") {
        return `<span class="badge good">${escapeHtml(value)}</span>`;
      }
      if (value) return `<span class="badge bad">${escapeHtml(value)}</span>`;
      return escapeHtml(value);
    }

    function renderMetrics(metrics) {
      const items = [
        ["rows", "Rows shown"],
        ["predicted_status_classes", "Predicted classes"],
        ["predicted_v", "Predicted V"],
        ["actual_available", "Rows with actual status"]
      ];
      summary.innerHTML = items.map(([key, label]) =>
        `<div class="metric"><b>${escapeHtml(metrics[key])}</b><span>${label}</span></div>`
      ).join("");
      summary.hidden = false;
    }

    function renderTable(columns, rows) {
      tableHead.innerHTML = `<tr>${columns.map(col => `<th>${escapeHtml(col)}</th>`).join("")}</tr>`;
      tableBody.innerHTML = rows.map(row =>
        `<tr>${columns.map(col => `<td>${badge(row[col])}</td>`).join("")}</tr>`
      ).join("");
      tableWrap.hidden = rows.length === 0;
    }

    form.addEventListener("submit", async event => {
      event.preventDefault();
      button.disabled = true;
      summary.hidden = true;
      tableWrap.hidden = true;
      statusLine.className = "status-line";
      statusLine.textContent = "Running prediction...";
      try {
        const response = await fetch("/api/predict", {
          method: "POST",
          headers: {"Content-Type": "application/json"},
          body: JSON.stringify({
            model_dir: document.getElementById("modelDir").value,
            start_date: document.getElementById("startDate").value,
            end_date: document.getElementById("endDate").value
          })
        });
        const payload = await response.json();
        if (!response.ok) throw new Error(payload.error || "Prediction failed");
        statusLine.textContent = payload.message;
        renderMetrics(payload.metrics);
        renderTable(payload.columns, payload.rows);
      } catch (error) {
        statusLine.className = "status-line error";
        statusLine.textContent = error.message;
      } finally {
        button.disabled = false;
      }
    });
  </script>
</body>
</html>
"""


def _parse_date(value: Any, label: str) -> date:
    if not isinstance(value, str) or not value:
        raise ValueError(f"{label} is required.")
    try:
        return date.fromisoformat(value)
    except ValueError as exc:
        raise ValueError(f"{label} must be YYYY-MM-DD.") from exc


def validate_request(payload: dict[str, Any]) -> tuple[str, str, str]:
    model_dir = payload.get("model_dir") or "models_multiclass"
    if model_dir not in {"models_multiclass", "models_prac"}:
        raise ValueError("model_dir must be either models_multiclass or models_prac.")
    start = _parse_date(payload.get("start_date"), "start_date")
    end = _parse_date(payload.get("end_date"), "end_date")
    if end <= start:
        raise ValueError("end_date must be after start_date. End date is exclusive.")
    return model_dir, start.isoformat(), end.isoformat()


def prediction_records(
    start_date: str,
    end_date: str,
    *,
    model_dir: str = "models_multiclass",
) -> dict[str, Any]:
    settings = Settings()
    engine = get_engine(settings)
    try:
        chunks = list(
            iter_date_range(
                engine,
                settings,
                start_date,
                end_date,
                require_status=False,
            )
        )
    finally:
        engine.dispose()

    if not chunks:
        return {
            "message": "No rows found for the selected date range.",
            "columns": [],
            "rows": [],
            "metrics": {
                "rows": 0,
                "predicted_status_classes": 0,
                "predicted_v": 0,
                "actual_available": 0,
            },
        }

    frame = pd.concat(chunks, ignore_index=True)
    model_path = Path(settings.model_root.parent / model_dir)
    predictions = predict_frame(frame, model_dir=model_path)
    display = predictions.copy()
    if "predicted_status_probability" in display:
        display["predicted_status_probability"] = display["predicted_status_probability"].round(4)

    preferred = [
        "id",
        "created_at",
        "actual_status",
        "predicted_status",
        "predicted_status_probability",
        "model_version",
    ]
    columns = [column for column in preferred if column in display.columns]
    display = display[columns]
    rows = json.loads(display.to_json(orient="records", date_format="iso"))
    return {
        "message": (
            f"Predicted all {len(rows):,} row(s) in the selected range "
            f"using {model_dir}. End date is exclusive."
        ),
        "columns": columns,
        "rows": rows,
        "metrics": {
            "rows": len(rows),
            "predicted_status_classes": int(predictions["predicted_status"].nunique()),
            "predicted_v": int(predictions["predicted_status"].eq("V").sum()),
            "actual_available": int(predictions.get("actual_status", pd.Series(dtype=str)).ne("").sum())
            if "actual_status" in predictions
            else 0,
        },
    }


class PredictionHandler(BaseHTTPRequestHandler):
    server_version = "DO2PredictionUI/0.1"

    def do_GET(self) -> None:
        if self.path not in {"/", "/index.html"}:
            self.send_error(HTTPStatus.NOT_FOUND)
            return
        self._send_text(INDEX_HTML, "text/html; charset=utf-8")

    def do_POST(self) -> None:
        if self.path != "/api/predict":
            self.send_error(HTTPStatus.NOT_FOUND)
            return
        try:
            content_length = int(self.headers.get("Content-Length", "0"))
            payload = json.loads(self.rfile.read(content_length) or b"{}")
            model_dir, start_date, end_date = validate_request(payload)
            response = prediction_records(start_date, end_date, model_dir=model_dir)
            self._send_json(response, HTTPStatus.OK)
        except Exception as exc:  # noqa: BLE001
            self._send_json({"error": str(exc)}, HTTPStatus.BAD_REQUEST)

    def log_message(self, format: str, *args: Any) -> None:
        return

    def _send_text(self, body: str, content_type: str) -> None:
        encoded = body.encode("utf-8")
        self.send_response(HTTPStatus.OK)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def _send_json(self, body: dict[str, Any], status: HTTPStatus) -> None:
        encoded = json.dumps(body).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)


def main() -> None:
    parser = argparse.ArgumentParser(description="Run a local browser UI for DO2 predictions.")
    parser.add_argument("--host", default=DEFAULT_HOST)
    parser.add_argument("--port", default=DEFAULT_PORT, type=int)
    parser.add_argument("--no-browser", action="store_true")
    args = parser.parse_args()

    server = ThreadingHTTPServer((args.host, args.port), PredictionHandler)
    url = f"http://{args.host}:{args.port}"
    print(f"DO2 prediction UI running at {url}")
    if not args.no_browser:
        threading.Timer(0.5, webbrowser.open, args=(url,)).start()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping DO2 prediction UI")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
