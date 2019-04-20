package controllers;

import com.google.gson.Gson;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import main.MapSquare;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
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
    private int mapHeight;
    private int mapWidth;
    private boolean mapSet = false;
    private boolean changedMap = false;
    private File saveLocation = null;
    private Stage stage;
    private final KeyCombination undoComb =
            new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN);
    private final KeyCombination redoComb =
            new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    private final KeyCombination saveComb =
            new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
    private final KeyCombination newMapComb =
            new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
    private action currentAction;

    public void undo() { history.undo(); }

    public void redo() { history.redo(); }

    public void setActionDrawing() {
        currentAction = action.paintTile;
    }

    public void setActionRuler() {
        currentAction = action.ruler;
    }

    public void setUpStage() {
        if(log.isDebugEnabled()) log.debug("Setting stage up");
        // UNDO
        stage.getScene().getAccelerators().put(undoComb,
                this::undo);
        // REDO
        stage.getScene().getAccelerators().put(redoComb,
                this::redo);
        // SAVE MAP
        stage.getScene().getAccelerators().put(saveComb,
                this::saveMap);
        // NEW MAP
        stage.getScene().getAccelerators().put(newMapComb,
                this::popUpNewMapSettings);

    }

    public enum action {
        paintTile,
        ruler,
    }

    class History {
        public static final int historyCap = 50;
        private action[] historyTable = new action[historyCap];
        private String[] arguments = new String[historyCap];
        private int actualPos;
        private int newPos = 0;
        private int size = 0;
        // amount of performed "undo" operations (important for possible redo)
        private int undos = 0;

        public void append(action action, String arguments) {
            //Prevent from multiplying same action
            if(historyTable[actualPos] == action && this.arguments[actualPos].equals(arguments))
                return;
            historyTable[newPos] = action;
            this.arguments[newPos] = arguments;
            actualPos = newPos;
            newPos = (newPos + 1)%historyCap;
            if(size != historyCap)
                size++;
            undos = 0;
            changedMap = true;
            updateTitle();
            if(log.isDebugEnabled())
                log.debug(arguments + '\n' + "size: " + size);
        }

        public void undo() {
            if(size == 0)
                return;
            String[] arguments = this.arguments[actualPos].split(";");
            doAction(actualPos, arguments);
            undos++;
            size--;
            // Java's % operator doesn't work since it returns negative values, here's small workaround
            actualPos = actualPos == 0 ? historyCap - 1 : actualPos - 1;
            newPos = newPos == 0 ? historyCap - 1 : newPos - 1;
            if(log.isDebugEnabled())
                log.debug(Arrays.toString(arguments) + '\n'
                        + "size: " + size + "\nactual pos: " + actualPos + "\nundos: " + undos);
        }

        public void redo() {
            if(undos == 0)
                return;
            String[] arguments = this.arguments[newPos].split(";");
            doAction(newPos, arguments);
            undos--;
            size++;
            actualPos = (actualPos + 1)%historyCap;
            newPos = (newPos + 1)%historyCap;
            if(log.isDebugEnabled())
                log.debug(Arrays.toString(arguments) + '\n'
                        + "size: " + size + "\nactual pos: " + actualPos + "\nundos: " + undos);
        }

        private void doAction(int actionPos, String[] arguments) {
            switch(historyTable[actionPos]) {
                case paintTile:
                    int posX = Integer.parseInt(arguments[0]);
                    int posY = Integer.parseInt(arguments[1]);
                    //We're changing argument for possible undo/redo
                    this.arguments[actionPos] = posX + ";" + posY + ";" + map[posY][posX].getImage();
                    map[posY][posX].setGraphic(new ImageView(cachedImages.get(arguments[2])), arguments[2]);
                    break;
            }
        }
    }

    private History history = new History();

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
    public MenuItem undoBtn;

    @FXML
    public MenuItem redoBtn;

    @FXML
    public ToggleGroup mainToolbar;

    @FXML
    public ToggleButton drawToolButton;

    @FXML
    public void initialize() {
        packageChoiceBox.getSelectionModel()
            .selectedItemProperty()
            .addListener((observableValue, o, t1) -> { changePackage();});
        // Check whether there are packages to be loaded at the beginning
        updatePackages();
        // This listener allows tiles to move with divider
        splitPane.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> {
            if((double)newVal < (double)oldVal) {
                updateTilesLayout(1);
                return;
            }
            if(!packageChosen)
                return;
            if (maxInRow < (int) tilesChooseView.getWidth() / (tileSize + tilesGap)) {
                updateTilesLayout();
            }
        });
        // Set default tool to start with
        drawToolButton.setSelected(true);
        currentAction = action.paintTile;
        // Set few default squares to memory
        Image defaultSquare = new Image(getClass().getResourceAsStream("/images/defaultSquare.png"));
        cachedImages.put("/images/defaultSquare.png", defaultSquare);
        Image missingSquare = new Image(getClass().getResourceAsStream("/images/missingSquare.png"));
        cachedImages.put("/images/missingSquare.png", missingSquare);
    }

    void setStage(Stage stage) { this.stage = stage; }

    // Title should has a '*' whenever there are unsaved changes
    void updateTitle() {
        String name;
        if(saveLocation != null)
            name = saveLocation.getName();
        else
            name = "untitled";
        if(changedMap)
            name += "*";
        stage.setTitle(String.format("%s - Tabletop RPG Battlemap", name));
    }

    void makeNewMap(int width, int height) {
        mapWidth = width;
        mapHeight = height;
        map = new MapSquare[width][height];
        for(int i = 0; i < width; i ++)
            for (int j = 0; j < height; j++) {
                map[i][j] = setUpMapSquare(i, j);
                mapView.getChildren().add(map[i][j]);
            }
    }

    private MapSquare setUpMapSquare(int posY, int posX) {
        MapSquare mapSquare = new MapSquare(posX, posY);
        mapSquare.setGraphic(new ImageView(cachedImages.get("/images/defaultSquare.png")), "/images/defaultSquare.png");
        mapSquare.setLayoutX(posX * (tileSize + 1));
        mapSquare.setLayoutY(posY * (tileSize + 1));
        mapSquare.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getButton() == MouseButton.PRIMARY) {
                if(currentAction == action.paintTile)
                    setMapSquareGraphic(posY, posX, mapSquare);
            }
        });
        mapSquare.setOnMouseDragged(mouseEvent -> {
            if(mouseEvent.isSecondaryButtonDown()) moveCamera(mouseEvent);
        });
        mapSquare.setOnDragDetected(mouseEvent -> {
            mapSquare.startFullDrag();
        });
        mapSquare.setOnMouseDragOver(mouseDragEvent -> {
            if(mouseDragEvent.getButton() == MouseButton.PRIMARY) {
                if(currentAction == action.paintTile)
                    setMapSquareGraphic(posY, posX, mapSquare);
            }
        });
        return mapSquare;
    }

    private void setMapSquareGraphic(int posY, int posX, MapSquare mapSquare) {
        // Do not allow for setting same graphic over and over again
        if(mapSquare.getImage().equals(currentImage))
            return;
        String arguments = posX + ";" + posY + ";" + mapSquare.getImage();
        mapSquare.setGraphic(new ImageView(cachedImages.get(currentImage)), currentImage);
        history.append(action.paintTile, arguments);
    }

    // TODO Maybe this method is not necessary
    private void setMapSquareGraphic(double mousePosX, double mousePosY) {
        int posX, posY;
        posX = (int)mousePosX / (tileSize + tilesGap);
        posY = (int)mousePosY / (tileSize + tilesGap);
        if((posX > mapWidth) || (posY > mapHeight))
            return;
        setMapSquareGraphic(posY, posX, map[posY][posX]);
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

    public void popUpNewMapSettings() {
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
        ErrorPopUpController controller =
                (ErrorPopUpController)popUpNewWindow("errorPopUp.fxml", "Error");
        controller.setErrorMsg(errorMsg);
    }

    // This function sets tiles of chosen package so user can use them
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

    // Special function for "Save as" button which forces FileChooser to open
    public void saveMapAs() {
        saveMap(true);
    }

    public void saveMap() {
        saveMap(false);
    }

    public void saveMap(boolean forceFileChooser) {
        // Check if map is not saved already nor user is forcing fileChooser
        if(saveLocation != null && !forceFileChooser) {
            if(saveLocation.exists()) {
                saveMapToFile(saveLocation);
                return;
            }
        }
        File file = chooseMapFile(true);
        if(file != null)
            saveMapToFile(file);
    }

    public File chooseMapFile(boolean saving) {
        FileChooser fileChooser = new FileChooser();
        File startingDirectory = new File("res/maps");
        boolean result = true;
        if(!startingDirectory.exists()) {
            result = startingDirectory.mkdir();
        }
        if(!result)
            startingDirectory = new File("c:/");
        fileChooser.setInitialDirectory(startingDirectory);
        if(saving)
            fileChooser.setInitialFileName("New map.txt");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showSaveDialog(mapView.getScene().getWindow());
        return file;
    }

    //TODO add some exceptions for missing images or something
    private void saveMapToFile(File file) {
        PrintWriter writer;
        try {
            writer = new PrintWriter(file);
            writer.println(mapHeight + " " + mapWidth);
        } catch (FileNotFoundException e) {
            log.error(e.getStackTrace());
            return;
        }
        for (MapSquare[] squaresLine : map) {
            for (MapSquare square : squaresLine) {
                writer.println(square.getImage());
                if(log.isDebugEnabled()) log.debug(square.getImage());
            }
        }
        saveLocation = file;
        changedMap = false;
        updateTitle();
        writer.close();
    }



}
