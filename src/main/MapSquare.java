package main;


import javafx.scene.control.Button;
import javafx.scene.image.ImageView;

public class MapSquare extends Button {
    private String image;
    private double posX;
    private double posY;
    private int layer;

    public MapSquare(double posX, double posY, int layer) {
        setStyle("-fx-background-color: transparent; -fx-padding: 5, 5, 5, 5;");
        this.posX = posX;
        this.posY = posY;
        this.layer = layer;
    }

    public void setGraphic(ImageView imageView, String image) {
        setGraphic(imageView);
        this.image = image;
    }

    public String getImage() {
        return image;
    }


    public double getPosX() {
        return posX;
    }

    public double getPosY() {
        return posY;
    }

    public int getLayer() { return layer; }

}
