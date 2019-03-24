package controllers;

import com.google.gson.Gson;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SplitPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import main.MapSquare;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

//TODO Get rid of useless event parameters
public class OfflineMapEditorController {

    private static final Logger log = Logger.getLogger(OfflineMapEditorController.class);
    static final int tilesGap = 10;
    static final int tileSize = 50;
    static final int tileStartingPosY = 30;
    int maxInRow;
    private boolean packageChosen = false;
    private MapSquare[][] map;
    private ArrayList<Button> tilesMap = new ArrayList<>();
    private HashMap<String, Image> cachedImages = new HashMap<>();
    private String currentImage = "/images/exampleSquare2.png";
    private String selectedPackage;
    private Gson gson = new Gson();

    public void undo() { history.undo(); }

    public void redo() { history.redo(); }

    public enum action {
        paintTile,
    }

    /*  History schemas
        paintTile:
        posX;posY;previousImage
    */
    class History {
        private action[] historyTable = new action[50];
        private String[] arguments = new String[50];
        private int actualPos;
        private int newPos = 0;
        private int size = 0;
        private int undos = 0;

        public void append(action action, String argument) {
            historyTable[newPos] = action;
            arguments[newPos] = argument;
            actualPos = newPos;
            newPos = (newPos + 1)%50;
            if(size != 50)
                size++;
            undos = 0;
        }

        public void undo() {
            if(size == 0)
                return;
            String[] arguments = this.arguments[actualPos].split(";");
            if(log.isDebugEnabled()) {
                for(String string : arguments) {
                    log.debug(string);
                }
            }
            switch(historyTable[actualPos]) {
                case paintTile:
                    int posX = Integer.parseInt(arguments[0]);
                    int posY = Integer.parseInt(arguments[1]);
                    map[posY][posX].setGraphic(new ImageView(cachedImages.get(arguments[2])), arguments[2]);
                    break;
            }
            undos++;
            size--;
            actualPos = (actualPos - 1)%50;
        }

        public void redo() {
            if(undos == 0)
                return;


        }
    }

    History history = new History();

    //Extensions that Image class of javafx can handle
    private String[] tilesExtensions = {"jpg", "jpeg", "png", "bmp", "gif"};

    @FXML
    public AnchorPane tilesChooseView;

    @FXML
    public SplitPane splitPane;

    @FXML
    private AnchorPane mapView;

    @FXML
    private ChoiceBox packageChoiceBox;

