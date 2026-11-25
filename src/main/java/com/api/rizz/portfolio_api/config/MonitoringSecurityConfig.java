package com.api.rizz.portfolio_api.config;

import com.api.rizz.portfolio_api.security.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
public class MonitoringSecurityConfig {

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public SecurityFilterChain monitoringFilterChain(
      HttpSecurity http,
      RateLimitFilter rateLimitFilter,
      @Value("${monitoring.metrics.password:}") String password)
      throws Exception {

    http.securityMatcher(EndpointRequest.to("prometheus"))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(cache -> cache.disable())
        .addFilterBefore(rateLimitFilter, BasicAuthenticationFilter.class);

    if (password.isBlank()) {
      return http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll()).build();
    }

    var encoder = new BCryptPasswordEncoder();

    var user =
        User.withUsername("prometheus")
            .password(encoder.encode(password))
            .roles("MONITORING")
            .build();

    var users = new InMemoryUserDetailsManager(user);
    var provider = new DaoAuthenticationProvider(users);
    provider.setPasswordEncoder(encoder);

    return http.authenticationManager(new ProviderManager(provider))
        .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("MONITORING"))
        .httpBasic(Customizer.withDefaults())
        .build();
  }
}
