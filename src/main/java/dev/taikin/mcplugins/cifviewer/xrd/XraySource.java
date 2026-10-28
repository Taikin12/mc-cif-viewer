package dev.taikin.mcplugins.cifviewer.xrd;

import java.util.Locale;
import java.util.Map;

public final class XraySource {

    private static final Map<String, Double> K_ALPHA1 = Map.of(
            "CR", 2.28970,
            "FE", 1.93604,
            "CO", 1.78897,
            "NI", 1.65790,
            "CU", 1.54056,
            "MO", 0.70930,
            "AG", 0.55941,
            "W", 0.20901);

    private XraySource() {
    }

    public static double wavelength(String source) {
        String value = source == null ? "" : source.trim();
        try {
            double wavelength = Double.parseDouble(value);
            if (Double.isFinite(wavelength) && wavelength > 0.0) {
                return wavelength;
            }
        } catch (NumberFormatException ignored) {
        }

        String normalized = value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (normalized.length() >= 2) {
            Double wavelength = K_ALPHA1.get(normalized.substring(0, 2));
            if (wavelength != null) {
                return wavelength;
            }
        }
        Double wavelength = K_ALPHA1.get(normalized);
        if (wavelength != null) {
            return wavelength;
        }
        throw new IllegalArgumentException("Unknown X-ray source '" + source
                + "'. Use Cr, Fe, Co, Ni, Cu, Mo, Ag, W, or a wavelength in A.");
    }
}
