package com.api.rizz.portfolio_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // Menonaktifkan Cross-Site Request Forgery (CSRF) agar POST/PATCH/DELETE bisa
        // dieksekusi via Postman/Bruno
        .csrf(csrf -> csrf.disable())
        // Mengubah otorisasi endpoint
        .authorizeHttpRequests(auth -> auth
            .anyRequest().permitAll()); // Mengizinkan seluruh akses tanpa kredensial login

    return http.build();
  }
}
