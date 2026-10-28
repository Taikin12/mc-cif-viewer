package dev.taikin.mcplugins.cifviewer.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class PolyhedronGeometryTest {

    @Test
    void findsTetrahedronEdges() {
        List<Vector> points = List.of(
                new Vector(1, 1, 1),
                new Vector(1, -1, -1),
                new Vector(-1, 1, -1),
                new Vector(-1, -1, 1));

        assertEquals(6, PolyhedronGeometry.convexHullEdges(points).size());
    }

    @Test
    void findsOctahedronEdges() {
        List<Vector> points = List.of(
                new Vector(1, 0, 0), new Vector(-1, 0, 0),
                new Vector(0, 1, 0), new Vector(0, -1, 0),
                new Vector(0, 0, 1), new Vector(0, 0, -1));

        assertEquals(12, PolyhedronGeometry.convexHullEdges(points).size());
    }

    @Test
    void omitsCubeFaceDiagonals() {
        List<Vector> points = List.of(
                new Vector(-1, -1, -1), new Vector(-1, -1, 1),
                new Vector(-1, 1, -1), new Vector(-1, 1, 1),
                new Vector(1, -1, -1), new Vector(1, -1, 1),
                new Vector(1, 1, -1), new Vector(1, 1, 1));

        assertEquals(12, PolyhedronGeometry.convexHullEdges(points).size());
    }

    @Test
    void handlesPlanarSquare() {
        List<Vector> points = List.of(
                new Vector(-1, -1, 0), new Vector(-1, 1, 0),
                new Vector(1, -1, 0), new Vector(1, 1, 0));

        assertEquals(4, PolyhedronGeometry.convexHullEdges(points).size());
    }
}
