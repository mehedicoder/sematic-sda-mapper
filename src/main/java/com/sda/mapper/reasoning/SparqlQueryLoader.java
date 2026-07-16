package com.sda.mapper.reasoning;

import com.sda.mapper.error.OntologyLoadingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and caches SPARQL query files from the classpath.
 * Query files use the {@code .rq} extension and are located under
 * {@code src/main/resources/queries/}.
 */
@Component
public class SparqlQueryLoader {

    private static final Logger log = LoggerFactory.getLogger(SparqlQueryLoader.class);

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Loads a SPARQL query file from the classpath.
     * Subsequent calls with the same path return the cached content.
     *
     * @param queryPath classpath-relative path to the {@code .rq} file
     * @return the SPARQL query string
     * @throws OntologyLoadingException if the query file cannot be found or read
     */
    public String loadQuery(String queryPath) {
        return cache.computeIfAbsent(queryPath, path -> {
            log.debug("Loading SPARQL query from classpath: {}", path);
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                throw new OntologyLoadingException(
                    "SPARQL query file not found on classpath: " + path);
            }
            try (InputStream in = resource.getInputStream()) {
                return new String(in.readAllBytes());
            } catch (IOException e) {
                throw new OntologyLoadingException(
                    "Failed to read SPARQL query file: " + path, e);
            }
        });
    }
}
