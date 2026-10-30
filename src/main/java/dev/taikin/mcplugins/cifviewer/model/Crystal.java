package dev.taikin.mcplugins.cifviewer.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class Crystal {

    private final String name;
    private final Lattice lattice;
    private final List<Atom> atoms;

    public Crystal(String name, Lattice lattice, List<Atom> atoms) {
        if (atoms.isEmpty()) {
            throw new IllegalArgumentException("Crystal contains no atoms.");
        }
        this.name = name;
        this.lattice = lattice;
        this.atoms = Collections.unmodifiableList(atoms);
    }

    public String name() {
        return name;
    }

    public Lattice lattice() {
        return lattice;
    }

    public List<Atom> atoms() {
        return atoms;
    }

    public Map<String, Integer> elementCounts() {
        Map<String, Integer> counts = new TreeMap<>();
        for (Atom atom : atoms) {
            counts.merge(atom.element(), 1, Integer::sum);
        }
        return new LinkedHashMap<>(counts);
    }

    public String contents() {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, Integer> entry : elementCounts().entrySet()) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(entry.getKey());
            if (entry.getValue() > 1) {
                text.append(entry.getValue());
            }
        }
        return text.toString();
    }

    public record Atom(String element, double x, double y, double z, double occupancy, int charge,
            String label, double[] uij) {

        public Atom {
            uij = uij == null ? null : uij.clone();
        }

        public Atom(String element, double x, double y, double z, double occupancy, int charge,
                String label) {
            this(element, x, y, z, occupancy, charge, label, null);
        }

        @Override
        public double[] uij() {
            return uij == null ? null : uij.clone();
        }
    }
}
