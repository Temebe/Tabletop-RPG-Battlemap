package main;

import javafx.scene.control.Button;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import shapes.StatusBar;

import static controllers.OfflineMapEditorController.tileSize;

// TODO posX and posY seem to be obsolete
public class CharacterSquare extends Button {
    private String imagePath;
    private double posX;
    private double posY;
    //size is an amount of how tiles is both directions character cover (size 3 means character is 3 x 3)
    private int size = 0;
    private String name = "noname";
    private StatusBar firstBar = null;
    private StatusBar secondBar = null;
    private StatusBar thirdBar = null;
    private Pane parent;
    private boolean clicked;
    private ImageView imageView;
    private int cid; //character id

    public enum barType {
        first,
        second,
        third
    }

    public CharacterSquare(double posX, double posY, Pane parent, int cid) {
        setStyle("-fx-background-color: transparent; -fx-padding: 5, 5, 5, 5;");
        this.posX = posX;
        this.posY = posY;
        this.parent = parent;
        this.cid = cid;
    }

    public void setGraphic(ImageView imageView, String image) {
        int width = (int)imageView.getImage().getWidth();
        this.size = width/tileSize;
        imageView.setFitWidth(size * tileSize);
        imageView.setFitHeight(size * tileSize);
        if(size == 0)
            size = 1;
        this.imageView = imageView;
        setGraphic(this.imageView);
        this.imagePath = image;

    }

    public String getImagePath() {
        return imagePath;
    }


    public double getPosX() {
        return posX;
    }

    public double getPosY() {
        return posY;
    }

    public int getSize() {
        return size;
    }

    public StatusBar getBar(barType type) {
        switch (type) {
            case first:
                return firstBar;
            case second:
                return secondBar;
            case third:
                return thirdBar;
            default:
                throw new IllegalArgumentException("Type of bar incorrect");
        }
    }

    //TODO It probably could be easily reduced,
    public void setBar(StatusBar statusBar, barType type) {
        switch (type) {
            case first:
                checkBar(this.firstBar, statusBar);
                this.firstBar = statusBar;
                break;
            case second:
                checkBar(this.secondBar, statusBar);
                this.secondBar = statusBar;
                break;
            case third:
                checkBar(this.thirdBar, statusBar);
                this.thirdBar = statusBar;
                break;
            default:
                throw new IllegalArgumentException("Type of bar incorrect");

        }
    }

    // checkBar checks which object parent have to add and which have to remove
    private void checkBar(StatusBar statusBar, StatusBar newStatusBar) {
        if(newStatusBar == null) {
            if(statusBar != null) {
                parent.getChildren().remove(statusBar);
                return;
            }
        }
        if(statusBar == null && newStatusBar != null) {
            parent.getChildren().add(newStatusBar);
            return;
        }
        if(statusBar != null && newStatusBar != statusBar) {
            parent.getChildren().remove(statusBar);
            parent.getChildren().add(newStatusBar);
        }
    }

    public void setLayoutPos(double x, double y) {
        setLayoutX(x);
        setLayoutY(y);
        setBarsOnStage();
    }

    public void setPosX(double posX) {
        this.posX = posX;
    }

    public void setPosY(double posY) {
        this.posY = posY;
    }

    public void setBarsOnStage() {
        int height = 10 + (size - 1) * 5;
        if(firstBar != null) {
            firstBar.updateSize(this.size);
            firstBar.setLayoutX(this.getLayoutX());
            firstBar.setLayoutY(this.getLayoutY() - height);
        }
        if(secondBar != null) {
            secondBar.updateSize(this.size);
            secondBar.setLayoutX(this.getLayoutX());
            secondBar.setLayoutY(this.getLayoutY() - 2*height);
        }
        if(thirdBar != null) {
            thirdBar.updateSize(this.size);
            thirdBar.setLayoutX(this.getLayoutX());
            thirdBar.setLayoutY(this.getLayoutY() - 3*height);
        }
    }

    public void setSize(int size) {
        if(size <= 0) {
            size = 1;
        }
        this.size = size;
        imageView.setFitWidth(size * tileSize);
        imageView.setFitHeight(size * tileSize);
    }

    public void setVisibility(boolean visible) {
        this.setVisible(visible);
        if(firstBar != null) {
            firstBar.setVisible(visible);
        }
        if(secondBar != null) {
            secondBar.setVisible(visible);
        }
        if(thirdBar != null) {
            thirdBar.setVisible(visible);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public boolean isClicked() {
        return clicked;
    }

    public void unclick() {
        this.clicked = false;
        this.setEffect(null);
    }

    public void click() {
        this.clicked = true;
        this.setEffect(new Glow());
    }

    public void removeBars() {
        if(firstBar != null) {
            parent.getChildren().remove(firstBar);
        }
        if(secondBar != null) {
            parent.getChildren().remove(secondBar);
        }
        if(thirdBar != null) {
            parent.getChildren().remove(thirdBar);
        }
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public int getCid() {
        return cid;
    }
}
