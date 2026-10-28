package dev.taikin.mcplugins.cifviewer.cif;

import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.taikin.mcplugins.cifviewer.model.Crystal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SampleCifsTest {

    @Test
    void parsesBundledSamples() throws IOException {
        try (Stream<Path> stream = Files.list(Path.of("samples"))) {
            List<Path> files = stream.filter(path -> path.toString().endsWith(".cif")).toList();
            assertFalse(files.isEmpty());
            for (Path path : files) {
                Crystal crystal = new CifParser().parse(path.getFileName().toString(), Files.readString(path));
                assertFalse(crystal.atoms().isEmpty(), path.toString());
            }
        }
    }
}
