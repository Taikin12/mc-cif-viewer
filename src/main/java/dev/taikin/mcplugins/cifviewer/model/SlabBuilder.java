package dev.taikin.mcplugins.cifviewer.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Builds a finite slab with vacuum from a bulk crystal and an (hkl) surface.
 *
 * <p>The slab cell uses two in-plane lattice vectors as periodic directions and
 * a third vector along the surface normal whose length is the slab thickness
 * plus the vacuum gap. Atoms are taken from a window of {@code layers}
 * interplanar spacings and wrapped into the new cell.</p>
 */
public final class SlabBuilder {

    private static final double EPSILON = 1.0e-9;

    private record Surface(double[] u, double[] v, int maxCoefficient) {
    }

    private record Candidate(Crystal.Atom atom, double[] position) {
    }

    private SlabBuilder() {
    }

    public static Crystal build(Crystal source, int h, int k, int l, int layers, double vacuum) {
        if (h == 0 && k == 0 && l == 0) {
            throw new IllegalArgumentException("Miller 指数に 0,0,0 は使えません。");
        }
        if (layers < 1 || layers > 12) {
            throw new IllegalArgumentException("層数は 1〜12 で指定してください。");
        }
        if (!Double.isFinite(vacuum) || vacuum < 0.0) {
            throw new IllegalArgumentException("真空層は 0 Å 以上で指定してください。");
        }

        Lattice lattice = source.lattice();
        double[] normal = lattice.reciprocalVector(h, k, l);
        normalize(normal);
        double spacing = lattice.dSpacing(h, k, l);
        if (!(spacing > EPSILON)) {
            throw new IllegalArgumentException("この面の面間隔を計算できません。");
        }
        double thickness = layers * spacing;
        Surface surface = surfaceVectors(lattice, normal);

        int range = Math.max(10, Math.max(layers + 4, surface.maxCoefficient() + 2));
        double minimum = Double.POSITIVE_INFINITY;
        for (Crystal.Atom atom : source.atoms()) {
            for (int a = -range; a <= range; a++) {
                for (int b = -range; b <= range; b++) {
                    for (int c = -range; c <= range; c++) {
                        double[] position = lattice.toCartesian(
                                atom.x() + a, atom.y() + b, atom.z() + c);
                        minimum = Math.min(minimum, dot(position, normal));
                    }
                }
            }
        }
        if (!Double.isFinite(minimum)) {
            throw new IllegalArgumentException("スラブに使える原子がありません。");
        }

        double wLength = thickness + vacuum;
        double[] w = scale(normal, wLength);
        Lattice slabLattice = Lattice.fromMatrix(new double[][] {
                surface.u(), surface.v(), w
        });
        double[] reference = scale(normal, minimum);
        double shift = (vacuum / 2.0) / wLength;
        double upper = minimum + thickness - 1.0e-5 * Math.max(1.0, spacing);

        List<Candidate> candidates = new ArrayList<>();
        for (Crystal.Atom atom : source.atoms()) {
            for (int a = -range; a <= range; a++) {
                for (int b = -range; b <= range; b++) {
                    for (int c = -range; c <= range; c++) {
                        double[] position = lattice.toCartesian(
                                atom.x() + a, atom.y() + b, atom.z() + c);
                        double projection = dot(position, normal);
                        if (projection < minimum - EPSILON || projection >= upper) {
                            continue;
                        }
                        double[] g = slabLattice.toFractional(
                                position[0] - reference[0],
                                position[1] - reference[1],
                                position[2] - reference[2]);
                        double[] fractional = {
                                wrap(g[0]),
                                wrap(g[1]),
                                Math.max(0.0, Math.min(1.0, g[2] + shift))
                        };
                        candidates.add(new Candidate(atom, fractional));
                    }
                }
            }
        }

        List<Crystal.Atom> atoms = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Candidate candidate : candidates) {
            double[] f = candidate.position();
            String key = candidate.atom().element() + ":"
                    + Math.round(f[0] * 1.0e5) + ":"
                    + Math.round(f[1] * 1.0e5) + ":"
                    + Math.round(f[2] * 1.0e5);
            if (!seen.add(key)) {
                continue;
            }
            atoms.add(new Crystal.Atom(candidate.atom().element(), f[0], f[1], f[2],
                    candidate.atom().occupancy(), candidate.atom().charge(), candidate.atom().label()));
        }
        if (atoms.isEmpty()) {
            throw new IllegalArgumentException("スラブに原子が残りませんでした。層数を増やしてください。");
        }
        atoms.sort(Comparator.comparingDouble(Crystal.Atom::z)
                .thenComparingDouble(Crystal.Atom::x)
                .thenComparingDouble(Crystal.Atom::y)
                .thenComparing(Crystal.Atom::element));

