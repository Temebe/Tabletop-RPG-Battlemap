package controllers;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import main.*;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.jmx.gui.Client;
import shapes.Arrow;
import shapes.StatusBar;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

//TODO Get rid of useless event parameters
//TODO Make two layers of firstLayer
public class OfflineMapEditorController {

    private static final Logger log = Logger.getLogger(OfflineMapEditorController.class);
    static final int tilesGap = 10;
    public static final int tileSize = 50;
    static final int tileStartingPosY = 30;
    int maxInRow;
    private boolean packageChosen = false;
    private MapSquare[][] firstLayer;
    private MapSquare[][] secondLayer;
    private ArrayList<Button> tilesList = new ArrayList<>();
    private ArrayList<Button> charactersTilesList = new ArrayList<>();
    private ArrayList<CharacterSquare> charactersList = new ArrayList<>();
    private HashMap<String, Image> cachedImages = new HashMap<>();
    private String currentTilePath = "/images/exampleSquare2.png";
    private String currentCharacterPath = "/images/exampleSquare2.png";
    private String selectedPackage;
    private Gson gson = new Gson();
    private int mapHeight;
    private int mapWidth;
    private boolean mapSet = false;
    private boolean changedMap = false;
    private boolean secondLayerVisible = true;
    private boolean characterTileChosen = false;
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
    private Arrow arrow;
    // Variable which remembers where arrow started
    private MapSquare arrowBegin = null;
    private MapSquare arrowEnd = null;
    private String chat = "";
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    // Client side
    private ClientSideSocket client = null;
    private int PID;

    // Server side
    private Server server = null;
    private ArrayList<Player> playersList;
    private int freePID = 0; // PID that haven't been granted yet

    public enum packageType { characters, tiles }

    public void undo() { history.undo(); }

    public void redo() { history.redo(); }

    public void setActionDrawing() {
        currentAction = action.paintTile;
    }

    public void setActionRuler() {
        currentAction = action.ruler;
    }

    public void setActionStandard() {
        currentAction = action.standard;
        disableLayers(false, false);
    }

    public void setActionErase() {
        currentAction = action.erase;
    }

    // This method is for actions that needs to be made at the very beginning of stage's presence
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
        standard,
        paintTile,
        ruler,
        erase,
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
                    int layer = Integer.parseInt(arguments[3]);
                    MapSquare square = layer == 1 ? firstLayer[posY][posX] : secondLayer[posY][posX];
                    //We're changing argument for possible undo/redo
                    this.arguments[actionPos] =
                            posX + ";"
                            + posY + ";"
                            + firstLayer[posY][posX].getImagePath() + ";"
                            + layer;
                    square.setGraphic(new ImageView(cachedImages.get(arguments[2])), arguments[2]);
                    break;
            }
        }
    }

    class DragDelta {
        public double x;
        public double y;
    }

    private History history = new History();

    //Extensions that Image class of javafx can handle
    private String[] tilesExtensions = {"jpg", "jpeg", "png", "bmp", "gif"};

    @FXML
    public AnchorPane charactersChooseView;

    @FXML
    public AnchorPane tilesChooseView;

    @FXML
    public SplitPane splitPane;

    @FXML
    private AnchorPane mapView;

    @FXML
    private ChoiceBox tilesPgChoiceBox;

    @FXML
    public ChoiceBox charactersPgChoiceBox;

    @FXML
    public MenuItem undoBtn;

    @FXML
    public MenuItem redoBtn;

    @FXML
    public ToggleGroup mainToolbar;

    @FXML
    public ToggleButton drawToolButton;

    @FXML
    public ToggleButton secondLayerTglBtn;

    @FXML
    public ToggleButton characterVisibilityButton;

    @FXML
    public Label leftStatusLabel;

    @FXML
    public TextField chatField;

    @FXML
    public TextArea chatBox;

    @FXML
    public void initialize() {
        tilesPgChoiceBox.getSelectionModel()
            .selectedItemProperty()
            .addListener((observableValue, o, t1) -> { changePackage(packageType.tiles); });
        charactersPgChoiceBox.getSelectionModel()
                .selectedItemProperty()
                .addListener((observableValue, o, t1) -> { changePackage(packageType.characters); });
        // Check whether there are packages to be loaded at the beginning
        updatePackages();
        // This listener allows tiles to move with divider
        splitPane.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> {
            if((double)newVal < (double)oldVal) {
                updatePackagesLayout(1);
                return;
            }
            if(!packageChosen)
                return;
            if (maxInRow < (int) tilesChooseView.getWidth() / (tileSize + tilesGap)) {
                updatePackagesLayout();
            }
        });
        // Set default tool to start with
        drawToolButton.setSelected(true);
        currentAction = action.paintTile;
        // Set few default squares to memory
        Image defaultSquare = new Image(getClass().getResourceAsStream("/images/defaultSquare.png"),
                tileSize, tileSize, false, false);
        cachedImages.put("/images/defaultSquare.png", defaultSquare);
        Image missingSquare = new Image(getClass().getResourceAsStream("/images/missingSquare.png"),
                tileSize, tileSize, false, false);
        cachedImages.put("/images/missingSquare.png", missingSquare);
        Image transparent = new Image(getClass().getResourceAsStream("/images/transparent.png"),
                tileSize, tileSize, false, false);
        cachedImages.put("/images/transparent.png", transparent);
        secondLayerTglBtn.setOnAction(actionEvent -> {
            secondLayerVisible = secondLayerTglBtn.isSelected();
            disableLayers(secondLayerVisible, !secondLayerVisible);
        });
        //TODO Add camera drag to mapView
