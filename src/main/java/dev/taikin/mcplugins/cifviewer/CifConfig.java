package dev.taikin.mcplugins.cifviewer;

import dev.taikin.miviewer.common.ElementInfo;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class CifConfig {

    public enum RenderMode {
        MODEL,
        CIRCLE,
        SYMBOL
    }

    private final List<Path> sourceDirectories;
    private final int scanDepth;
    private final double blocksPerAngstrom;
    private final double atomScale;
    private final int maxAtoms;
    private final int maxRepeat;
    private final int maxModels;
    private final double placementDistance;
    private final boolean showInfo;
    private final RenderMode renderMode;
    private final boolean sphereShadow;
    private final Map<String, int[]> atomColors;
    private final int[] defaultAtomColor;
    private final String materialsProjectApiKey;
    private final String materialsProjectEndpoint;
    private final int materialsProjectTimeoutSeconds;
    private final String codBaseUrl;
    private final int codTimeoutSeconds;
    private final int codMaxBytes;
    private final double xrdWavelength;
    private final double xrdTwoThetaMin;
    private final double xrdTwoThetaMax;
    private final double xrdStep;
    private final double xrdFwhm;
    private final int xrdMaxHkl;

    public CifConfig(FileConfiguration configuration, File dataFolder, Logger logger) {
        List<String> configuredSources = configuration.getStringList("sources");
        if (configuredSources.isEmpty()) {
            configuredSources = List.of("cif");
        }

        List<Path> sources = new ArrayList<>();
        for (String configured : configuredSources) {
            if (configured == null || configured.isBlank()) {
                continue;
            }
            String value = configured.trim();
            if (value.equals("~")) {
                value = System.getProperty("user.home");
            } else if (value.startsWith("~/")) {
                value = System.getProperty("user.home") + value.substring(1);
            }
            Path path = Path.of(value);
            if (!path.isAbsolute()) {
                path = dataFolder.toPath().resolve(path);
            }
            sources.add(path.normalize().toAbsolutePath());
        }
        this.sourceDirectories = List.copyOf(sources);

        this.scanDepth = clamp(configuration.getInt("scan-depth", 4), 1, 16);
        this.blocksPerAngstrom = clamp(configuration.getDouble("blocks-per-angstrom", 2.0), 0.1, 10.0);
        this.atomScale = clamp(configuration.getDouble("atom-scale", 2.0), 0.2, 6.0);
        this.maxAtoms = clamp(configuration.getInt("max-atoms", 2000), 1, 20000);
        this.maxRepeat = clamp(configuration.getInt("max-repeat", 4), 1, 8);
        this.maxModels = clamp(configuration.getInt("max-models", 8), 1, 27);
        this.placementDistance = clamp(configuration.getDouble("placement-distance", 6.0), 2.0, 32.0);
        this.showInfo = configuration.getBoolean("show-info", true);
        String renderModeName = configuration.getString("render-mode", "sphere").trim();
        this.renderMode = switch (renderModeName.toLowerCase(Locale.ROOT)) {
            case "sphere", "model" -> RenderMode.MODEL;
            case "circle" -> RenderMode.CIRCLE;
            case "symbol", "text" -> RenderMode.SYMBOL;
            default -> {
                logger.warning("Unknown render-mode '" + renderModeName + "', using sphere.");
                yield RenderMode.MODEL;
            }
        };
        this.sphereShadow = configuration.getBoolean("sphere-shadow", true);

        Map<String, int[]> colors = new LinkedHashMap<>();
        ConfigurationSection colorSection = configuration.getConfigurationSection("atom-colors");
        if (colorSection != null) {
            for (String key : colorSection.getKeys(false)) {
                String element = ElementInfo.elementOf(key, key);
                if (element == null || !ElementInfo.isElement(element)) {
                    logger.warning("Unknown element in atom-colors: " + key);
                    continue;
                }
                int[] color = parseColor(colorSection.getString(key), logger, "atom-colors." + key);
                if (color != null) {
                    colors.put(element, color);
                }
            }
        }
        this.atomColors = Map.copyOf(colors);
        this.defaultAtomColor = parseColor(configuration.getString("default-atom-color", ""),
                logger, "default-atom-color");

        String apiKey = configuration.getString("materials-project.api-key", "").trim();
        if (apiKey.isEmpty()) {
            String environmentKey = System.getenv("MP_API_KEY");
            if (environmentKey != null) {
                apiKey = environmentKey.trim();
            }
        }
        this.materialsProjectApiKey = apiKey;
        this.materialsProjectEndpoint = configuration.getString("materials-project.endpoint",
                "https://api.materialsproject.org/materials/summary/").trim();
        this.materialsProjectTimeoutSeconds = clamp(
                configuration.getInt("materials-project.timeout-seconds", 20), 1, 120);
        this.codBaseUrl = configuration.getString("cod.base-url",
                "https://www.crystallography.net/cod/").trim();
        this.codTimeoutSeconds = clamp(configuration.getInt("cod.timeout-seconds", 20), 1, 120);
        this.codMaxBytes = clamp(configuration.getInt("cod.max-bytes", 5 * 1024 * 1024), 1024, 20 * 1024 * 1024);
        this.xrdWavelength = clamp(configuration.getDouble("xrd.wavelength", 1.5406), 0.1, 10.0);
        this.xrdTwoThetaMin = clamp(configuration.getDouble("xrd.two-theta-min", 10.0), 0.1, 179.0);
        this.xrdTwoThetaMax = clamp(configuration.getDouble("xrd.two-theta-max", 90.0), 0.2, 179.9);
        this.xrdStep = clamp(configuration.getDouble("xrd.step", 0.02), 0.001, 0.2);
        this.xrdFwhm = clamp(configuration.getDouble("xrd.fwhm", 0.2), 0.01, 5.0);
        this.xrdMaxHkl = clamp(configuration.getInt("xrd.max-hkl", 40), 1, 80);
    }

    public List<Path> sourceDirectories() {
        return sourceDirectories;
    }

    public int scanDepth() {
        return scanDepth;
    }

    public double blocksPerAngstrom() {
        return blocksPerAngstrom;
    }

    public double atomScale() {
        return atomScale;
    }

    public int maxAtoms() {
        return maxAtoms;
    }

    public int maxRepeat() {
        return maxRepeat;
    }

    public int maxModels() {
        return maxModels;
    }

    public double placementDistance() {
        return placementDistance;
    }

    public boolean showInfo() {
        return showInfo;
    }

    public RenderMode renderMode() {
        return renderMode;
    }

    public boolean sphereShadow() {
        return sphereShadow;
    }

    public int[] atomColor(String element) {
        int[] color = atomColors.get(element);
        if (color != null) {
            return color;
        }
        if (defaultAtomColor != null) {
            return defaultAtomColor;
        }
        return ElementInfo.color(element);
    }

    public String materialsProjectApiKey() {
        return materialsProjectApiKey;
    }

    public String materialsProjectEndpoint() {
        return materialsProjectEndpoint;
    }

    public int materialsProjectTimeoutSeconds() {
        return materialsProjectTimeoutSeconds;
    }

    public boolean materialsProjectEnabled() {
        return !materialsProjectApiKey.isEmpty();
    }

    public String codBaseUrl() {
        return codBaseUrl;
    }

    public int codTimeoutSeconds() {
        return codTimeoutSeconds;
    }

    public int codMaxBytes() {
        return codMaxBytes;
    }

    public double xrdWavelength() {
        return xrdWavelength;
    }

    public double xrdTwoThetaMin() {
        return xrdTwoThetaMin;
    }

    public double xrdTwoThetaMax() {
        return xrdTwoThetaMax;
    }

    public double xrdStep() {
        return xrdStep;
    }

    public double xrdFwhm() {
        return xrdFwhm;
    }

    public int xrdMaxHkl() {
        return xrdMaxHkl;
    }

    private static int[] parseColor(String value, Logger logger, String key) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }

        String hex = text;
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        } else if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        if (hex.matches("[0-9a-fA-F]{6}")) {
            int rgb = Integer.parseInt(hex, 16);
            return new int[] {(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF};
        }

        NamedTextColor named = NamedTextColor.NAMES.value(text.toLowerCase(Locale.ROOT));
        if (named != null) {
            int rgb = named.value();
            return new int[] {(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF};
        }

        logger.warning("Invalid color for " + key + ": " + value);
        return null;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
