package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;

public class NewZipPackageController extends PopUpController {

    @FXML
    public Text errorText;
    @FXML
    public Button chooseDirectoryButton;
    @FXML
    public TextField pathField;
    @FXML
    public ComboBox packageTypeComboBox;

    @FXML
    public void initialize() {
        ObservableList<String> list = FXCollections.observableArrayList("tiles", "characters");
        packageTypeComboBox.setItems(list);
        packageTypeComboBox.getSelectionModel().selectFirst();
    }

    public void cancel() {
        Stage stage = (Stage)chooseDirectoryButton.getScene().getWindow();
        stage.close();
    }

    public void loadPackageFromZip(ActionEvent actionEvent) {
        String source = pathField.getText();
        String resPath = "res/packages/" + packageTypeComboBox.getValue();
        System.out.println(resPath);

        try {
            ZipFile zipFile = new ZipFile(source);
            if (zipFile.isEncrypted()) {
                throw new ZipException("File is encrypted");
            }
            zipFile.extractAll(resPath);
            cancel();
        } catch (ZipException e) {
            e.printStackTrace();
            errorText.setText(e.getMessage());
            errorText.setVisible(true);
        }
        parent.updatePackages();
    }

    public void chooseDirectory() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose zip file with resources");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Zip package", "*.zip"));
        File selectedFile = fileChooser.showOpenDialog(chooseDirectoryButton.getScene().getWindow());
        pathField.setText(selectedFile.getAbsolutePath());
    }

}
