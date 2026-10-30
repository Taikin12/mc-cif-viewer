package dev.taikin.mcplugins.cifviewer.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MillerPlaneTest {

    @Test
    void clipsCubicOneHundredPlaneToCell() {
        List<double[]> polygon = MillerPlane.polygon(1, 0, 0, 1.0, 1.0, 1.0);

        assertEquals(4, polygon.size());
        for (double[] point : polygon) {
            assertEquals(0.5, point[0], 1.0e-9);
            assertTrue(point[1] >= -1.0e-9 && point[1] <= 1.0 + 1.0e-9);
            assertTrue(point[2] >= -1.0e-9 && point[2] <= 1.0 + 1.0e-9);
        }
    }

    @Test
    void clipsOneOneOnePlaneThroughCellCentre() {
        List<double[]> polygon = MillerPlane.polygon(1, 1, 1, 1.0, 1.0, 1.0);

        assertEquals(6, polygon.size());
        for (double[] point : polygon) {
            assertEquals(1.5, point[0] + point[1] + point[2], 1.0e-9);
        }
    }

    @Test
    void clipsZeroIndicesToSupercell() {
        List<double[]> polygon = MillerPlane.polygon(0, 0, 1, 2.0, 3.0, 2.0);

        assertFalse(polygon.isEmpty());
        for (double[] point : polygon) {
            assertEquals(1.0, point[2], 1.0e-9);
            assertTrue(point[0] >= -1.0e-9 && point[0] <= 2.0 + 1.0e-9);
            assertTrue(point[1] >= -1.0e-9 && point[1] <= 3.0 + 1.0e-9);
        }
    }

    @Test
    void rejectsZeroMillerIndices() {
        assertTrue(MillerPlane.polygon(0, 0, 0, 1.0, 1.0, 1.0).isEmpty());
    }
}
