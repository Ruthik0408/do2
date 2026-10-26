import numpy as np
import pandas as pd

from do2_model import predict, ui


class _FakeModel:
    def predict_proba(self, frame):
        return np.array([[0.8, 0.2, 0.0], [0.1, 0.3, 0.6]])


class _FakeModel4:
    def predict_proba(self, frame):
        return np.array([[0.8, 0.1, 0.0, 0.1], [0.1, 0.2, 0.6, 0.1]])


def test_predict_frame_outputs_multiclass_status_and_stable_feature_order(monkeypatch):
    metadata = {
        "feature_columns": ["do2_type"],
        "categorical_columns": ["do2_type"],
        "text_columns": [],
        "classes": ["V", "I", "MR"],
        "model_version": "test-version",
    }
    monkeypatch.setattr(predict, "load_model_assets", lambda model_dir=None: (_FakeModel(), metadata))

    output = predict.predict_frame(
        pd.DataFrame({"extra": ["ignored", "ignored"], "do2_type": ["new", "old"], "status": ["V", "I"]})
    )

    assert output["predicted_status"].tolist() == ["V", "MR"]
    assert output["predicted_status_probability"].tolist() == [0.8, 0.6]
    assert output["prediction_layer"].tolist() == ["layer2_ml", "layer2_ml"]
    assert output["layer2_ml_status"].tolist() == ["V", "MR"]
    assert output["probability_status_V"].tolist() == [0.8, 0.1]
    assert output["probability_status_MR"].tolist() == [0.0, 0.6]
    assert output["model_version"].tolist() == ["test-version", "test-version"]


def test_predict_frame_keeps_employee_snapshot_rule_as_layer2_feature(monkeypatch):
    metadata = {
        "feature_columns": ["do2_type", "n_from_date_after_fs_due_date"],
        "categorical_columns": ["do2_type"],
        "text_columns": [],
        "classes": ["V", "I", "MR", "N"],
        "model_version": "test-version",
    }
    monkeypatch.setattr(predict, "load_model_assets", lambda model_dir=None: (_FakeModel4(), metadata))

    output = predict.predict_frame(
        pd.DataFrame(
            {
                "do2_type": ["new", "old"],
                "from_date": ["2024-02-01", "2024-01-01"],
                "employee_fs_due_date": ["2024-01-31", "2025-01-01"],
                "employee_status_code": [1, 1],
            }
        )
    )

    assert output["layer2_ml_status"].tolist() == ["V", "MR"]
    assert output["predicted_status"].tolist() == ["V", "MR"]
    assert output["prediction_layer"].tolist() == ["layer2_ml", "layer2_ml"]
    assert output["layer1_reason"].tolist() == ["", ""]


def test_predict_frame_keeps_appendix_j_signal_as_layer2_feature(monkeypatch):
    metadata = {
        "feature_columns": ["do2_type", "i_appendix_required_from_date_missing"],
        "categorical_columns": ["do2_type"],
        "text_columns": [],
        "classes": ["V", "I", "MR", "N"],
        "model_version": "test-version",
    }
    monkeypatch.setattr(predict, "load_model_assets", lambda model_dir=None: (_FakeModel4(), metadata))

    output = predict.predict_frame(
        pd.DataFrame(
            {
                "do2_type": ["new", "old"],
                "i_appendix_required_from_date_missing": [1, 0],
            }
        )
    )

    assert output["layer2_ml_status"].tolist() == ["V", "MR"]
    assert output["predicted_status"].tolist() == ["V", "MR"]
    assert output["prediction_layer"].tolist() == ["layer2_ml", "layer2_ml"]
    assert output["layer1_reason"].tolist() == ["", ""]


def test_predict_frame_uses_family_identity_layer1_rules(monkeypatch):
    metadata = {
        "feature_columns": ["do2_type", "i_identity_uid_conflict", "i_biomet_uid_invalid"],
        "categorical_columns": ["do2_type"],
        "text_columns": [],
        "classes": ["V", "I", "MR", "N"],
        "model_version": "test-version",
    }
    monkeypatch.setattr(predict, "load_model_assets", lambda model_dir=None: (_FakeModel4(), metadata))

    output = predict.predict_frame(
        pd.DataFrame(
            {
                "do2_type": ["new", "old"],
                "description": ["UID", "BIOMET"],
                "column_a": ["SELF", "SELF"],
                "column_b": ["NAME", "NAME"],
                "column_c": ["123456789012", "ABC"],
                "i_employee_uid_duplicate_count": [1, 0],
            }
        )
    )

    assert output["layer2_ml_status"].tolist() == ["V", "MR"]
    assert output["predicted_status"].tolist() == ["I", "I"]
    assert output["prediction_layer"].tolist() == ["layer1_rules", "layer1_rules"]
    assert output.loc[0, "layer1_reason"] == "UID/Aadhaar already exists for another employee or relation."
    assert output.loc[1, "layer1_reason"] == "BIOMET UID must be a 12 digit number."


def test_prediction_ui_reads_all_chunks_in_date_range(monkeypatch):
    chunks = [
        pd.DataFrame({"id": [1], "do2_type": ["new"], "status": ["V"]}),
        pd.DataFrame({"id": [2], "do2_type": ["old"], "status": ["I"]}),
    ]

    class _FakeEngine:
        def dispose(self):
            pass

    def fake_iter_date_range(engine, settings, start_date, end_date, *, require_status):
        assert start_date == "2025-01-01"
        assert end_date == "2025-01-02"
        assert require_status is False
        yield from chunks

    metadata = {
        "feature_columns": ["do2_type"],
        "categorical_columns": ["do2_type"],
        "text_columns": [],
        "classes": ["V", "I", "MR"],
    }
    monkeypatch.setattr(ui, "get_engine", lambda settings: _FakeEngine())
    monkeypatch.setattr(ui, "iter_date_range", fake_iter_date_range)
    monkeypatch.setattr(predict, "load_model_assets", lambda model_dir=None: (_FakeModel(), metadata))
    monkeypatch.setattr(ui, "predict_frame", predict.predict_frame)

    output = ui.prediction_records("2025-01-01", "2025-01-02")

    assert output["metrics"]["rows"] == 2
    assert output["metrics"]["predicted_status_classes"] == 2
    assert output["metrics"]["predicted_v"] == 1
    assert output["rows"][0]["id"] == 1
    assert output["rows"][1]["id"] == 2
