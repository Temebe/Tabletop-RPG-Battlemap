package main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ToggleButton;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    private boolean gmPasswordVisible = false;

    @FXML
    public Button offlineButton;

    @FXML
    private ToggleButton toggleGM;

    @FXML
    private Text gmPasswordText;

    @FXML
    private PasswordField gmPasswordField;

    public void toggleGMLogin(javafx.event.ActionEvent actionEvent) {
        gmPasswordVisible = !gmPasswordVisible;
        gmPasswordField.setVisible(gmPasswordVisible);
        gmPasswordText.setVisible(gmPasswordVisible);
    }

    public void startOfflineMode(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("offlineMapEditor.fxml"));
            Stage stage = (Stage) offlineButton.getScene().getWindow();
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
