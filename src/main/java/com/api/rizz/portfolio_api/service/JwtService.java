package com.api.rizz.portfolio_api.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  @Value("${app.jwt.secret}")
  private String secretKey;

  @Value("${app.jwt.expiration}")
  private long jwtExpiration;

  public String generateToken(UserDetails userDetails) {
    return Jwts.builder()
        .subject(userDetails.getUsername())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
        .signWith(getSigningKey())
        .compact();
  }

  // * Bisa melempar io.jsonwebtoken.JwtException (token expired/malformed/signature invalid)
  // * atau IllegalArgumentException (token null/kosong) karena tetap melakukan parsing token.
  // * Caller (JwtAuthFilter) WAJIB menangkap exception ini - jangan biarkan lolos begitu saja,
  // * karena kalau lolos dari filter, GlobalExceptionHandler tidak akan pernah menangkapnya.
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  // * Sama seperti extractUsername, method ini melakukan parsing ulang sehingga bisa melempar
  // * exception yang sama.
  public boolean isTokenValid(String token, UserDetails userDetails) {
    final Claims claims = extractAllClaims(token);
    return claims.getSubject().equals(userDetails.getUsername())
        && claims.getExpiration().after(new Date());
  }

  private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    return claimsResolver.apply(extractAllClaims(token));
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }

  private SecretKey getSigningKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
