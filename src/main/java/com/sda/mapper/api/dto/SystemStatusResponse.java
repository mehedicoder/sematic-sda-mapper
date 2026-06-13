package com.sda.mapper.api.dto;

/**
 * Stable response returned by the system smoke endpoint.
 *
 * @param status application status
 * @param application application identifier
 */
public record SystemStatusResponse(String status, String application) {
}
