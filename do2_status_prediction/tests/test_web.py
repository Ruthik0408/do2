import pandas as pd

from do2_model import web


class _FakeEngine:
    def __init__(self):
        self.disposed = False

    def dispose(self):
        self.disposed = True


def test_prediction_payload_compares_actual_and_predicted_status(monkeypatch):
    engine = _FakeEngine()

    def fake_iter_date_range(
        received_engine,
        settings,
        start_date,
        end_date,
        *,
        require_status,
        chunk_size,
    ):
        assert received_engine is engine
        assert start_date == "2025-01-01"
        assert end_date == "2025-02-01"
        assert require_status is True
        assert chunk_size > 2
        yield pd.DataFrame(
            {
                "id": [1, 2, 3],
                "do2_date": ["2025-01-05", "2025-01-06", "2025-01-07"],
                "do2_type": ["A", "B", "C"],
                "status": ["V", "I", "V"],
            }
        )

    def fake_predict_frame(frame):
        assert frame["id"].tolist() == [1, 2, 3]
        return pd.DataFrame(
            {
                "id": [1, 2, 3],
                "actual_status": ["V", "I", "V"],
                "actual_status_group": ["V", "NON_VALID", "V"],
                "predicted_status_group": ["V", "V", "NON_VALID"],
                "non_valid_probability": [0.1, 0.4, 0.8],
                "threshold_used": [0.5, 0.5, 0.5],
                "recommended_action": [
                    "AUTO_VALID_CANDIDATE",
                    "AUTO_VALID_CANDIDATE",
                    "HUMAN_REVIEW",
                ],
            }
        )

    monkeypatch.setattr(web, "get_engine", lambda settings: engine)
    monkeypatch.setattr(web, "iter_date_range", fake_iter_date_range)
    monkeypatch.setattr(web, "predict_frame", fake_predict_frame)

    payload = web.prediction_payload("2025-01-01", "2025-02-01")

    assert engine.disposed is True
    assert payload["summary"]["rows_returned"] == 3
    assert payload["summary"]["matches"] == 1
    assert payload["summary"]["mismatches"] == 2
    assert payload["summary"]["actual_non_valid"] == 1
    assert payload["summary"]["predicted_v"] == 2
    assert payload["rows"][0]["actual_status_group"] == "V"
    assert payload["rows"][1]["matches_actual"] is False
