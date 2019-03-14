package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

//TODO Get rid of useless event parameters
public class OfflineMapEditorController {

    static final int mapWidth = 15;
    static final int mapHeight = 15;
    private Button[][] map;
    private HashMap<String, Image> cachedImages = new HashMap<>();
    private String currentImage = "/images/exampleSquare2.png";
    private String selectedPackage;

    @FXML
    private AnchorPane mapView;

    @FXML
    private ChoiceBox packageChoiceBox;

    void makeNewMap(int width, int height) {
        Image exampleSquare = new Image(getClass().getResourceAsStream("/images/exampleSquare.png"));
        Image exampleSquare2 = new Image(getClass().getResourceAsStream("/images/exampleSquare2.png"));
        cachedImages.put("/images/exampleSquare.png", exampleSquare);
        cachedImages.put("/images/exampleSquare2.png", exampleSquare2);
        int exampleSquareHeight = (int) exampleSquare.getHeight();
        int exampleSquareWidth = (int) exampleSquare.getWidth();
        map = new Button[width][height];
        for(int i = 0; i < width; i ++)
            for (int j = 0; j < height; j++) {
                map[i][j] = setUpMapSquare(i, j, "/images/exampleSquare.png");
                mapView.getChildren().add(map[i][j]);
            }
    }

    private Button setUpMapSquare(int posY, int posX, String image) {
        Button mapSquare = new Button();
        mapSquare.setStyle("-fx-background-color: transparent; -fx-padding: 5, 5, 5, 5;");
        mapSquare.setGraphic(new ImageView(cachedImages.get(image)));
        mapSquare.setLayoutX(posX * (cachedImages.get(image).getWidth() + 1));
        mapSquare.setLayoutY(posY * (cachedImages.get(image).getHeight() + 1));
        mapSquare.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getButton() == MouseButton.PRIMARY)
                mapSquare.setGraphic(new ImageView(cachedImages.get(currentImage)));
        });
        mapSquare.setOnMouseDragged(mouseEvent -> moveCamera(mouseEvent));
        return mapSquare;
    }

    public void moveCamera(MouseEvent mouseEvent) {
        if(!mouseEvent.isSecondaryButtonDown()) return;
        mapView.setTranslateX(mouseEvent.getX());
        mapView.setTranslateY(mouseEvent.getY());
    }

    public void popUpNewMapSettings(ActionEvent actionEvent) {
        popUpNewWindow("newMapSettings.fxml");
    }

    public void loadZipPackageWindow(ActionEvent actionEvent) {
        popUpNewWindow("newZipPackageWindow.fxml");
    }

    private popUpController popUpNewWindow(String windowName) {
        return popUpNewWindow(windowName, "");
    }

    private popUpController popUpNewWindow(String windowName, String title) {
        Stage newWindow = new Stage();
        newWindow.setTitle(title);
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = null;
        try {
            root = (Parent) fxmlLoader.load(getClass().getResource("/fxml/" + windowName).openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        popUpController controller = fxmlLoader.getController();
        controller.setParent(this);
        newWindow.setScene(new Scene(root));
        newWindow.show();
        return controller;
    }


    //TODO Make sure /res/packages is always created
    //TODO Make this String[] -> observable list -> items prettier
    public void updatePackages() {
        File file = new File("res/packages");
        String[] packages = file.list((current, name) -> new File(current, name).isDirectory());
        if(packages == null) {
            popUpError("Lorem ipsum");
            return;
        }
        System.out.println(Arrays.toString(packages));
        ObservableList<String> packagesList = FXCollections.observableArrayList(packages);
        packageChoiceBox.setItems(packagesList);
    }

    public void popUpError(String errorMsg) {
        ErrorPopUpController controller = (ErrorPopUpController)popUpNewWindow("errorPopUp.fxml", "Error");
        controller.setErrorMsg(errorMsg);
    }

    public void changePackage(MouseEvent mouseEvent) {
        System.out.println(packageChoiceBox.getValue());
    }
}
