package layout;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import model.Function;
import model.Point;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class GraphicCanvas {
    private static final double MIN_SCALE = 0.7;
    private static final double MAX_SCALE = 3.0;
    private static final double SCALING_STEP = 0.1;
    private static final int SCALING_ROUNDING = 1;
    private static final double START_CANVAS_SIZE = 600;
    private static final double MARK_SPACING = 20;

    private static final double MAX_CANVAS_SIZE = 4000;
    private static final double X_RESIZING_BORDER = 0.85;
    private static final double Y_RESIZING_BORDER = 0.15;
    private static final double RESIZING_STEP = 0.5;
    private static final double SCROLL_PANE_CENTER_POSITION = 0.5;

    private double currentScale;
    private double newScale;
    private double canvasSize;
    private double singleScaleSegment;

    private boolean hasToRedraw;
    private boolean hasToErase;

    private Point nextPoint;
    private double maxX;
    private double minY;

    private ScrollPane scrollPane;
    private Canvas canvas;
    private GraphicsContext graphic;

    private ObservableList<Function> functions;
    private ObservableList<Point> prevPoints;
    private ObservableList<Color> functionDrawingColors;
    private ObservableList<Integer> functionPointsIterators;

    private Lock drawingLock;


    public GraphicCanvas(ObservableList<Function> functions) {
        newScale = MIN_SCALE;
        currentScale = newScale;
        singleScaleSegment = MARK_SPACING / newScale;

        hasToRedraw = false;
        nextPoint = new Point();
        maxX = Function.MIN_X_DOWN_LIMIT;
        minY = Function.MAX_X_UP_LIMIT;

        canvas = new Canvas();
        graphic = canvas.getGraphicsContext2D();
        erase();
        updateCanvasConfig();

        scrollPane = new ScrollPane();
        initScrollPaneConfig();

        this.functions = functions;
        prevPoints = FXCollections.observableArrayList();
        functionDrawingColors = FXCollections.observableArrayList();
        functionPointsIterators = FXCollections.observableArrayList();
        Random forColors = new Random(System.currentTimeMillis());

        for (int funIterator = 0; funIterator < functions.size(); funIterator++) {
            prevPoints.add(null);
            functionDrawingColors.add(Color.color(forColors.nextDouble(), forColors.nextDouble(), forColors.nextDouble()));
            functionPointsIterators.add(-1);
        }
        drawingLock = new ReentrantLock();
    }

    public ScrollPane getScrollPane() {
        return scrollPane;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public double getNewScale() {
        return newScale;
    }

    public double getCurrentScale() {
        return currentScale;
    }

    public ObservableList<Color> getFunctionDrawingColors() {
        return functionDrawingColors;
    }

    public ObservableList<Function> getFunctions() {
        return functions;
    }

    private void initScrollPaneConfig() {
        scrollPane.setPannable(true);
        scrollPane.setContent(canvas);
        scrollPane.setVvalue(SCROLL_PANE_CENTER_POSITION);
        scrollPane.setHvalue(SCROLL_PANE_CENTER_POSITION);
    }

    private void updateCanvasConfig() {
        canvas.setWidth(canvasSize);
        canvas.setHeight(canvasSize);
    }

    public void update() {
        if (hasToErase) {
            eraseFunctionGraphics();
            hasToErase = false;
        }

        if (hasToRedraw) {
            redrawFunctionGraphics();
            hasToRedraw = false;
        }

        if (newScale != currentScale) {
            drawingLock.lock();

            canvasSize *= newScale / currentScale;
            updateCanvasConfig();
            redraw();

            currentScale = newScale;

            scrollPane.setVvalue(SCROLL_PANE_CENTER_POSITION);
            scrollPane.setHvalue(SCROLL_PANE_CENTER_POSITION);

            drawingLock.unlock();
        }

        if ((maxX >= X_RESIZING_BORDER * canvasSize) || (minY <= Y_RESIZING_BORDER * canvasSize)) {
            drawingLock.lock();

            if (canvasSize < MAX_CANVAS_SIZE) {
                canvasSize += RESIZING_STEP * canvasSize * newScale;

                updateCanvasConfig();
                redraw();

                scrollPane.setVvalue(SCROLL_PANE_CENTER_POSITION);
                scrollPane.setHvalue(SCROLL_PANE_CENTER_POSITION);
            }

            drawingLock.unlock();
        }

        drawFunctionGraphics();
    }

    private void updateCoordinateAxes() {
        double markLineWidth = 0.5;
        double markTextSize = 9;

        graphic.setStroke(Color.GRAY);
        graphic.setLineWidth(markLineWidth);
        graphic.setFont(Font.font(markTextSize));

        double halfCanvasSize = canvasSize / 2;

        graphic.strokeLine(halfCanvasSize, 0, halfCanvasSize, canvasSize);
        graphic.strokeLine(0, halfCanvasSize, canvasSize, halfCanvasSize);

        graphic.strokeLine(canvasSize - 10, halfCanvasSize - 4, canvasSize, halfCanvasSize);
        graphic.strokeLine(canvasSize - 10, halfCanvasSize + 4, canvasSize, halfCanvasSize);
        graphic.strokeText("x", canvasSize - 10, halfCanvasSize - 10);

        graphic.strokeLine(halfCanvasSize - 4,10, halfCanvasSize, 0);
        graphic.strokeLine(halfCanvasSize + 4,10, halfCanvasSize, 0);
        graphic.strokeText("y", halfCanvasSize - 15, 10);


        double coordinateMarkHalfLength = 5;
        int whereToWriteMarkText = 2;

        for (double eachCoordinateMark = 0; eachCoordinateMark < canvasSize / 2 - MARK_SPACING; eachCoordinateMark += MARK_SPACING) {
            if (eachCoordinateMark == 0) {
                continue;
            }

            graphic.strokeLine(eachCoordinateMark + halfCanvasSize, halfCanvasSize - coordinateMarkHalfLength,
                               eachCoordinateMark + halfCanvasSize, halfCanvasSize + coordinateMarkHalfLength);

            graphic.strokeLine(halfCanvasSize - coordinateMarkHalfLength, halfCanvasSize - eachCoordinateMark,
                               halfCanvasSize + coordinateMarkHalfLength, halfCanvasSize - eachCoordinateMark);

            graphic.strokeLine(halfCanvasSize - eachCoordinateMark, halfCanvasSize - coordinateMarkHalfLength,
                               halfCanvasSize - eachCoordinateMark, halfCanvasSize + coordinateMarkHalfLength);

            graphic.strokeLine(halfCanvasSize - coordinateMarkHalfLength, halfCanvasSize + eachCoordinateMark,
                               halfCanvasSize + coordinateMarkHalfLength, halfCanvasSize + eachCoordinateMark);

            if ((eachCoordinateMark % (MARK_SPACING * whereToWriteMarkText)) == 0) {
                graphic.strokeText(String.valueOf((int) (eachCoordinateMark / newScale)),
                                halfCanvasSize + eachCoordinateMark,halfCanvasSize + 3 * coordinateMarkHalfLength);

                graphic.strokeText(String.valueOf((int) (eachCoordinateMark / newScale)),
                                halfCanvasSize + 2 * coordinateMarkHalfLength, halfCanvasSize - eachCoordinateMark);

                graphic.strokeText(String.valueOf((int) (-eachCoordinateMark / newScale)),
                                halfCanvasSize - eachCoordinateMark,halfCanvasSize + 3 * coordinateMarkHalfLength);

                graphic.strokeText(String.valueOf((int) (-eachCoordinateMark / newScale)),
                                halfCanvasSize + 2 * coordinateMarkHalfLength, halfCanvasSize + eachCoordinateMark);
            }
        }

        graphic.strokeText("0", halfCanvasSize + coordinateMarkHalfLength,
                             halfCanvasSize + 2 * coordinateMarkHalfLength);
    }

    public void incrementScale() {
        if (canvasSize > MAX_CANVAS_SIZE) {
            return;
        }

        if (newScale < MAX_SCALE) {
            newScale = new BigDecimal(newScale + SCALING_STEP).setScale(SCALING_ROUNDING, RoundingMode.HALF_UP).doubleValue();
        }
        singleScaleSegment = MARK_SPACING / newScale;
    }

    public void decrementScale() {
        if (newScale > MIN_SCALE) {
            newScale = new BigDecimal(newScale - SCALING_STEP).setScale(SCALING_ROUNDING, RoundingMode.HALF_UP).doubleValue();
        }
        singleScaleSegment = MARK_SPACING / newScale;
    }

    private void redraw() {
        hasToRedraw = true;
    }

    public void erase() {
        hasToErase = true;
    }

    private void redrawFunctionGraphics() {
        drawingLock.lock();

        maxX = Function.MIN_X_DOWN_LIMIT;
        minY = Function.MAX_X_UP_LIMIT;

        graphic.setFill(Color.WHITE);
        graphic.fillRect(0,0, canvas.getWidth(), canvas.getHeight());
        updateCoordinateAxes();

        double functionLineWidth = 1;
        double halfCanvasSize = canvasSize / 2;
        graphic.setLineWidth(functionLineWidth);

        for (int funIterator = 0; funIterator < prevPoints.size(); funIterator++) {
            prevPoints.set(funIterator, null);
        }

        for (int funIterator = 0; funIterator < functions.size(); funIterator++) {
            for (int pointsIterator = 0; pointsIterator <= functionPointsIterators.get(funIterator); pointsIterator++) {
                nextPoint = new Point(functions.get(funIterator).getPoints().get(pointsIterator).getX() * newScale
                                         + halfCanvasSize,
                                      -functions.get(funIterator).getPoints().get(pointsIterator).getY() * newScale
                                         + halfCanvasSize);

                if (prevPoints.get(funIterator) == null) {
                    prevPoints.set(funIterator, nextPoint);
                }

                graphic.setStroke(functionDrawingColors.get(funIterator));
                graphic.strokeLine(prevPoints.get(funIterator).getX(), prevPoints.get(funIterator).getY(),
                                   nextPoint.getX(), nextPoint.getY());

                prevPoints.set(funIterator, nextPoint);
            }
        }

        drawingLock.unlock();
    }

    private void drawFunctionGraphics() {
        double functionLineWidth = 1;
        double halfCanvasSize = canvasSize / 2;
        graphic.setLineWidth(functionLineWidth);

        for (int funIterator = 0; funIterator < functions.size(); funIterator++) {
            if ((functions.get(funIterator).getPoints().size() == 0)
                    || (functionPointsIterators.get(funIterator) == -1)
                    || ((functions.get(funIterator).getPoints().size() - 1) <= functionPointsIterators.get(funIterator))) {
                continue;
            }

            nextPoint = new Point(functions.get(funIterator).getPoints().get(functionPointsIterators.get(funIterator)).getX()
                                     * newScale + halfCanvasSize,
                                 -functions.get(funIterator).getPoints().get(functionPointsIterators.get(funIterator)).getY()
                                     * newScale + halfCanvasSize);

            if (prevPoints.get(funIterator) == null) {
                prevPoints.set(funIterator, nextPoint);
            }

            graphic.setStroke(functionDrawingColors.get(funIterator));
            graphic.strokeLine(prevPoints.get(funIterator).getX(), prevPoints.get(funIterator).getY(),
                               nextPoint.getX(), nextPoint.getY());

            prevPoints.set(funIterator, nextPoint);

            if (nextPoint.getX() > maxX) {
                maxX = nextPoint.getX();
            }
            if (nextPoint.getY() < minY) {
                minY = nextPoint.getY();
            }
        }
    }

    private void eraseFunctionGraphics() {
        drawingLock.lock();

        canvasSize = START_CANVAS_SIZE * newScale;
        updateCanvasConfig();

        for (int funIterator = 0; funIterator < prevPoints.size(); funIterator++) {
            prevPoints.set(funIterator, null);
            functionPointsIterators.set(funIterator, -1);
        }

        maxX = Function.MIN_X_DOWN_LIMIT;
        minY = Function.MAX_X_UP_LIMIT;

        graphic.setFill(Color.WHITE);
        graphic.fillRect(0,0, canvas.getWidth(), canvas.getHeight());
        updateCoordinateAxes();

        drawingLock.unlock();
    }

    public void updateFunctionIterator(Function function) {
        drawingLock.lock();

        functionPointsIterators.set(functions.indexOf(function), functionPointsIterators.get(functions.indexOf(function)) + 1);

        drawingLock.unlock();
    }

    public double getSingleScaleSegment() {
        return singleScaleSegment;
    }
}
