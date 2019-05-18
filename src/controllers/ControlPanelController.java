package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import network_interface.PlayerGroup;

import java.util.ArrayList;

public class ControlPanelController extends PopUpController {
    private ArrayList<PlayerGroup> permissionGroups;
    private ObservableList<String> groupsList;
    private PlayerGroup currentGroup;

    @FXML public ListView<String> groupsListView;
    @FXML public TextField groupNameField;
    @FXML public Text groupIdField;
    @FXML public Text errorText;
    @FXML public ToggleButton yesDrawingBtn;
    @FXML public ToggleButton noDrawingBtn;
    @FXML public ToggleButton yesDeletingBtn;
    @FXML public ToggleButton noDeletingBtn;
    @FXML public ToggleButton yesCreatingBtn;
    @FXML public ToggleButton noCreatingBtn;
    @FXML public ToggleButton yesEditingBtn;
    @FXML public ToggleButton noEditingBtn;
    @FXML public ToggleButton yesMovingBtn;
    @FXML public ToggleButton noMovingBtn;
    @FXML public PasswordField passwordField;
    @FXML public Text generalInfoText;

    // TODO add window with question whether user wants to go to another group w/o saving
    public void setPermissionGroups(ArrayList<PlayerGroup> permissionGroups) {
        this.permissionGroups = permissionGroups;
        groupsList = FXCollections.observableArrayList();
        groupsListView.setItems(groupsList);
        for(PlayerGroup group : permissionGroups) {
            groupsList.add(group.getGroupName());
        }
        groupsListView.setOnMouseClicked(mouseEvent -> {
            String chosenName = groupsListView.getSelectionModel().getSelectedItem();
            PlayerGroup chosenGroup = getGroupByName(chosenName);
            currentGroup = chosenGroup;
            if(chosenGroup != null) {
                setGroupParameters(chosenGroup);
            }
        });
        currentGroup = permissionGroups.get(0);
        setGroupParameters(currentGroup);
    }

    private PlayerGroup getGroupByName(String name) {
        for(PlayerGroup group : permissionGroups) {
            if(group.getGroupName().equals(name)) {
                return group;
            }
        }
        return null;
    }

    private boolean isNameOccupied(String name) {
        for(PlayerGroup group : permissionGroups) {
            if(group.getGroupName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private int getFreeGroupId() {
        int freeId = 0;
        for(PlayerGroup group : permissionGroups) {
            int groupId = group.getGroupId();
            if(groupId >= freeId) {
                freeId = groupId + 1;
            }
        }
        return freeId;
    }

    private void setGroupParameters(PlayerGroup group) {
         groupNameField.setText(group.getGroupName());
         groupIdField.setText(Integer.toString(group.getGroupId()));
        setRadioBtn(yesDrawingBtn, noDrawingBtn, group.getPermission(PlayerGroup.drawingPerm));
        setRadioBtn(yesDeletingBtn, noDeletingBtn, group.getPermission(PlayerGroup.deletingPerm));
        setRadioBtn(yesCreatingBtn, noCreatingBtn, group.getPermission(PlayerGroup.creatingPerm));
        setRadioBtn(yesEditingBtn, noEditingBtn, group.getPermission(PlayerGroup.editingPerm));
        setRadioBtn(yesMovingBtn, noMovingBtn, group.getPermission(PlayerGroup.movingPerm));
        errorText.setVisible(false);
    }

    private void setRadioBtn(ToggleButton yesButton, ToggleButton noButton, boolean permissionGranted) {
        if(permissionGranted) {
            yesButton.setSelected(true);
        } else {
            noButton.setSelected(true);
        }
    }

    @FXML
    public void saveGroups() {
        String proposedName = groupNameField.getText();
        // Additional check if name was changed because our current name is of course occupied by our current group
        // So later if would always return true
        boolean nameHasChanged = !currentGroup.getGroupName().equals(proposedName);
        if(proposedName.trim().isEmpty()) {
            errorText.setVisible(true);
            errorText.setText("Name cannot be left empty!");
            return;
        }
        if(isNameOccupied(proposedName) && nameHasChanged) {
            errorText.setVisible(true);
            errorText.setText("Name is already taken");
            return;
        }
        groupsList.remove(currentGroup.getGroupName());
        groupsList.add(proposedName);
        currentGroup.setGroupName(proposedName);
        currentGroup.setPermission(PlayerGroup.drawingPerm, yesDrawingBtn.isSelected());
        currentGroup.setPermission(PlayerGroup.deletingPerm, yesDeletingBtn.isSelected());
        currentGroup.setPermission(PlayerGroup.creatingPerm, yesCreatingBtn.isSelected());
        currentGroup.setPermission(PlayerGroup.editingPerm, yesEditingBtn.isSelected());
        currentGroup.setPermission(PlayerGroup.movingPerm, yesMovingBtn.isSelected());
        errorText.setVisible(false);
    }

    // TODO Think about using StringBuilder or not
    public void createPermissionGroup() {
        String groupName = "New group";
        while(isNameOccupied(groupName)) {
            groupName = groupName + "1";
        }
        int groupId = getFreeGroupId();
        permissionGroups.add(new PlayerGroup(groupId, groupName));
        groupsList.add(groupName);
        groupsListView.getSelectionModel().selectLast();
    }

    public void saveGeneral() {
        parent.changePassword(passwordField.getText());
        generalInfoText.setVisible(true);
        generalInfoText.setText("Saved successfully");
    }

    public void cancel() {
        Stage stage = (Stage) errorText.getScene().getWindow();
        stage.close();
    }
}
