package controller;

import layout.GraphicCanvas;
import model.Function;
import model.Point;


public class LinearFunctionCalcTask implements Runnable {
    private final Function linearFunction;
    private final int calcTaskNumber;
    private final GraphicCanvas graphicCanvas;

    public LinearFunctionCalcTask(Function linearFunction, int calcTaskNumber, GraphicCanvas graphicCanvas) {
        this.linearFunction = linearFunction;
        this.calcTaskNumber = calcTaskNumber;
        this.graphicCanvas = graphicCanvas;
    }

    @Override
    public void run() {
        double a = 5;
        double b = -1;
        double step = 1;
        int sleepTime = 40;

        for (double x = linearFunction.getXDownLimit(); x <= linearFunction.getXUpLimit(); x += step) {
            linearFunction.getPoints().add(new Point(x, a*x + b));
            graphicCanvas.updateFunctionIterator(linearFunction);

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        Thread.currentThread().interrupt();
    }
}
