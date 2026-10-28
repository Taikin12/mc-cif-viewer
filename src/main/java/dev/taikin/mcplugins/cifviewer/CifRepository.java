package dev.taikin.mcplugins.cifviewer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class CifRepository {

    private final CifViewerPlugin plugin;
    private final Map<String, Path> files = new LinkedHashMap<>();
    private final List<Path> roots = new ArrayList<>();

    public CifRepository(CifViewerPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        files.clear();
        roots.clear();

        CifConfig config = plugin.config();
        for (Path source : config.sourceDirectories()) {
            if (Files.isRegularFile(source)) {
                roots.add(source);
                if (isCif(source)) {
                    files.putIfAbsent(source.getFileName().toString(), source);
                }
                continue;
            }
            if (!Files.isDirectory(source)) {
                plugin.getLogger().warning("CIF source does not exist or is not a directory: " + source);
                continue;
            }
            roots.add(source);
            try (Stream<Path> stream = Files.walk(source, config.scanDepth())) {
                stream.filter(Files::isRegularFile)
                        .filter(this::isCif)
                        .sorted()
                        .forEach(path -> files.putIfAbsent(key(source, path), path));
            } catch (IOException e) {
                plugin.getLogger().warning("Could not scan CIF source " + source + ": " + e.getMessage());
            }
        }
    }

    public List<String> keys() {
        return files.keySet().stream().sorted().toList();
    }

    public Path resolve(String name) {
        String normalized = name.trim().replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }

        Path exact = files.get(normalized);
        if (exact != null) {
            return exact;
        }

        String baseName = normalized.contains("/")
                ? normalized.substring(normalized.lastIndexOf('/') + 1)
                : normalized;
        List<Path> matches = files.entrySet().stream()
                .filter(entry -> entry.getKey().equals(baseName) || entry.getKey().endsWith("/" + baseName))
                .map(Map.Entry::getValue)
                .distinct()
                .toList();
        if (matches.size() == 1) {
            return matches.get(0);
        }
        if (matches.size() > 1) {
            return null;
        }

        try {
            Path absolute = Path.of(name).toAbsolutePath().normalize();
            if (!isCif(absolute) || !Files.isRegularFile(absolute)) {
                return null;
            }
            for (Path root : roots) {
                if (Files.isRegularFile(root) ? absolute.equals(root) : absolute.startsWith(root)) {
                    return absolute;
                }
            }
        } catch (InvalidPathException e) {
            return null;
        }
        return null;
    }

    private String key(Path root, Path file) {
        return root.relativize(file).toString().replace(File.separatorChar, '/');
    }

    private boolean isCif(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".cif");
    }
}
