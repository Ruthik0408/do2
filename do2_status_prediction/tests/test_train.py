from do2_model import predict, train, train_multiclass


def test_default_train_and_predict_modules_use_multiclass_implementation():
    assert train.run_training is train_multiclass.run_training
    assert predict.load_model_assets.__defaults__[0].name == "models_multiclass"
