package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import network_interface.Player;

public class BanController extends PopUpController {

    @FXML
    public TextField reasonField;

    @FXML
    public Button banButton;

    private String nickname = null;

    void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void ban() {
        Player player = parent.getPlayer(nickname);
        if(player != null) {
            player.getSocket().ban(reasonField.getText());
        }
        cancel();
    }

    public void cancel() {
        Stage stage = (Stage) banButton.getScene().getWindow();
        stage.close();
    }
}
