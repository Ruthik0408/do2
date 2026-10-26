import numpy as np
import pandas as pd
import pytest

from do2_model import predict_multiclass
from do2_model.multiclass import (
    dropped_multiclass_status_report,
    filter_multiclass_status_rows,
    make_multiclass_status_target,
)


class _FakeMulticlassModel:
    def predict_proba(self, frame):
        return np.array(
            [
                [0.05, 0.10, 0.80, 0.05],
                [0.70, 0.10, 0.10, 0.10],
            ]
        )


def test_multiclass_target_keeps_every_nonblank_training_status():
    frame = pd.DataFrame({"status": ["V", " p ", "D", "I", "MR", "C", "", None]})

    filtered = filter_multiclass_status_rows(frame)

    assert filtered["status"].tolist() == ["V", " p ", "D", "I", "MR", "C"]
    assert make_multiclass_status_target(filtered).tolist() == ["V", "P", "D", "I", "MR", "C"]


def test_multiclass_target_rejects_unfiltered_blank_status():
    with pytest.raises(ValueError):
        make_multiclass_status_target(pd.DataFrame({"status": ["V", ""]}))


def test_dropped_multiclass_status_report_marks_filtered_values():
    report = dropped_multiclass_status_report(
        pd.DataFrame({"status": ["V", "D", "PS", "MR", "", None]})
    )

    usage = dict(zip(report["status"], report["used_for_multiclass_training"]))
    assert usage["V"] is True
    assert usage["MR"] is True
    assert usage["D"] is True
    assert usage["PS"] is True
    assert usage["__BLANK__"] is False


def test_predict_multiclass_outputs_predicted_status_and_probabilities(monkeypatch):
    metadata = {
        "feature_columns": ["do2_type"],
        "categorical_columns": ["do2_type"],
        "text_columns": [],
        "classes": ["D", "I", "P", "V"],
        "model_version": "multiclass-test",
    }
    monkeypatch.setattr(
        predict_multiclass,
        "load_model_assets",
        lambda model_dir: (_FakeMulticlassModel(), metadata),
    )

    output = predict_multiclass.predict_frame(
        pd.DataFrame({"do2_type": ["0", "1"], "status": ["V", "D"]})
    )

    assert output["predicted_status"].tolist() == ["P", "D"]
    assert output["predicted_status_probability"].tolist() == [0.8, 0.7]
    assert output["probability_status_V"].tolist() == [0.05, 0.1]
    assert output["actual_status"].tolist() == ["V", "D"]
