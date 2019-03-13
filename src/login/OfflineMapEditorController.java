package login;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Camera;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Border;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class OfflineMapEditorController {

    static final int mapWidth = 15;
    static final int mapHeight = 15;
    private Button[][] map;
    private HashMap<String, Image> cachedImages = new HashMap<>();
    private String currentImage = "exampleSquare2.png";

    @FXML
    private AnchorPane mapView;

    public void makeNewMap(int heigth, int width) {
        Image exampleSquare = new Image(getClass().getResourceAsStream("exampleSquare.png"));
        Image exampleSquare2 = new Image(getClass().getResourceAsStream("exampleSquare2.png"));
        cachedImages.put("exampleSquare.png", exampleSquare);
        cachedImages.put("exampleSquare2.png", exampleSquare2);
        int exampleSquareHeight = (int) exampleSquare.getHeight();
        int exampleSquareWidth = (int) exampleSquare.getWidth();
        map = new Button[width][heigth];
        for(int i = 0; i < width; i ++)
            for (int j = 0; j < heigth; j++) {
                map[i][j] = setUpMapSquare(i, j, "exampleSquare.png");
                mapView.getChildren().add(map[i][j]);
            }
    }



    private Button setUpMapSquare(int posX, int posY, String image) {
        Button mapSquare = new Button();
        mapSquare.setStyle("-fx-background-color: transparent; -fx-padding: 5, 5, 5, 5;");
        mapSquare.setGraphic(new ImageView(cachedImages.get(image)));
        mapSquare.setLayoutX(posX * (cachedImages.get(image).getHeight() + 1));
        mapSquare.setLayoutY(posY * (cachedImages.get(image).getWidth() + 1));
        mapSquare.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.isPrimaryButtonDown())
                mapSquare.setGraphic(new ImageView(cachedImages.get(currentImage)));
        });
        mapSquare.setOnMouseDragged(mouseEvent -> moveCamera(mouseEvent));
        return mapSquare;
    }

    public void moveCamera(MouseEvent mouseEvent) {
        System.out.println("CHECK");
        if(!mouseEvent.isSecondaryButtonDown()) return;
        mapView.setTranslateX(mouseEvent.getX());
        mapView.setTranslateY(mouseEvent.getY());
    }

    public void popUpNewMapSettings(ActionEvent actionEvent) {
        Stage newMapSettings = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = null;
        try {
            root = fxmlLoader.load(getClass().getResource("newMapSettings.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        NewMapSettings controller = (NewMapSettings)fxmlLoader.getController();
       // controller.setParent(this);
        newMapSettings.setScene(new Scene(root));
        newMapSettings.show();
    }

    //Just test function
    /*public void makeNewMap(ActionEvent actionEvent) {
        Random random = new Random(System.currentTimeMillis());
        int x = abs(random.nextInt()%400);
        int y = abs(random.nextInt()%400);
        System.out.println("Pos = " + x + " " + y);
        Rectangle rectangle = new Rectangle(x, y, 100, 100);
        rectangle.setFill(Color.BLACK);
        mapView.getChildren().add(rectangle);
    }*/
}
