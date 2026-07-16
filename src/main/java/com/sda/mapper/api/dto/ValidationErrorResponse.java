package com.sda.mapper.api.dto;

import java.util.List;

/**
 * Standard validation error response for API endpoints.
 *
 * @param errors list of field-level errors
 */
public record ValidationErrorResponse(List<FieldError> errors) {

    /**
     * A single field-level validation error.
     *
     * @param field   the field name
     * @param message the error message
     */
    public record FieldError(String field, String message) {
    }
}
