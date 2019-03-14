package main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;

public class OfflineMapEditorController {

    static final int mapWidth = 15;
    static final int mapHeight = 15;
    private Button[][] map;
    private HashMap<String, Image> cachedImages = new HashMap<>();
    private String currentImage = "images/exampleSquare2.png";

    @FXML
    private AnchorPane mapView;

    public void makeNewMap(int width, int heigth) {
        Image exampleSquare = new Image(getClass().getResourceAsStream("res/images/exampleSquare.png"));
        Image exampleSquare2 = new Image(getClass().getResourceAsStream("res/images/exampleSquare2.png"));
        cachedImages.put("res/images/exampleSquare.png", exampleSquare);
        cachedImages.put("res/images/exampleSquare2.png", exampleSquare2);
        int exampleSquareHeight = (int) exampleSquare.getHeight();
        int exampleSquareWidth = (int) exampleSquare.getWidth();
        map = new Button[width][heigth];
        for(int i = 0; i < width; i ++)
            for (int j = 0; j < heigth; j++) {
                map[i][j] = setUpMapSquare(i, j, "res/images/exampleSquare.png");
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
        Stage newMapSettings = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = null;
        try {
            root = (Parent) fxmlLoader.load(getClass().getResource("newMapSettings.fxml").openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        NewMapSettings controller = (NewMapSettings)fxmlLoader.getController();
        controller.setParent(this);
        newMapSettings.setScene(new Scene(root));
        newMapSettings.show();
    }

}
