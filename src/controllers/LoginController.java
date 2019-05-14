package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import network_interface.ClientSideSocket;
import network_interface.Server;

import java.io.IOException;

public class LoginController {
    private boolean gmPasswordVisible = false;
    BattlemapController controller;

    @FXML
    public Button offlineButton;

    @FXML
    private Text gmPasswordText;

    @FXML
    public TextField ipField;

    @FXML
    public TextField portField;

    @FXML
    public TextField nameField;

    @FXML
    public PasswordField passwordField;

    @FXML
    private PasswordField gmPasswordField;

    public void toggleGMLogin() {
        gmPasswordVisible = !gmPasswordVisible;
        gmPasswordField.setVisible(gmPasswordVisible);
        gmPasswordText.setVisible(gmPasswordVisible);
    }

    public void startOfflineMode() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/battlemap.fxml"));
            Stage stage = (Stage) offlineButton.getScene().getWindow();
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.setTitle("untitled - Tabletop RPG Battlemap");
            controller = (BattlemapController)loader.getController();
            controller.setStage(stage);
            // Line below initializes actions that need stage to be already created
            // which is not the case for initialize method
            controller.setUpStage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logIn() {
        int port = Integer.parseInt(portField.getText());
        String nickname = nameField.getText();
        Server server = null;
        ClientSideSocket client = null;
        if(gmPasswordVisible) {
            server = new Server(port);
        } else {
            client = new ClientSideSocket(ipField.getText(), port);
        }
        startOfflineMode();
        if(server != null) {
            server.setController(controller);
            while(!server.isControllerSet()) {}
            client = new ClientSideSocket("127.0.0.1", port);
            client.setController(controller);
            controller.setServer(server, client);
            client.requestNickname(nickname);
        } else {
            if(client != null) {
                client.setController(controller);
                controller.setClient(client);
                client.requestNickname(nickname);
            }
        }
    }
}
