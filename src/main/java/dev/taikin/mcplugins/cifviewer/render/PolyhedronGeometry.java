package dev.taikin.mcplugins.cifviewer.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.util.Vector;

public final class PolyhedronGeometry {

    public record Edge(int first, int second) {
        public Edge {
            if (first > second) {
                int swap = first;
                first = second;
                second = swap;
            }
        }
    }

    private record Projected(int index, double x, double y) {
    }

    private PolyhedronGeometry() {
    }

    public static List<Edge> convexHullEdges(List<Vector> points) {
        Set<Edge> edges = new LinkedHashSet<>();
        for (List<Integer> face : convexHullFaces(points)) {
            for (int index = 0; index < face.size(); index++) {
                int first = face.get(index);
                int second = face.get((index + 1) % face.size());
                if (first != second) {
                    edges.add(new Edge(first, second));
                }
            }
        }
        return List.copyOf(edges);
    }

    public static List<List<Integer>> convexHullFaces(List<Vector> points) {
        if (points.size() < 3) {
            return List.of();
        }
        double tolerance = Math.max(1.0e-8, diameter(points) * 1.0e-6);
        List<List<Integer>> faces = new ArrayList<>();
        Set<List<Integer>> seen = new HashSet<>();

        for (int i = 0; i < points.size() - 2; i++) {
            Vector a = points.get(i);
            for (int j = i + 1; j < points.size() - 1; j++) {
                Vector ab = points.get(j).clone().subtract(a);
                for (int k = j + 1; k < points.size(); k++) {
                    Vector normal = ab.clone().crossProduct(points.get(k).clone().subtract(a));
                    if (normal.lengthSquared() < tolerance * tolerance) {
                        continue;
                    }
                    normal.normalize();
                    double offset = normal.dot(a);
                    boolean positive = false;
                    boolean negative = false;
                    for (Vector point : points) {
                        double distance = normal.dot(point) - offset;
                        positive |= distance > tolerance;
                        negative |= distance < -tolerance;
                        if (positive && negative) {
                            break;
                        }
                    }
                    if (positive && negative) {
                        continue;
                    }

                    List<Integer> coplanar = new ArrayList<>();
                    for (int index = 0; index < points.size(); index++) {
                        if (Math.abs(normal.dot(points.get(index)) - offset) <= tolerance) {
                            coplanar.add(index);
                        }
                    }
                    List<Integer> key = new ArrayList<>(coplanar);
                    key.sort(Integer::compareTo);
                    if (!seen.add(key)) {
                        continue;
                    }
                    List<Integer> boundary = faceBoundary(coplanar, points, normal, tolerance);
                    if (boundary.size() >= 3) {
                        faces.add(boundary);
                    }
                }
            }
        }
        return List.copyOf(faces);
    }

    private static List<Integer> faceBoundary(List<Integer> indices, List<Vector> points,
            Vector normal, double tolerance) {
        if (indices.size() < 3) {
            return List.of();
        }
        Vector origin = points.get(indices.getFirst());
        Vector u = null;
        for (int index : indices) {
            Vector candidate = points.get(index).clone().subtract(origin);
            if (candidate.lengthSquared() > tolerance * tolerance) {
                u = candidate.normalize();
                break;
            }
        }
        if (u == null) {
            return List.of();
        }
        Vector v = normal.clone().crossProduct(u).normalize();
        List<Projected> projected = new ArrayList<>(indices.size());
        for (int index : indices) {
            Vector relative = points.get(index).clone().subtract(origin);
            projected.add(new Projected(index, relative.dot(u), relative.dot(v)));
        }
        projected.sort(Comparator.comparingDouble(Projected::x).thenComparingDouble(Projected::y));

        List<Projected> lower = new ArrayList<>();
        for (Projected point : projected) {
            while (lower.size() >= 2 && cross(lower.get(lower.size() - 2), lower.getLast(), point) <= 1.0e-12) {
                lower.removeLast();
            }
            lower.add(point);
        }
        List<Projected> upper = new ArrayList<>();
        for (int index = projected.size() - 1; index >= 0; index--) {
            Projected point = projected.get(index);
            while (upper.size() >= 2 && cross(upper.get(upper.size() - 2), upper.getLast(), point) <= 1.0e-12) {
                upper.removeLast();
            }
            upper.add(point);
        }
        lower.removeLast();
        upper.removeLast();
        lower.addAll(upper);
        List<Integer> boundary = new ArrayList<>(lower.size());
        for (Projected point : lower) {
            if (boundary.isEmpty() || boundary.getLast() != point.index()) {
                boundary.add(point.index());
            }
        }
        if (boundary.size() > 1 && boundary.getFirst().equals(boundary.getLast())) {
            boundary.removeLast();
        }
        return boundary;
    }

    private static double cross(Projected a, Projected b, Projected c) {
        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
    }

    private static double diameter(List<Vector> points) {
        double maximum = 0.0;
        for (int i = 0; i < points.size(); i++) {
            for (int j = i + 1; j < points.size(); j++) {
                maximum = Math.max(maximum, points.get(i).distance(points.get(j)));
            }
        }
        return maximum;
    }
}
