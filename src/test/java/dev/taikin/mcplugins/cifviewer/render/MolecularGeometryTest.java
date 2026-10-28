package dev.taikin.mcplugins.cifviewer.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class MolecularGeometryTest {

    @Test
    void computesDistanceInAngstrom() {
        double distance = MolecularGeometry.distance(
                new Vector(0, 0, 0), new Vector(3, 4, 0), 2.0);

        assertEquals(2.5, distance, 1.0e-9);
    }

    @Test
    void computesRightAngle() {
        double angle = MolecularGeometry.angle(
                new Vector(1, 0, 0), new Vector(0, 0, 0), new Vector(0, 1, 0));

        assertEquals(90.0, angle, 1.0e-9);
    }

    @Test
    void computesStraightAngle() {
        double angle = MolecularGeometry.angle(
                new Vector(1, 0, 0), new Vector(0, 0, 0), new Vector(-1, 0, 0));

        assertEquals(180.0, angle, 1.0e-9);
    }

    @Test
    void computesOrthogonalDihedral() {
        double angle = MolecularGeometry.dihedral(
                new Vector(0, 0, 0),
                new Vector(1, 0, 0),
                new Vector(1, 1, 0),
                new Vector(1, 1, 1));

        assertEquals(90.0, Math.abs(angle), 1.0e-9);
    }

    @Test
    void computesPlanarDihedral() {
        double angle = MolecularGeometry.dihedral(
                new Vector(0, 0, 0),
                new Vector(1, 0, 0),
                new Vector(1, 1, 0),
                new Vector(0, 1, 0));

        double absolute = Math.abs(angle);
        assertTrue(absolute < 1.0e-6 || Math.abs(absolute - 180.0) < 1.0e-6);
    }
}
