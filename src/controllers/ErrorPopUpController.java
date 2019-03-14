package controllers;

import javafx.event.ActionEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class ErrorPopUpController extends popUpController{


    public Text errorText;

    public void setErrorMsg(String errorMsg) {
        errorText.setText(errorMsg);
    }
    public void cancel(ActionEvent actionEvent) {
        Stage stage = (Stage) errorText.getScene().getWindow();
        stage.close();
    }
}
