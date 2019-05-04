package controllers;

import javafx.scene.text.Text;
import javafx.stage.Stage;

public class ErrorPopUpController extends PopUpController {


    public Text errorText;

    public void setErrorMsg(String errorMsg) {
        errorText.setText(errorMsg);
    }
    public void cancel() {
        Stage stage = (Stage) errorText.getScene().getWindow();
        stage.close();
    }
}
