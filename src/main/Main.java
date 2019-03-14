package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    private Stage primaryStage;


    @Override
    public void start(Stage primaryStage) throws Exception{
        this.primaryStage = primaryStage;
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = fxmlLoader.load(getClass().getResource("res/fxml/login.fxml"));
        primaryStage.setTitle("Tabletop RPG Battlemap");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public void changeScene(String fxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        primaryStage.getScene().setRoot(root);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
