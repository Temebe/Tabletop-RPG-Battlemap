package shapes;

import javafx.scene.Group;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import static controllers.OfflineMapEditorController.tileSize;

public class StatusBar extends Group {
    private Rectangle background;
    private Rectangle bar;
    private double amount;
    private double maxAmount;
    private int size;
    private String color;

    public StatusBar(double maxAmount, int size, Paint paint) {
        if(maxAmount <= 0)
            throw new IllegalArgumentException("Max amount has to be value greater than 0");
        double width = size * tileSize;
        double height = 10 + ((size - 1) * 5);
        background = new Rectangle(width, height, Paint.valueOf("0x7f7f7f"));
        bar = new Rectangle(width, height, paint);
        this.getChildren().add(background);
        this.getChildren().add(bar);
        this.maxAmount = maxAmount;
        this.amount = maxAmount;
        this.size = size;
        this.setMouseTransparent(true);
        this.color = paint.toString();
    }

    public void setAmount(double amount) {
        if(amount > maxAmount) {
            amount = maxAmount;
        }
        if(amount < 0) {
            amount = 0;
        }
        bar.setWidth((amount/maxAmount) * size * tileSize);
        this.amount = amount;
    }

    public void setMaxAmount(double maxAmount) {
        if(maxAmount < amount) {
            maxAmount = amount;
        }
        if(maxAmount < 0) {
            maxAmount = 0;
        }
        bar.setWidth((amount/maxAmount) * size * tileSize);
        this.maxAmount = maxAmount;
    }

    public void setColor(Paint paint) {
        bar.setFill(paint);
        this.color = paint.toString();
    }

    public double getAmount() {
        return amount;
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public String getColor() {
        return color;
    }

}
