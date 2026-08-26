package com.sda.mapper.parser;

import com.sda.mapper.hub.parser.RequirementTtlParser;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies that every device-description and requirement file shipped with the
 * documented use cases in {@code docs/usecases} parses successfully through the
 * same code paths the running application uses.
 */
@DisplayName("All documented use-case input files parse successfully")
class UseCaseDocumentsParsingTest {

    private static final Path USE_CASES_DIR = Paths.get("docs", "usecases");

    private final ParserFactory parserFactory = new ParserFactory(
        new MTPAmlParser(), new UaServerDatasheetParser(),
        new OpcUaNodeSetParser(), new AasParser());
    private final UploadService uploadService = new UploadService(parserFactory);

    @Test
    @DisplayName("Should parse every device-description file in docs/usecases without error")
    void shouldParseAllDeviceDescriptions() throws IOException {
        Path base = resolveUseCasesDir();
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(base)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> {
                    String n = p.getFileName().toString().toLowerCase();
                    return n.endsWith(".json") || n.endsWith(".xml") || n.endsWith(".aml");
                })
                .forEach(files::add);
        }
        assertFalse(files.isEmpty(), "expected to find use-case device-description files under " + base);

        List<org.junit.jupiter.api.function.Executable> checks = new ArrayList<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            checks.add(() -> {
                try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
                    ParseResult result = uploadService.parseUpload(name, in);
                    assertNotNull(result, name + " produced null result");
                    assertNotNull(result.model(), name + " produced null model");
                    assertFalse(result.model().isEmpty(),
                        name + " parsed to an empty model (0 triples)");
                } catch (Exception e) {
                    fail(name + " failed to parse: " + e.getMessage());
                }
            });
        }
        assertAll(checks);
    }

    @Test
    @DisplayName("Should parse every requirement TTL in docs/usecases into a non-empty model")
    void shouldParseAllRequirementTtl() throws IOException {
        Path base = resolveUseCasesDir();
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(base)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> {
                    String n = p.getFileName().toString().toLowerCase();
                    return n.endsWith(".ttl")
                        && (n.contains("req") || n.contains("requirement"));
                })
                .forEach(files::add);
        }
        assertFalse(files.isEmpty(), "expected to find use-case requirement TTL files under " + base);

        List<org.junit.jupiter.api.function.Executable> checks = new ArrayList<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            checks.add(() -> {
                try {
                    RequirementTtlParser parser = new RequirementTtlParser();
                    CanonicalAssetModel model = parser.parse(
                        new BufferedInputStream(Files.newInputStream(file)), name);
                    assertNotNull(model, name + " produced null model");
                    boolean hasContent = !model.requirements().isEmpty()
                        || !model.requiredCapabilities().isEmpty();
                    assertFalse(model.requirements().isEmpty() && model.requiredCapabilities().isEmpty(),
                        name + " produced neither requirements nor required capabilities");
                } catch (Exception e) {
                    fail(name + " failed to parse: " + e.getMessage());
                }
            });
        }
        assertAll(checks);
    }

    private Path resolveUseCasesDir() throws IOException {
        if (Files.isDirectory(USE_CASES_DIR)) {
            return USE_CASES_DIR;
        }
        // Fallback when tests run from a different working directory.
        Path alt = Paths.get(System.getProperty("user.dir"), "docs", "usecases");
        if (Files.isDirectory(alt)) {
            return alt;
        }
        throw new IOException("Could not locate docs/usecases directory (cwd="
            + System.getProperty("user.dir") + ")");
    }
}
