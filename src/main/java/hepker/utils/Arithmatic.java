package hepker.utils;

/**
 * Utility class containing functions for AI related Maths
 */
public final class Arithmatic {

    /**
     * Prevents class instantiation
     */
    private Arithmatic() {

    }

    /**
     * Sigmoid rounding function which returns a result (0.0,max). Makes data
     * more readable when working with large datasets
     *
     * @param x The value we will smooth on a scale of (0.0,max)
     * @param xNaught The value which will be the center of the graph. Return value will be 0.5
     *                when x = xNaught
     * @param max The upper asymptote of the return value
     * @param k Scalar rate of change of the slope. A higher steepness (k) makes the rate of change slower,
     *          and the resulting graph appears wider (stretched). As (k) becomes smaller, function becomes
     *          more sensitive to lower negative inputs
     * @return A value (0.0,max) that accurately represents the input
     */
    public static double getSigmoid(double x, double xNaught, double max, double k) {
        return max / (1 + Math.exp(-k * (x - xNaught)));
    }

    /**
     * Returns Euclidian distance between two 2D points
     *
     * @param xNaught starting x-coordinate
     * @param xF final x-coordinate
     * @param yNaught starting y-coordinate
     * @param yF final y-coordinate
     * @return a double representing the distance between two 2D points
     */
    public static double getDistance(double xNaught, double xF, double yNaught, double yF) {
        return Math.sqrt(Math.pow(xF - xNaught, 2) + Math.pow(yF - yNaught, 2));
    }
}
