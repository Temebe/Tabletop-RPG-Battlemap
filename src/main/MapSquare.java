package main;


import javafx.scene.control.Button;
import javafx.scene.image.ImageView;

public class MapSquare extends Button {
    private String image;
    private int posX, posY;

    public MapSquare(int posX, int posY) {
        setStyle("-fx-background-color: transparent; -fx-padding: 5, 5, 5, 5;");
        this.posX = posX;
        this.posY = posY;
    }

    public void setGraphic(ImageView imageView, String image) {
        setGraphic(imageView);
        this.image = image;
    }

    public String getImage() {
        return image;
    }


    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }
}
