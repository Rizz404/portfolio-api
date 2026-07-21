package com.api.rizz.portfolio_api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
/** RequestLoggingFilter */
public class RequestLoggingFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long startTime = System.currentTimeMillis();

    try {
      filterChain.doFilter(request, response);
    } finally {
      long duration = System.currentTimeMillis() - startTime;
      int status = response.getStatus();
      String method = request.getMethod();
      String requestUri = request.getRequestURI();

      logRequest(status, method, requestUri, duration);
    }
  }

  private void logRequest(int status, String method, String uri, long duration) {
    String logMessage =
        String.format("Request: [%s] %s - Status: %d - Time: %d ms", method, uri, status, duration);

    if (status >= 500) {
      // * Server Error (5xx)
      log.error("🔴 SERVER ERROR | {}", logMessage);
    } else if (status >= 400) {
      // * Client Error (4xx - misal: 404 Not Found, 401 Unauthorized)
      log.warn("🟡 CLIENT ERROR | {}", logMessage);
    } else {
      // * Success (2xx / 3xx)
      log.info("🟢 SUCCESS | {}", logMessage);
    }
  }
}
