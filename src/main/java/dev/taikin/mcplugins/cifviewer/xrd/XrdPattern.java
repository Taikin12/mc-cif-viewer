package dev.taikin.mcplugins.cifviewer.xrd;

public record XrdPattern(
        String name,
        double wavelength,
        double twoThetaMin,
        double twoThetaMax,
        double step,
        double[] twoTheta,
        double[] intensity,
        int reflections) {
}
