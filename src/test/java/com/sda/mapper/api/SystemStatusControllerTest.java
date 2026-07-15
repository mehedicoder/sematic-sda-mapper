package com.sda.mapper.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Verifies the public smoke and operational health endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("System Status Controller")
class SystemStatusControllerTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Should return deterministic system status with version and application name")
    void returnsDeterministicSystemStatus() throws Exception {
        HttpResponse<String> response = get("/api/v1/system/status");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow())
                .startsWith("application/json");
        assertThat(response.body())
                .isEqualTo("{\"status\":\"UP\",\"application\":\"semantic-sda-mapper\"}");
    }

    @Test
    @DisplayName("Should expose actuator health endpoint")
    void exposesActuatorHealthEndpoint() throws Exception {
        HttpResponse<String> healthResponse = get("/actuator/health");

        assertThat(healthResponse.statusCode()).isEqualTo(200);
        assertThat(healthResponse.body()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("Should return 404 for unknown API paths")
    void rejectsUnknownApiPath() throws Exception {
        HttpResponse<String> response = get("/api/v1/system/unknown");

        assertThat(response.statusCode()).isIn(404, 500);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
