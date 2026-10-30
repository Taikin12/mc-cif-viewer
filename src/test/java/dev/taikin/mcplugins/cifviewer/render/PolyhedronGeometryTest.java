package dev.taikin.mcplugins.cifviewer.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class PolyhedronGeometryTest {

    @Test
    void findsTetrahedronFaces() {
        List<Vector> points = List.of(
                new Vector(1, 1, 1),
                new Vector(1, -1, -1),
                new Vector(-1, 1, -1),
                new Vector(-1, -1, 1));

        List<List<Integer>> faces = PolyhedronGeometry.convexHullFaces(points);
        assertEquals(4, faces.size());
        assertEquals(List.of(3, 3, 3, 3), faces.stream().map(List::size).toList());
    }

    @Test
    void findsOctahedronFaces() {
        List<Vector> points = List.of(
                new Vector(1, 0, 0), new Vector(-1, 0, 0),
                new Vector(0, 1, 0), new Vector(0, -1, 0),
                new Vector(0, 0, 1), new Vector(0, 0, -1));

        assertEquals(8, PolyhedronGeometry.convexHullFaces(points).size());
    }

    @Test
    void findsCubeFacesWithoutDiagonals() {
        List<Vector> points = List.of(
                new Vector(-1, -1, -1), new Vector(-1, -1, 1),
                new Vector(-1, 1, -1), new Vector(-1, 1, 1),
                new Vector(1, -1, -1), new Vector(1, -1, 1),
                new Vector(1, 1, -1), new Vector(1, 1, 1));

        List<List<Integer>> faces = PolyhedronGeometry.convexHullFaces(points);
        assertEquals(6, faces.size());
        assertEquals(List.of(4, 4, 4, 4, 4, 4), faces.stream().map(List::size).toList());
    }

    @Test
    void findsPlanarSquareFace() {
        List<Vector> points = List.of(
                new Vector(-1, -1, 0), new Vector(-1, 1, 0),
                new Vector(1, -1, 0), new Vector(1, 1, 0));

        List<List<Integer>> faces = PolyhedronGeometry.convexHullFaces(points);
        assertEquals(1, faces.size());
        assertEquals(4, faces.getFirst().size());
    }

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
