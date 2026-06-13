package com.sda.mapper.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies health probes enabled by the container profile.
 */
@ActiveProfiles("container")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContainerHealthEndpointTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void exposesLivenessAndReadinessProbes() throws Exception {
        HttpResponse<String> livenessResponse = get("/actuator/health/liveness");
        HttpResponse<String> readinessResponse = get("/actuator/health/readiness");

        assertThat(livenessResponse.statusCode()).isEqualTo(200);
        assertThat(livenessResponse.body()).contains("\"status\":\"UP\"");
        assertThat(readinessResponse.statusCode()).isEqualTo(200);
        assertThat(readinessResponse.body()).contains("\"status\":\"UP\"");
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
