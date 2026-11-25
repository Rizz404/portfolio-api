package com.api.rizz.portfolio_api;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "monitoring.metrics.password=")
class PortfolioApiApplicationTests {

  @Container
  static final PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:18-alpine")
          .withDatabaseName("portfolio_test")
          .withUsername("portfolio_test")
          .withPassword("portfolio_test_password");

  @LocalServerPort private int port;

  @DynamicPropertySource
  static void configureDatabase(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Test
  void contextLoads() {}

  @Test
  void metricsDeniedWhenPasswordNotConfigured() throws Exception {
    var response = get("/actuator/prometheus");
    assertEquals(403, response.statusCode());
  }

  @Test
  void coreHealthRemainsPublicWithoutInternalDetails() throws Exception {
    var response = get("/actuator/health/core");
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("\"UP\""));
    assertFalse(response.body().contains("\"components\""));
    assertFalse(response.body().contains("\"details\""));
  }

  private HttpResponse<String> get(String path) throws Exception {
    try (var client = HttpClient.newHttpClient()) {
      var request =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1" + path))
              .timeout(Duration.ofSeconds(15))
              .GET()
              .build();

      return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
  }
}
