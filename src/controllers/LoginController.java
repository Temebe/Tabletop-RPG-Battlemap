package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import network_interface.ClientSideSocket;
import network_interface.Server;

import java.io.IOException;

public class LoginController {
    private boolean gmPasswordVisible = false;
    private BattlemapController controller;

    @FXML
    public Button offlineButton;

    @FXML
    public Button logInButton;

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

    @FXML
    public Text infoText;

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
        logInButton.setDisable(true);
        infoText.setVisible(true);
        infoText.setText("Connecting...");
        infoText.setFill(Color.BLACK);
        int port = Integer.parseInt(portField.getText());
        String nickname = nameField.getText();
        Server server = null;
        ClientSideSocket client = null;
        if(gmPasswordVisible) {
            server = new Server(port);
            startOfflineMode();
            server.setController(controller);
            controller.changePassword(passwordField.getText());
            //noinspection StatementWithEmptyBody
            while(!server.isControllerSet()) {}
            client = new ClientSideSocket("127.0.0.1", port, passwordField.getText());
            client.setController(controller);
            controller.setServer(server, client);
            client.requestNickname(nickname);
            return;
        }
        client = new ClientSideSocket(ipField.getText(), port, passwordField.getText());
        if(!tryToConnect(client)) {
            logInButton.setDisable(false);
            return;
        }
        startOfflineMode();
        client.setController(controller);
        controller.setClient(client);
        client.requestNickname(nickname);
    }

    private boolean tryToConnect(ClientSideSocket client) {
        long start = System.currentTimeMillis();
        while(System.currentTimeMillis() - start < 3000) {
            try {
                if (client.getSocket().isConnected() || client.isUnknownHost() || client.isOtherError()) {
                    break;
                }
            } catch (NullPointerException e) {
                setErrorMessage("Unable to connect to the server!");
                return false;
            }
        }
        if(!client.getSocket().isConnected() || client.isUnknownHost() || client.isOtherError()) {
            setErrorMessage("Unable to connect to the server!");
            return false;
        }
        infoText.setText("Connected, checking password...");
        start = System.currentTimeMillis();
        while(System.currentTimeMillis() - start < 3000 && client.isWaitingForAcceptance()) {
            System.out.println(System.currentTimeMillis() - start);
        }
        if(client.isWaitingForAcceptance()) {
            client.close();
            setErrorMessage("Connected but waited too long for response");
            return false;
        } else if(client.isAccessDenied()) {
            client.close();
            setErrorMessage("Wrong password");
            return false;
        } else if(client.isBanned()) {
            client.close();
            setErrorMessage("You are banned from this server!");
            return false;
        }
        return true;
    }

    private void setErrorMessage(String msg) {
        infoText.setFill(Color.RED);
        infoText.setText(msg);
    }
}