        String name = String.format(Locale.ROOT, "%s (%d %d %d) %dL",
                source.name(), h, k, l, layers);
        return new Crystal(name, slabLattice, atoms);
    }

    private static Surface surfaceVectors(Lattice lattice, double[] normal) {
        for (int limit : new int[] {4, 6, 10}) {
            List<double[]> candidates = new ArrayList<>();
            List<Integer> coefficients = new ArrayList<>();
            for (int a = -limit; a <= limit; a++) {
                for (int b = -limit; b <= limit; b++) {
                    for (int c = -limit; c <= limit; c++) {
                        if (a == 0 && b == 0 && c == 0) {
                            continue;
                        }
                        double[] vector = lattice.toCartesian(a, b, c);
                        double length = length(vector);
                        if (length < EPSILON) {
                            continue;
                        }
                        if (Math.abs(dot(vector, normal)) <= 1.0e-3 * length) {
                            candidates.add(vector);
                            coefficients.add(Math.max(Math.abs(a), Math.max(Math.abs(b), Math.abs(c))));
                        }
                    }
                }
            }
            if (candidates.size() < 2) {
                continue;
            }
            Integer[] order = new Integer[candidates.size()];
            for (int index = 0; index < order.length; index++) {
                order[index] = index;
            }
            java.util.Arrays.sort(order, Comparator.comparingDouble(
                    index -> length(candidates.get(index))));
            for (int first = 0; first < order.length - 1; first++) {
                double[] u = candidates.get(order[first]);
                for (int second = first + 1; second < order.length; second++) {
                    double[] v = candidates.get(order[second]);
                    if (length(cross(u, v)) > 0.1 * length(u) * length(v)) {
                        int maxCoefficient = Math.max(coefficients.get(order[first]),
                                coefficients.get(order[second]));
                        return new Surface(u, v, maxCoefficient);
                    }
                }
            }
        }
        throw new IllegalArgumentException("面内の格子ベクトルが見つかりませんでした。");
    }

    private static double wrap(double value) {
        double wrapped = value - Math.floor(value);
        if (wrapped < 1.0e-5 || wrapped > 1.0 - 1.0e-5) {
            return 0.0;
        }
        return wrapped;
    }

    private static double[] cross(double[] u, double[] v) {
        return new double[] {
            u[1] * v[2] - u[2] * v[1],
            u[2] * v[0] - u[0] * v[2],
            u[0] * v[1] - u[1] * v[0]
        };
    }

    private static double dot(double[] u, double[] v) {
        return u[0] * v[0] + u[1] * v[1] + u[2] * v[2];
    }

    private static double length(double[] v) {
        return Math.sqrt(dot(v, v));
    }

    private static double[] scale(double[] v, double factor) {
        return new double[] {v[0] * factor, v[1] * factor, v[2] * factor};
    }

    private static void normalize(double[] vector) {
        double length = length(vector);
        if (!(length > EPSILON)) {
            throw new IllegalArgumentException("面法線を計算できません。");
        }
        vector[0] /= length;
        vector[1] /= length;
        vector[2] /= length;
    }
}
