package dev.taikin.mcplugins.cifviewer.render;

import dev.taikin.mcplugins.cifviewer.CifConfig;
import dev.taikin.mcplugins.cifviewer.CifViewerPlugin;
import dev.taikin.mcplugins.cifviewer.model.Crystal;
import dev.taikin.mcplugins.cifviewer.model.ElementInfo;
import dev.taikin.mcplugins.cifviewer.model.Lattice;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DyedItemColor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class CrystalRenderer {

    public static final String TAG = "cifviewer";
    private static final int MAX_BONDS = 4000;
    private static final int MAX_LABELS = 200;
    private static final int MAX_POLYHEDRON_CENTERS = 32;

    public record ModelRef(UUID playerId, int modelId) {
    }

    private final CifViewerPlugin plugin;
    private final java.nio.file.Path storageFile;
    private ModelRegistry registry = new ModelRegistry();
    private final Map<UUID, ModelRef> interactions = new HashMap<>();
    private final Map<UUID, Map<Long, Integer>> chunkTickets = new HashMap<>();
    private boolean loaded;
    private boolean dirty;
    private boolean saveScheduled;

    public CrystalRenderer(CifViewerPlugin plugin) {
        this.plugin = plugin;
        this.storageFile = plugin.getDataFolder().toPath().resolve("models.yml");
    }

    public int view(Player player, Crystal crystal, double blocksPerAngstrom, int repeat) {
        CifConfig config = plugin.config();
        if (!Double.isFinite(blocksPerAngstrom) || blocksPerAngstrom <= 0.0) {
            throw new IllegalArgumentException("Scale must be greater than zero.");
        }
        if (repeat < 1 || repeat > config.maxRepeat()) {
            throw new IllegalArgumentException("Repeat must be between 1 and " + config.maxRepeat() + ".");
        }

        long requested = (long) crystal.atoms().size() * repeat * repeat * repeat;
        if (requested > config.maxAtoms()) {
            throw new IllegalArgumentException("Too many atoms to display (" + requested + "). Limit is "
                    + config.maxAtoms() + "; reduce repeat or use a smaller cell.");
        }

        if (registry.size() >= config.maxModels()) {
            throw new IllegalArgumentException("Too many models. Limit is " + config.maxModels() + ".");
        }

        Location origin = placement(player, config, registry.size());
        ViewSession session = new ViewSession(crystal, origin, blocksPerAngstrom, repeat, config.atomScale());
        registry.add(player.getUniqueId(), session);
        try {
            render(player, session);
        } catch (RuntimeException e) {
            registry.remove(session.id());
            throw e;
        }
        markDirty();
        return session.renderedAtoms();
    }

    public ViewSession session(Player player) {
        return registry.active(player.getUniqueId());
    }

    public int activeId(Player player) {
        return registry.activeId(player.getUniqueId());
    }

    public List<ViewSession> models(Player player) {
        return registry.models();
    }

    public ViewSession model(int id) {
        return registry.get(id);
    }

    public boolean select(Player player, int id) {
        boolean selected = registry.select(player.getUniqueId(), id);
        if (selected) {
            markDirty();
        }
        return selected;
    }

    public ModelRef modelRef(UUID entityId) {
        return interactions.get(entityId);
    }

    public boolean isAdmin(Player player) {
        return player.hasPermission("cifviewer.admin");
    }

    public boolean canEdit(Player player, ViewSession session) {
        if (session == null) {
            return false;
        }
        if (isAdmin(player) || player.getUniqueId().equals(session.owner())) {
            return true;
        }
        return session.sharedEdit() && player.hasPermission("cifviewer.edit.shared");
    }

    public boolean canDelete(Player player, ViewSession session) {
        return session != null && (isAdmin(player) || player.getUniqueId().equals(session.owner()));
    }

    public boolean canShare(Player player, ViewSession session) {
        return canDelete(player, session);
    }

    public boolean canDelete(Player player, int id) {
        return canDelete(player, registry.get(id));
    }

    public boolean canShare(Player player, int id) {
        return canShare(player, registry.get(id));
    }

    public void adjustScale(Player player, double multiplier) {
        ViewSession session = requireEditableSession(player);
        session.blocksPerAngstrom(clamp(session.blocksPerAngstrom() * multiplier, 0.1, 10.0));
        refresh(player, session);
    }

    public void adjustRepeat(Player player, int delta) {
        ViewSession session = requireEditableSession(player);
        int max = plugin.config().maxRepeat();
        updateRepeat(player, session,
                clamp(session.repeatX() + delta, 1, max),
                clamp(session.repeatY() + delta, 1, max),
                clamp(session.repeatZ() + delta, 1, max));
    }

    public void adjustRepeat(Player player, ViewSession.Axis axis, int delta) {
        ViewSession session = requireEditableSession(player);
        int max = plugin.config().maxRepeat();
        int repeatX = session.repeatX();
        int repeatY = session.repeatY();
        int repeatZ = session.repeatZ();
        switch (axis) {
            case X -> repeatX = clamp(repeatX + delta, 1, max);
            case Y -> repeatY = clamp(repeatY + delta, 1, max);
            case Z -> repeatZ = clamp(repeatZ + delta, 1, max);
        }
        updateRepeat(player, session, repeatX, repeatY, repeatZ);
    }

    public void resetRepeat(Player player) {
        ViewSession session = requireEditableSession(player);
        updateRepeat(player, session, 1, 1, 1);
    }

    private void updateRepeat(Player player, ViewSession session, int repeatX, int repeatY, int repeatZ) {
        if (repeatX == session.repeatX() && repeatY == session.repeatY() && repeatZ == session.repeatZ()) {
            return;
        }
        long requested = (long) session.crystal().atoms().size() * repeatX * repeatY * repeatZ;
        if (requested > plugin.config().maxAtoms()) {
            throw new IllegalArgumentException("Too many atoms to display (" + requested + "). Limit is "
                    + plugin.config().maxAtoms() + ".");
        }
        session.repeat(repeatX, repeatY, repeatZ);
        session.selectedAtoms(List.of());
        session.polyhedronCenters(List.of());
        refresh(player, session);
    }

    public void rotate(Player player, double deltaX, double deltaY, double deltaZ) {
        ViewSession session = requireEditableSession(player);
        session.rotation(
                normalize(session.rotationX() + deltaX),
                normalize(session.rotationY() + deltaY),
                normalize(session.rotationZ() + deltaZ));
        refresh(player, session);
    }

    public void adjustAtomScale(Player player, double multiplier) {
        ViewSession session = requireEditableSession(player);
        session.atomScale(clamp(session.atomScale() * multiplier, 0.2, 8.0));
        refresh(player, session);
    }

    public void setRotationAxis(Player player, ViewSession.Axis axis) {
        requireEditableSession(player).rotationAxis(axis);
        markDirty();
    }

    public void cycleRotationAxis(Player player) {
        ViewSession session = requireEditableSession(player);
        session.rotationAxis(switch (session.rotationAxis()) {
            case X -> ViewSession.Axis.Y;
            case Y -> ViewSession.Axis.Z;
            case Z -> ViewSession.Axis.X;
        });
        markDirty();
    }

    public void rotateAroundAxis(Player player, double degrees) {
        ViewSession session = requireEditableSession(player);
        switch (session.rotationAxis()) {
            case X -> session.rotation(
                    normalize(session.rotationX() + degrees),
                    session.rotationY(),
                    session.rotationZ());
            case Y -> session.rotation(
                    session.rotationX(),
                    normalize(session.rotationY() + degrees),
                    session.rotationZ());
            case Z -> session.rotation(
                    session.rotationX(),
                    session.rotationY(),
                    normalize(session.rotationZ() + degrees));
        }
        refresh(player, session);
    }

    public void toggleSpaceFilling(Player player) {
        ViewSession session = requireEditableSession(player);
        session.spaceFilling(!session.spaceFilling());
        refresh(player, session);
    }

    public void toggleUnitCell(Player player) {
        ViewSession session = requireEditableSession(player);
        session.showUnitCell(!session.showUnitCell());
        refresh(player, session);
    }

    public void toggleBonds(Player player) {
        ViewSession session = requireEditableSession(player);
        session.showBonds(!session.showBonds());
        refresh(player, session);
    }

    public void cycleRadiusMode(Player player) {
        ViewSession session = requireEditableSession(player);
        session.radiusMode(switch (session.radiusMode()) {
            case VDW -> ViewSession.RadiusMode.IONIC;
            case IONIC -> ViewSession.RadiusMode.COVALENT;
            case COVALENT -> ViewSession.RadiusMode.VDW;
        });
        refresh(player, session);
    }

    public void replaceOrigin(Player player) {
        ViewSession session = requireEditableSession(player);
        session.origin(placement(player, plugin.config()));
        refresh(player, session);
    }

    public void setOrigin(Player player, Location origin) {
        ViewSession session = requireEditableSession(player);
        session.origin(origin);
        refresh(player, session);
    }

    public void reset(Player player) {
        ViewSession session = requireEditableSession(player);
        session.blocksPerAngstrom(plugin.config().blocksPerAngstrom());
        session.repeat(1, 1, 1);
        session.atomScale(plugin.config().atomScale());
        session.spaceFilling(false);
        session.showUnitCell(false);
        session.showBonds(false);
        session.showLabels(false);
        session.showPolyhedron(false);
        session.selectedAtoms(List.of());
        session.polyhedronCenters(List.of());
        session.radiusMode(ViewSession.RadiusMode.VDW);
        session.rotation(0.0, 0.0, 0.0);
        session.rotationAxis(ViewSession.Axis.Y);
        session.origin(placement(player, plugin.config()));
        refresh(player, session);
    }

    public void teleport(Player player, int id) {
        ViewSession session = registry.get(id);
        if (session == null) {
            throw new IllegalStateException("モデル #" + id + " はありません。");
        }
        if (!player.hasPermission("cifviewer.use")) {
            throw new IllegalStateException("テレポートする権限がありません。");
        }
        player.teleport(session.origin().clone().add(0.0, 2.0, 0.0));
    }

    public void toggleSharedEdit(Player player) {
        ViewSession session = requireSession(player);
        if (!canShare(player, session)) {
            throw new IllegalStateException("共有設定を変更する権限がありません。");
        }
        session.sharedEdit(!session.sharedEdit());
        markDirty();
    }

    public void setSharedEdit(Player player, int id, boolean shared) {
        ViewSession session = registry.get(id);
        if (session == null) {
            throw new IllegalStateException("モデル #" + id + " はありません。");
        }
        if (!canShare(player, session)) {
            throw new IllegalStateException("共有設定を変更する権限がありません。");
        }
        session.sharedEdit(shared);
        markDirty();
    }

    public void toggleLabels(Player player) {
        ViewSession session = requireSession(player);
        session.showLabels(!session.showLabels());
        refresh(player, session);
    }

    public void togglePolyhedron(Player player) {
        ViewSession session = requireSession(player);
        if (!session.showPolyhedron() && session.polyhedronCenters().isEmpty()) {
            throw new IllegalStateException("CIF原子棒で多面体中心を選択してください。");
        }
        session.showPolyhedron(!session.showPolyhedron());
        refresh(player, session);
    }

    public String addAtomSelection(Player player) {
        ViewSession session = requireSession(player);
        if (!player.getWorld().getUID().equals(session.worldId())) {
            return "このモデルは別のワールドにあります。";
        }
        ViewSession.AtomMarker marker = findAtom(player, session);
        if (marker == null) {
            return "原子が見つかりません。";
        }
        List<Integer> selected = new ArrayList<>(session.selectedAtoms());
        if (selected.size() >= 4) {
            selected.clear();
        }
        if (!selected.contains(marker.index())) {
            selected.add(marker.index());
        }
        session.selectedAtoms(selected);
        refresh(player, session);
        return selectionText(session);
    }

    public String togglePolyhedronCenter(Player player) {
        ViewSession session = requireSession(player);
        if (!player.getWorld().getUID().equals(session.worldId())) {
            return "このモデルは別のワールドにあります。";
        }
        ViewSession.AtomMarker marker = findAtom(player, session);
        if (marker == null) {
            return "原子が見つかりません。";
        }
        List<Integer> centers = new ArrayList<>(session.polyhedronCenters());
        if (centers.contains(marker.index())) {
            centers.remove(Integer.valueOf(marker.index()));
        } else {
            if (centers.size() >= MAX_POLYHEDRON_CENTERS) {
                return "多面体中心は最大 " + MAX_POLYHEDRON_CENTERS + " 個です。";
            }
            centers.add(marker.index());
        }
        session.polyhedronCenters(centers);
        refresh(player, session);
        return "多面体中心: " + centers.size() + " 個 (" + marker.label() + ")";
    }

    public String clearAtomSelection(Player player) {
        ViewSession session = requireSession(player);
        if (session.selectedAtoms().isEmpty()) {
            return "測定用の原子選択はありません。";
        }
        session.selectedAtoms(List.of());
        refresh(player, session);
        return "測定用の原子選択を解除しました。";
    }

    public String clearPolyhedronCenters(Player player) {
        ViewSession session = requireSession(player);
        if (session.polyhedronCenters().isEmpty()) {
            return "多面体中心は選択されていません。";
        }
        session.polyhedronCenters(List.of());
        refresh(player, session);
        return "多面体中心をすべて解除しました。";
    }

    private ViewSession.AtomMarker findAtom(Player player, ViewSession session) {
        Vector eye = player.getEyeLocation().toVector();
        Vector direction = player.getEyeLocation().getDirection().normalize();
        Vector origin = session.origin().toVector();
        ViewSession.AtomMarker best = null;
        double bestT = 64.0;
        for (ViewSession.AtomMarker marker : session.atomMarkers()) {
            Vector center = origin.clone().add(marker.position());
            double radius = Math.max(0.35, marker.radius());
            Vector oc = eye.clone().subtract(center);
            double b = oc.dot(direction);
            double c = oc.dot(oc) - radius * radius;
            double discriminant = b * b - c;
            if (discriminant < 0.0) {
                continue;
            }
            double t = -b - Math.sqrt(discriminant);
            if (t < 0.0) {
                t = -b + Math.sqrt(discriminant);
            }
            if (t >= 0.0 && t < bestT) {
                bestT = t;
                best = marker;
            }
        }
        if (best != null) {
            return best;
        }

        double bestDistance = 1.0;
        for (ViewSession.AtomMarker marker : session.atomMarkers()) {
            Vector center = origin.clone().add(marker.position());
            Vector toCenter = center.clone().subtract(eye);
            double t = toCenter.dot(direction);
            if (t < 0.0 || t > 64.0) {
                continue;
            }
            Vector closest = eye.clone().add(direction.clone().multiply(t));
            double distance = closest.distance(center);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = marker;
            }
        }
        return best;
    }

    private String selectionText(ViewSession session) {
        List<Integer> selected = session.selectedAtoms();
        List<ViewSession.AtomMarker> markers = session.atomMarkers();
        List<ViewSession.AtomMarker> atoms = new ArrayList<>();
        for (int index : selected) {
            if (index >= 0 && index < markers.size()) {
                atoms.add(markers.get(index));
            }
        }
        if (atoms.isEmpty()) {
            return "原子未選択";
        }
        if (atoms.size() == 1) {
            return "選択: " + atoms.getFirst().label();
        }
        if (atoms.size() == 2) {
            double distance = MolecularGeometry.distance(
                    atoms.get(0).position(), atoms.get(1).position(), session.blocksPerAngstrom());
            return String.format(Locale.ROOT, "距離 %s–%s: %.2f Å",
                    atoms.get(0).label(), atoms.get(1).label(), distance);
        }
        if (atoms.size() == 3) {
            double angle = MolecularGeometry.angle(
                    atoms.get(0).position(), atoms.get(1).position(), atoms.get(2).position());
            return String.format(Locale.ROOT, "角度 %s–%s–%s: %.1f°",
                    atoms.get(0).label(), atoms.get(1).label(), atoms.get(2).label(), angle);
        }
        double angle = MolecularGeometry.dihedral(
                atoms.get(0).position(), atoms.get(1).position(),
                atoms.get(2).position(), atoms.get(3).position());
        return String.format(Locale.ROOT, "二面角 %s–%s–%s–%s: %.1f°",
                atoms.get(0).label(), atoms.get(1).label(), atoms.get(2).label(),
                atoms.get(3).label(), angle);
    }

    public int clear(Player player) {
        ViewSession session = session(player);
        if (session == null) {
            return 0;
        }
        return clear(player, session.id());
    }

    public int clear(Player player, int id) {
        ViewSession session = registry.get(id);
        if (session == null || !canDelete(player, session)) {
            return 0;
        }
        registry.remove(id);
        int removed = removeEntities(session);
        releaseChunks(session);
        markDirty();
        return removed;
    }

    public int clearAll(Player player) {
        int removed = 0;
        for (ViewSession session : registry.models()) {
            if (isAdmin(player) || player.getUniqueId().equals(session.owner())) {
                registry.remove(session.id());
                removed += removeEntities(session);
                releaseChunks(session);
            }
        }
        markDirty();
        return removed;
    }

    public void clearAll() {
        for (ViewSession session : registry.models()) {
            removeEntities(session);
            releaseChunks(session);
        }
        registry.clear();
        removeTaggedEntities();
        interactions.clear();
    }

    public void removeOrphans() {
        removeTaggedEntities();
        interactions.clear();
    }

    private void markDirty() {
        dirty = true;
        if (saveScheduled) {
            return;
        }
        saveScheduled = true;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            saveScheduled = false;
            save();
        }, 100L);
    }

    public void save() {
        if (!loaded || !dirty) {
            return;
        }
        try {
            ModelStore.save(storageFile, registry);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save models.yml: " + e.getMessage());
        }
    }

    public void load() {
        if (registry.size() != 0) {
            return;
        }
        ModelStore.LoadResult result = ModelStore.load(storageFile, plugin.getLogger());
        registry = result.registry();
        for (ViewSession session : registry.models()) {
            try {
                render(session.origin().getWorld(), session.owner(), session);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Could not restore model #" + session.id() + ": "
                        + e.getMessage());
            }
        }
        this.loaded = true;
        this.dirty = result.migrated();
        if (result.migrated()) {
            markDirty();
        }
    }

    private void refresh(Player player, ViewSession session) {
        if (!player.isOnline()) {
            return;
        }
        if (!player.getWorld().getUID().equals(session.worldId())) {
            player.sendMessage(Component.text("このモデルは別のワールドにあります。",
                    NamedTextColor.RED));
            return;
        }
        removeEntities(session);
        releaseChunks(session);
        render(player, session);
        markDirty();
    }

    private void render(Player player, ViewSession session) {
        UUID owner = session.owner() == null ? player.getUniqueId() : session.owner();
        render(player.getWorld(), owner, session);
    }

    private void render(World world, UUID ownerId, ViewSession session) {
        Crystal crystal = session.crystal();
        Lattice lattice = crystal.lattice();
        double scale = session.blocksPerAngstrom();
        int repeatX = session.repeatX();
        int repeatY = session.repeatY();
        int repeatZ = session.repeatZ();
        CifConfig.RenderMode mode = plugin.config().renderMode();

        List<RawAtom> raw = new ArrayList<>();
        List<ViewSession.AtomMarker> markers = new ArrayList<>();
        for (int i = 0; i < repeatX; i++) {
            for (int j = 0; j < repeatY; j++) {
                for (int k = 0; k < repeatZ; k++) {
                    for (Crystal.Atom atom : crystal.atoms()) {
                        double[] cartesian = lattice.toCartesian(atom.x() + i, atom.y() + j, atom.z() + k);
                        double x = cartesian[0] * scale;
                        double y = cartesian[2] * scale;
                        double z = cartesian[1] * scale;
                        raw.add(new RawAtom(atom, x, y, z));
                    }
                }
            }
        }

        Location origin = session.origin();

        double rotationX = Math.toRadians(session.rotationX());
        double rotationY = Math.toRadians(session.rotationY());
        double rotationZ = Math.toRadians(session.rotationZ());
        List<UUID> entities = new ArrayList<>(raw.size() + 2);
        double renderedMaxY = Double.NEGATIVE_INFINITY;
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        List<Integer> selected = session.selectedAtoms();
        Set<Integer> highlighted = new HashSet<>(selected);
        highlighted.addAll(session.polyhedronCenters());

        try {
            for (int atomIndex = 0; atomIndex < raw.size(); atomIndex++) {
                RawAtom atom = raw.get(atomIndex);
                Vector position = new Vector(atom.x, atom.y, atom.z);
                position.rotateAroundX(rotationX);
                position.rotateAroundY(rotationY);
                position.rotateAroundZ(rotationZ);
                renderedMaxY = Math.max(renderedMaxY, position.getY());

                int[] rgb = plugin.config().atomColor(atom.atom.element());
                double atomScale;
                if (session.spaceFilling() && mode == CifConfig.RenderMode.MODEL) {
                    double radiusAngstrom = switch (session.radiusMode()) {
                        case VDW -> ElementInfo.vanDerWaalsRadius(atom.atom.element());
                        case IONIC -> ElementInfo.ionicRadius(atom.atom.element(), atom.atom.charge());
                        case COVALENT -> ElementInfo.radius(atom.atom.element());
                    };
                    atomScale = 2.0 * radiusAngstrom * scale * (session.atomScale() / 2.0);
                } else {
                    atomScale = session.atomScale() * (ElementInfo.radius(atom.atom.element()) / 1.4);
                }
                if (mode == CifConfig.RenderMode.CIRCLE) {
                    atomScale *= 1.15;
                }
                atomScale = Math.max(0.2, Math.min(session.spaceFilling() ? 20.0 : 6.0, atomScale));

                double radius = atomScale / 2.0;
                minX = Math.min(minX, position.getX() - radius);
                minY = Math.min(minY, position.getY() - radius);
                minZ = Math.min(minZ, position.getZ() - radius);
                maxX = Math.max(maxX, position.getX() + radius);
                maxY = Math.max(maxY, position.getY() + radius);
                maxZ = Math.max(maxZ, position.getZ() + radius);

                Location location = new Location(world,
                        origin.getX() + position.getX(),
                        origin.getY() + position.getY(),
                        origin.getZ() + position.getZ());
                Entity display;
                if (mode == CifConfig.RenderMode.MODEL) {
                    display = spawnItem(world, location, rgb, (float) atomScale);
                } else {
                    int alpha = (int) Math.round(200.0 * (0.45 + 0.55 * atom.atom.occupancy()));
                    Color background;
                    TextColor textColor;
                    String glyph;
                    if (mode == CifConfig.RenderMode.CIRCLE) {
                        background = Color.fromARGB(0, 0, 0, 0);
                        textColor = TextColor.color(rgb[0], rgb[1], rgb[2]);
                        glyph = "\u25CF";
                    } else {
                        background = Color.fromARGB(alpha, rgb[0], rgb[1], rgb[2]);
                        textColor = luminance(rgb) >= 145
                                ? TextColor.color(0, 0, 0)
                                : TextColor.color(255, 255, 255);
                        glyph = atom.atom.element();
                    }
                    display = spawn(world, location, Component.text(glyph, textColor),
                            background, (float) atomScale,
                            mode == CifConfig.RenderMode.CIRCLE && plugin.config().sphereShadow());
                }
                entities.add(display.getUniqueId());
                if (highlighted.contains(atomIndex)) {
                    display.setGlowing(true);
                }
                String label = atom.atom.label() == null || atom.atom.label().isBlank()
                        ? atom.atom.element()
                        : atom.atom.label();
                markers.add(new ViewSession.AtomMarker(atomIndex, label, atom.atom.element(),
                        position.clone(), atomScale / 2.0, rgb));
            }
            session.atomMarkers(markers);

            if (!markers.isEmpty()) {
                Set<Integer> selectedSet = highlighted;
                int labelCount = 0;
                for (ViewSession.AtomMarker marker : markers) {
                    boolean isSelected = selectedSet.contains(marker.index());
                    if (!isSelected && (!session.showLabels() || labelCount >= MAX_LABELS)) {
                        continue;
                    }
                    Location labelLocation = new Location(world,
                            origin.getX() + marker.position().getX(),
                            origin.getY() + marker.position().getY() + marker.radius() + 0.25,
                            origin.getZ() + marker.position().getZ());
                    TextDisplay label = spawn(world, labelLocation,
                            Component.text(marker.label(), TextColor.color(
                                    marker.rgb()[0], marker.rgb()[1], marker.rgb()[2])),
                            Color.fromARGB(0, 0, 0, 0), 0.6f, true);
                    label.setGlowing(isSelected);
                    entities.add(label.getUniqueId());
                    labelCount++;
                }
            }

            if (session.showPolyhedron()) {
                List<Integer> centers = session.polyhedronCenters();
                for (int centerIndex : centers) {
                    if (centerIndex < 0 || centerIndex >= markers.size()) {
                        continue;
                    }
                    ViewSession.AtomMarker center = markers.get(centerIndex);
                    List<PolyhedronVertex> vertices = coordinationVertices(
                            markers, center, lattice, scale, rotationX, rotationY, rotationZ);
                    List<Vector> points = vertices.stream().map(PolyhedronVertex::position).toList();
                    for (PolyhedronGeometry.Edge edge : PolyhedronGeometry.convexHullEdges(points)) {
                        Vector start = points.get(edge.first()).clone();
                        Vector end = points.get(edge.second()).clone();
                        Vector direction = end.clone().subtract(start);
                        double length = direction.length();
                        if (length < 1.0e-6) {
                            continue;
                        }
                        Location midpoint = new Location(world,
                                origin.getX() + (start.getX() + end.getX()) / 2.0,
                                origin.getY() + (start.getY() + end.getY()) / 2.0,
                                origin.getZ() + (start.getZ() + end.getZ()) / 2.0);
                        ItemDisplay stick = spawnStick(world, midpoint, direction, length, 0.065,
                                new int[] {255, 196, 64});
                        entities.add(stick.getUniqueId());
                    }
                    for (Vector point : points) {
                        minX = Math.min(minX, point.getX() - 0.04);
                        minY = Math.min(minY, point.getY() - 0.04);
                        minZ = Math.min(minZ, point.getZ() - 0.04);
                        maxX = Math.max(maxX, point.getX() + 0.04);
                        maxY = Math.max(maxY, point.getY() + 0.04);
                        maxZ = Math.max(maxZ, point.getZ() + 0.04);
                        renderedMaxY = Math.max(renderedMaxY, point.getY());
                    }
                }
            }

            if (session.showUnitCell()) {
                Vector a = latticeVector(lattice, 1.0, 0.0, 0.0, scale);
                Vector b = latticeVector(lattice, 0.0, 1.0, 0.0, scale);
                Vector c = latticeVector(lattice, 0.0, 0.0, 1.0, scale);
                a.rotateAroundX(rotationX);
                a.rotateAroundY(rotationY);
                a.rotateAroundZ(rotationZ);
                b.rotateAroundX(rotationX);
                b.rotateAroundY(rotationY);
                b.rotateAroundZ(rotationZ);
                c.rotateAroundX(rotationX);
                c.rotateAroundY(rotationY);
                c.rotateAroundZ(rotationZ);

                Vector[] corners = new Vector[8];
                for (int corner = 0; corner < 8; corner++) {
                    Vector point = new Vector();
                    if ((corner & 1) != 0) {
                        point.add(a);
                    }
                    if ((corner & 2) != 0) {
                        point.add(b);
                    }
                    if ((corner & 4) != 0) {
                        point.add(c);
                    }
                    corners[corner] = point;
                }
                int[][] edges = {
                        {0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5}, {2, 3},
                        {2, 6}, {3, 7}, {4, 5}, {4, 6}, {5, 7}, {6, 7}
                };
                for (int[] edge : edges) {
                    Vector start = corners[edge[0]].clone();
                    Vector end = corners[edge[1]].clone();
                    Vector direction = end.clone().subtract(start);
                    double length = direction.length();
                    if (length < 1.0e-6) {
                        continue;
                    }
                    Location midpoint = new Location(world,
                            origin.getX() + (start.getX() + end.getX()) / 2.0,
                            origin.getY() + (start.getY() + end.getY()) / 2.0,
                            origin.getZ() + (start.getZ() + end.getZ()) / 2.0);
                    ItemDisplay stick = spawnStick(world, midpoint, direction, length, 0.05,
                            new int[] {255, 255, 255});
                    entities.add(stick.getUniqueId());
                }
                for (Vector corner : corners) {
                    minX = Math.min(minX, corner.getX() - 0.05);
                    minY = Math.min(minY, corner.getY() - 0.05);
                    minZ = Math.min(minZ, corner.getZ() - 0.05);
                    maxX = Math.max(maxX, corner.getX() + 0.05);
                    maxY = Math.max(maxY, corner.getY() + 0.05);
                    maxZ = Math.max(maxZ, corner.getZ() + 0.05);
                    renderedMaxY = Math.max(renderedMaxY, corner.getY());
                }
            }

            if (session.showBonds() && !session.spaceFilling() && mode == CifConfig.RenderMode.MODEL) {
                double[] nearest = new double[raw.size()];
                for (int index = 0; index < nearest.length; index++) {
                    nearest[index] = Double.POSITIVE_INFINITY;
                }
                for (int firstIndex = 0; firstIndex < raw.size(); firstIndex++) {
                    RawAtom first = raw.get(firstIndex);
                    for (int secondIndex = firstIndex + 1; secondIndex < raw.size(); secondIndex++) {
                        RawAtom second = raw.get(secondIndex);
                        double dx = first.x - second.x;
                        double dy = first.y - second.y;
                        double dz = first.z - second.z;
                        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (distance < 1.0e-6) {
                            continue;
                        }
                        nearest[firstIndex] = Math.min(nearest[firstIndex], distance);
                        nearest[secondIndex] = Math.min(nearest[secondIndex], distance);
                    }
                }

                int bondCount = 0;
                outer:
                for (int firstIndex = 0; firstIndex < raw.size(); firstIndex++) {
                    RawAtom first = raw.get(firstIndex);
                    for (int secondIndex = firstIndex + 1; secondIndex < raw.size(); secondIndex++) {
                        RawAtom second = raw.get(secondIndex);
                        double dx = first.x - second.x;
                        double dy = first.y - second.y;
                        double dz = first.z - second.z;
                        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (distance < 1.0e-6) {
                            continue;
                        }
                        double cutoff = Math.max(nearest[firstIndex], nearest[secondIndex]) * 1.15;
                        if (distance > cutoff) {
                            continue;
                        }
                        Vector start = new Vector(first.x, first.y, first.z);
                        Vector end = new Vector(second.x, second.y, second.z);
                        start.rotateAroundX(rotationX);
                        start.rotateAroundY(rotationY);
                        start.rotateAroundZ(rotationZ);
                        end.rotateAroundX(rotationX);
                        end.rotateAroundY(rotationY);
                        end.rotateAroundZ(rotationZ);
                        Vector direction = end.clone().subtract(start);
                        double length = direction.length();
                        if (length < 1.0e-6) {
                            continue;
                        }
                        Location midpoint = new Location(world,
                                origin.getX() + (start.getX() + end.getX()) / 2.0,
                                origin.getY() + (start.getY() + end.getY()) / 2.0,
                                origin.getZ() + (start.getZ() + end.getZ()) / 2.0);
                        int[] color = averageColor(plugin.config().atomColor(first.atom.element()),
                                plugin.config().atomColor(second.atom.element()));
                        ItemDisplay stick = spawnStick(world, midpoint, direction, length, 0.08, color);
                        entities.add(stick.getUniqueId());
                        bondCount++;
                        if (bondCount >= MAX_BONDS) {
                            break outer;
                        }
                    }
                }
            }

            if (!raw.isEmpty()) {
                double width = Math.max(maxX - minX, maxZ - minZ);
                double height = maxY - minY;
                Location interactionLocation = new Location(world,
                        origin.getX() + (minX + maxX) / 2.0,
                        origin.getY() + minY,
                        origin.getZ() + (minZ + maxZ) / 2.0);
                Interaction interaction = spawnInteraction(world, interactionLocation, width, height,
                        ownerId, session.id());
                entities.add(interaction.getUniqueId());
            }

            if (plugin.config().showInfo()) {
                Location infoLocation = new Location(world, origin.getX(), origin.getY() + renderedMaxY + 1.5,
                        origin.getZ());
                String info = crystal.name() + "\n"
                        + crystal.contents() + " (" + crystal.atoms().size() + " atoms/cell)\n"
                        + String.format(Locale.ROOT,
                                "a=%.3f b=%.3f c=%.3f A  alpha=%.2f beta=%.2f gamma=%.2f deg",
                                lattice.a(), lattice.b(), lattice.c(),
                                lattice.alpha(), lattice.beta(), lattice.gamma()) + "\n"
                        + String.format(Locale.ROOT, "%d atoms  %.2f blocks/A  repeat %d x %d x %d",
                                raw.size(), scale, repeatX, repeatY, repeatZ);
                TextDisplay infoDisplay = spawn(world, infoLocation, Component.text(info),
                        Color.fromARGB(210, 0, 0, 0), 0.9f, false);
                entities.add(infoDisplay.getUniqueId());
            }
        } catch (RuntimeException e) {
            for (UUID id : entities) {
                interactions.remove(id);
                Entity entity = Bukkit.getEntity(id);
                if (entity != null) {
                    entity.remove();
                }
            }
            throw e;
        }

        retainChunks(world, session, origin, minX, maxX, minZ, maxZ);
        session.entities(List.copyOf(entities));
        session.renderedAtoms(raw.size());
    }

    private void retainChunks(World world, ViewSession session, Location origin,
            double minX, double maxX, double minZ, double maxZ) {
        int minChunkX = (int) Math.floor((origin.getX() + minX) / 16.0);
        int maxChunkX = (int) Math.floor((origin.getX() + maxX) / 16.0);
        int minChunkZ = (int) Math.floor((origin.getZ() + minZ) / 16.0);
        int maxChunkZ = (int) Math.floor((origin.getZ() + maxZ) / 16.0);
        Map<Long, Integer> counts = chunkTickets.computeIfAbsent(world.getUID(), id -> new HashMap<>());
        Set<Long> chunks = new HashSet<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                long key = chunkKey(chunkX, chunkZ);
                chunks.add(key);
                int count = counts.getOrDefault(key, 0);
                if (count == 0) {
                    world.addPluginChunkTicket(chunkX, chunkZ, plugin);
                }
                counts.put(key, count + 1);
            }
        }
        session.chunks(chunks);
    }

    private void releaseChunks(ViewSession session) {
        World world = session.origin().getWorld();
        if (world == null) {
            session.chunks(Set.of());
            return;
        }
        Map<Long, Integer> counts = chunkTickets.get(world.getUID());
        if (counts == null) {
            session.chunks(Set.of());
            return;
        }
        for (long key : session.chunks()) {
            int count = counts.getOrDefault(key, 0) - 1;
            if (count <= 0) {
                counts.remove(key);
                world.removePluginChunkTicket(chunkX(key), chunkZ(key), plugin);
            } else {
                counts.put(key, count);
            }
        }
        if (counts.isEmpty()) {
            chunkTickets.remove(world.getUID());
        }
        session.chunks(Set.of());
    }

    private long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    private int chunkX(long key) {
        return (int) (key >> 32);
    }

    private int chunkZ(long key) {
        return (int) key;
    }

    private int removeEntities(ViewSession session) {
        int removed = 0;
        for (UUID id : session.entities()) {
            interactions.remove(id);
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
                removed++;
            }
        }
        session.entities(List.of());
        return removed;
    }

    private void removeTaggedEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : List.copyOf(world.getEntities())) {
                if (entity.getScoreboardTags().contains(TAG)) {
                    entity.remove();
                }
            }
        }
    }

    private Location placement(Player player, CifConfig config) {
        return placement(player, config, 0);
    }

    private Location placement(Player player, CifConfig config, int slot) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().clone();
        if (direction.lengthSquared() < 1.0e-9) {
            direction = new Vector(0, 0, 1);
        }
        direction.normalize();

        double distance = config.placementDistance();
        RayTraceResult hit = player.getWorld().rayTraceBlocks(eye, direction, distance, FluidCollisionMode.NEVER, true);
        if (hit != null && hit.getHitPosition() != null) {
            double hitDistance = eye.toVector().distance(hit.getHitPosition());
            distance = Math.max(2.0, hitDistance - 1.0);
        }

        Location origin = eye.clone().add(direction.multiply(distance));
        origin.setY(player.getLocation().getY());
        if (slot > 0) {
            Vector horizontal = eye.getDirection().clone();
            horizontal.setY(0);
            if (horizontal.lengthSquared() < 1.0e-9) {
                horizontal = new Vector(0, 0, 1);
            }
            horizontal.normalize();
            Vector right = horizontal.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            int column = slot % 3;
            int row = slot / 3;
            origin.add(right.multiply(12.0 * column));
            origin.add(horizontal.multiply(-12.0 * row));
        }
        return origin;
    }

    private List<PolyhedronVertex> coordinationVertices(List<ViewSession.AtomMarker> markers,
            ViewSession.AtomMarker center, Lattice lattice, double scale,
            double rotationX, double rotationY, double rotationZ) {
        Vector a = latticeVector(lattice, 1.0, 0.0, 0.0, scale);
        Vector b = latticeVector(lattice, 0.0, 1.0, 0.0, scale);
        Vector c = latticeVector(lattice, 0.0, 0.0, 1.0, scale);
        rotate(a, rotationX, rotationY, rotationZ);
        rotate(b, rotationX, rotationY, rotationZ);
        rotate(c, rotationX, rotationY, rotationZ);

        Map<PointKey, PolyhedronVertex> candidates = new LinkedHashMap<>();
        double nearest = Double.POSITIVE_INFINITY;
        for (ViewSession.AtomMarker marker : markers) {
            double covalentLimit = (ElementInfo.radius(center.element())
                    + ElementInfo.radius(marker.element())) * scale * 1.2;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        Vector point = marker.position().clone()
                                .add(a.clone().multiply(dx))
                                .add(b.clone().multiply(dy))
                                .add(c.clone().multiply(dz));
                        double distance = point.distance(center.position());
                        if (distance < 1.0e-5 || distance > covalentLimit) {
                            continue;
                        }
                        PointKey key = PointKey.of(point);
                        candidates.putIfAbsent(key, new PolyhedronVertex(marker.element(), point));
                        nearest = Math.min(nearest, distance);
                    }
                }
            }
        }
        if (!Double.isFinite(nearest)) {
            return List.of();
        }
        double shellLimit = nearest * 1.15;
        List<PolyhedronVertex> neighbors = new ArrayList<>();
        for (PolyhedronVertex candidate : candidates.values()) {
            if (candidate.position().distance(center.position()) <= shellLimit) {
                neighbors.add(candidate);
            }
        }
        return neighbors;
    }

    private void rotate(Vector vector, double rotationX, double rotationY, double rotationZ) {
        vector.rotateAroundX(rotationX);
        vector.rotateAroundY(rotationY);
        vector.rotateAroundZ(rotationZ);
    }

    private Vector latticeVector(Lattice lattice, double x, double y, double z, double scale) {
        double[] cartesian = lattice.toCartesian(x, y, z);
        return new Vector(cartesian[0] * scale, cartesian[2] * scale, cartesian[1] * scale);
    }

    private int[] averageColor(int[] first, int[] second) {
        return new int[] {
                (first[0] + second[0]) / 2,
                (first[1] + second[1]) / 2,
                (first[2] + second[2]) / 2
        };
    }

    private ItemDisplay spawnStick(World world, Location location, Vector direction, double length,
            double thickness, int[] rgb) {
        ItemStack item = new ItemStack(Material.PAPER);
        item.setData(DataComponentTypes.ITEM_MODEL, Key.key("cifviewer", "stick"));
        item.setData(DataComponentTypes.DYED_COLOR,
                DyedItemColor.dyedItemColor(Color.fromRGB(rgb[0], rgb[1], rgb[2])));
        return world.spawn(location, ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setViewRange(1.5f);
            entity.setPersistent(false);
            entity.setTransformation(new Transformation(
                    new Vector3f(),
                    rotationTo(direction),
                    new Vector3f((float) thickness, (float) length, (float) thickness),
                    new AxisAngle4f()));
            entity.addScoreboardTag(TAG);
        });
    }

    private AxisAngle4f rotationTo(Vector direction) {
        Vector target = direction.clone().normalize();
        double dot = target.getY();
        if (dot > 1.0 - 1.0e-9) {
            return new AxisAngle4f();
        }
        if (dot < -1.0 + 1.0e-9) {
            return new AxisAngle4f((float) Math.PI, 1.0f, 0.0f, 0.0f);
        }
        Vector axis = new Vector(0, 1, 0).crossProduct(target).normalize();
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, dot)));
        return new AxisAngle4f((float) angle,
                (float) axis.getX(), (float) axis.getY(), (float) axis.getZ());
    }

    private Interaction spawnInteraction(World world, Location location, double width, double height,
            UUID playerId, int modelId) {
        Interaction interaction = world.spawn(location, Interaction.class, entity -> {
            entity.setInteractionWidth((float) Math.max(0.5, width));
            entity.setInteractionHeight((float) Math.max(0.5, height));
            entity.setResponsive(true);
            entity.setPersistent(false);
            entity.addScoreboardTag(TAG);
        });
        interactions.put(interaction.getUniqueId(), new ModelRef(playerId, modelId));
        return interaction;
    }

    private ItemDisplay spawnItem(World world, Location location, int[] rgb, float scale) {
        ItemStack item = new ItemStack(Material.PAPER);
        item.setData(DataComponentTypes.ITEM_MODEL, Key.key("cifviewer", "sphere"));
        item.setData(DataComponentTypes.DYED_COLOR,
                DyedItemColor.dyedItemColor(Color.fromRGB(rgb[0], rgb[1], rgb[2])));
        return world.spawn(location, ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setViewRange(1.5f);
            entity.setPersistent(false);
            entity.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f(),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f()));
            entity.addScoreboardTag(TAG);
        });
    }

    private TextDisplay spawn(World world, Location location, Component text, Color background, float scale,
            boolean shadowed) {
        return world.spawn(location, TextDisplay.class, entity -> {
            entity.text(text);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setSeeThrough(false);
            entity.setShadowed(shadowed);
            entity.setDefaultBackground(false);
            entity.setBackgroundColor(background);
            entity.setLineWidth(300);
            entity.setViewRange(1.5f);
            entity.setPersistent(false);
            entity.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f(),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f()));
            entity.addScoreboardTag(TAG);
        });
    }

    private ViewSession requireSession(Player player) {
        ViewSession session = session(player);
        if (session == null) {
            throw new IllegalStateException("No structure is being displayed. Use /cif view <file> first.");
        }
        return session;
    }

    private ViewSession requireEditableSession(Player player) {
        ViewSession session = requireSession(player);
        if (!canEdit(player, session)) {
            throw new IllegalStateException("このモデルを編集する権限がありません。");
        }
        return session;
    }

    private int luminance(int[] rgb) {
        return (int) Math.round(0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]);
    }

    private double normalize(double angle) {
        double normalized = angle % 360.0;
        return normalized < 0.0 ? normalized + 360.0 : normalized;
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record RawAtom(Crystal.Atom atom, double x, double y, double z) {
    }

    private record PolyhedronVertex(String element, Vector position) {
    }

    private record PointKey(long x, long y, long z) {
        private static PointKey of(Vector point) {
            return new PointKey(
                    Math.round(point.getX() * 10000.0),
                    Math.round(point.getY() * 10000.0),
                    Math.round(point.getZ() * 10000.0));
        }
    }
}
