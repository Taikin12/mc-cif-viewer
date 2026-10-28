package dev.taikin.mcplugins.cifviewer.model;

public final class Lattice {

    private static final double EPSILON = 1.0e-9;

    private final double a;
    private final double b;
    private final double c;
    private final double alpha;
    private final double beta;
    private final double gamma;
    private final double[][] matrix;
    private final double[][] inverse;

    private Lattice(double a, double b, double c, double alpha, double beta, double gamma, double[][] matrix) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
        this.matrix = matrix;
        this.inverse = invert(matrix);
    }

    public static Lattice fromCell(double a, double b, double c, double alpha, double beta, double gamma) {
        if (!(a > 0) || !(b > 0) || !(c > 0)
                || !Double.isFinite(alpha) || !Double.isFinite(beta) || !Double.isFinite(gamma)) {
            throw new IllegalArgumentException("Cell lengths must be positive.");
        }
        double ar = Math.toRadians(alpha);
        double br = Math.toRadians(beta);
        double gr = Math.toRadians(gamma);
        double cosAlpha = Math.cos(ar);
        double cosBeta = Math.cos(br);
        double cosGamma = Math.cos(gr);
        double sinGamma = Math.sin(gr);
        if (Math.abs(sinGamma) < EPSILON) {
            throw new IllegalArgumentException("Cell angle gamma is invalid.");
        }
        double cy = c * (cosAlpha - cosBeta * cosGamma) / sinGamma;
        double cz2 = c * c - (c * cosBeta) * (c * cosBeta) - cy * cy;
        double cz = Math.sqrt(Math.max(0.0, cz2));
        double[][] m = {
            {a, 0.0, 0.0},
            {b * cosGamma, b * sinGamma, 0.0},
            {c * cosBeta, cy, cz}
        };
        return new Lattice(a, b, c, alpha, beta, gamma, m);
    }

    public static Lattice fromMatrix(double[][] matrix) {
        if (matrix == null || matrix.length != 3) {
            throw new IllegalArgumentException("Lattice matrix must have three rows.");
        }
        for (double[] row : matrix) {
            if (row == null || row.length != 3) {
                throw new IllegalArgumentException("Lattice matrix must have three columns.");
            }
        }
        double[][] m = {
            {matrix[0][0], matrix[0][1], matrix[0][2]},
            {matrix[1][0], matrix[1][1], matrix[1][2]},
            {matrix[2][0], matrix[2][1], matrix[2][2]}
        };
        for (double[] row : m) {
            for (double value : row) {
                if (!Double.isFinite(value)) {
                    throw new IllegalArgumentException("Lattice matrix contains a non-finite value.");
                }
            }
        }
        double a = length(m[0]);
        double b = length(m[1]);
        double c = length(m[2]);
        if (!(a > 0) || !(b > 0) || !(c > 0)) {
            throw new IllegalArgumentException("Lattice vectors must be non-zero.");
        }
        double alpha = Math.toDegrees(Math.acos(clamp(dot(m[1], m[2]) / (b * c))));
        double beta = Math.toDegrees(Math.acos(clamp(dot(m[0], m[2]) / (a * c))));
        double gamma = Math.toDegrees(Math.acos(clamp(dot(m[0], m[1]) / (a * b))));
        return new Lattice(a, b, c, alpha, beta, gamma, m);
    }

    public double a() {
        return a;
    }

    public double b() {
        return b;
    }

    public double c() {
        return c;
    }

    public double alpha() {
        return alpha;
    }

    public double beta() {
        return beta;
    }

    public double gamma() {
        return gamma;
    }

    public double[] toCartesian(double fx, double fy, double fz) {
        return new double[] {
            fx * matrix[0][0] + fy * matrix[1][0] + fz * matrix[2][0],
            fx * matrix[0][1] + fy * matrix[1][1] + fz * matrix[2][1],
            fx * matrix[0][2] + fy * matrix[1][2] + fz * matrix[2][2]
        };
    }

    public double[] toFractional(double x, double y, double z) {
        return new double[] {
            inverse[0][0] * x + inverse[0][1] * y + inverse[0][2] * z,
            inverse[1][0] * x + inverse[1][1] * y + inverse[1][2] * z,
            inverse[2][0] * x + inverse[2][1] * y + inverse[2][2] * z
        };
    }

    private static double[][] invert(double[][] m) {
        double det = determinant(m);
        if (Math.abs(det) < EPSILON) {
            throw new IllegalArgumentException("Lattice matrix is singular.");
        }
        double inv = 1.0 / det;
        return new double[][] {
            {
                (m[1][1] * m[2][2] - m[1][2] * m[2][1]) * inv,
                (m[0][2] * m[2][1] - m[0][1] * m[2][2]) * inv,
                (m[0][1] * m[1][2] - m[0][2] * m[1][1]) * inv
            },
            {
                (m[1][2] * m[2][0] - m[1][0] * m[2][2]) * inv,
                (m[0][0] * m[2][2] - m[0][2] * m[2][0]) * inv,
                (m[0][2] * m[1][0] - m[0][0] * m[1][2]) * inv
            },
            {
                (m[1][0] * m[2][1] - m[1][1] * m[2][0]) * inv,
                (m[0][1] * m[2][0] - m[0][0] * m[2][1]) * inv,
                (m[0][0] * m[1][1] - m[0][1] * m[1][0]) * inv
            }
        };
    }

    private static double determinant(double[][] m) {
        return m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1])
                - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
                + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
    }

    private static double length(double[] v) {
        return Math.sqrt(dot(v, v));
    }

    private static double dot(double[] u, double[] v) {
        return u[0] * v[0] + u[1] * v[1] + u[2] * v[2];
    }

    private static double clamp(double value) {
        return Math.max(-1.0, Math.min(1.0, value));
    }
}
