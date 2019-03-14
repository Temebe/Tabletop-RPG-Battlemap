package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class NewMapSettings {

    @FXML
    public AnchorPane createMapDialogue;
    @FXML
    public Button cancelButton;
    @FXML
    public TextField heightValue;
    @FXML
    public TextField widthValue;

    OfflineMapEditorController parent;


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

    public void cancel(ActionEvent actionEvent) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    public void setParent(OfflineMapEditorController parent) {
        this.parent = parent;
    }
}
