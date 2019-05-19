package controllers;

import javafx.application.Platform;
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
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import main.CharacterSquare;
import main.MapSquare;
import network_interface.*;
import org.apache.log4j.Logger;
import shapes.Arrow;
import shapes.StatusBar;

import java.io.*;
import java.util.*;

public class BattlemapController {

    private static final Logger log = Logger.getLogger(BattlemapController.class);
    private static final int tilesGap = 10;
    public static final int tileSize = 50;
    //Extensions that Image class of javafx can handle
    final private String[] tilesExtensions = {"jpg", "jpeg", "png", "bmp", "gif"};
    private int maxInRow;
    private boolean packageChosen = false;
    private MapSquare[][] firstLayer;
    private MapSquare[][] secondLayer;
    private ArrayList<Button> tilesList = new ArrayList<>();
    private ArrayList<Button> charactersTilesList = new ArrayList<>();
    private ArrayList<CharacterSquare> charactersList = new ArrayList<>();
    private HashMap<String, Image> cachedImages = new HashMap<>();
    private String currentTilePath = "/images/defaultSquare.png";
    private String currentCharacterPath = "/images/defaultSquare.png";
    private int mapHeight;
    private int mapWidth;
    private boolean mapSet = false;
    private boolean changedMap = false;
    private boolean secondLayerVisible = true;
    private boolean characterTileChosen = false;
    private boolean tabPaneVisible = true;
    private boolean toolbarVisible = true;
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
    private DragDelta cameraDragDelta = new DragDelta();
    private ArrayList<ToggleButton> toolbarButtons = new ArrayList<>();
    private int freeCid = 0;
    private Chat chat;
    private History history = new History();

    // Client side
    private ClientSideSocket client = null;

    // Server side
    private Server server = null;
    private ArrayList<Player> playersList;
    private ObservableList<String> observablePlayersList;
    private int freePID = 0; // PID that haven't been granted yet
    private ContextMenu contextMenu;
    private String selectedPlayer = null;
    private ArrayList<PlayerGroup> playerGroups;
    private String password = "";
    //private ArrayList<String> bannedIps = new ArrayList<>();
    private ObservableList<String> bannedIps = FXCollections.observableArrayList();

    @FXML
    public AnchorPane charactersChooseView;

    @FXML
    public AnchorPane tilesChooseView;

    @FXML
    public SplitPane splitPane;

    @FXML
    private AnchorPane mapView;

    @FXML
    private ChoiceBox<String> tilesPgChoiceBox;

    @FXML
    public ChoiceBox<String> charactersPgChoiceBox;

    @FXML
    public MenuItem undoBtn;

    @FXML
    public MenuItem redoBtn;

    @FXML
    public ToggleGroup mainToolbar;

    @FXML
    public ToggleButton standardToolButton;

    @FXML
    public ToggleButton drawToolButton;

    @FXML
    public ToggleButton rulerToolButton;

    @FXML
    public ToggleButton eraserToolButton;

    @FXML
    public ToggleButton secondLayerTglBtn;

    @FXML
    public ToggleButton characterVisibilityButton;

    @FXML
    public Label leftStatusLabel;

    @FXML
    public TextField chatField;

    @FXML
    public ScrollPane chatBoxScrollPane;

    @FXML
    public AnchorPane chatBox;

    @FXML
    public ListView<String> playersListView;

    @FXML
    public TabPane tabPane;

    @FXML
    public ToolBar toolbar;

    @FXML
    public MenuItem toolbarVisibilityItem;

    @FXML
    public MenuItem tabPaneVisibilityItem;

    @FXML
    public Tab chatTab;

    public enum packageType { characters, tiles }

    public enum action {
        standard,
        paintTile,
        ruler,
        erase,
    }

    class History {
        private static final int historyCap = 50;
        private action[] historyTable = new action[historyCap];
        private String[] arguments = new String[historyCap];
        private int actualPos;
        private int newPos = 0;
        private int size = 0;
        // amount of performed "undo" operations (important for possible redo)
        private int undos = 0;

