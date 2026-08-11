from __future__ import annotations

import argparse
import json
import math
import mimetypes
from http import HTTPStatus
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

import pandas as pd
from sqlalchemy.exc import SQLAlchemyError

from do2_model.config import Settings
from do2_model.database import get_engine, iter_date_range
from do2_model.evaluation import classification_metrics
from do2_model.features import leakage_columns_present
from do2_model.predict import load_model_assets, predict_frame

STATIC_ROOT = Path(__file__).resolve().parent / "web_static"


def _json_safe(value):
    if value is None:
        return None
    if isinstance(value, float) and math.isnan(value):
        return None
    if hasattr(value, "item"):
        return value.item()
    return value


def _print_ui_run_diagnostics(
    *,
    start_date: str,
    end_date: str,
    frame: pd.DataFrame,
    predictions: pd.DataFrame,
) -> None:
    _model, metadata = load_model_assets()
    feature_columns = metadata.get("feature_columns", [])
    categorical_columns = metadata.get("categorical_columns", [])
    threshold = float(metadata["selected_probability_non_valid_threshold"])
    leakage_present = leakage_columns_present(frame.columns)
    unused_input_columns = sorted(set(frame.columns) - set(feature_columns))

    print("\n" + "=" * 80, flush=True)
    print(f"UI prediction run: {start_date} <= do2_date < {end_date}", flush=True)
    print(f"Rows loaded: {len(frame):,}", flush=True)
    print(f"Model version: {metadata.get('model_version', 'unknown')}", flush=True)
    print(f"Threshold: {threshold:.4f}", flush=True)
    print(f"Feature engineering: {len(feature_columns)} model columns used", flush=True)
    print(f"Categorical features ({len(categorical_columns)}): {', '.join(categorical_columns)}", flush=True)
    print(f"Feature columns: {', '.join(feature_columns)}", flush=True)
    print(
        f"Leakage/label columns present in input and dropped: "
        f"{', '.join(leakage_present) if leakage_present else 'none'}",
        flush=True,
    )
    print(
        f"Input columns not used by model ({len(unused_input_columns)}): "
        f"{', '.join(unused_input_columns) if unused_input_columns else 'none'}",
        flush=True,
    )

    if not {"actual_status_group", "non_valid_probability"}.issubset(predictions.columns):
        print("Metrics/confusion matrix skipped: actual labels or probabilities missing.", flush=True)
        print("=" * 80, flush=True)
        return

    y_true = predictions["actual_status_group"].eq("NON_VALID").astype("int8").to_numpy()
    probability = predictions["non_valid_probability"].to_numpy()
    metrics = classification_metrics(y_true, probability, threshold)
    matrix = pd.DataFrame(
        [
            [metrics["true_valid_predicted_valid"], metrics["valid_flagged_non_valid"]],
            [metrics["non_valid_missed_as_valid"], metrics["non_valid_detected"]],
        ],
        index=pd.Index(["Actual V", "Actual NON_VALID"], name="actual"),
        columns=pd.Index(["Predicted V", "Predicted NON_VALID"], name="predicted"),
    )

    print("\nConfusion matrix", flush=True)
    print(matrix.to_string(), flush=True)
    print("\nMetrics", flush=True)
    for key in (
        "accuracy",
        "precision_non_valid",
        "recall_non_valid",
        "f1_non_valid",
        "f2_non_valid",
        "roc_auc",
        "pr_auc_non_valid",
        "human_review_rate",
        "valid_review_rate",
        "false_valid_rate",
    ):
        if key in metrics:
            print(f"{key}: {metrics[key]:.6f}", flush=True)
    print("=" * 80, flush=True)


