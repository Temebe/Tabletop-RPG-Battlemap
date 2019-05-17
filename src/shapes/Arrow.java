package shapes;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import static controllers.BattlemapController.tileSize;

public class Arrow extends Group {
    private final static double arrowLength = 10;
    private final static double arrowWidth = 7;
    private double startX;
    private double startY;
    private double endX;
    private double endY;
    private Line line;
    private Line arrowHeadLeft;
    private Line arrowHeadRight;
    private Text distanceText;
    private Rectangle textBackground;

    public Arrow(double startX, double startY, double endX, double endY) {
        super();
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        arrowHeadLeft = new Line();
        arrowHeadRight = new Line();
        line = new Line(startX, startY, endX, endY);
        distanceText = new Text("0 ft");
        distanceText.setFill(Color.AZURE);
        distanceText.setFont(Font.font(null, FontWeight.BOLD, 14));
        textBackground = new Rectangle(distanceText.getLayoutBounds().getWidth(),
                distanceText.getLayoutBounds().getHeight(), Color.BLACK);
        calculateArrowHead();
        this.getChildren().add(arrowHeadLeft);
        this.getChildren().add(arrowHeadRight);
        this.getChildren().add(line);
        this.getChildren().add(textBackground);
        this.getChildren().add(distanceText);
    }

    public void setEndPos(double endX, double endY) {
        this.endX = endX;
        this.endY = endY;
        calculateArrowHead();
        line.setEndX(endX);
        line.setEndY(endY);
        arrowHeadLeft.setEndX(endX);
        arrowHeadLeft.setEndY(endY);
        arrowHeadRight.setEndX(endX);
        arrowHeadRight.setEndY(endY);
        distanceText.setText(calculateDistance());
        distanceText.setLayoutX(endX);
        distanceText.setLayoutY(endY + 5 + distanceText.getLayoutBounds().getHeight());
        textBackground.setLayoutX(endX);
        textBackground.setLayoutY(endY + 5);
        textBackground.setWidth(distanceText.getLayoutBounds().getWidth());
        textBackground.setHeight(distanceText.getLayoutBounds().getHeight());
    }

    // Piece of code from https://stackoverflow.com/questions/41353685/how-to-draw-arrow-javafx-pane, thanks!
    private void calculateArrowHead() {
        if (endX == startX && endY == startY) {
            // arrow parts of length 0
            arrowHeadLeft.setStartX(endX);
            arrowHeadLeft.setStartY(endY);
            arrowHeadRight.setStartX(endX);
            arrowHeadRight.setStartY(endY);
            return;
        }

        double factor = arrowLength / Math.hypot(startX - endX, startY - endY);
        double factorO = arrowWidth / Math.hypot(startX - endX, startY - endY);

        // part in direction of main line
        double dx = (startX - endX) * factor;
        double dy = (startY - endY) * factor;

        // part orthogonal to main line
        double ox = (startX - endX) * factorO;
        double oy = (startY - endY) * factorO;

        arrowHeadLeft.setStartX(endX + dx - oy);
        arrowHeadLeft.setStartY(endY + dy + ox);
        arrowHeadRight.setStartX(endX + dx + oy);
        arrowHeadRight.setStartY(endY + dy - ox);
    }

    private String calculateDistance() {
        int distanceX = Math.abs((int)((endX - startX) / tileSize));
        int distanceY = Math.abs((int)((endY - startY) / tileSize));
        int distance = distanceX > distanceY ? distanceX : distanceY;
        return (distance * 5) + " ft";
    }
}
