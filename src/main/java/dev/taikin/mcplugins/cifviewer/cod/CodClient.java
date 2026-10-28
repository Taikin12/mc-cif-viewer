package dev.taikin.mcplugins.cifviewer.cod;

import dev.taikin.mcplugins.cifviewer.CifConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class CodClient {

    private final CifConfig config;
    private final Path cacheDir;
    private final HttpClient client;

    public CodClient(CifConfig config, Path cacheDir) {
        this.config = config;
        this.cacheDir = cacheDir;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.codTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Path cachePath(String id) {
        if (!id.matches("\\d+")) {
            throw new IllegalArgumentException("COD ID must be numeric.");
        }
        return cacheDir.resolve("cod-" + id + ".cif");
    }

    public CompletableFuture<String> fetch(String id) {
        Path cache = cachePath(id);
        URI uri = URI.create(baseUrl() + id + ".cif");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(config.codTimeoutSeconds()))
                .header("User-Agent", "mc-cif-viewer")
                .header("Accept", "chemical/x-cif,text/plain,*/*")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.statusCode() == 404) {
                        throw new CompletionException(new IllegalStateException(
                                "COD entry not found: " + id));
                    }
                    if (response.statusCode() / 100 != 2) {
                        throw new CompletionException(new IllegalStateException(
                                "COD returned HTTP " + response.statusCode() + " for " + id));
                    }
                    byte[] body = response.body();
                    if (body.length > config.codMaxBytes()) {
                        throw new CompletionException(new IllegalStateException(
                                "COD CIF is too large (" + body.length + " bytes)."));
                    }
                    return new String(body, StandardCharsets.UTF_8);
                });
    }

    public Path save(String id, String text) throws IOException {
        Path path = cachePath(id);
        Files.createDirectories(path.getParent());
        Files.writeString(path, text, StandardCharsets.UTF_8);
        return path;
    }

    private String baseUrl() {
        String base = config.codBaseUrl();
        return base.endsWith("/") ? base : base + "/";
    }
}
