package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import network_interface.Player;

public class KickController extends PopUpController {

    @FXML
    public TextField reasonField;

    @FXML
    public Button kickButton;

    private String nickname = null;

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void kick() {
        Player player = parent.getPlayer(nickname);
        if(player != null) {
            player.getSocket().kick(reasonField.getText());
        }
        cancel();
    }

    public void cancel() {
        Stage stage = (Stage) kickButton.getScene().getWindow();
        stage.close();
    }
}
