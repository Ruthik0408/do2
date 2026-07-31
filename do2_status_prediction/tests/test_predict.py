import numpy as np
import pandas as pd

from do2_model import predict


class _FakeModel:
    def predict_proba(self, frame):
        return np.array([[0.8, 0.2], [0.1, 0.9]])


def test_predict_frame_reuses_saved_threshold_and_stable_feature_order(monkeypatch):
    metadata = {
        "feature_columns": ["do2_type"],
        "categorical_columns": ["do2_type"],
        "selected_probability_non_valid_threshold": 0.5,
        "threshold_policy": "max_f1",
        "model_version": "test-version",
    }
    monkeypatch.setattr(predict, "load_model_assets", lambda: (_FakeModel(), metadata))

    output = predict.predict_frame(
        pd.DataFrame({"extra": ["ignored", "ignored"], "do2_type": ["new", "old"], "status": ["V", "I"]})
    )

    assert output["non_valid_probability"].tolist() == [0.2, 0.9]
    assert output["predicted_is_non_valid"].tolist() == [0, 1]
    assert output["predicted_status_group"].tolist() == ["V", "NON_VALID"]
    assert output["threshold_used"].tolist() == [0.5, 0.5]
    assert output["model_version"].tolist() == ["test-version", "test-version"]
