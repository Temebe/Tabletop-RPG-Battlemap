package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintStream;

public class Main extends Application {

    private Stage primaryStage;
    private static final Logger log = Logger.getLogger(Main.class);


    @Override
    public void start(Stage primaryStage) throws Exception{
        System.setOut(new PrintStream(System.out, true, "utf-8"));
        this.primaryStage = primaryStage;
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = fxmlLoader.load(getClass().getResource("/fxml/login.fxml"));
        primaryStage.setTitle("Tabletop RPG Battlemap");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
        log.info("Test of log4j");
    }

    public void changeScene(String fxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        primaryStage.getScene().setRoot(root);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
