package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import main.CharacterSquare;
import shapes.StatusBar;

public class CharacterSettingsController extends PopUpController {

    @FXML
    public Button okButton;

    @FXML
    public Button cancelButton;

    @FXML
    public ImageView characterImage;

    @FXML
    public TextField nameField;

    @FXML
    public TextField firstCurrentValue;

    @FXML
    public TextField firstMaxValue;

    @FXML
    public ColorPicker firstColorPicker;

    @FXML
    public TextField secondCurrentValue;

    @FXML
    public TextField secondMaxValue;

    @FXML
    public ColorPicker secondColorPicker;

    @FXML
    public TextField thirdCurrentValue;

    @FXML
    public TextField thirdMaxValue;

    @FXML
    public ColorPicker thirdColorPicker;

    private CharacterSquare character;
    private StatusBar firstBar;
    private StatusBar secondBar;
    private StatusBar thirdBar;

    // Not sure if there isn't better way to do this
    public void setStartingValues(CharacterSquare character) {
        this.character = character;
        characterImage.setImage(new Image(character.getImagePath()));
        nameField.setText(character.getName());
        this.firstBar = character.getBar(CharacterSquare.barType.first);
        this.secondBar = character.getBar(CharacterSquare.barType.second);
        this.thirdBar = character.getBar(CharacterSquare.barType.third);
        if(firstBar != null) {
            firstCurrentValue.setText(Double.toString(firstBar.getAmount()));
            firstMaxValue.setText(Double.toString(firstBar.getMaxAmount()));
            firstColorPicker.setValue(Color.valueOf(firstBar.getColor()));
        }
        if(secondBar != null) {
            secondCurrentValue.setText(Double.toString(secondBar.getAmount()));
            secondMaxValue.setText(Double.toString(secondBar.getMaxAmount()));
            secondColorPicker.setValue(Color.valueOf(secondBar.getColor()));
        }
        if(thirdBar != null) {
            thirdCurrentValue.setText(Double.toString(thirdBar.getAmount()));
            thirdMaxValue.setText(Double.toString(thirdBar.getMaxAmount()));
            thirdColorPicker.setValue(Color.valueOf(thirdBar.getColor()));
        }
    }

    public void accept() {
        character.setName(nameField.getText());
        checkChanges(firstBar, CharacterSquare.barType.first, firstCurrentValue, firstMaxValue, firstColorPicker);
        checkChanges(secondBar, CharacterSquare.barType.second, secondCurrentValue, secondMaxValue, secondColorPicker);
        checkChanges(thirdBar, CharacterSquare.barType.third, thirdCurrentValue, thirdMaxValue, thirdColorPicker);
        character.setBarsOnStage();
        cancel();
    }

    // Function that checks changes to apply to bars of character
    //TODO fight these ladders
    private void checkChanges(StatusBar statusBar, CharacterSquare.barType type, TextField currentValueField,
                              TextField maxValueField, ColorPicker colorPicker) {
        if(statusBar == null) {
            if(!currentValueField.getText().trim().isEmpty() && !maxValueField.getText().trim().isEmpty()) {
                StatusBar newBar = new StatusBar(Double.valueOf(maxValueField.getText()), character.getSize(),
                        colorPicker.getValue());
                newBar.setAmount(Double.valueOf(currentValueField.getText()));
                character.setBar(newBar, type);
            }
        }
        else {
            if(!currentValueField.getText().trim().isEmpty() && !maxValueField.getText().trim().isEmpty()) {
                double currentValue = Double.valueOf(currentValueField.getText());
                double maxValue = Double.valueOf(maxValueField.getText());
                statusBar.setMaxAmount(maxValue);
                statusBar.setAmount(currentValue);
                statusBar.setColor(colorPicker.getValue());
            }
            else {
                if(currentValueField.getText().trim().isEmpty() && maxValueField.getText().trim().isEmpty()) {
                    character.setBar(null, type);
                }
            }
        }
    }

    public void cancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
