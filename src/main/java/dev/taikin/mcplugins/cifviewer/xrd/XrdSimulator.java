package dev.taikin.mcplugins.cifviewer.xrd;

import dev.taikin.mcplugins.cifviewer.model.Crystal;
import dev.taikin.miviewer.common.ElementInfo;
import dev.taikin.mcplugins.cifviewer.model.Lattice;
import java.util.HashMap;
import java.util.Map;

public final class XrdSimulator {

    private static final double FWHM_TO_SIGMA = 1.0 / (2.0 * Math.sqrt(2.0 * Math.log(2.0)));

    private XrdSimulator() {
    }

    public static XrdPattern simulate(Crystal crystal, double wavelength, double twoThetaMin,
            double twoThetaMax, double step, double fwhm, int maxHkl) {
        if (!(wavelength > 0.0) || !Double.isFinite(wavelength)) {
            throw new IllegalArgumentException("Wavelength must be positive.");
        }
        if (!(twoThetaMin > 0.0) || !(twoThetaMax > twoThetaMin) || twoThetaMax >= 180.0) {
            throw new IllegalArgumentException("Invalid 2theta range.");
        }
        if (!(step > 0.0) || !(fwhm > 0.0)) {
            throw new IllegalArgumentException("Step and FWHM must be positive.");
        }

        Lattice lattice = crystal.lattice();
        double minTheta = Math.toRadians(twoThetaMin / 2.0);
        double maxTheta = Math.toRadians(twoThetaMax / 2.0);
        double dMax = wavelength / (2.0 * Math.sin(minTheta));
        double dMin = wavelength / (2.0 * Math.sin(maxTheta));
        int hMax = Math.min(maxHkl, (int) Math.ceil(lattice.a() / dMin) + 2);
        int kMax = Math.min(maxHkl, (int) Math.ceil(lattice.b() / dMin) + 2);
        int lMax = Math.min(maxHkl, (int) Math.ceil(lattice.c() / dMin) + 2);

        Map<Long, Double> peaks = new HashMap<>();
        int reflections = 0;
        for (int h = -hMax; h <= hMax; h++) {
            for (int k = -kMax; k <= kMax; k++) {
                for (int l = -lMax; l <= lMax; l++) {
                    if (h == 0 && k == 0 && l == 0) {
                        continue;
                    }
                    double d = lattice.dSpacing(h, k, l);
                    if (d <= 0.0 || d > dMax || d < dMin) {
                        continue;
                    }
                    double sinTheta = wavelength / (2.0 * d);
                    if (sinTheta <= 0.0 || sinTheta >= 1.0) {
                        continue;
                    }
                    double twoTheta = 2.0 * Math.toDegrees(Math.asin(sinTheta));
                    if (twoTheta < twoThetaMin || twoTheta > twoThetaMax) {
                        continue;
                    }
                    double theta = Math.toRadians(twoTheta / 2.0);
                    double sin = Math.sin(theta);
                    double cos = Math.cos(theta);
                    double lp = (1.0 + Math.cos(2.0 * theta) * Math.cos(2.0 * theta))
                            / (sin * sin * cos);
                    double real = 0.0;
                    double imaginary = 0.0;
                    for (Crystal.Atom atom : crystal.atoms()) {
                        double f = ElementInfo.atomicNumber(atom.element());
                        if (f <= 0.0) {
                            f = 1.0;
                        }
                        double phase = 2.0 * Math.PI * (h * atom.x() + k * atom.y() + l * atom.z());
                        double weight = f * atom.occupancy();
                        real += weight * Math.cos(phase);
                        imaginary += weight * Math.sin(phase);
                    }
                    double structureFactor = real * real + imaginary * imaginary;
                    if (structureFactor < 1.0e-9) {
                        continue;
                    }
                    long key = Math.round(twoTheta * 1000.0);
                    peaks.merge(key, structureFactor * lp, Double::sum);
                    reflections++;
                }
            }
        }

        int points = Math.max(2, (int) Math.ceil((twoThetaMax - twoThetaMin) / step) + 1);
        double[] twoTheta = new double[points];
        double[] intensity = new double[points];
        for (int index = 0; index < points; index++) {
            twoTheta[index] = twoThetaMin + index * step;
        }

        double sigma = fwhm * FWHM_TO_SIGMA;
        double cutoff = sigma * 4.0;
        for (Map.Entry<Long, Double> peak : peaks.entrySet()) {
            double center = peak.getKey() / 1000.0;
            double height = peak.getValue();
            int from = Math.max(0, (int) Math.floor((center - cutoff - twoThetaMin) / step));
            int to = Math.min(points - 1, (int) Math.ceil((center + cutoff - twoThetaMin) / step));
            for (int index = from; index <= to; index++) {
                double delta = (twoTheta[index] - center) / sigma;
                intensity[index] += height * Math.exp(-0.5 * delta * delta);
            }
        }

        double maximum = 0.0;
        for (double value : intensity) {
            maximum = Math.max(maximum, value);
        }
        if (maximum > 0.0) {
            for (int index = 0; index < intensity.length; index++) {
                intensity[index] /= maximum;
            }
        }

        return new XrdPattern(crystal.name(), wavelength, twoThetaMin, twoThetaMax, step,
                twoTheta, intensity, reflections);
    }
}
