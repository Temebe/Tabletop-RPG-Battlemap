package shapes;

import javafx.scene.Group;
import javafx.scene.shape.Line;

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

    public Arrow(double startX, double startY, double endX, double endY) {
        super();
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        arrowHeadLeft = new Line();
        arrowHeadRight = new Line();
        line = new Line(startX, startY, endX, endY);
        calculateArrowHead();
        this.getChildren().add(arrowHeadLeft);
        this.getChildren().add(arrowHeadRight);
        this.getChildren().add(line);
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
}
