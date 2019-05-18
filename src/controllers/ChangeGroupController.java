package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import network_interface.Player;
import network_interface.PlayerGroup;

import java.util.ArrayList;

public class ChangeGroupController extends PopUpController {
    private Player player;
    private ArrayList<PlayerGroup> playerGroups;
    private PlayerGroup selectedGroup;

    @FXML public Text playerInfoText;
    @FXML public Text actualGroupText;
    @FXML public Text drawingPermText;
    @FXML public Text deletingPermText;
    @FXML public Text creatingPermText;
    @FXML public Text editingPermText;
    @FXML public Text movingPermText;
    @FXML public ChoiceBox<String> groupsChoiceBox;

    public void setStartingParameters(Player player, ArrayList<PlayerGroup> playerGroups) {
        this.player = player;
        this.playerGroups = playerGroups;
        ObservableList<String> observableList = FXCollections.observableArrayList();
        for(PlayerGroup group : playerGroups) {
            observableList.add(group.getGroupName());
        }
        groupsChoiceBox.setItems(observableList);
        groupsChoiceBox.setOnAction(actionEvent -> {
            PlayerGroup group = getGroup(groupsChoiceBox.getValue());
            if(group != null) {
                setGroupParameters(group);
                selectedGroup = group;
            }
        });
        PlayerGroup playerGroup = getGroup(player.getPermissionGroup());
        if(playerGroup != null) {
            groupsChoiceBox.getSelectionModel().select(playerGroup.getGroupName());
            actualGroupText.setText(playerGroup.getGroupName());
            setGroupParameters(playerGroup);
        }
        playerInfoText.setText(player.getNickname());
    }

    @FXML
    public void accept() {
        if(selectedGroup != null) {
            player.setPermissionGroup(selectedGroup.getGroupId());
        } else {
            parent.popUpError("There was problem assigning group to player (Group doesn't exist)");
        }
        Stage stage = (Stage) playerInfoText.getScene().getWindow();
        stage.close();
    }

    private void setGroupParameters(PlayerGroup group) {
        setPermissionText(drawingPermText, group.getPermission(PlayerGroup.drawingPerm));
        setPermissionText(deletingPermText, group.getPermission(PlayerGroup.deletingPerm));
        setPermissionText(creatingPermText, group.getPermission(PlayerGroup.creatingPerm));
        setPermissionText(editingPermText, group.getPermission(PlayerGroup.editingPerm));
        setPermissionText(movingPermText, group.getPermission(PlayerGroup.movingPerm));
    }

    private void setPermissionText(Text text, boolean permissionGranted) {
        if(permissionGranted) {
            text.setFill(Color.GREEN);
            text.setText("YES");
        } else {
            text.setFill(Color.RED);
            text.setText("NO");
        }
    }

    private PlayerGroup getGroup(String groupName) {
        for(PlayerGroup group : playerGroups) {
            if(group.getGroupName().equals(groupName)) {
                return group;
            }
        }
        return null;
    }

    private PlayerGroup getGroup(int groupId) {
        for(PlayerGroup group : playerGroups) {
            if(group.getGroupId() == groupId) {
                return group;
            }
        }
        return null;
    }
}
