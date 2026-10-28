package dev.taikin.mcplugins.cifviewer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LatticeTest {

    @Test
    void computesCubicDSpacing() {
        Lattice lattice = Lattice.fromCell(5.0, 5.0, 5.0, 90.0, 90.0, 90.0);

        assertEquals(5.0, lattice.dSpacing(1, 0, 0), 1.0e-9);
        assertEquals(5.0 / Math.sqrt(2.0), lattice.dSpacing(1, 1, 0), 1.0e-9);
        assertEquals(5.0 / Math.sqrt(3.0), lattice.dSpacing(1, 1, 1), 1.0e-9);
    }
}
