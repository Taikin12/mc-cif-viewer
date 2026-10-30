package dev.taikin.mcplugins.cifviewer.cif;

import dev.taikin.mcplugins.cifviewer.model.Crystal;
import dev.taikin.miviewer.common.ElementInfo;
import dev.taikin.mcplugins.cifviewer.model.Lattice;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CifParser {

    private static final int MAX_ATOMS = 20000;
    private static final int MAX_SYMMETRY_OPERATIONS = 192;
    private static final double POSITION_TOLERANCE = 1.0e-4;

    public Crystal parse(String name, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("CIF file is empty.");
        }

        List<List<String>> blocks = splitBlocks(tokenize(text));
        IllegalArgumentException failure = null;
        for (List<String> block : blocks) {
            try {
                return parseBlock(name, block);
            } catch (IllegalArgumentException e) {
                if (failure == null) {
                    failure = e;
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
        throw new IllegalArgumentException("CIF contains no data.");
    }

    private Crystal parseBlock(String name, List<String> tokens) {
        ParsedData data = read(tokens);
        Lattice lattice = readLattice(data);
        Loop atomLoop = findAtomLoop(data.loops);
        if (atomLoop == null) {
            throw new IllegalArgumentException("No _atom_site coordinates found in CIF.");
        }

        List<Crystal.Atom> baseAtoms = readAtoms(atomLoop, lattice);
        if (baseAtoms.isEmpty()) {
            throw new IllegalArgumentException("No usable atom sites found in CIF.");
        }

        List<SymmetryOperation> operations = readSymmetry(data);
        List<Crystal.Atom> atoms = expand(baseAtoms, operations);
        if (atoms.isEmpty()) {
            throw new IllegalArgumentException("Symmetry expansion produced no atoms.");
        }
        return new Crystal(name, lattice, atoms);
    }

    private List<List<String>> splitBlocks(List<String> tokens) {
        List<List<String>> blocks = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String token : tokens) {
            if (token.toLowerCase(Locale.ROOT).startsWith("data_")) {
                if (!current.isEmpty()) {
                    blocks.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            current.add(token);
        }
        if (!current.isEmpty()) {
            blocks.add(current);
        }
        if (blocks.isEmpty()) {
            blocks.add(List.of());
        }
        return blocks;
    }

    private Lattice readLattice(ParsedData data) {
        double a = requiredNumber(value(data, "_cell_length_a"), "_cell_length_a");
        double b = requiredNumber(value(data, "_cell_length_b"), "_cell_length_b");
        double c = requiredNumber(value(data, "_cell_length_c"), "_cell_length_c");
        double alpha = numberOrDefault(value(data, "_cell_angle_alpha"), 90.0);
        double beta = numberOrDefault(value(data, "_cell_angle_beta"), 90.0);
        double gamma = numberOrDefault(value(data, "_cell_angle_gamma"), 90.0);
        return Lattice.fromCell(a, b, c, alpha, beta, gamma);
    }

    private List<Crystal.Atom> readAtoms(Loop loop, Lattice lattice) {
        List<Crystal.Atom> atoms = new ArrayList<>();
        for (Map<String, String> record : loop.records) {
            String label = record.getOrDefault("_atom_site_label", "");
            String type = record.getOrDefault("_atom_site_type_symbol", label);
            String element = ElementInfo.elementOf(type, label);
            if (element == null) {
                continue;
            }
            int charge = ElementInfo.oxidationState(type);
            if (charge == 0) {
                charge = ElementInfo.oxidationState(label);
            }
            if (charge == 0) {
                Double chargeValue = optionalNumber(record.get("_atom_site_charge"));
                if (chargeValue != null) {
                    charge = (int) Math.round(chargeValue);
                }
            }

            Double occupancyValue = optionalNumber(record.get("_atom_site_occupancy"));
            double occupancy = occupancyValue == null ? 1.0 : occupancyValue;
            if (occupancy <= 0.0) {
                continue;
            }

            double[] fractional = fractionalCoordinates(record, lattice);
            if (fractional == null) {
                continue;
            }
            atoms.add(new Crystal.Atom(
                    element,
                    fractional[0],
                    fractional[1],
                    fractional[2],
                    Math.min(1.0, occupancy),
                    charge,
                    label.isBlank() ? element : label));
        }
        return atoms;
    }

    private double[] fractionalCoordinates(Map<String, String> record, Lattice lattice) {
        Double fx = optionalNumber(record.get("_atom_site_fract_x"));
        Double fy = optionalNumber(record.get("_atom_site_fract_y"));
        Double fz = optionalNumber(record.get("_atom_site_fract_z"));
        if (fx != null && fy != null && fz != null) {
            return new double[] {wrap(fx), wrap(fy), wrap(fz)};
        }

        Double cx = optionalNumber(record.get("_atom_site_cartn_x"));
        Double cy = optionalNumber(record.get("_atom_site_cartn_y"));
        Double cz = optionalNumber(record.get("_atom_site_cartn_z"));
        if (cx != null && cy != null && cz != null) {
            double[] result = lattice.toFractional(cx, cy, cz);
            return new double[] {wrap(result[0]), wrap(result[1]), wrap(result[2])};
        }
        return null;
    }

    private List<SymmetryOperation> readSymmetry(ParsedData data) {
        Set<String> raw = new LinkedHashSet<>();
        addIfPresent(raw, value(data, "_symmetry_equiv_pos_as_xyz"));
        addIfPresent(raw, value(data, "_space_group_symop_operation_xyz"));

        for (Loop loop : data.loops) {
            collectColumn(raw, loop, "_symmetry_equiv_pos_as_xyz");
            collectColumn(raw, loop, "_space_group_symop_operation_xyz");
        }

        if (raw.isEmpty()) {
            raw.add("x,y,z");
        }
        if (raw.size() > MAX_SYMMETRY_OPERATIONS) {
            throw new IllegalArgumentException("Too many symmetry operations: " + raw.size());
        }

        List<SymmetryOperation> operations = new ArrayList<>(raw.size());
        for (String operation : raw) {
            operations.add(SymmetryOperation.parse(operation));
        }
        return operations;
    }

    private List<Crystal.Atom> expand(List<Crystal.Atom> baseAtoms, List<SymmetryOperation> operations) {
        Map<String, List<Crystal.Atom>> byElement = new LinkedHashMap<>();
        int count = 0;
        for (Crystal.Atom atom : baseAtoms) {
            List<Crystal.Atom> bucket = byElement.computeIfAbsent(atom.element(), key -> new ArrayList<>());
            for (SymmetryOperation operation : operations) {
                double[] position = operation.apply(atom.x(), atom.y(), atom.z());
                Crystal.Atom candidate = new Crystal.Atom(
                        atom.element(),
                        position[0],
                        position[1],
                        position[2],
                        atom.occupancy(),
                        atom.charge(),
                        atom.label());
                if (addUnique(bucket, candidate)) {
                    count++;
                    if (count > MAX_ATOMS) {
                        throw new IllegalArgumentException("CIF expands to more than " + MAX_ATOMS + " atoms.");
                    }
                }
            }
        }

        List<Crystal.Atom> result = new ArrayList<>(count);
        for (List<Crystal.Atom> bucket : byElement.values()) {
            result.addAll(bucket);
        }
        return result;
    }

    private boolean addUnique(List<Crystal.Atom> bucket, Crystal.Atom candidate) {
        for (int i = 0; i < bucket.size(); i++) {
            Crystal.Atom existing = bucket.get(i);
            if (samePosition(existing, candidate)) {
                if (candidate.occupancy() > existing.occupancy()) {
                    bucket.set(i, candidate);
                }
                return false;
            }
        }
        bucket.add(candidate);
        return true;
    }

    private boolean samePosition(Crystal.Atom first, Crystal.Atom second) {
        return close(first.x(), second.x()) && close(first.y(), second.y()) && close(first.z(), second.z());
    }

    private boolean close(double first, double second) {
        double difference = Math.abs(first - second);
        difference = Math.min(difference, 1.0 - difference);
        return difference <= POSITION_TOLERANCE;
    }

    private Loop findAtomLoop(List<Loop> loops) {
        for (Loop loop : loops) {
            if (loop.has("_atom_site_fract_x") || loop.has("_atom_site_cartn_x")) {
                return loop;
            }
        }
        return null;
    }

    private void collectColumn(Set<String> target, Loop loop, String tag) {
        int index = loop.tags.indexOf(tag);
        if (index < 0) {
            return;
        }
        for (Map<String, String> record : loop.records) {
            addIfPresent(target, record.get(tag));
        }
    }

    private void addIfPresent(Set<String> target, String value) {
        if (value != null && !value.isBlank() && !isMissing(value)) {
            target.add(value.trim());
        }
    }

    private String value(ParsedData data, String tag) {
        String scalar = data.scalars.get(tag);
        if (scalar != null) {
            return scalar;
        }
        for (Loop loop : data.loops) {
            if (loop.has(tag) && !loop.records.isEmpty()) {
                return loop.records.get(0).get(tag);
            }
        }
        return null;
    }

    private double requiredNumber(String value, String tag) {
        Double number = optionalNumber(value);
        if (number == null) {
            throw new IllegalArgumentException("Missing or invalid " + tag + " in CIF.");
        }
        return number;
    }

    private double numberOrDefault(String value, double fallback) {
        Double number = optionalNumber(value);
        return number == null ? fallback : number;
    }

    static Double optionalNumber(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.isEmpty() || isMissing(text)) {
            return null;
        }
        text = text.replaceAll("\\(\\d+\\)", "");
        text = text.replace('D', 'E').replace('d', 'e');
        if (text.matches("[-+]?\\d+/[-+]?\\d+")) {
            String[] parts = text.split("/");
            double denominator = Double.parseDouble(parts[1]);
            if (Math.abs(denominator) < 1.0e-12) {
                return null;
            }
            return Double.parseDouble(parts[0]) / denominator;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isMissing(String value) {
        String text = value.trim();
        return text.equals("?") || text.equals(".");
    }

    private double wrap(double value) {
        double wrapped = value - Math.floor(value);
        if (wrapped < 1.0e-9 || wrapped > 1.0 - 1.0e-9) {
            return 0.0;
        }
        return wrapped;
    }

    private ParsedData read(List<String> tokens) {
        Map<String, String> scalars = new LinkedHashMap<>();
        List<Loop> loops = new ArrayList<>();

        int index = 0;
        while (index < tokens.size()) {
            String token = tokens.get(index);
            String lower = token.toLowerCase(Locale.ROOT);

            if (lower.equals("loop_")) {
                index++;
                List<String> tags = new ArrayList<>();
                while (index < tokens.size() && tokens.get(index).startsWith("_")) {
                    tags.add(normalizeTag(tokens.get(index)));
                    index++;
                }
                if (tags.isEmpty()) {
                    continue;
                }
                List<String> values = new ArrayList<>();
                while (index < tokens.size() && !isStructural(tokens.get(index))) {
                    values.add(tokens.get(index));
                    index++;
                }
                int columns = tags.size();
                int rows = values.size() / columns;
                List<Map<String, String>> records = new ArrayList<>(rows);
                for (int row = 0; row < rows; row++) {
                    Map<String, String> record = new LinkedHashMap<>();
                    for (int column = 0; column < columns; column++) {
                        record.put(tags.get(column), values.get(row * columns + column));
                    }
                    records.add(record);
                }
                loops.add(new Loop(List.copyOf(tags), List.copyOf(records)));
                continue;
            }

            if (token.startsWith("_")) {
                index++;
                if (index < tokens.size() && !isStructural(tokens.get(index))) {
                    scalars.put(normalizeTag(token), tokens.get(index));
                    index++;
                }
                continue;
            }

            index++;
        }

        return new ParsedData(scalars, loops);
    }

    private String normalizeTag(String tag) {
        return tag.toLowerCase(Locale.ROOT).replace('.', '_');
    }

    private boolean isStructural(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        if (token.startsWith("_")) {
            return true;
        }
        return lower.equals("loop_") || lower.startsWith("data_") || lower.startsWith("save_") || lower.equals("stop_");
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        int index = 0;
        int length = text.length();

        while (index < length) {
            char current = text.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }
            if (current == '#') {
                while (index < length && text.charAt(index) != '\n') {
                    index++;
                }
                continue;
            }
            if (current == ';' && isLineStart(text, index)) {
                int contentStart = index + 1;
                if (contentStart < length && text.charAt(contentStart) == '\r') {
                    contentStart++;
                }
                if (contentStart < length && text.charAt(contentStart) == '\n') {
                    contentStart++;
                }
                int closing = findTextEnd(text, contentStart);
                if (closing < 0) {
                    throw new IllegalArgumentException("Unterminated semicolon text field in CIF.");
                }
                tokens.add(text.substring(contentStart, closing).strip());
                index = closing + 1;
                continue;
            }
            if (current == '\'' || current == '"') {
                char quote = current;
                index++;
                StringBuilder value = new StringBuilder();
                boolean closed = false;
                while (index < length) {
                    char c = text.charAt(index);
                    if (c == quote) {
                        if (index + 1 < length && text.charAt(index + 1) == quote) {
                            value.append(quote);
                            index += 2;
                            continue;
                        }
                        closed = true;
                        index++;
                        break;
                    }
                    value.append(c);
                    index++;
                }
                if (!closed) {
                    throw new IllegalArgumentException("Unterminated quoted value in CIF.");
                }
                tokens.add(value.toString());
                continue;
            }

            int start = index;
            while (index < length) {
                char c = text.charAt(index);
                if (Character.isWhitespace(c) || c == '#') {
                    break;
                }
                index++;
            }
            tokens.add(text.substring(start, index));
        }
        return tokens;
    }

    private boolean isLineStart(String text, int index) {
        if (index == 0) {
            return true;
        }
        char previous = text.charAt(index - 1);
        return previous == '\n' || previous == '\r';
    }

    private int findTextEnd(String text, int start) {
        int lineStart = start;
        int length = text.length();
        while (lineStart <= length) {
            if (lineStart < length && text.charAt(lineStart) == ';') {
                return lineStart;
            }
            int newline = text.indexOf('\n', lineStart);
            if (newline < 0) {
                return -1;
            }
            lineStart = newline + 1;
        }
        return -1;
    }

    private record ParsedData(Map<String, String> scalars, List<Loop> loops) {
    }

    private record Loop(List<String> tags, List<Map<String, String>> records) {
        private boolean has(String tag) {
            return tags.contains(tag);
        }
    }
}
