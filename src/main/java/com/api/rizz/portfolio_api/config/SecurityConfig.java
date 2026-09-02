package com.api.rizz.portfolio_api.config;

import com.api.rizz.portfolio_api.security.JwtAuthFilter;
import com.api.rizz.portfolio_api.security.RateLimitFilter;
import com.api.rizz.portfolio_api.service.UserDetailServiceImpl;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** SecurityConfig */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthFilter jwtAuthFilter;
  private final RateLimitFilter rateLimitFilter;
  private final UserDetailServiceImpl userDetailsService;

  // * Daftar origin yang diizinkan CORS, dipisah koma lewat env CORS_ALLOWED_ORIGINS
  // * (mis. "https://example.com,https://admin.example.com"). Default ke origin dev kalau
  // * env-nya tidak di-set, supaya tetap jalan di local tanpa konfigurasi tambahan.
  @Value("${app.cors.allowed-origins:http://localhost:5173}")
  private String allowedOrigins;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Mengubah otorisasi endpoint
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .authenticationProvider(authenticationProvider())
        // * JwtAuthFilter didaftarkan lebih dulu (relatif ke UsernamePasswordAuthenticationFilter,
        // * filter bawaan Spring Security yang order-nya sudah dikenal) supaya posisinya di filter
        // * chain "terdaftar". Baru setelah itu RateLimitFilter bisa dipasang relatif ke
        // * JwtAuthFilter.class - kalau dibalik, Spring Security akan melempar
        // * IllegalArgumentException "does not have a registered order" karena JwtAuthFilter
        // * belum dikenal saat itu.
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        // * RateLimitFilter dipasang paling depan (sebelum JwtAuthFilter) supaya request yang
        // * kelebihan kuota langsung ditolak 429 tanpa sempat diproses autentikasi.
        .addFilterBefore(rateLimitFilter, JwtAuthFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOrigins(
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList());

    // * Mengizinkan metode HTTP yang diperlukan
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

    // * Mengizinkan header yang dikirim dari klien (penting untuk JWT)
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

    // * Untuk cookie
    configuration.setAllowCredentials(true);

    // * Menerapkan konfigurasi ini ke semua endpoint (/**)
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
    return configuration.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
