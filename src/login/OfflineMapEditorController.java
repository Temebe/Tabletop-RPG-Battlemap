package login;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class OfflineMapEditorController {

    @FXML
    private AnchorPane mapView;

    public void makeNewMap(ActionEvent actionEvent) {
        Rectangle rectangle = new Rectangle(100, 100, Color.BLACK);
        mapView.getChildren().add(rectangle);
    }
}
