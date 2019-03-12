package login;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ToggleButton;
import javafx.scene.text.Text;

public class Controller {
    @FXML
    private ToggleButton toggleGM;

    @FXML
    private Text gmPasswordText;

    @FXML
    private PasswordField gmPasswordField;

    public void test(javafx.event.ActionEvent actionEvent) {
        gmPasswordField.setVisible(true);
    }
}
