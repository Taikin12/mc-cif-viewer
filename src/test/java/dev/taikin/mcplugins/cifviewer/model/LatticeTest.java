package dev.taikin.mcplugins.cifviewer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LatticeTest {

    @Test
    void roundTripsFractionalCoordinatesForMonoclinicCell() {
        Lattice lattice = Lattice.fromCell(5.0, 6.0, 7.0, 90.0, 105.0, 90.0);
        double[] cartesian = lattice.toCartesian(0.2, 0.3, 0.4);
        double[] fractional = lattice.toFractional(cartesian[0], cartesian[1], cartesian[2]);

        assertEquals(0.2, fractional[0], 1.0e-9);
        assertEquals(0.3, fractional[1], 1.0e-9);
        assertEquals(0.4, fractional[2], 1.0e-9);
    }

    @Test
    void reciprocalVectorMatchesDSpacing() {
        Lattice lattice = Lattice.fromCell(4.0, 5.0, 6.0, 90.0, 100.0, 90.0);
        double[] reciprocal = lattice.reciprocalVector(1, 2, 3);
        double length = Math.sqrt(reciprocal[0] * reciprocal[0]
                + reciprocal[1] * reciprocal[1] + reciprocal[2] * reciprocal[2]);

        assertEquals(1.0 / lattice.dSpacing(1, 2, 3), length, 1.0e-9);
    }

    @Test
    void computesCubicDSpacing() {
        Lattice lattice = Lattice.fromCell(5.0, 5.0, 5.0, 90.0, 90.0, 90.0);

        assertEquals(5.0, lattice.dSpacing(1, 0, 0), 1.0e-9);
        assertEquals(5.0 / Math.sqrt(2.0), lattice.dSpacing(1, 1, 0), 1.0e-9);
        assertEquals(5.0 / Math.sqrt(3.0), lattice.dSpacing(1, 1, 1), 1.0e-9);
    }
}
