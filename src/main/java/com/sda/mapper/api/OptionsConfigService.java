package com.sda.mapper.api;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Loads configuration options from classpath JSON file.
 */
@Service
public class OptionsConfigService {

    private static final Logger log = LoggerFactory.getLogger(OptionsConfigService.class);
    private static final String OPTIONS_FILE = "config/options.json";

    private final ObjectMapper objectMapper;
    private Map<String, List<Option>> options = Collections.emptyMap();

    public OptionsConfigService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        loadOptions();
    }

    private void loadOptions() {
        try {
            ClassPathResource resource = new ClassPathResource(OPTIONS_FILE);
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    options = objectMapper.readValue(inputStream, 
                        new TypeReference<Map<String, List<Option>>>() {});
                    log.info("Loaded {} option categories from {}", options.size(), OPTIONS_FILE);
                }
            } else {
                log.warn("Options file not found: {}", OPTIONS_FILE);
            }
        } catch (IOException e) {
            log.error("Failed to load options from {}: {}", OPTIONS_FILE, e.getMessage());
            throw new RuntimeException("Failed to load options configuration", e);
        }
    }

    /**
     * Gets options for a specific field.
     *
     * @param fieldName the field name (e.g., "protocol", "accessMode")
     * @return list of options for the field, or empty list if not found
     */
    public List<Option> getOptions(String fieldName) {
        return options.getOrDefault(fieldName, Collections.emptyList());
    }

    /**
     * Gets all available option field names.
     *
     * @return list of field names
     */
    public List<String> getFieldNames() {
        return List.copyOf(options.keySet());
    }

    /**
     * Checks if a field exists in the options.
     *
     * @param fieldName the field name to check
     * @return true if the field exists, false otherwise
     */
    public boolean hasField(String fieldName) {
        return options.containsKey(fieldName);
    }
}
