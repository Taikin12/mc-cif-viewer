package dev.taikin.mcplugins.cifviewer.xrd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.taikin.mcplugins.cifviewer.model.Crystal;
import dev.taikin.mcplugins.cifviewer.model.Lattice;
import java.util.List;
import org.junit.jupiter.api.Test;

class XrdSimulatorTest {

    @Test
    void findsFirstCubicPeak() {
        Lattice lattice = Lattice.fromCell(4.0, 4.0, 4.0, 90.0, 90.0, 90.0);
        Crystal crystal = new Crystal("cubic", lattice, List.of(
                new Crystal.Atom("Po", 0.0, 0.0, 0.0, 1.0, 0, "Po1")));

        XrdPattern pattern = XrdSimulator.simulate(crystal, 1.5406, 10.0, 90.0, 0.02, 0.2, 10);

        int maximum = 0;
        for (int index = 1; index < pattern.intensity().length - 1; index++) {
            if (pattern.intensity()[index] > pattern.intensity()[maximum]) {
                maximum = index;
            }
        }
        double expected = 2.0 * Math.toDegrees(Math.asin(1.5406 / (2.0 * 4.0)));
        assertEquals(expected, pattern.twoTheta()[maximum], 0.06);
        assertTrue(pattern.reflections() > 0);
    }
}
