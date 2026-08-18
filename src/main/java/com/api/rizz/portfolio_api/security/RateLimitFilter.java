package com.api.rizz.portfolio_api.security;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * RateLimitFilter
 *
 * <p>Membatasi jumlah request per client (per IP) dalam periode waktu tertentu memakai fixed window
 * counter yang disimpan in-memory. Kalau limit-nya terlampaui, request langsung ditolak dengan 429
 * Too Many Requests beserta header Retry-After (detik) tanpa membebani lapisan autentikasi/database
 * di baliknya.
 *
 * <p>* Diletakkan paling depan di security filter chain (lihat SecurityConfig) supaya percobaan *
 * brute force ke endpoint login pun sudah kena batas sebelum sempat diproses *
 * JwtAuthFilter/AuthenticationManager.
 *
 * <p>* Catatan: state disimpan in-memory per instance aplikasi. Kalau nanti di-deploy multi *
 * instance (horizontal scaling), pertimbangkan pindah ke penyimpanan terpusat (mis. Redis) * supaya
 * limit-nya konsisten antar instance.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private final int capacity;
  private final long windowMillis;
  private final Map<String, RequestWindow> buckets = new ConcurrentHashMap<>();
  private final ScheduledExecutorService cleanupExecutor =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "rate-limit-cleanup");
            thread.setDaemon(true);
            return thread;
          });

  public RateLimitFilter(
      @Value("${app.rate-limit.capacity:100}") int capacity,
      @Value("${app.rate-limit.window-seconds:60}") long windowSeconds) {
    this.capacity = capacity;
    this.windowMillis = TimeUnit.SECONDS.toMillis(windowSeconds);

    // * Bersihkan entry yang sudah tidak aktif secara berkala biar map tidak membengkak tanpa
    // * batas kalau banyak IP unik yang pernah request (mis. karena scanning/bot).
    cleanupExecutor.scheduleAtFixedRate(
        this::evictStaleEntries, windowSeconds, windowSeconds, TimeUnit.SECONDS);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String clientKey = resolveClientKey(request);
    RequestWindow window = buckets.computeIfAbsent(clientKey, key -> new RequestWindow());

    long retryAfterMillis;
    boolean limitExceeded;
    synchronized (window) {
      long now = System.currentTimeMillis();
      if (now - window.startedAt >= windowMillis) {
        window.startedAt = now;
        window.count = 0;
      }
      window.count++;
      retryAfterMillis = windowMillis - (now - window.startedAt);
      limitExceeded = window.count > capacity;
    }

    if (limitExceeded) {
      log.warn(
          "Rate limit exceeded for {} on {} {}",
          clientKey,
          request.getMethod(),
          request.getRequestURI());
      writeTooManyRequests(response, retryAfterMillis);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private void writeTooManyRequests(HttpServletResponse response, long retryAfterMillis)
      throws IOException {
    long retryAfterSeconds = Math.max(1, (retryAfterMillis + 999) / 1000);

    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

    // * Ditulis manual (bukan lewat ErrorResponse + ObjectMapper) karena classic
    // * com.fasterxml.jackson:jackson-databind di project ini hanya ada di runtime scope (lewat
    // * jjwt-jackson), jadi tidak bisa dipakai di compile time. Payload-nya tetap-format sama
    // * dengan ErrorResponse ("status"/"message") supaya konsisten di sisi klien.
    String body =
        "{\"status\":\"error\",\"message\":\"Too many requests. Please try again in "
            + retryAfterSeconds
            + " second(s).\"}";
    response.getWriter().write(body);
  }

  // * Prioritaskan X-Forwarded-For kalau aplikasi ada di belakang reverse proxy/load balancer
  // * (mis. Nginx, Railway, Render), fallback ke remote address langsung kalau tidak ada.
  // * Trade-off: header ini bisa dipalsukan client kalau tidak ada proxy tepercaya di depan
  // * aplikasi - cukup aman untuk skala portfolio API ini, tapi kalau butuh lebih ketat sebaiknya
  // * hanya percaya header ini dari proxy yang dikenal (configured trusted proxy).
  private String resolveClientKey(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private void evictStaleEntries() {
    long now = System.currentTimeMillis();
    buckets.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= windowMillis * 2);
  }

  @PreDestroy
  public void shutdown() {
    cleanupExecutor.shutdownNow();
  }

  private static class RequestWindow {
    private volatile long startedAt = System.currentTimeMillis();
    private int count = 0;
  }
}
