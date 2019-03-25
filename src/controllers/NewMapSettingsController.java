package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class NewMapSettingsController extends popUpController{

    @FXML
    public AnchorPane createMapDialogue;
    @FXML
    public Button cancelButton;
    @FXML
    public TextField heightValue;
    @FXML
    public TextField widthValue;

    public void createMap(ActionEvent actionEvent) {
        if(!heightValue.getText().matches("[0-9]*"))
            return;
        if(!widthValue.getText().matches("[0-9]*"))
            return;
        int height = Integer.parseInt(heightValue.getText());
        int width = Integer.parseInt(widthValue.getText());
        parent.makeNewMap(height, width);
        cancel(actionEvent);
    }

    @FXML
    public void initialize() {
        //default values for height and width
        heightValue.setText("10");
        widthValue.setText("10");
    }

    public void cancel(ActionEvent actionEvent) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
