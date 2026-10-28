package dev.taikin.mcplugins.cifviewer.render;

import org.bukkit.util.Vector;

public final class MolecularGeometry {

    private MolecularGeometry() {
    }

    public static double distance(Vector first, Vector second, double blocksPerAngstrom) {
        if (!(blocksPerAngstrom > 0.0)) {
            return 0.0;
        }
        return first.distance(second) / blocksPerAngstrom;
    }

    public static double angle(Vector first, Vector center, Vector second) {
        Vector a = first.clone().subtract(center);
        Vector b = second.clone().subtract(center);
        double denominator = a.length() * b.length();
        if (denominator < 1.0e-9) {
            return 0.0;
        }
        return Math.toDegrees(Math.acos(clamp(a.dot(b) / denominator)));
    }

    public static double dihedral(Vector first, Vector second, Vector third, Vector fourth) {
        Vector b1 = second.clone().subtract(first);
        Vector b2 = third.clone().subtract(second);
        Vector b3 = fourth.clone().subtract(third);
        Vector n1 = b1.clone().crossProduct(b2);
        Vector n2 = b2.clone().crossProduct(b3);
        double x = n1.dot(n2);
        double y = n1.clone().crossProduct(n2).dot(b2.clone().normalize());
        return Math.toDegrees(Math.atan2(y, x));
    }

    private static double clamp(double value) {
        return Math.max(-1.0, Math.min(1.0, value));
    }
}
