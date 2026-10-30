package dev.taikin.mcplugins.cifviewer.mp;

import dev.taikin.mcplugins.cifviewer.CifConfig;
import dev.taikin.mcplugins.cifviewer.model.Crystal;
import dev.taikin.miviewer.common.ElementInfo;
import dev.taikin.mcplugins.cifviewer.model.Lattice;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class MaterialsProjectClient {

    private final CifConfig config;
    private final HttpClient client;

    public MaterialsProjectClient(CifConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.materialsProjectTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public boolean enabled() {
        return config.materialsProjectEnabled();
    }

    public CompletableFuture<Crystal> fetch(String materialId) {
        if (!enabled()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Materials Project API key is not configured."));
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(url(materialId)))
                .timeout(Duration.ofSeconds(config.materialsProjectTimeoutSeconds()))
                .header("X-API-KEY", config.materialsProjectApiKey())
                .header("Accept", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new CompletionException(new IllegalStateException(errorMessage(response)));
                    }
                    return parse(materialId, response.body());
                });
    }

    private String url(String materialId) {
        String endpoint = config.materialsProjectEndpoint();
        String encoded = URLEncoder.encode(materialId, StandardCharsets.UTF_8);
        if (endpoint.contains("{id}")) {
            return endpoint.replace("{id}", encoded);
        }
        String base = endpoint.endsWith("/") ? endpoint : endpoint + "/";
        return base + "?material_ids=" + encoded + "&_fields=structure";
    }

    private Crystal parse(String materialId, String body) {
        Map<String, Object> root = object(JsonParser.parse(body), "response");
        Map<String, Object> document = root;
        Object data = root.get("data");
        if (data instanceof List<?> list) {
            if (list.isEmpty()) {
                throw new IllegalStateException("Materials Project returned no data for " + materialId + ".");
            }
            document = object(list.get(0), "data[0]");
        }

        Object structureValue = document.get("structure");
        if (!(structureValue instanceof Map<?, ?>)) {
            structureValue = root.get("structure");
        }
        Map<String, Object> structure = object(structureValue, "structure");
        Lattice lattice = lattice(object(structure.get("lattice"), "lattice"));

        Object sitesValue = structure.get("sites");
        if (!(sitesValue instanceof List<?> sites) || sites.isEmpty()) {
            throw new IllegalStateException("Materials Project structure contains no sites.");
        }

        List<Crystal.Atom> atoms = new ArrayList<>();
        for (Object siteValue : sites) {
            Map<String, Object> site = object(siteValue, "site");
            double[] fractional = coordinates(site, lattice);
            if (fractional == null) {
                continue;
            }
            String label = string(site.get("label"), "");
            Object speciesValue = site.get("species");
            if (speciesValue instanceof List<?> speciesList && !speciesList.isEmpty()) {
                for (Object speciesValueItem : speciesList) {
                    Map<String, Object> species = object(speciesValueItem, "species");
                    String element = ElementInfo.elementOf(string(species.get("element"), ""), label);
                    if (element == null) {
                        continue;
                    }
                    double occupancy = number(species.get("occu"), 1.0);
                    if (occupancy <= 0.0) {
                        continue;
                    }
                    int charge = (int) Math.round(number(species.get("oxidation_state"), 0.0));
                    if (charge == 0) {
                        charge = ElementInfo.oxidationState(label);
                    }
                    atoms.add(new Crystal.Atom(element, fractional[0], fractional[1], fractional[2],
                            Math.min(1.0, occupancy), charge, label.isBlank() ? element : label));
                }
            } else {
                String element = ElementInfo.elementOf(label, label);
                if (element != null) {
                    int charge = ElementInfo.oxidationState(label);
                    atoms.add(new Crystal.Atom(element, fractional[0], fractional[1], fractional[2], 1.0,
                            charge, label.isBlank() ? element : label));
                }
            }
        }

        if (atoms.isEmpty()) {
            throw new IllegalStateException("Materials Project structure contains no usable sites.");
        }
        return new Crystal(materialId, lattice, atoms);
    }

    private Lattice lattice(Map<String, Object> lattice) {
        Object matrixValue = lattice.get("matrix");
        if (matrixValue instanceof List<?>) {
            List<?> rows = (List<?>) matrixValue;
            if (rows.size() != 3) {
                throw new IllegalStateException("Materials Project lattice matrix must have three rows.");
            }
            double[][] matrix = new double[3][3];
            for (int row = 0; row < 3; row++) {
                List<?> columns = list(rows.get(row), "lattice row");
                if (columns.size() != 3) {
                    throw new IllegalStateException("Materials Project lattice matrix must have three columns.");
                }
                for (int column = 0; column < 3; column++) {
                    matrix[row][column] = number(columns.get(column), Double.NaN);
                }
            }
            return Lattice.fromMatrix(matrix);
        }

        return Lattice.fromCell(
                number(lattice.get("a"), Double.NaN),
                number(lattice.get("b"), Double.NaN),
                number(lattice.get("c"), Double.NaN),
                number(lattice.get("alpha"), 90.0),
                number(lattice.get("beta"), 90.0),
                number(lattice.get("gamma"), 90.0));
    }

    private double[] coordinates(Map<String, Object> site, Lattice lattice) {
        Object abcValue = site.get("abc");
        if (abcValue instanceof List<?> abc && abc.size() == 3) {
            double x = number(abc.get(0), Double.NaN);
            double y = number(abc.get(1), Double.NaN);
            double z = number(abc.get(2), Double.NaN);
            if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
                return new double[] {wrap(x), wrap(y), wrap(z)};
            }
        }

        Object xyzValue = site.get("xyz");
        if (xyzValue instanceof List<?> xyz && xyz.size() == 3) {
            double x = number(xyz.get(0), Double.NaN);
            double y = number(xyz.get(1), Double.NaN);
            double z = number(xyz.get(2), Double.NaN);
            if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
                double[] result = lattice.toFractional(x, y, z);
                return new double[] {wrap(result[0]), wrap(result[1]), wrap(result[2])};
            }
        }
        return null;
    }

    private String errorMessage(HttpResponse<String> response) {
        String body = response.body();
        if (body != null && !body.isBlank()) {
            try {
                Map<String, Object> root = object(JsonParser.parse(body), "error");
                String message = string(root.get("message"), "");
                if (message.isBlank()) {
                    message = string(root.get("error"), "");
                }
                if (!message.isBlank()) {
                    return "Materials Project API error (" + response.statusCode() + "): " + message;
                }
            } catch (RuntimeException ignored) {
            }
            String trimmed = body.strip();
            if (trimmed.length() > 240) {
                trimmed = trimmed.substring(0, 240) + "...";
            }
            return "Materials Project API error (" + response.statusCode() + "): " + trimmed;
        }
        return "Materials Project API error (HTTP " + response.statusCode() + ").";
    }

    private Map<String, Object> object(Object value, String description) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalStateException("Materials Project response is missing " + description + ".");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) map;
        return result;
    }

    private List<?> list(Object value, String description) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalStateException("Materials Project response is missing " + description + ".");
        }
        return list;
    }

    private String string(Object value, String fallback) {
        return value instanceof String text ? text : fallback;
    }

    private double number(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private double wrap(double value) {
        double wrapped = value - Math.floor(value);
        if (wrapped < 1.0e-9 || wrapped > 1.0 - 1.0e-9) {
            return 0.0;
        }
        return wrapped;
    }
}