        void append(action action, String arguments) {
            //Prevent clients from using history
            if(isClient() != isServer()) {
                return;
            }
            if(client != null)
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

        void undo() {
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

        void redo() {
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

    @SuppressWarnings("WeakerAccess")
    class DragDelta {
        public double x = 0;
        public double y = 0;

        public void reset() {
            x = 0;
            y = 0;
        }
    }

    public void undo() { history.undo(); }

    public void redo() { history.redo(); }

    public void setActionDrawing() {
        currentAction = action.paintTile;
        secondLayerVisible = secondLayerTglBtn.isSelected();
        disableLayers(secondLayerVisible, !secondLayerVisible);
        setCharactersMouseTransparent(true);
    }

    public void setActionRuler() {
        currentAction = action.ruler;
        disableLayers(false, false);
        setCharactersMouseTransparent(true);
    }

    public void setActionStandard() {
        currentAction = action.standard;
        disableLayers(false, false);
        setCharactersMouseTransparent(false);
    }

    public void changeChatFontSize(ActionEvent actionEvent) {
        MenuItem chosenOption = (MenuItem)actionEvent.getSource();
        int size = Integer.parseInt(chosenOption.getText());
        chat.setFontSize(size);
    }

    public void setActionErase() {
        currentAction = action.erase;
        setCharactersMouseTransparent(false);
    }

    @FXML
    public void changeTabPaneVisibility() {
        if(tabPaneVisible) {
            tabPaneVisible = false;
            tabPane.setMaxWidth(0);
            splitPane.setDividerPosition(0, 0);
            splitPane.setMouseTransparent(true);
            tabPaneVisibilityItem.setText("Show left pane");
        } else {
            tabPaneVisible = true;
            tabPane.setMaxWidth(Region.USE_COMPUTED_SIZE);
            splitPane.setDividerPosition(0, 0.3942);
            splitPane.setMouseTransparent(false);
            tabPaneVisibilityItem.setText("Hide left pane");
        }
    }

    @FXML
    public void changeToolbarVisibility() {
        if(toolbarVisible) {
            toolbarVisible = false;
            toolbar.getItems().removeAll(toolbarButtons);
            toolbar.setMaxHeight(0);
            toolbar.setMinHeight(0);
            log.debug(toolbar.getHeight());
            toolbarVisibilityItem.setText("Show toolbar");
        } else {
            toolbarVisible = true;
            toolbar.getItems().addAll(toolbarButtons);
            toolbar.setMaxHeight(Region.USE_COMPUTED_SIZE);
            toolbar.setMinHeight(Region.USE_COMPUTED_SIZE);
            log.debug(toolbar.getHeight());
            toolbarVisibilityItem.setText("Hide toolbar");
        }
    }

    @FXML
    public void centerMapView() {
        mapView.setTranslateX(0);
        mapView.setTranslateY(0);
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

    @FXML
    public void initialize() {
        setChoiceBoxesUp();
        // Check whether there are packages to be loaded at the beginning
        updatePackages();
        // This listener allows tiles to move with divider
        setSplitPaneUp();
        // Set default tool and layer to start with
        drawToolButton.setSelected(true);
        currentAction = action.paintTile;
        secondLayerVisible = secondLayerTglBtn.isSelected();
        disableLayers(secondLayerVisible, !secondLayerVisible);
        // Load few default squares to memory
        loadDefaultImages();
        secondLayerTglBtn.setOnAction(actionEvent -> {
            secondLayerVisible = secondLayerTglBtn.isSelected();
            disableLayers(secondLayerVisible, !secondLayerVisible);
        });
        characterVisibilityButton.setSelected(true);
        addCameraHandling();
        setToolbarButtons();
        chat = new Chat(chatBox, chatBoxScrollPane, chatField,this, 0);
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
        observablePlayersList = FXCollections.observableArrayList();
        playersListView.setItems(observablePlayersList);
        setPlayersListViewUp();
        setPlayerGroupsUp();
    }

    private void setChoiceBoxesUp() {
        tilesPgChoiceBox.getSelectionModel()
                .selectedItemProperty()
                .addListener((observableValue, o, t1) -> { changePackage(packageType.tiles); });
        charactersPgChoiceBox.getSelectionModel()
                .selectedItemProperty()
                .addListener((observableValue, o, t1) -> { changePackage(packageType.characters); });
    }

    private void setSplitPaneUp() {
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
    }

    private void loadDefaultImages() {
        Image defaultSquare = new Image(getClass().getResourceAsStream("/images/defaultSquare.png"),
                tileSize, tileSize, false, false);
        cachedImages.put("/images/defaultSquare.png", defaultSquare);
        Image missingSquare = new Image(getClass().getResourceAsStream("/images/missingSquare.png"),
                tileSize, tileSize, false, false);
        cachedImages.put("/images/missingSquare.png", missingSquare);
        Image transparent = new Image(getClass().getResourceAsStream("/images/transparent.png"),
                tileSize, tileSize, false, false);
        cachedImages.put("/images/transparent.png", transparent);
    }

    private void addCameraHandling() {
        mapView.setOnMousePressed(mouseEvent -> {
            cameraDragDelta.x = mapView.getTranslateX() - mouseEvent.getScreenX();
            cameraDragDelta.y = mapView.getTranslateY() - mouseEvent.getScreenY();
        });
        mapView.setOnDragEntered(mouseDragEvent -> mapView.startFullDrag());
        mapView.setOnMouseDragged(mouseEvent -> moveCamera(mouseEvent, cameraDragDelta));
    }

    private void setToolbarButtons() {
        toolbarButtons.addAll(Arrays.asList(standardToolButton, drawToolButton, rulerToolButton,
                eraserToolButton, secondLayerTglBtn, characterVisibilityButton));
    }

    private void setPlayersListViewUp() {
        setContextMenuUp();
        playersListView.setOnContextMenuRequested(contextMenuEvent -> {
            selectedPlayer = playersListView.getSelectionModel().getSelectedItem();
            log.debug("Selected " + selectedPlayer);
            contextMenu.show(playersListView, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
        });
    }

    private void setPlayerGroupsUp() {
        PlayerGroup gameMasterGroup = new PlayerGroup(0, "Game masters");
        PlayerGroup playerGroup = new PlayerGroup(1, "Players");
        gameMasterGroup.setPermission(PlayerGroup.drawingPerm, true);
        gameMasterGroup.setPermission(PlayerGroup.deletingPerm, true);
        gameMasterGroup.setPermission(PlayerGroup.creatingPerm, true);
        gameMasterGroup.setPermission(PlayerGroup.editingPerm, true);
        gameMasterGroup.setPermission(PlayerGroup.movingPerm, true);
        playerGroup.setPermission(PlayerGroup.movingPerm, true);
        playerGroups = new ArrayList<>();
        playerGroups.add(gameMasterGroup);
        playerGroups.add(playerGroup);
    }

    private void setContextMenuUp() {
        contextMenu = new ContextMenu();
        MenuItem kickItem = new MenuItem("Kick");
        kickItem.setOnAction(actionEvent -> {
            KickController controller = (KickController)popUpNewWindow("kickPopUp.fxml",
                    "Kick player " + selectedPlayer);
            controller.setParent(this);
            controller.setNickname(selectedPlayer);
        });
        MenuItem banItem = new MenuItem("Ban");
        banItem.setOnAction(actionEvent -> {
            BanController controller = (BanController)popUpNewWindow("banPopUp.fxml",
                    "Ban player " + selectedPlayer);
            controller.setParent(this);
            controller.setNickname(selectedPlayer);
        });
        MenuItem pmItem = new MenuItem("Send private message");
        pmItem.setOnAction(actionEvent -> {
            tabPane.getSelectionModel().select(chatTab);
            chatField.setText("/pm " + selectedPlayer + " ");
        });
        MenuItem changeGroupItem = new MenuItem("Change player's group");
        changeGroupItem.setOnAction(actionEvent -> {
            ChangeGroupController controller = (ChangeGroupController)popUpNewWindow("changeGroup.fxml",
                    "Change " + selectedPlayer + "'s group");
            assert controller != null;
            controller.setStartingParameters(getPlayer(selectedPlayer), playerGroups);
        });
        contextMenu.getItems().addAll(kickItem, banItem, pmItem, changeGroupItem);
    }

    // TODO set maximum nick size? or sth
    public void logInPlayer(String nickname, ServerSideSocket socket) {
        Player newPlayer = new Player(socket);
        nickname = nickname.replace(" ", "_");
        if(playersList.isEmpty()) {
            newPlayer.setNickname(nickname);
            newPlayer.setPermissionGroup(0);
        } else {
            // If nick is occupied simply put 1 and stop when it hits free nick
            while(isNameOccupied(nickname)) {
                nickname += "1";
            }
            newPlayer.setNickname(nickname);
        }
        playersList.add(newPlayer);
        newPlayer.setPID(freePID);
        socket.sendPID(freePID++, nickname);
        Platform.runLater(() -> observablePlayersList.add(newPlayer.getNickname()));
        if(!mapSet) {
            return;
        }
        if(newPlayer.getPID() != 0 && mapSet) {
            broadcastMap(newPlayer);
        }
    }

    public void logOutPlayer(ServerSideSocket socket) {
        for (Player player : playersList) {
            if(player.getSocket() == socket) {
                playersList.remove(player);
                observablePlayersList.remove(player.getNickname());
                return;
            }
        }
    }

    public void logOut() {
        client = null;
    }

    void setClient(ClientSideSocket client) {
        this.client = client;
    }

    public ClientSideSocket getClient() {
        return client;
    }

    public void setPID(int PID) {
        chat.setPID(PID);
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

    public boolean isClient() {
        return (client != null && server == null);
    }

    public boolean isServer() {
        return (client != null && server != null);
    }

    public boolean isConnected() {
        return client != null;
    }

    @FXML
    void sendChatMessage() {
        String msg = chatField.getText().trim();
        if(msg.isEmpty()) {
            return;
        }
        chat.sendMessage(msg);
    }

    public void broadcastChatMessage(String message, String arguments) {
        for(Player player : playersList) {
            player.getSocket().sendChatMessage(message, arguments);
        }
    }

    public void sendMessage(String message, String arguments, int PID) {
        Player player = getPlayer(PID);
        if(player != null) {
            player.getSocket().sendChatMessage(message, arguments);
        }
    }

    public Player getPlayer(int PID) {
        for(Player player : playersList) {
            if(player.getPID() == PID) {
                return player;
            }
        }
        return null;
    }

    public Player getPlayer(String nickname) {
        for(Player player : playersList) {
            if(player.getNickname().equals(nickname)) {
                return player;
            }
        }
        return null;
    }

    public PlayerGroup getPlayerGroup(int playerPID) {
        return getPlayerGroup(getPlayer(playerPID));
    }

    public PlayerGroup getPlayerGroup(String nickname) {
        return getPlayerGroup(getPlayer(nickname));
    }

    private PlayerGroup getPlayerGroup(Player player) {
        if(player == null) {
            return null;
        }
        int playerGroup = player.getPermissionGroup();
        for(PlayerGroup group : playerGroups) {
            if(group.getGroupId() == playerGroup) {
                return group;
            }
        }
        return null;
    }

    public ArrayList<PlayerGroup> getPlayerGroups() {
        return playerGroups;
    }

    public boolean passwordMatches(String password) {
        return this.password.equals(password);
    }

    public boolean isBanned(String ip) {
        for(String bannedIp : bannedIps) {
            if(ip.matches(bannedIp)){
                return true;
            }
        }
        return false;
    }

    public void addBannedIp(String ip) {
        bannedIps.add(ip);
    }

    public void unbanIp(String ip) {
        bannedIps.remove(ip);
    }

    public String getPassword() {
        return password;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public void broadcastCloseMap() {
        for(Player player : playersList) {
            player.getSocket().sendCloseMap();
        }
    }

    public void broadcastMap() {
        broadcastMap(null);
    }

    // null if you really want to broadcast map to every player
    public void broadcastMap(Player player) {
        File map = new File("temp/servmap.map");
        try {
            if(map.exists()) {
                if(!map.delete()) {
                    popUpError("Couldn't create temporary map file for player to send (deletion error)");
                    return;
                }
            }
            if(!map.createNewFile()) {
                popUpError("Couldn't create temporary map file for player to send (create error)");
            }
        } catch (IOException e) {
            e.printStackTrace();
            popUpError("Couldn't create temporary map file for player to send (stream exception)");
            return;
        }
        Platform.runLater(() -> {
            saveMapToFile(map, true);
            if(player != null) {
                player.getSocket().sendMap(map);
                return;
            }
            for(Player p : playersList) {
                if(p.getPID() == 0) {
                    continue;
                }
                if(log.isDebugEnabled()) log.debug("Sending map to " + p.getPID());
                p.getSocket().sendMap(map);
            }
        });
    }

    public void broadcastDrawing(int posX, int posY, String imagePath, int layerNum) {
        for(Player player : playersList) {
            player.getSocket().sendDrawAct(posX, posY, imagePath, layerNum);
        }
    }

    public void broadcastCreatingCharacter(int posX, int posY, String imagePath) {
        int cid = freeCid++;
        for(Player player : playersList) {
            player.getSocket().sendCharCreateAct(posX, posY, imagePath, cid);
        }
    }

    public void broadcastDeletingCharacter(int cid) {
        for(Player player : playersList) {
            player.getSocket().sendCharDeleteAct(cid);
        }
    }

    public void broadcastMovingCharacter(int cid, int posX, int posY) {
        for(Player player : playersList) {
            player.getSocket().sendCharMoveAct(cid, posX, posY);
        }
    }

    public void broadcastSettingCharacter(int cid, String name, int size,
                                          String color1, double amount1, double maxAmount1,
                                          String color2, double amount2, double maxAmount2,
                                          String color3, double amount3, double maxAmount3) {
        for(Player player : playersList) {
            player.getSocket().sendCharSetAct(cid, name, size,
                    color1, amount1, maxAmount1,
                    color2, amount2, maxAmount2,
                    color3, amount3, maxAmount3);
        }
    }

    public String getPlayerNick(int PID) throws IllegalArgumentException {
        for (Player player : playersList) {
            if(player.getPID() == PID) {
                return player.getNickname();
            }
        }
        throw new IllegalArgumentException();
    }

    public Chat getChat() {
        return chat;
    }

    @FXML
    public void closeMap() {
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
        for(CharacterSquare character : charactersList) {
            mapView.getChildren().remove(character);
            character.removeBars();
        }
        charactersList.clear();
        mapSet = false;
//        if(server != null) {
//            mapSet = true;
//            broadcastCloseMap();
//        }
    }

    // It may be confusing but first layer is actually the one under
    void makeNewMap(int width, int height) {
        if(mapSet) {
            closeMap();
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
        secondLayerVisible = secondLayerTglBtn.isSelected();
        disableLayers(secondLayerVisible, !secondLayerVisible);
        freeCid = 0;
        if(isServer()) {
            broadcastMap();
        }
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
            dragDelta.x = mapView.getTranslateX() - mouseEvent.getScreenX();
            dragDelta.y = mapView.getTranslateY() - mouseEvent.getScreenY();
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
            if(arrowBegin != null) {
                arrowBegin = null;
                arrowEnd = null;
                mapView.getChildren().remove(arrow);
            }
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
        if(isConnected()) {
            client.requestDrawing(posX, posY, image, mapSquare.getLayer());
            return;
        }
        String arguments = posX + ";" + posY + ";" + mapSquare.getImagePath() + ";" + mapSquare.getLayer();
        mapSquare.setGraphic(new ImageView(cachedImages.get(image)), image);
        history.append(action.paintTile, arguments);
    }

    // Setting graphic for clients
    public void setMapSquareGraphic(int posY, int posX, String image, int layerNum) {
        if((posY > mapHeight - 1) || (posX > mapWidth - 1)) {
            return;
        }
        MapSquare[][] layer;
        if(layerNum == 1) {
            layer = firstLayer;
        } else {
            layer = secondLayer;
        }
        loadGraphic(image, true);
        layer[posY][posX].setGraphic(new ImageView(cachedImages.get(image)), image);
        if(isServer()) {
            String arguments = posX + ";" + posY + ";" + image + ";" + layerNum;
            history.append(action.paintTile, arguments);
        }
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

    private void setCharactersMouseTransparent(boolean value) {
        for(CharacterSquare character : charactersList) {
            character.setMouseTransparent(value);
        }
    }

    private void putCharacterOnSquare(MapSquare mapSquare) {
        if(isConnected()) {
            client.requestNewCharacter((int)mapSquare.getPosX(), (int)mapSquare.getPosY(), currentCharacterPath);
            return;
        }
        ImageView imageView = new ImageView(cachedImages.get(currentCharacterPath));
        int width = (int)imageView.getImage().getWidth()%tileSize;
        int height = (int)imageView.getImage().getHeight()%tileSize;
        CharacterSquare characterSquare = new CharacterSquare(mapSquare.getPosX(), mapSquare.getPosY(),
                mapView, freeCid++);
        setUpCharacter(characterSquare);
    }

    // Version of creating character for clients
    public void putCharacterOnSquare(int posX, int posY, String imagePath, int cid) {
        loadGraphic(imagePath, false);
        ImageView imageView = new ImageView(cachedImages.get(imagePath));
        int width = (int)imageView.getImage().getWidth()%tileSize;
        int height = (int)imageView.getImage().getHeight()%tileSize;
        CharacterSquare characterSquare = new CharacterSquare(posX, posY,
                mapView, cid);
        setUpCharacter(characterSquare);
        loadGraphic(imagePath, false);
        characterSquare.setGraphic(new ImageView(cachedImages.get(imagePath)), imagePath);
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
                    deleteCharacter(characterSquare);
                }
            }
        });

        characterSquare.setOnMousePressed(mouseEvent -> {
            dragDelta.x = characterSquare.getTranslateX() - mouseEvent.getScreenX();
            dragDelta.y = characterSquare.getTranslateY() - mouseEvent.getScreenY();
        });
        characterSquare.setOnDragDetected(mouseEvent -> characterSquare.startFullDrag());
        characterSquare.setOnMouseDragged(mouseEvent -> {
            characterSquare.setTranslateX(mouseEvent.getScreenX() + dragDelta.x);
            characterSquare.setTranslateY(mouseEvent.getScreenY() + dragDelta.y);
        });
        characterSquare.setOnMouseReleased(mouseEvent -> moveCharacter(characterSquare));
    }

    private void moveCharacter(CharacterSquare character) {
        character.unclick();
        for (CharacterSquare ch: charactersList) {
            ch.unclick();
        }
        int endX = (int)character.getTranslateX() / tileSize;
        int endY = (int)character.getTranslateY() / tileSize;
        if(endX >= 0 && endY >= 0 && endX <= mapWidth - 1 && endY <= mapHeight - 1) {
            if(isConnected()) {
                character.setTranslatePos(character.getPosX() * tileSize,
                        character.getPosY() * tileSize);
                client.requestMoveCharacter(character.getCid(), endX, endY);
                return;
            }
            character.setTranslatePos(endX * tileSize, endY * tileSize);
            character.setPosX(endX);
            character.setPosY(endY);
        } else {
            character.setTranslatePos(character.getPosX() * tileSize,
                    character.getPosY() * tileSize);
        }
    }

    public void moveCharacter(int cid, int posX, int posY) {
        CharacterSquare character = null;
        for (CharacterSquare ch : charactersList) {
            if(ch.getCid() == cid) {
                character = ch;
                break;
            }
        }
        if(character == null) {
            return;
        }
        character.setTranslatePos((double)posX * tileSize, (double)posY * tileSize);
        character.setPosX(posX);
        character.setPosY(posY);

    }

    private void deleteCharacter(CharacterSquare character) {
        if(isConnected()) {
            client.requestDelCharacter(character.getCid());
            return;
        }
        charactersList.remove(character);
        mapView.getChildren().remove(character);
        character.removeBars();
    }

    public void deleteCharacter(int cid) {
        for(CharacterSquare character : charactersList) {
            if(character.getCid() == cid) {
                charactersList.remove(character);
                mapView.getChildren().remove(character);
                character.removeBars();
                return;
            }
        }
    }

    // TODO Get rid of duplicate
    public void setCharacter(String[] data) {
        int cid = Integer.parseInt(data[0]);
        String name = data[1];
        int size = Integer.parseInt(data[2]);
        String color1 = data[3];
        double amount1 = Double.parseDouble(data[4]);
        double maxAmount1 = Double.parseDouble(data[5]);
        String color2 = data[6];
        double amount2 = Double.parseDouble(data[7]);
        double maxAmount2 = Double.parseDouble(data[8]);
        String color3 = data[9];
        double amount3 = Double.parseDouble(data[10]);
        double maxAmount3 = Double.parseDouble(data[11]);
        setCharacter(cid, name, size,
                color1, amount1, maxAmount1,
                color2, amount2, maxAmount2,
                color3, amount3, maxAmount3);
    }

    // TODO do some methods to shorten this
    public void setCharacter(int cid, String name, int size,
                             String color1, double amount1, double maxAmount1,
                             String color2, double amount2, double maxAmount2,
                             String color3, double amount3, double maxAmount3) {
        CharacterSquare character = null;
        for(CharacterSquare ch : charactersList) {
            if(ch.getCid() == cid) {
                character = ch;
            }
        }
        if(character == null) {
            return;
        }
        character.setName(name);
        character.setSize(size);
        if(!color1.equals("null")) {
            StatusBar bar = new StatusBar(maxAmount1, size, Paint.valueOf(color1));
            bar.setAmount(amount1);
            character.setBar(bar, CharacterSquare.barType.first);
        } else {
            character.setBar(null, CharacterSquare.barType.first);
        }
        if(!color2.equals("null")) {
            StatusBar bar = new StatusBar(maxAmount2, size, Paint.valueOf(color2));
            bar.setAmount(amount2);
            character.setBar(bar, CharacterSquare.barType.second);
        } else {
            character.setBar(null, CharacterSquare.barType.second);
        }
        if(!color3.equals("null")) {
            StatusBar bar = new StatusBar(maxAmount3, size, Paint.valueOf(color3));
            bar.setAmount(amount3);
            character.setBar(bar, CharacterSquare.barType.third);
        } else {
            character.setBar(null, CharacterSquare.barType.third);
        }
        character.setBarsOnStage();
    }

    private Button setUpTile(String imagePath, packageType type) {
        log.debug(imagePath);
        Button tile = new Button();
        tile.setStyle("-fx-background-color: transparent; -fx-padding: 5, 5, 5, 5;");
            // We're treating these cases separately since our characters may vary in size
        if(type == packageType.characters) {
            loadGraphic(imagePath, false);
        }
        else {
            loadGraphic(imagePath, true);
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

    private void moveCamera(MouseEvent mouseEvent, DragDelta dragDelta) {
        mapView.setTranslateX(mouseEvent.getScreenX() + dragDelta.x);
        mapView.setTranslateY(mouseEvent.getScreenY() + dragDelta.y);
    }

    private void popUpCharacterSettings(CharacterSquare character) {
        String name = character.getName().equals("") ? "Unnamed character" : character.getName();
        CharacterSettingsController controller =
                (CharacterSettingsController) popUpNewWindow("characterSettings.fxml", name);
        Objects.requireNonNull(controller, "Unexpected error when loading window")
                .setStartingValues(character);
    }

    public void popUpNewMapSettings() {
        popUpNewWindow("newMapSettings.fxml");
    }

    public void loadZipPackageWindow() {
        popUpNewWindow("newZipPackageWindow.fxml");
    }

    public void popUpControlPanel() {
        if(isServer()) {
            ControlPanelController controller = (ControlPanelController)popUpNewWindow("controlPanel.fxml",
                    "Control panel");
            if(controller != null) {
                controller.setParent(this);
                controller.setPermissionGroups(playerGroups);
                controller.setGeneralSettings(bannedIps);
            }
        } else {
            popUpError("You are not hosting any game!");
        }
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
            log.error("Problem with loading window " + windowName + ", maybe corrupted fxml files?");
        } catch (NullPointerException e) {
            log.error("Couldn't find " + windowName + " file");
        }
        PopUpController controller = fxmlLoader.getController();
        controller.setParent(this);
        // TODO try later assert
        if(root == null) {
            return null;
        }
        newWindow.setScene(new Scene(root));
        newWindow.show();
        return controller;
    }

    public void popUpError(String errorMsg) {
        Platform.runLater(() -> {
            ErrorPopUpController controller =
                    (ErrorPopUpController)popUpNewWindow("errorPopUp.fxml", "Error");
            controller.setErrorMsg(errorMsg);
        });
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
        if(!mapSet) {
            return;
        }
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
                saveMapToFile(saveLocation, false);
                return;
            }
        }
        File file = chooseMapFile(true);
        if(file != null)
            saveMapToFile(file, false);
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
            fileChooser.setInitialFileName("New map.map");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("MAP files (*.map)", "*.map");
        fileChooser.getExtensionFilters().add(extFilter);
        File file;
        if(saving) {
            file = fileChooser.showSaveDialog(mapView.getScene().getWindow());
        } else {
            file = fileChooser.showOpenDialog(mapView.getScene().getWindow());
        }
        return file;
    }

    // tempSave true means that it is saved as a temporary file and there is no need to i.e. set saveLocation
    //TODO add some exceptions for missing images or something
    private void saveMapToFile(File file, boolean tempSave) {
        PrintWriter writer;
        try {
            writer = new PrintWriter(file);
            writer.println(mapHeight + " " + mapWidth);
        } catch (FileNotFoundException e) {
            log.error(e.getStackTrace());
            return;
        }
        writeDownLayers(firstLayer, writer);
        writeDownLayers(secondLayer, writer);
        for(CharacterSquare character : charactersList) {
            writeDownCharacter(character, writer);
        }
        if(!tempSave) {
            saveLocation = file;
            changedMap = false;
            updateTitle();
        }
        writer.close();
    }

    private void writeDownLayers(MapSquare[][] layer, PrintWriter writer) {
        for(MapSquare[] squaresLine : layer) {
            for (MapSquare square : squaresLine) {
                writer.println(square.getImagePath());
                if(log.isDebugEnabled()) log.debug(square.getImagePath());
            }
        }
        writer.println("###");
    }

    private void writeDownCharacter(CharacterSquare character, PrintWriter writer) {
        StatusBar firstBar = character.getBar(CharacterSquare.barType.first);
        StatusBar secondBar = character.getBar(CharacterSquare.barType.second);
        StatusBar thirdBar = character.getBar(CharacterSquare.barType.third);
        writer.println(character.getName() + ";" + character.getCid());
        writer.println(character.getImagePath() + ";" + character.getLayoutX() + ";"
                + character.getLayoutY() + ";" + character.getSize());
        writeDownStatusBar(firstBar, writer);
        writeDownStatusBar(secondBar, writer);
        writeDownStatusBar(thirdBar, writer);
    }

    private void writeDownStatusBar(StatusBar statusBar, PrintWriter writer) {
        if(statusBar != null) {
            writer.println(statusBar.getColor() + ";" +  statusBar.getAmount() + ";" + statusBar.getMaxAmount());
        } else {
            writer.println("#null");
        }
    }

    @FXML
    public void loadMap() {
        if(log.isDebugEnabled()) log.debug("TEST");
        File map = chooseMapFile(false);
        loadMap(map);
    }

    public void loadMap(File map) {
        if(mapSet) {
            closeMap();
        }
        try {
            loadMapFromFile(map);
        } catch (Exception e) {
            e.printStackTrace();
            popUpError("There was problem with loading map.");
        }
        mapSet = true;
        if(isServer()) {
            broadcastMap();
        }
    }

    // TODO Ask if some1 is sure he want's to override existing map
    public void loadMapFromFile(File file){
        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new FileReader(file));
            line = br.readLine();
        } catch (Exception e) {
            popUpError("There was problem with reading map file");
            return;
        }
        int width = Integer.parseInt(line.split(" ")[0]);
        int height = Integer.parseInt(line.split(" ")[1]);
        makeNewMap(width, height);
        try {
            loadLayer(br, firstLayer);
            loadLayer(br, secondLayer);
            loadCharacters(br);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadLayer(BufferedReader br, MapSquare[][] layer) throws IOException{
        String path;
        int x = 0;
        int y = 0;
        while((path = br.readLine()) != null) {
            if(path.equals("###")) {
                break;
            }
            loadGraphic(path, true);
            layer[y][x].setGraphic(new ImageView(cachedImages.get(path)), path);
            x = (x + 1)%mapWidth;
            if(x == 0) {
                y = (y + 1)%mapHeight;
            }
        }
    }

    private void loadCharacters(BufferedReader br) throws IOException{
        String line;
        String[] args;
        CharacterSquare newCharacter;
        while((line = br.readLine()) != null) {
            args = line.split(";");
            newCharacter = new CharacterSquare(0, 0, mapView, 0);
            setUpCharacter(newCharacter);
            newCharacter.setName(args[0]);
            newCharacter.setCid(Integer.parseInt(args[1]));
            line = br.readLine();
            args = line.split(";");
            loadGraphic(args[0], false);
            newCharacter.setGraphic(new ImageView(args[0]), args[0]);
            newCharacter.setTranslateX(Double.parseDouble(args[1]));
            newCharacter.setTranslateY(Double.parseDouble(args[2]));
            newCharacter.setSize(Integer.parseInt(args[3]));
            newCharacter.setBar(loadStatusBar(br, newCharacter.getSize()), CharacterSquare.barType.first);
            newCharacter.setBar(loadStatusBar(br, newCharacter.getSize()), CharacterSquare.barType.second);
            newCharacter.setBar(loadStatusBar(br, newCharacter.getSize()), CharacterSquare.barType.third);
            newCharacter.setBarsOnStage();
            if(freeCid <= newCharacter.getCid()) {
                freeCid = newCharacter.getCid() + 1;
            }
        }
    }

    private StatusBar loadStatusBar(BufferedReader br, int size) throws IOException{
        String line = br.readLine();
        if(line.equals("#null")) {
            return null;
        }
        StatusBar result;
        String[] args = line.split(";");
        String paint = args[0];
        double maxAmount = Double.parseDouble(args[1]);
        double amount = Double.parseDouble(args[1]);
        result = new StatusBar(maxAmount, size, Paint.valueOf(paint));
        result.setAmount(amount);
        return result;
    }

//    public String packArguments(String ...arguments) {
//        StringBuilder result = new StringBuilder();
//        for (String arg : arguments) {
//            result.append(arg).append(";");
//        }
//        if(result.length() > 0) {
//            result.deleteCharAt(result.length() - 1);
//        }
//        return result.toString();
//    }

    public void toggleCharacterVisibility() {
        for (CharacterSquare character: charactersList) {
            character.setVisibility(characterVisibilityButton.isSelected());
        }
    }

    public void loadGraphic(String path, boolean forceSize) {
        if(cachedImages.containsKey(path)) {
            return;
        }
        if (new File("res/" + path).exists()) {
            if(forceSize) {
                cachedImages.put(path, new Image(path, tileSize, tileSize, false, false));
            } else {
                cachedImages.put(path, new Image(path));
            }
        } else {
            cachedImages.put(path, new Image("/images/missingSquare.png"));
        }
    }
}
