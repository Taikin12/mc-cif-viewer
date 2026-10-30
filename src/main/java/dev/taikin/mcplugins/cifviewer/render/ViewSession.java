package dev.taikin.mcplugins.cifviewer.render;

import dev.taikin.mcplugins.cifviewer.model.Crystal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class ViewSession {

    public enum Axis {
        X,
        Y,
        Z
    }

    public enum RadiusMode {
        VDW,
        IONIC,
        COVALENT
    }

    public record AtomMarker(int index, String label, String element, Vector position, double radius, int[] rgb) {
    }

    private final UUID worldId;
    private final Crystal crystal;
    private int id;
    private UUID owner;
    private boolean sharedEdit;
    private Location origin;
    private double blocksPerAngstrom;
    private int repeatX;
    private int repeatY;
    private int repeatZ;
    private double rotationX;
    private double rotationY;
    private double rotationZ;
    private Axis rotationAxis = Axis.Y;
    private double atomScale;
    private boolean spaceFilling;
    private boolean showUnitCell;
    private boolean showBonds;
    private boolean showLabels;
    private boolean showPolyhedron;
    private boolean showPolyhedronFaces = true;
    private boolean showEllipsoids = true;
    private boolean showMiller;
    private int millerH = 1;
    private int millerK;
    private int millerL;
    private List<Integer> selectedAtoms = List.of();
    private List<Integer> polyhedronCenters = List.of();
    private List<AtomMarker> atomMarkers = List.of();
    private RadiusMode radiusMode = RadiusMode.VDW;
    private int renderedAtoms;
    private List<UUID> entities = List.of();
    private Set<Long> chunks = Set.of();

    ViewSession(Crystal crystal, Location origin, double blocksPerAngstrom, int repeat, double atomScale) {
        this.worldId = origin.getWorld().getUID();
        this.crystal = crystal;
        this.origin = origin.clone();
        this.blocksPerAngstrom = blocksPerAngstrom;
        this.repeatX = repeat;
        this.repeatY = repeat;
        this.repeatZ = repeat;
        this.atomScale = atomScale;
    }

    public UUID worldId() {
        return worldId;
    }

    public int id() {
        return id;
    }

    public UUID owner() {
        return owner;
    }

    public boolean sharedEdit() {
        return sharedEdit;
    }

    public Crystal crystal() {
        return crystal;
    }

    public Location origin() {
        return origin.clone();
    }

    public double blocksPerAngstrom() {
        return blocksPerAngstrom;
    }

    public int repeatX() {
        return repeatX;
    }

    public int repeatY() {
        return repeatY;
    }

    public int repeatZ() {
        return repeatZ;
    }

    public double rotationX() {
        return rotationX;
    }

    public double rotationY() {
        return rotationY;
    }

    public double rotationZ() {
        return rotationZ;
    }

    public Axis rotationAxis() {
        return rotationAxis;
    }

    public double atomScale() {
        return atomScale;
    }

    public boolean spaceFilling() {
        return spaceFilling;
    }

    public boolean showUnitCell() {
        return showUnitCell;
    }

    public boolean showBonds() {
        return showBonds;
    }

    public boolean showLabels() {
        return showLabels;
    }

    public boolean showPolyhedron() {
        return showPolyhedron;
    }

    public boolean showPolyhedronFaces() {
        return showPolyhedronFaces;
    }

    public boolean showEllipsoids() {
        return showEllipsoids;
    }

    public boolean showMiller() {
        return showMiller;
    }

    public int millerH() {
        return millerH;
    }

    public int millerK() {
        return millerK;
    }

    public int millerL() {
        return millerL;
    }

    public List<Integer> selectedAtoms() {
        return selectedAtoms;
    }

    public List<Integer> polyhedronCenters() {
        return polyhedronCenters;
    }

    List<AtomMarker> atomMarkers() {
        return atomMarkers;
    }

    public RadiusMode radiusMode() {
        return radiusMode;
    }

    public int renderedAtoms() {
        return renderedAtoms;
    }

    List<UUID> entities() {
        return entities;
    }

    Set<Long> chunks() {
        return chunks;
    }

    void chunks(Set<Long> chunks) {
        this.chunks = Set.copyOf(chunks);
    }

    void entities(List<UUID> entities) {
        this.entities = entities;
    }

    void renderedAtoms(int renderedAtoms) {
        this.renderedAtoms = renderedAtoms;
    }

    void id(int id) {
        this.id = id;
    }

    void owner(UUID owner) {
        this.owner = owner;
    }

    void sharedEdit(boolean sharedEdit) {
        this.sharedEdit = sharedEdit;
    }

    void origin(Location origin) {
        this.origin = origin.clone();
    }

    void blocksPerAngstrom(double blocksPerAngstrom) {
        this.blocksPerAngstrom = blocksPerAngstrom;
    }

    void repeat(int repeatX, int repeatY, int repeatZ) {
        this.repeatX = repeatX;
        this.repeatY = repeatY;
        this.repeatZ = repeatZ;
    }

    void repeatX(int repeatX) {
        this.repeatX = repeatX;
    }

    void repeatY(int repeatY) {
        this.repeatY = repeatY;
    }

    void repeatZ(int repeatZ) {
        this.repeatZ = repeatZ;
    }

    void rotation(double x, double y, double z) {
        this.rotationX = x;
        this.rotationY = y;
        this.rotationZ = z;
    }

    void rotationAxis(Axis rotationAxis) {
        this.rotationAxis = rotationAxis;
    }

    void atomScale(double atomScale) {
        this.atomScale = atomScale;
    }

    void spaceFilling(boolean spaceFilling) {
        this.spaceFilling = spaceFilling;
    }

    void showUnitCell(boolean showUnitCell) {
        this.showUnitCell = showUnitCell;
    }

    void showBonds(boolean showBonds) {
        this.showBonds = showBonds;
    }

    void showLabels(boolean showLabels) {
        this.showLabels = showLabels;
    }

    void showPolyhedron(boolean showPolyhedron) {
        this.showPolyhedron = showPolyhedron;
    }

    void showPolyhedronFaces(boolean showPolyhedronFaces) {
        this.showPolyhedronFaces = showPolyhedronFaces;
    }

    void showEllipsoids(boolean showEllipsoids) {
        this.showEllipsoids = showEllipsoids;
    }

    void showMiller(boolean showMiller) {
        this.showMiller = showMiller;
    }

    void miller(int h, int k, int l) {
        this.millerH = h;
        this.millerK = k;
        this.millerL = l;
    }

    void selectedAtoms(List<Integer> selectedAtoms) {
        this.selectedAtoms = List.copyOf(selectedAtoms);
    }

    void polyhedronCenters(List<Integer> polyhedronCenters) {
        this.polyhedronCenters = List.copyOf(polyhedronCenters);
    }

    void atomMarkers(List<AtomMarker> atomMarkers) {
        this.atomMarkers = List.copyOf(atomMarkers);
    }

    void radiusMode(RadiusMode radiusMode) {
        this.radiusMode = radiusMode;
    }
}
