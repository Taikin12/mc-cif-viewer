package dev.taikin.mcplugins.cifviewer.render;

import dev.taikin.mcplugins.cifviewer.model.Crystal;
import dev.taikin.mcplugins.cifviewer.model.Lattice;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ModelStore {

    public record LoadResult(ModelRegistry registry, boolean migrated) {
    }

    private ModelStore() {
    }

    public static void save(Path file, ModelRegistry registry) throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("version", 2);
        configuration.set("next-id", registry.nextId());
        ConfigurationSection modelsSection = configuration.createSection("models");
        for (ViewSession session : registry.models()) {
            ConfigurationSection section = modelsSection.createSection(Integer.toString(session.id()));
            writeSession(section, session);
        }
        ConfigurationSection activeSection = configuration.createSection("active");
        for (Map.Entry<UUID, Integer> entry : registry.activeMap().entrySet()) {
            activeSection.set(entry.getKey().toString(), entry.getValue());
        }
        File parent = file.toFile().getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        configuration.save(file.toFile());
    }

    public static LoadResult load(Path file, Logger logger) {
        ModelRegistry registry = new ModelRegistry();
        if (!file.toFile().isFile()) {
            return new LoadResult(registry, false);
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file.toFile());
        if (configuration.isConfigurationSection("models")) {
            loadV2(configuration, registry, logger);
            return new LoadResult(registry, false);
        }
        if (configuration.isConfigurationSection("players")) {
            loadV1(configuration, registry, logger);
            return new LoadResult(registry, true);
        }
        return new LoadResult(registry, false);
    }

    private static void loadV2(YamlConfiguration configuration, ModelRegistry registry, Logger logger) {
        registry.nextId(configuration.getInt("next-id", 1));
        ConfigurationSection modelsSection = configuration.getConfigurationSection("models");
        if (modelsSection != null) {
            for (String modelKey : modelsSection.getKeys(false)) {
                int modelId;
                try {
                    modelId = Integer.parseInt(modelKey);
                } catch (NumberFormatException e) {
                    logger.warning("Invalid model id in models.yml: " + modelKey);
                    continue;
                }
                ConfigurationSection section = modelsSection.getConfigurationSection(modelKey);
                if (section == null) {
                    continue;
                }
                try {
                    ViewSession session = readSession(section, null, false);
                    if (session != null) {
                        registry.restore(modelId, session);
                    }
                } catch (RuntimeException e) {
                    logger.warning("Could not restore model " + modelKey + ": " + e.getMessage());
                }
            }
        }
        ConfigurationSection activeSection = configuration.getConfigurationSection("active");
        if (activeSection != null) {
            for (String playerKey : activeSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(playerKey);
                    registry.setActive(playerId, activeSection.getInt(playerKey));
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid player UUID in models.yml active section: " + playerKey);
                }
            }
        }
    }

    private static void loadV1(YamlConfiguration configuration, ModelRegistry registry, Logger logger) {
        ConfigurationSection playersSection = configuration.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }
        for (String playerKey : playersSection.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(playerKey);
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid player UUID in models.yml: " + playerKey);
                continue;
            }
            ConfigurationSection playerSection = playersSection.getConfigurationSection(playerKey);
            if (playerSection == null) {
                continue;
            }
            ConfigurationSection modelsSection = playerSection.getConfigurationSection("models");
            if (modelsSection == null) {
                continue;
            }
            Map<Integer, Integer> ids = new HashMap<>();
            for (String modelKey : modelsSection.getKeys(false)) {
                int oldId;
                try {
                    oldId = Integer.parseInt(modelKey);
                } catch (NumberFormatException e) {
                    logger.warning("Invalid model id in models.yml: " + modelKey);
                    continue;
                }
                ConfigurationSection section = modelsSection.getConfigurationSection(modelKey);
                if (section == null) {
                    continue;
                }
                try {
                    ViewSession session = readSession(section, playerId, false);
                    if (session != null) {
                        int newId = registry.nextId();
                        registry.restore(newId, session);
                        ids.put(oldId, newId);
                    }
                } catch (RuntimeException e) {
                    logger.warning("Could not restore model " + modelKey + ": " + e.getMessage());
                }
            }
            int activeId = playerSection.getInt("active", 0);
            Integer migratedActive = ids.get(activeId);
            if (migratedActive != null) {
                registry.setActive(playerId, migratedActive);
            } else if (playerSection.getInt("active", 0) != 0) {
                logger.warning("Active model " + activeId + " for " + playerId + " was not migrated.");
            }
        }
    }

    private static void writeSession(ConfigurationSection section, ViewSession session) {
        Crystal crystal = session.crystal();
        section.set("owner", session.owner() == null ? "" : session.owner().toString());
        section.set("shared-edit", session.sharedEdit());
        section.set("crystal.name", crystal.name());
        Lattice lattice = crystal.lattice();
        section.set("crystal.lattice.a", vector(lattice.toCartesian(1.0, 0.0, 0.0)));
        section.set("crystal.lattice.b", vector(lattice.toCartesian(0.0, 1.0, 0.0)));
        section.set("crystal.lattice.c", vector(lattice.toCartesian(0.0, 0.0, 1.0)));
        List<Map<String, Object>> atoms = new ArrayList<>(crystal.atoms().size());
        for (Crystal.Atom atom : crystal.atoms()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("element", atom.element());
            map.put("x", atom.x());
            map.put("y", atom.y());
            map.put("z", atom.z());
            map.put("occupancy", atom.occupancy());
            map.put("charge", atom.charge());
            map.put("label", atom.label());
            atoms.add(map);
        }
        section.set("crystal.atoms", atoms);

        Location origin = session.origin();
        section.set("world", origin.getWorld().getUID().toString());
        section.set("origin.x", origin.getX());
        section.set("origin.y", origin.getY());
        section.set("origin.z", origin.getZ());
        section.set("blocks-per-angstrom", session.blocksPerAngstrom());
        section.set("atom-scale", session.atomScale());
        section.set("repeat-x", session.repeatX());
        section.set("repeat-y", session.repeatY());
        section.set("repeat-z", session.repeatZ());
        section.set("rotation-x", session.rotationX());
        section.set("rotation-y", session.rotationY());
        section.set("rotation-z", session.rotationZ());
        section.set("rotation-axis", session.rotationAxis().name());
        section.set("space-filling", session.spaceFilling());
        section.set("unit-cell", session.showUnitCell());
        section.set("bonds", session.showBonds());
        section.set("show-labels", session.showLabels());
        section.set("polyhedron", session.showPolyhedron());
        section.set("polyhedron-faces", session.showPolyhedronFaces());
        section.set("polyhedron-centers", session.polyhedronCenters());
        section.set("miller", session.showMiller());
        section.set("miller-hkl", List.of(session.millerH(), session.millerK(), session.millerL()));
        section.set("radius-mode", session.radiusMode().name());
    }

    private static ViewSession readSession(ConfigurationSection section, UUID defaultOwner, boolean defaultShared) {
        String worldId = section.getString("world", "");
        World world;
        try {
            world = world(UUID.fromString(worldId));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("invalid world UUID");
        }
        if (world == null) {
            throw new IllegalStateException("world is not loaded");
        }

        UUID owner = defaultOwner;
        String ownerText = section.getString("owner", "");
        if (!ownerText.isBlank()) {
            try {
                owner = UUID.fromString(ownerText);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("invalid owner UUID");
            }
        }
        if (owner == null) {
            throw new IllegalStateException("missing owner");
        }
        boolean sharedEdit = section.getBoolean("shared-edit", defaultShared);

        String name = section.getString("crystal.name", "Crystal");
        double[][] matrix = {
            vector(section.getDoubleList("crystal.lattice.a"), "lattice.a"),
            vector(section.getDoubleList("crystal.lattice.b"), "lattice.b"),
            vector(section.getDoubleList("crystal.lattice.c"), "lattice.c")
        };
        Lattice lattice = Lattice.fromMatrix(matrix);
        List<Crystal.Atom> atoms = new ArrayList<>();
        for (Map<?, ?> map : section.getMapList("crystal.atoms")) {
            String element = String.valueOf(map.get("element"));
            atoms.add(new Crystal.Atom(
                    element,
                    number(map.get("x")),
                    number(map.get("y")),
                    number(map.get("z")),
                    number(map.get("occupancy")),
                    (int) Math.round(number(map.get("charge"))),
                    map.get("label") == null ? element : String.valueOf(map.get("label"))));
        }
        if (atoms.isEmpty()) {
            throw new IllegalStateException("no atoms");
        }
        Crystal crystal = new Crystal(name, lattice, atoms);

        Location origin = new Location(world,
                section.getDouble("origin.x"),
                section.getDouble("origin.y"),
                section.getDouble("origin.z"));
        ViewSession session = new ViewSession(
                crystal,
                origin,
                section.getDouble("blocks-per-angstrom", 2.0),
                section.getInt("repeat-x", 1),
                section.getDouble("atom-scale", 2.0));
        session.owner(owner);
        session.sharedEdit(sharedEdit);
        session.repeat(
                section.getInt("repeat-x", 1),
                section.getInt("repeat-y", 1),
                section.getInt("repeat-z", 1));
        session.rotation(
                section.getDouble("rotation-x"),
                section.getDouble("rotation-y"),
                section.getDouble("rotation-z"));
        try {
            session.rotationAxis(ViewSession.Axis.valueOf(
                    section.getString("rotation-axis", "Y").toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            session.rotationAxis(ViewSession.Axis.Y);
        }
        session.spaceFilling(section.getBoolean("space-filling", false));
        session.showUnitCell(section.getBoolean("unit-cell", false));
        session.showBonds(section.getBoolean("bonds", false));
        session.showLabels(section.getBoolean("show-labels", false));
        session.showPolyhedron(section.getBoolean("polyhedron", false));
        session.showPolyhedronFaces(section.getBoolean("polyhedron-faces", true));
        session.polyhedronCenters(section.getIntegerList("polyhedron-centers"));
        session.showMiller(section.getBoolean("miller", false));
        List<Integer> hkl = section.getIntegerList("miller-hkl");
        if (hkl.size() == 3) {
            session.miller(hkl.get(0), hkl.get(1), hkl.get(2));
        }
        try {
            session.radiusMode(ViewSession.RadiusMode.valueOf(
                    section.getString("radius-mode", "VDW").toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            session.radiusMode(ViewSession.RadiusMode.VDW);
        }
        return session;
    }

    private static List<Double> vector(double[] values) {
        return List.of(values[0], values[1], values[2]);
    }

    private static double[] vector(List<Double> values, String name) {
        if (values.size() != 3) {
            throw new IllegalStateException("invalid " + name);
        }
        return new double[] {values.get(0), values.get(1), values.get(2)};
    }

    private static double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private static World world(UUID id) {
        for (World world : Bukkit.getWorlds()) {
            if (world.getUID().equals(id)) {
                return world;
            }
        }
        return null;
    }
}
