package com.sda.mapper.hub.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static helpers for building and validating entity attribute maps.
 */
public final class Attributes {

    private Attributes() {
    }

    /**
     * Builds an insertion-ordered attribute map from alternating key/value pairs.
     * Pairs whose value is {@code null} are skipped.
     *
     * @param keyValues alternating key and value pairs
     * @return the attribute map
     */
    public static Map<String, Object> of(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            Object value = keyValues[i + 1];
            if (value != null) {
                map.put(String.valueOf(keyValues[i]), value);
            }
        }
        return map;
    }

    /**
     * Checks that all the given keys are present with non-blank values.
     *
     * @param attributes the attribute map to validate
     * @param keys       the keys that must be present and non-blank
     * @return {@code true} when all keys are satisfied
     */
    public static boolean hasAll(Map<String, Object> attributes, String... keys) {
        for (String key : keys) {
            Object value = attributes.get(key);
            if (value == null || String.valueOf(value).isBlank()) {
                return false;
            }
        }
        return true;
    }
}
