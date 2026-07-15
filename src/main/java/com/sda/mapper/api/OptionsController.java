package com.sda.mapper.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for serving configuration options.
 */
@RestController
@RequestMapping("/api/v1/options")
public class OptionsController {

    private final OptionsConfigService optionsService;

    public OptionsController(OptionsConfigService optionsService) {
        this.optionsService = optionsService;
    }

    /**
     * Gets options for a specific field.
     *
     * @param fieldName the field name (e.g., "protocol", "accessMode")
     * @return list of options for the field
     */
    @GetMapping("/{fieldName}")
    public ResponseEntity<List<Option>> getOptions(@PathVariable String fieldName) {
        if (!optionsService.hasField(fieldName)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(optionsService.getOptions(fieldName));
    }

    /**
     * Gets all available option field names.
     *
     * @return list of field names
     */
    @GetMapping
    public ResponseEntity<List<String>> getFieldNames() {
        return ResponseEntity.ok(optionsService.getFieldNames());
    }
}