    @FXML
    public void initialize() {
        packageChoiceBox.getSelectionModel()
            .selectedItemProperty()
            .addListener((observableValue, o, t1) -> { changePackage();});
        updatePackages();
        splitPane.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> {
            if((double)newVal < (double)oldVal) {
                updateTilesLayout(1);
                return;
            }
            if(!packageChosen)
                return;
            if (maxInRow < (int) tilesChooseView.getWidth() / (tileSize + tilesGap)) {
                if(log.isDebugEnabled()) {
                    log.debug("Max in row = " + maxInRow);
                    log.debug("New max in row = " + (int) (tilesChooseView.getWidth() + tilesGap) / tileSize);
                    log.debug("Width = " + tilesChooseView.getWidth());
                }
                updateTilesLayout();
            }
        });
        KeyCombination undoShortcut = new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN);
        Runnable undoRunnable = this::undo;
    }

    void makeNewMap(int width, int height) {
        Image exampleSquare = new Image(getClass().getResourceAsStream("/images/exampleSquare.png"));
        Image exampleSquare2 = new Image(getClass().getResourceAsStream("/images/exampleSquare2.png"));
        cachedImages.put("/images/exampleSquare.png", exampleSquare);
        cachedImages.put("/images/exampleSquare2.png", exampleSquare2);
        int exampleSquareHeight = (int) exampleSquare.getHeight();
        int exampleSquareWidth = (int) exampleSquare.getWidth();
        map = new MapSquare[width][height];
        for(int i = 0; i < width; i ++)
            for (int j = 0; j < height; j++) {
                map[i][j] = setUpMapSquare(i, j, "/images/exampleSquare.png");
                mapView.getChildren().add(map[i][j]);
            }
    }

    private MapSquare setUpMapSquare(int posY, int posX, String image) {
        MapSquare mapSquare = new MapSquare(posX, posY);
        mapSquare.setGraphic(new ImageView(cachedImages.get(image)), image);
        mapSquare.setLayoutX(posX * (tileSize + 1));
        mapSquare.setLayoutY(posY * (tileSize + 1));
        mapSquare.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getButton() == MouseButton.PRIMARY) {
                String arguments = posX + ";" + posY + ";" + mapSquare.getImage();
                mapSquare.setGraphic(new ImageView(cachedImages.get(currentImage)), currentImage);
                history.append(action.paintTile, arguments);
            }
        });
        mapSquare.setOnMouseDragged(mouseEvent -> moveCamera(mouseEvent));
        return mapSquare;
    }

    private Button setUpTile(String image) {
        Button tile = new Button();
        tile.setStyle("-fx-background-color: transparent; -fx-padding: 5, 5, 5, 5;");
        if(!cachedImages.containsKey(image))
            cachedImages.put(image, new Image(getClass().getResourceAsStream(image)));
        tile.setGraphic(new ImageView(cachedImages.get(image)));
        tile.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getButton() == MouseButton.PRIMARY) {
                currentImage = image;
                uncheckTiles();
                tile.setEffect(new DropShadow());
            }
        });
        return tile;
    }

    private void uncheckTiles() {
        for (Button tile : tilesMap) {
            tile.setEffect(null);
        }
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
    //TODO When current package folder is deleted, there is error
    public void updatePackages() {
        File file = new File("res/packages");
        String[] packages = file.list((current, name) -> new File(current, name).isDirectory());
        if(packages == null) {
            popUpError("Lorem ipsum");
            return;
        }
        ObservableList<String> packagesList = FXCollections.observableArrayList(packages);
        packageChoiceBox.setItems(packagesList);
        clearTilesLayout();
    }

    public void popUpError(String errorMsg) {
        ErrorPopUpController controller = (ErrorPopUpController)popUpNewWindow("errorPopUp.fxml", "Error");
        controller.setErrorMsg(errorMsg);
    }

    // This function sets tiles of choosen package so user can use them
    //TODO Throw if sb deleted res/packages while program works
    public void changePackage() {
        if(packageChoiceBox.getValue() == null)
            return;
        File packagesDir = new File("res/packages/" + packageChoiceBox.getValue());
        FileFilter filter = pathname -> hasProperExtension(pathname.getName());
        File[] tiles = packagesDir.listFiles(filter);

        Button tile;
        clearTilesLayout();
        if(tiles == null)
            return;
        for(int i = 0; i < tiles.length; i++) {
            tile = setUpTile("/packages/" + packageChoiceBox.getValue() + "/" + tiles[i].getName());
            tilesMap.add(tile);
            tilesChooseView.getChildren().add(tile);
        }
        packageChosen = true;
        updateTilesLayout();
    }

    // Function that checks whether file's name include proper extension for graphics
    public boolean hasProperExtension(String name) {
        int dotPos = name.lastIndexOf('.');
        String extension = name.substring(dotPos + 1);
        for(String properExtension : tilesExtensions) {
            if(properExtension.equalsIgnoreCase(extension))
                return true;
        }
        return false;
    }

    public void clearTilesLayout() {
        for(Button tile : tilesMap) tilesChooseView.getChildren().remove(tile);
        tilesMap.clear();
    }

    public void updateTilesLayout() {
        updateTilesLayout(0);
    }

    public void updateTilesLayout(int decreaseRowValue) {
        maxInRow = (int)(tilesChooseView.getWidth() / (tileSize + tilesGap)) - decreaseRowValue;
        int i = 0;
        int j = 0;
        for (Button tile : tilesMap) {
            tile.setLayoutX(i * (tileSize + tilesGap));
            tile.setLayoutY((j + 1) * (tileSize + tilesGap));
            i = (i+1)%maxInRow;
            if(i == 0)
                j++;
        }
    }

    public void saveMap() {
        try {
            gson.toJson(map, new FileWriter("/maps"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
