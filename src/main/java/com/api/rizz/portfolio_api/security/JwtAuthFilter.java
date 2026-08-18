package com.api.rizz.portfolio_api.security;

import com.api.rizz.portfolio_api.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    final String authHeader = request.getHeader("Authorization");

    // * Kalau tidak ada token atau bukan Bearer, skip filter ini (lanjut sebagai anonymous)
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    final String jwt = authHeader.substring(7);

    try {
      final String userEmail = jwtService.extractUsername(jwt);

      // * Kalau ada email tapi belum authenticated
      if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

        if (jwtService.isTokenValid(jwt, userDetails)) {
          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities());
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      }
    } catch (JwtException | IllegalArgumentException ex) {
      // * Token expired/malformed/signature invalid/kosong - JANGAN hentikan request di sini.
      // * Filter ini jalan sebelum DispatcherServlet, jadi exception yang lolos sampai keluar
      // * TIDAK akan ditangkap oleh GlobalExceptionHandler dan berujung 500 generic - termasuk
      // * untuk endpoint public yang seharusnya tetap bisa diakses tanpa token valid.
      // * Solusinya: treat sebagai anonymous saja. Endpoint public tetap jalan normal, endpoint
      // * yang butuh auth (@PreAuthorize) akan ditolak 401/403 secara wajar oleh Spring Security.
      log.warn(
          "Invalid JWT on {} {} ({}): {}",
          request.getMethod(),
          request.getRequestURI(),
          ex.getClass().getSimpleName(),
          ex.getMessage());
      SecurityContextHolder.clearContext();
    } catch (UsernameNotFoundException ex) {
      // * Token-nya valid, tapi user pemilik token sudah tidak ada (misal dihapus/dinonaktifkan).
      // * Sama seperti di atas, treat sebagai anonymous - jangan block request.
      log.warn(
          "JWT references unknown user on {} {}: {}",
          request.getMethod(),
          request.getRequestURI(),
          ex.getMessage());
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }
}
