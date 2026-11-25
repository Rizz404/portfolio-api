package com.api.rizz.portfolio_api;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MonitoringSecurityTests {

  private static final String PASSWORD = UUID.randomUUID().toString();

  @Container
  static final PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:18-alpine")
          .withDatabaseName("portfolio_test")
          .withUsername("portfolio_test")
          .withPassword("portfolio_test_password");

  @LocalServerPort private int port;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("monitoring.metrics.password", () -> PASSWORD);
  }

  @Test
  void metricsRequireAuthentication() throws Exception {
    assertEquals(401, get("/actuator/prometheus", null).statusCode());
  }

  @Test
  void metricsRejectInvalidUsername() throws Exception {
    assertEquals(401, get("/actuator/prometheus", basicAuth("invalid", PASSWORD)).statusCode());
  }

  @Test
  void metricsRejectInvalidPassword() throws Exception {
    assertEquals(
        401,
        get("/actuator/prometheus", basicAuth("prometheus", "wrong-" + PASSWORD)).statusCode());
  }

  @Test
  void metricsAcceptValidCredentials() throws Exception {
    var response = get("/actuator/prometheus", basicAuth("prometheus", PASSWORD));

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("# HELP"));
    assertTrue(response.body().contains("jvm_memory_used_bytes"));
  }

  @Test
  void publicApiRemainsAccessible() throws Exception {
    assertEquals(200, get("/projects", null).statusCode());
  }

  private String basicAuth(String username, String password) {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
  }

  private HttpResponse<String> get(String path, String authorization) throws Exception {
    try (var client = HttpClient.newHttpClient()) {
      var builder =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1" + path))
              .timeout(Duration.ofSeconds(15))
              .GET();

      if (authorization != null) {
        builder.header("Authorization", authorization);
      }

      return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
  }
}
