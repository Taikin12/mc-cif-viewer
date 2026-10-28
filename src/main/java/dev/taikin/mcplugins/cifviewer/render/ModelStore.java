package dev.taikin.mcplugins.cifviewer.render;

import dev.taikin.mcplugins.cifviewer.model.Crystal;
import dev.taikin.mcplugins.cifviewer.model.Lattice;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
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

    private ModelStore() {
    }

    public static void save(Path file, Map<UUID, PlayerModels> players) throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        ConfigurationSection playersSection = configuration.createSection("players");
        for (Map.Entry<UUID, PlayerModels> entry : players.entrySet()) {
            PlayerModels models = entry.getValue();
            ConfigurationSection playerSection = playersSection.createSection(entry.getKey().toString());
            playerSection.set("active", models.activeId());
            playerSection.set("next-id", models.nextId());
            ConfigurationSection modelsSection = playerSection.createSection("models");
            for (ViewSession session : models.models()) {
                ConfigurationSection section = modelsSection.createSection(Integer.toString(session.id()));
                writeSession(section, session);
            }
        }
        File parent = file.toFile().getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        configuration.save(file.toFile());
    }

    public static Map<UUID, PlayerModels> load(Path file, Logger logger) {
        Map<UUID, PlayerModels> result = new LinkedHashMap<>();
        if (!file.toFile().isFile()) {
            return result;
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file.toFile());
        ConfigurationSection playersSection = configuration.getConfigurationSection("players");
        if (playersSection == null) {
            return result;
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
            PlayerModels models = new PlayerModels();
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
                    ViewSession session = readSession(section, logger);
                    if (session != null) {
                        models.restore(modelId, session);
                    }
                } catch (RuntimeException e) {
                    logger.warning("Could not restore model " + modelKey + ": " + e.getMessage());
                }
            }
            if (models.size() == 0) {
                continue;
            }
            int activeId = playerSection.getInt("active", 0);
            if (!models.select(activeId)) {
                models.select(models.models().getFirst().id());
            }
            models.nextId(playerSection.getInt("next-id", models.nextId()));
            result.put(playerId, models);
        }
        return result;
    }

    private static void writeSession(ConfigurationSection section, ViewSession session) {
        Crystal crystal = session.crystal();
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
        section.set("radius-mode", session.radiusMode().name());
    }

    private static ViewSession readSession(ConfigurationSection section, Logger logger) {
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