def prediction_payload(start_date: str, end_date: str) -> dict:
    if not start_date or not end_date:
        raise ValueError("start_date and end_date are required")
    if start_date >= end_date:
        raise ValueError("start_date must be earlier than end_date")

    settings = Settings()
    engine = get_engine(settings)
    try:
        chunks = list(
            iter_date_range(
                engine,
                settings,
                start_date,
                end_date,
                require_status=True,
                chunk_size=settings.chunk_size,
            )
        )
    finally:
        engine.dispose()

    if not chunks:
        return {
            "range": {"start_date": start_date, "end_date": end_date},
            "summary": {
                "rows_returned": 0,
                "total_rows_loaded": 0,
                "matches": 0,
                "mismatches": 0,
                "actual_v": 0,
                "actual_non_valid": 0,
                "predicted_v": 0,
                "predicted_non_valid": 0,
            },
            "rows": [],
        }

    frame = pd.concat(chunks, ignore_index=True)
    total_rows_loaded = len(frame)
    frame = frame.copy()
    predictions = predict_frame(frame)
    predictions["matches_actual"] = (
        predictions["actual_status_group"] == predictions["predicted_status_group"]
    )
    _print_ui_run_diagnostics(
        start_date=start_date,
        end_date=end_date,
        frame=frame,
        predictions=predictions,
    )

    display_columns = [
        column
        for column in (
            "id",
            "created_at",
            settings.db_date_column,
            "do2_type",
            "army_no",
            "actual_status",
            "actual_status_group",
            "predicted_status_group",
            "non_valid_probability",
            "threshold_used",
            "recommended_action",
            "matches_actual",
        )
        if column in predictions.columns or column in frame.columns
    ]

    merged = pd.concat(
        [
            frame[[column for column in display_columns if column in frame.columns]],
            predictions[[column for column in display_columns if column in predictions.columns]],
        ],
        axis=1,
    )
    merged = merged.loc[:, ~merged.columns.duplicated()]

    rows = [
        {column: _json_safe(value) for column, value in record.items()}
        for record in merged.to_dict(orient="records")
    ]
    matches = int(predictions["matches_actual"].sum())
    returned = len(predictions)
    return {
        "range": {"start_date": start_date, "end_date": end_date},
        "summary": {
            "rows_returned": returned,
            "total_rows_loaded": total_rows_loaded,
            "matches": matches,
            "mismatches": returned - matches,
            "actual_v": int(predictions["actual_status_group"].eq("V").sum()),
            "actual_non_valid": int(predictions["actual_status_group"].eq("NON_VALID").sum()),
            "predicted_v": int(predictions["predicted_status_group"].eq("V").sum()),
            "predicted_non_valid": int(predictions["predicted_status_group"].eq("NON_VALID").sum()),
        },
        "rows": rows,
    }


class Do2RequestHandler(SimpleHTTPRequestHandler):
    def translate_path(self, path: str) -> str:
        parsed = urlparse(path)
        clean_path = parsed.path.lstrip("/") or "index.html"
        requested_path = (STATIC_ROOT / clean_path).resolve()
        if STATIC_ROOT.resolve() not in requested_path.parents and requested_path != STATIC_ROOT.resolve():
            return str(STATIC_ROOT / "index.html")
        return str(requested_path)

    def do_GET(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path == "/api/predict":
            self._handle_predict(parsed.query)
            return
        if parsed.path == "/api/health":
            self._send_json({"status": "ok"})
            return
        super().do_GET()

    def end_headers(self) -> None:
        self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def guess_type(self, path: str) -> str:
        guessed, _encoding = mimetypes.guess_type(path)
        return guessed or "application/octet-stream"

    def _handle_predict(self, query: str) -> None:
        params = parse_qs(query)
        try:
            payload = prediction_payload(
                params.get("start_date", [""])[0],
                params.get("end_date", [""])[0],
            )
        except (ValueError, RuntimeError, FileNotFoundError, KeyError, SQLAlchemyError) as exc:
            self._send_json({"error": str(exc)}, status=HTTPStatus.BAD_REQUEST)
            return
        self._send_json(payload)

    def _send_json(self, payload: dict, status: HTTPStatus = HTTPStatus.OK) -> None:
        body = json.dumps(payload, default=str).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the DO2 status prediction UI.")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8000)
    args = parser.parse_args()

    server = ThreadingHTTPServer((args.host, args.port), Do2RequestHandler)
    print(f"DO2 prediction UI running at http://{args.host}:{args.port}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
