package dev.taikin.mcplugins.cifviewer.render;

import java.util.ArrayList;
import java.util.List;

/**
 * Clips the (hkl) plane through the centre of a supercell to the cell box.
 * Coordinates are fractional; an empty result means the plane misses the box.
 */
public final class MillerPlane {

    private static final double EPSILON = 1.0e-9;

    private MillerPlane() {
    }

    public static List<double[]> polygon(int h, int k, int l, double sizeX, double sizeY, double sizeZ) {
        if (h == 0 && k == 0 && l == 0) {
            return List.of();
        }
        if (!(sizeX > 0.0) || !(sizeY > 0.0) || !(sizeZ > 0.0)) {
            return List.of();
        }
        double[] normal = {h, k, l};
        double[] center = {sizeX / 2.0, sizeY / 2.0, sizeZ / 2.0};
        double[] u = perpendicular(normal);
        double[] v = cross(normal, u);
        normalize(u);
        normalize(v);

        double radius = 4.0 * Math.sqrt(sizeX * sizeX + sizeY * sizeY + sizeZ * sizeZ) + 1.0;
        List<double[]> polygon = new ArrayList<>(4);
        polygon.add(point(center, u, v, 1.0, 1.0, radius));
        polygon.add(point(center, u, v, -1.0, 1.0, radius));
        polygon.add(point(center, u, v, -1.0, -1.0, radius));
        polygon.add(point(center, u, v, 1.0, -1.0, radius));

        polygon = clip(polygon, 0, 0.0, sizeX);
        if (!polygon.isEmpty()) {
            polygon = clip(polygon, 1, 0.0, sizeY);
        }
        if (!polygon.isEmpty()) {
            polygon = clip(polygon, 2, 0.0, sizeZ);
        }
        return deduplicate(polygon);
    }

    private static double[] perpendicular(double[] normal) {
        double ax = Math.abs(normal[0]);
        double ay = Math.abs(normal[1]);
        double az = Math.abs(normal[2]);
        if (ax <= ay && ax <= az) {
            return new double[] {0.0, -normal[2], normal[1]};
        }
        if (ay <= az) {
            return new double[] {-normal[2], 0.0, normal[0]};
        }
        return new double[] {-normal[1], normal[0], 0.0};
    }

    private static List<double[]> clip(List<double[]> polygon, int axis, double minimum, double maximum) {
        List<double[]> result = clipHalf(polygon, axis, minimum, true);
        if (result.isEmpty()) {
            return result;
        }
        return clipHalf(result, axis, maximum, false);
    }

    private static List<double[]> clipHalf(List<double[]> polygon, int axis, double bound,
            boolean keepGreater) {
        List<double[]> result = new ArrayList<>();
        double[] previous = polygon.getLast();
        double previousValue = previous[axis];
        boolean previousInside = keepGreater ? previousValue >= bound : previousValue <= bound;
        for (double[] current : polygon) {
            double currentValue = current[axis];
            boolean currentInside = keepGreater ? currentValue >= bound : currentValue <= bound;
            if (currentInside != previousInside) {
                double denominator = currentValue - previousValue;
                if (Math.abs(denominator) > EPSILON) {
                    double t = (bound - previousValue) / denominator;
                    result.add(lerp(previous, current, t));
                }
            }
            if (currentInside) {
                result.add(current);
            }
            previous = current;
            previousValue = currentValue;
            previousInside = currentInside;
        }
        return result;
    }

    private static List<double[]> deduplicate(List<double[]> polygon) {
        List<double[]> result = new ArrayList<>(polygon.size());
        for (double[] point : polygon) {
            if (result.isEmpty() || distance(result.getLast(), point) > EPSILON * 100.0) {
                result.add(point);
            }
        }
        if (result.size() > 1 && distance(result.getFirst(), result.getLast()) <= EPSILON * 100.0) {
            result.removeLast();
        }
        return result;
    }

    private static double[] point(double[] center, double[] u, double[] v,
            double s, double t, double radius) {
        return new double[] {
            center[0] + radius * (s * u[0] + t * v[0]),
            center[1] + radius * (s * u[1] + t * v[1]),
            center[2] + radius * (s * u[2] + t * v[2])
        };
    }

    private static double[] lerp(double[] first, double[] second, double t) {
        return new double[] {
            first[0] + (second[0] - first[0]) * t,
            first[1] + (second[1] - first[1]) * t,
            first[2] + (second[2] - first[2]) * t
        };
    }

    private static double distance(double[] first, double[] second) {
        double dx = first[0] - second[0];
        double dy = first[1] - second[1];
        double dz = first[2] - second[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double[] cross(double[] u, double[] v) {
        return new double[] {
            u[1] * v[2] - u[2] * v[1],
            u[2] * v[0] - u[0] * v[2],
            u[0] * v[1] - u[1] * v[0]
        };
    }

    private static void normalize(double[] vector) {
        double length = Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);
        if (length > EPSILON) {
            vector[0] /= length;
            vector[1] /= length;
            vector[2] /= length;
        }
    }
}
