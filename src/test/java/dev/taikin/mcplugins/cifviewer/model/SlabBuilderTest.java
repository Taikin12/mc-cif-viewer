package dev.taikin.mcplugins.cifviewer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.taikin.mcplugins.cifviewer.cif.CifParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlabBuilderTest {

    @Test
    void buildsTwoLayerSimpleCubicSlab() {
        Lattice lattice = Lattice.fromCell(4.0, 4.0, 4.0, 90.0, 90.0, 90.0);
        Crystal bulk = new Crystal("sc", lattice,
                List.of(new Crystal.Atom("Fe", 0.0, 0.0, 0.0, 1.0, 0, "Fe1")));

        Crystal slab = SlabBuilder.build(bulk, 1, 0, 0, 2, 6.0);

        assertEquals(2, slab.atoms().size());
        assertEquals(4.0, slab.lattice().a(), 1.0e-6);
        assertEquals(4.0, slab.lattice().b(), 1.0e-6);
        assertEquals(14.0, slab.lattice().c(), 1.0e-6);
        for (Crystal.Atom atom : slab.atoms()) {
            assertTrue(atom.z() > 0.2 && atom.z() < 0.6);
        }
    }

    @Test
    void buildsNaClSlabsFromSample() throws Exception {
        String text = Files.readString(Path.of("samples/NaCl.cif"));
        Crystal bulk = new CifParser().parse("NaCl", text);

        for (int[] hkl : new int[][] {{1, 0, 0}, {1, 1, 1}}) {
            Crystal slab = SlabBuilder.build(bulk, hkl[0], hkl[1], hkl[2], 3, 10.0);
            assertTrue(slab.atoms().size() >= 3, "atoms for " + java.util.Arrays.toString(hkl));
            assertTrue(slab.lattice().c() > 10.0);
            for (Crystal.Atom atom : slab.atoms()) {
                assertTrue(atom.x() >= 0.0 && atom.x() < 1.0);
                assertTrue(atom.y() >= 0.0 && atom.y() < 1.0);
                assertTrue(atom.z() > 0.0 && atom.z() < 1.0);
            }
        }
    }

    @Test
    void rejectsInvalidArguments() {
        Lattice lattice = Lattice.fromCell(4.0, 4.0, 4.0, 90.0, 90.0, 90.0);
        Crystal bulk = new Crystal("sc", lattice,
                List.of(new Crystal.Atom("Fe", 0.0, 0.0, 0.0, 1.0, 0, "Fe1")));

        assertThrows(IllegalArgumentException.class,
                () -> SlabBuilder.build(bulk, 0, 0, 0, 2, 6.0));
        assertThrows(IllegalArgumentException.class,
                () -> SlabBuilder.build(bulk, 1, 0, 0, 0, 6.0));
    }
}
