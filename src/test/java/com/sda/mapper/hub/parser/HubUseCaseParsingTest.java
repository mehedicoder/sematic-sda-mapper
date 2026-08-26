package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
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
 * Verifies that every use-case input file under {@code docs/usecases} is
 * recognised by the hub's own {@link HubFileTypeDetector} and parsed into a
 * non-empty canonical model by the {@link CanonicalParserRegistry} that the
 * Mapping Hub UI actually uses.
 */
@DisplayName("Use-case files parse through the hub UI parser path")
class HubUseCaseParsingTest {

    private static final Path USE_CASES_DIR = Paths.get("docs", "usecases");

    private final HubFileTypeDetector detector = new HubFileTypeDetector();
    private final CanonicalParserRegistry registry = new CanonicalParserRegistry(List.of(
        new AasModelParser(), new MtpModelParser(), new OpcUaModelParser(),
        new OpcUaNodeSetXmlParser(), new AutomationMlParser(), new RequirementTtlParser()));

    @Test
    @DisplayName("Hub detector + registry should parse every supported use-case file")
    void shouldParseAllViaHubPath() throws Exception {
        Path base = resolveUseCasesDir();
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(base)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> {
                    String n = p.getFileName().toString().toLowerCase();
                    // The hub routes ABOX_TTL through parseAndMergeAbox, not the
                    // registry, and that path is covered by MappingHubAboxUploadTest.
                    if (n.endsWith(".ttl") && !(n.contains("req") || n.contains("requirement"))) {
                        return false;
                    }
                    return n.endsWith(".json") || n.endsWith(".xml") || n.endsWith(".aml") || n.endsWith(".ttl");
                })
                .forEach(files::add);
        }
        assertFalse(files.isEmpty(), "expected use-case files under " + base);

        List<org.junit.jupiter.api.function.Executable> checks = new ArrayList<>();
        for (Path file : files) {
            String name = file.toString();
            checks.add(() -> {
                try {
                    AssetFileFormat format;
                    try (InputStream detect = new BufferedInputStream(Files.newInputStream(file))) {
                        format = detector.detect(file.getFileName().toString(), detect)
                            .orElseThrow(() -> new IllegalStateException(name + " not recognised by hub detector"));
                    }
                    try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
                        CanonicalAssetModel model = registry.parse(in, file.getFileName().toString(), format);
                        assertNotNull(model, name + " produced null model");
                        assertFalse(model.allEntities().isEmpty(),
                            name + " (" + format + ") parsed to an empty canonical model");
                    }
                } catch (Exception e) {
                    fail(name + " failed on hub path: " + e.getMessage());
                }
            });
        }
        assertAll(checks);
    }

    private Path resolveUseCasesDir() throws Exception {
        if (Files.isDirectory(USE_CASES_DIR)) {
            return USE_CASES_DIR;
        }
        Path alt = Paths.get(System.getProperty("user.dir"), "docs", "usecases");
        if (Files.isDirectory(alt)) {
            return alt;
        }
        throw new IllegalStateException("Could not locate docs/usecases");
    }
}