//        DragDelta dragDelta = new DragDelta();
//        mapView.setOnMousePressed(mouseEvent -> {
//            dragDelta.x = mapView.getLayoutX() - mouseEvent.getScreenX();
//            dragDelta.y = mapView.getLayoutY() - mouseEvent.getScreenY();
//        });
//        mapView.setOnMouseDragged(mouseEvent -> {
//            mapView.setLayoutX(mouseEvent.getScreenX() + dragDelta.x);
//            mapView.setLayoutY(mouseEvent.getScreenY() + dragDelta.y);
//        });
    }

    void setStage(Stage stage) { this.stage = stage; }

    // Title should has a '*' whenever there are unsaved changes
    private void updateTitle() {
        String name;
        if(saveLocation != null)
            name = saveLocation.getName();
        else
            name = "untitled";
        if(changedMap)
            name += "*";
        stage.setTitle(String.format("%s - Tabletop RPG Battlemap", name));
    }

    void setServer(Server server, ClientSideSocket client) {
        this.server = server;
        this.client = client;
        playersList = new ArrayList<>();
    }

    public void logPlayer(String nickname, ServerSideSocket socket) {
        Player newPlayer = new Player(socket);
        if(playersList.isEmpty()) {
            newPlayer.setNickname(nickname);
            newPlayer.setPermissionGroup(0);
        } else {
            // If nick is occupied simply put 1 and stop when it hits nick
            while(isNameOccupied(nickname)) {
                nickname += "1";
            }
            newPlayer.setNickname(nickname);
        }
        playersList.add(newPlayer);
        newPlayer.setPID(freePID);
        socket.sendPID(freePID, nickname);
        freePID++;
    }

    void setClient(ClientSideSocket client) {
        this.client = client;
    }

    public void setPID(int PID) {
        this.PID = PID;
    }

    public void setNickname(String nickname) {
        Platform.runLater(() -> leftStatusLabel.setText("Nick: " + nickname));
    }

    private boolean isNameOccupied(String nickname) {
        for (Player player : playersList) {
            if(player.getNickname().equals(nickname)) {
                return true;
            }
        }
        return false;
    }

    @FXML
    void sendMessage() {
        if(client != null && !chatField.getText().trim().isEmpty()) {
            client.sendMessage(PID, chatField.getText());
            chatField.setText("");
        }
    }

    public void receiveMessage(String nickname, String message) {
        Platform.runLater(() -> {
            chat += nickname + "(" + sdf.format(cal.getTime()) + "): " + message + '\n';
            chatBox.setText(chat);
                });
    }

    public void broadcastMessage(String nickname, String message) {
        for (Player player : playersList) {
            player.receiveMessage(nickname, message);
        }
    }
    public String getPlayerNick(int PID) throws IllegalArgumentException{
        for (Player player : playersList) {
            if(player.getPID() == PID) {
                return player.getNickname();
            }
        }
        throw new IllegalArgumentException();
    }

    @FXML
    void closeMap() {
        if(!mapSet) {
            popUpError("No map is opened!");
            return;
        }
        for(MapSquare[] row: firstLayer) {
            for(MapSquare square: row) {
                mapView.getChildren().remove(square);
            }
        }
        for(MapSquare[] row: secondLayer) {
            for(MapSquare square: row) {
                mapView.getChildren().remove(square);
            }
        }
        for(Button character : charactersTilesList) {
            mapView.getChildren().remove(character);
        }
        mapSet = false;
    }

    // It may be confusing but first layer is actually the one under
    void makeNewMap(int width, int height) {
        if(mapSet) {
            popUpError("Close existing map in order to set new one!");
        }
        mapWidth = width;
        mapHeight = height;
        firstLayer = new MapSquare[width][height];
        secondLayer = new MapSquare[width][height];
        for(int i = 0; i < width; i ++)
            for (int j = 0; j < height; j++) {
                firstLayer[i][j] = setUpMapSquare(i, j, "/images/defaultSquare.png", 1);
                secondLayer[i][j] = setUpMapSquare(i, j, "/images/transparent.png", 2);
                mapView.getChildren().add(firstLayer[i][j]);
                mapView.getChildren().add(secondLayer[i][j]);
            }
        saveLocation = null;
        updateTitle();
        mapSet = true;
    }

    //TODO think of reducing ifs and this whole method
    private MapSquare setUpMapSquare(int posY, int posX, String image, int layer) {
        MapSquare mapSquare = new MapSquare(posX, posY, layer);
        mapSquare.setGraphic(new ImageView(cachedImages.get(image)), image);
        mapSquare.setLayoutX(posX * tileSize);
        mapSquare.setLayoutY(posY * tileSize);
        DragDelta dragDelta = new DragDelta();
        dragDelta.x = 0;
        dragDelta.y = 0;
        mapSquare.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getButton() == MouseButton.PRIMARY) {
                if(currentAction == action.paintTile)
                    setMapSquareGraphic(posY, posX, mapSquare);
                if((currentAction == action.standard) && characterTileChosen) {
                    putCharacterOnSquare(mapSquare);
                }
            }
        });
        mapSquare.setOnMousePressed(mouseEvent -> {
            dragDelta.x = mapSquare.getLayoutX() - mouseEvent.getScreenX();
            dragDelta.y = mapSquare.getLayoutY() - mouseEvent.getScreenY();
        });
        mapSquare.setOnMouseDragged(mouseEvent -> {
            if(mouseEvent.isSecondaryButtonDown()) moveCamera(mouseEvent, dragDelta);
        });
        mapSquare.setOnDragDetected(mouseEvent -> mapSquare.startFullDrag());
        mapSquare.setOnMouseDragOver(mouseDragEvent -> {
            if(mouseDragEvent.getButton() == MouseButton.PRIMARY) {
                if(currentAction == action.paintTile)
                    setMapSquareGraphic(posY, posX, mapSquare);
                if(currentAction == action.ruler) {
                    if (arrowBegin == null) {
                        arrowBegin = mapSquare;
                        makeArrow();
                    }
                    moveArrow(mapSquare);
                }
                if(currentAction == action.erase)
                    setMapSquareGraphic(posY, posX, mapSquare, "images/transparent.png");
            }
        });
        mapSquare.setOnMouseDragReleased(mouseDragEvent -> {
            arrowBegin = null;
            arrowEnd = null;
            mapView.getChildren().remove(arrow);
        });
        return mapSquare;
    }

    private void setMapSquareGraphic(int posY, int posX, MapSquare mapSquare) {
        setMapSquareGraphic(posY, posX, mapSquare, currentTilePath);
    }

    private void setMapSquareGraphic(int posY, int posX, MapSquare mapSquare, String image) {
        // Do not allow for setting same graphic over and over again
        if(mapSquare.getImagePath().equals(image))
            return;
        String arguments = posX + ";" + posY + ";" + mapSquare.getImagePath() + ";" + mapSquare.getLayer();
        mapSquare.setGraphic(new ImageView(cachedImages.get(image)), image);
        history.append(action.paintTile, arguments);
    }

    // TODO Maybe this method is not necessary
    private void setMapSquareGraphic(double mousePosX, double mousePosY) {
        int posX, posY;
        posX = (int)mousePosX / (tileSize + tilesGap);
        posY = (int)mousePosY / (tileSize + tilesGap);
        if((posX > mapWidth) || (posY > mapHeight))
            return;
        setMapSquareGraphic(posY, posX, firstLayer[posY][posX]);
    }

    private void putCharacterOnSquare(MapSquare mapSquare) {
        ImageView imageView = new ImageView(cachedImages.get(currentCharacterPath));
        int width = (int)imageView.getImage().getWidth()%tileSize;
        int height = (int)imageView.getImage().getHeight()%tileSize;
        // First we need to check whether character can fit in chosen spot
        if((width < (mapSquare.getPosX() - 1)) || (height < (mapSquare.getPosY() - 1))) {
            popUpError("Character won't fit in this spot!");
        }
        // Probably unnecessary condition
//        for(Button btn: charactersList) {
//            CharacterSquare character = (CharacterSquare)btn;
//            if((character.getPosX() == mapSquare.getPosX()) || (character.getPosY() == mapSquare.getPosY())) {
//                popUpError("There is already character placed!");
//                return;
//            }
//        }
        CharacterSquare characterSquare = new CharacterSquare(mapSquare.getPosX(), mapSquare.getPosY(), mapView);
        setUpCharacter(characterSquare);
    }

    private void setUpCharacter(CharacterSquare characterSquare) {
        characterSquare.setLayoutX(characterSquare.getPosX() * tileSize);
        characterSquare.setLayoutY(characterSquare.getPosY() * tileSize);
        characterSquare.setGraphic(new ImageView(cachedImages.get(currentCharacterPath)), currentCharacterPath);
        mapView.getChildren().add(characterSquare);
        charactersList.add(characterSquare);
        DragDelta dragDelta = new DragDelta();
        dragDelta.x = 0;
        dragDelta.y = 0;
//        StatusBar healthBar = new StatusBar(100, characterSquare.getSize(), Paint.valueOf("0xff0000"));
//        healthBar.setLayoutX(characterSquare.getLayoutX());
//        healthBar.setLayoutY(characterSquare.getLayoutY() -(10 + ((characterSquare.getSize() - 1) * 5) ));
//        mapView.getChildren().add(healthBar);
//        if(log.isDebugEnabled())
//            log.debug(healthBar.getLayoutX() + " " + healthBar.getLayoutY());
//        healthBar.setAmount(80);
        // TODO reduce this ladder
        characterSquare.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getButton() == MouseButton.PRIMARY) {
                if(currentAction == action.standard) {
                    if(characterSquare.isClicked()) {
                        characterSquare.unclick();
                        popUpCharacterSettings(characterSquare);
                    }
                    else {
                        for (CharacterSquare character: charactersList) {
                            character.unclick();
                        }
                        characterSquare.click();
                    }
                }
                if(currentAction == action.erase) {
                    charactersList.remove(characterSquare);
                    mapView.getChildren().remove(characterSquare);
                    characterSquare.removeBars();
                }
            }
        });

        characterSquare.setOnMousePressed(mouseEvent -> {
            dragDelta.x = characterSquare.getLayoutX() - mouseEvent.getScreenX();
            dragDelta.y = characterSquare.getLayoutY() - mouseEvent.getScreenY();
        });
        characterSquare.setOnDragDetected(mouseEvent -> {
            characterSquare.startFullDrag();
        });
        characterSquare.setOnMouseDragged(mouseEvent -> {
            characterSquare.setLayoutX(mouseEvent.getScreenX() + dragDelta.x);
            characterSquare.setLayoutY(mouseEvent.getScreenY() + dragDelta.y);
        });
        characterSquare.setOnMouseDragReleased(mouseDragEvent -> {
            int endX = (int)characterSquare.getLayoutX() / tileSize;
            int endY = (int)characterSquare.getLayoutY() / tileSize;
            if(endX > 0 && endY > 0 && endX <= mapWidth - 1 && endY <= mapHeight - 1) {
                characterSquare.setLayoutPos(endX * tileSize, endY * tileSize);
            }
            dragDelta.x = 0;
            dragDelta.y = 0;
            characterSquare.unclick();
            for (CharacterSquare character: charactersList) {
                character.unclick();
            }
        });
    }

    private Button setUpTile(String imagePath, packageType type) {
        log.debug(imagePath);
        Button tile = new Button();
        tile.setStyle("-fx-background-color: transparent; -fx-padding: 5, 5, 5, 5;");
        if(!cachedImages.containsKey(imagePath)) {
            // We're treating these cases separately since our characters may vary in size
            if(type == packageType.characters) {
                cachedImages.put(imagePath, new Image(imagePath));
            }
            else
                cachedImages.put(imagePath, new Image(imagePath, tileSize, tileSize, false, false));
        }
        // TODO provide comment here
        ImageView imageView = new ImageView(cachedImages.get(imagePath));
        imageView.setFitHeight(tileSize);
        imageView.setFitWidth(tileSize);
        tile.setGraphic(imageView);
        tile.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getButton() == MouseButton.PRIMARY) {
                uncheckTiles(type);
                if(type == packageType.characters) {
                    currentCharacterPath = imagePath;
                    characterTileChosen = true;
                }
                else
                    currentTilePath = imagePath;
                tile.setEffect(new DropShadow());
            }
        });
        // Additional treatment for characters

        return tile;
    }

    private void uncheckTiles(packageType type) {
        ArrayList<Button> list;
        if(type == packageType.characters) {
            list = charactersTilesList;
            characterTileChosen = false;
        }
        else
            list = tilesList;

        for (Button tile : list) {
            tile.setEffect(null);
        }
    }

    // As we create our new arrow we assume that it starts and ends in same tile
    private void makeArrow() {
        double posX = arrowBegin.getLayoutX() + (double)(tileSize / 2);
        double posY = arrowBegin.getLayoutY() + (double)(tileSize / 2);
        arrow = new Arrow(posX, posY, posX, posY);
        mapView.getChildren().add(arrow);
        log.debug("Make!" + posX + " " + posY);
    }

    // We're only moving an arrow head thus we need only end position
    private void moveArrow(MapSquare end) {
        if(end == arrowEnd)
            return;
        arrowEnd = end;
        double endX = arrowEnd.getLayoutX() + (double)(tileSize / 2);
        double endY = arrowEnd.getLayoutY() + (double)(tileSize / 2);
        arrow.setEndPos(endX, endY);
        log.debug("Move!" + endX + " " + endY);
    }

    public void moveCamera(MouseEvent mouseEvent, DragDelta dragDelta) {
        if(!mouseEvent.isSecondaryButtonDown()) return;
        mapView.setTranslateX(mouseEvent.getScreenX() + dragDelta.x);
        mapView.setTranslateY(mouseEvent.getScreenY() + dragDelta.y);
    }

    public void popUpCharacterSettings(CharacterSquare character) {
        String name = character.getName().equals("") ? "Unnamed character" : character.getName();
        CharacterSettingsController controller =
                (CharacterSettingsController) popUpNewWindow("characterSettings.fxml", name);
        controller.setStartingValues(character);
    }

    public void popUpNewMapSettings() {
        popUpNewWindow("newMapSettings.fxml");
    }

    public void loadZipPackageWindow() {
        popUpNewWindow("newZipPackageWindow.fxml");
    }

    private PopUpController popUpNewWindow(String windowName) {
        return popUpNewWindow(windowName, "");
    }

    private PopUpController popUpNewWindow(String windowName, String title) {
        Stage newWindow = new Stage();
        newWindow.setTitle(title);
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = null;
        try {
            root = fxmlLoader.load(getClass().getResource("/fxml/" + windowName).openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        PopUpController controller = fxmlLoader.getController();
        controller.setParent(this);
        newWindow.setScene(new Scene(root));
        newWindow.show();
        return controller;
    }

    public void popUpError(String errorMsg) {
        ErrorPopUpController controller =
                (ErrorPopUpController)popUpNewWindow("errorPopUp.fxml", "Error");
        controller.setErrorMsg(errorMsg);
    }

    // If package type is not specified we just update both types
    public void updatePackages() {
        updatePackages(packageType.characters);
        updatePackages(packageType.tiles);
    }

    //TODO Make sure /res/packages/tiles is always created
    //TODO Make this String[] -> observable list -> items prettier
    //TODO When current package folder is deleted, there is error
    public void updatePackages(packageType type) {
        File file;
        ChoiceBox choiceBox;
        if(type == packageType.characters) {
            file = new File("res/packages/characters");
            choiceBox = charactersPgChoiceBox;
        }
        else {
            file = new File("res/packages/tiles");
            choiceBox = tilesPgChoiceBox;
        }
        String[] packages = file.list((current, name) -> new File(current, name).isDirectory());
        if(packages == null) {
//            popUpError("Lorem ipsum");
            return;
        }
        ObservableList<String> packagesList = FXCollections.observableArrayList(packages);
        choiceBox.setItems(packagesList);
        clearPackagesLayout(type);
    }
    // This function sets tiles of chosen package so user can use them
    //TODO Throw if sb deleted res/packages/tiles while program works
    public void changePackage(packageType type) {
        ChoiceBox choiceBox;
        AnchorPane chooseView;
        ArrayList<Button> list;
        String path;
        if(type == packageType.characters) {
            choiceBox = charactersPgChoiceBox;
            chooseView = charactersChooseView;
            list = charactersTilesList;
            path = "/packages/characters/";
        }
        else {
            choiceBox = tilesPgChoiceBox;
            chooseView = tilesChooseView;
            list = tilesList;
            path = "/packages/tiles/";
        }

        if(choiceBox.getValue() == null)
            return;
        File packagesDir = new File("res" + path + choiceBox.getValue());
        FileFilter filter = pathname -> hasProperExtension(pathname.getName());
        File[] tiles = packagesDir.listFiles(filter);

        Button tile;
        clearPackagesLayout(type);
        if(tiles == null)
            return;

        for(int i = 0; i < tiles.length; i++) {
//            log.debug(path + choiceBox.getValue() + "/" + tiles[i].getName());
            tile = setUpTile(path + choiceBox.getValue() + "/" + tiles[i].getName(), type);
            list.add(tile);
            chooseView.getChildren().add(tile);
        }
        packageChosen = true;
        updatePackagesLayout(type);
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

    public void clearPackagesLayout(packageType type) {
        ArrayList<Button> list;
        AnchorPane chooseView;
        if(type == packageType.characters) {
            list = charactersTilesList;
            chooseView = charactersChooseView;
        }
        else {
            list = tilesList;
            chooseView = tilesChooseView;
        }

        for(Object obj : list) chooseView.getChildren().remove(obj);
        list.clear();
    }

    public void updatePackagesLayout() {
        updatePackagesLayout(0, packageType.characters);
        updatePackagesLayout(0, packageType.tiles);
    }

    public void updatePackagesLayout(packageType type) {
        updatePackagesLayout(0, type);
    }

    public void updatePackagesLayout(int decreaseRowValue) {
        updatePackagesLayout(0, packageType.characters);
        updatePackagesLayout(0, packageType.tiles);
    }

    // TODO Characters are overlapping each other if they're big enough
    public void updatePackagesLayout(int decreaseRowValue, packageType type) {
        ArrayList<Button> list;
        AnchorPane chooseView;
        if(type == packageType.characters) {
            list = charactersTilesList;
            chooseView = charactersChooseView;
        }
        else {
            list = tilesList;
            chooseView = tilesChooseView;
        }

        maxInRow = (int)(chooseView.getWidth() / (tileSize + tilesGap)) - decreaseRowValue;
        int x = 0;
        int y = 0;
        for (Button tile : list) {
            tile.setLayoutX(x * (tileSize + tilesGap));
            tile.setLayoutY((y + 1) * (tileSize + tilesGap));
            x = (x +1)%maxInRow;
            if(x == 0)
                y++;
        }

        // TODO It is called when split pane is moving, so add some additional check
        if(list.isEmpty()) {
            //popUpError("Package is empty!");
            return;
        }
        // Resize anchor pane so all tiles will be reachable
        Button lastTile = (Button)list.get(list.size() - 1);
        chooseView.setMinHeight(lastTile.getLayoutY() + tileSize);
    }

    // TODO Think about better name
    // Method that enables user to change only one layer at once
    public void disableLayers(Boolean first, Boolean second) {
        for (MapSquare row[] : secondLayer) {
            for (MapSquare square: row) {
                square.setDisable(second);
            }
        }
        for (MapSquare row[] : firstLayer) {
            for (MapSquare square: row) {
                square.setDisable(first);
            }
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
        // Check if firstLayer is not saved already nor user is forcing fileChooser
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
            fileChooser.setInitialFileName("New firstLayer.txt");
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
        for (MapSquare[] squaresLine : firstLayer) {
            for (MapSquare square : squaresLine) {
                writer.println(square.getImagePath());
                if(log.isDebugEnabled()) log.debug(square.getImagePath());
            }
        }
        saveLocation = file;
        changedMap = false;
        updateTitle();
        writer.close();
    }

    public void toggleCharacterVisibility() {
        for (CharacterSquare character: charactersList) {
            character.setVisibility(characterVisibilityButton.isSelected());
        }
    }
}
