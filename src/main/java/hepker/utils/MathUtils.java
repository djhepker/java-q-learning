package hepker.utils;

public class MathUtils {

    private MathUtils() {

    }

    /**
     * Sigmoid rounding function which returns a result (0.0,max). Makes data
     * more readable when working with large datasets. X values above 700 or below -700 risk data overflow due
     * to exponential function
     *
     * @param x The value we will smooth on a scale of (0.0,max)
     * @param xNaught The value which will be the center of the graph. Return value will be 0.5
     *                when x = xNaught
     * @param max The upper asymptote of the return value
     * @param k Scalar rate of change controlling the steepness of the curve.
     *          A higher value of k makes the sigmoid steeper and the transition around xNaught faster,
     *          resulting in a more "step-like" curve. A smaller k value flattens the curve,
     *          making the change more gradual and the graph appear wider (stretched).
     * @return A value (0.0,max) that accurately represents the input
     */
    public static double getSigmoid(double x, double xNaught, double max, double k) {
        return max / (1 + Math.exp(-k * (x - xNaught)));
    }
}
