package com.api.rizz.portfolio_api.service;

import com.api.rizz.portfolio_api.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  // * Claim pembeda access token vs refresh token, biar refresh token gak bisa dipakai
  // * menembus endpoint yang butuh access token (dicek di JwtAuthFilter).
  private static final String CLAIM_TOKEN_TYPE = "type";
  private static final String TOKEN_TYPE_ACCESS = "access";
  private static final String TOKEN_TYPE_REFRESH = "refresh";

  @Value("${app.jwt.secret}")
  private String secretKey;

  @Value("${app.jwt.expiration}")
  private long jwtExpiration;

  @Value("${app.jwt.refresh-expiration}")
  private long refreshExpiration;

  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);

    // * Sisipkan info yang sering dibutuhkan di sisi client (mis. buat tampilan) tanpa perlu
    // * decode lalu hit endpoint /users/me lagi. Tetap dianggap data non-sensitif & bukan
    // * sumber otorisasi utama - role tetap divalidasi ulang dari DB tiap request lewat
    // * UserDetailsService, klaim ini cuma buat kenyamanan client.
    if (userDetails instanceof User user) {
      claims.put("id", user.getId());
      claims.put("nickname", user.getNickname());
      claims.put("role", user.getRole().name());
    }

    return buildToken(claims, userDetails, jwtExpiration);
  }

  public String generateRefreshToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);

    // * Sengaja TIDAK menyisipkan id/nickname/role di refresh token - refresh token cuma
    // * dipakai buat menukar dirinya sendiri jadi access token baru (lihat AuthService#refresh),
    // * jadi payload-nya dijaga seminim mungkin.
    return buildToken(claims, userDetails, refreshExpiration);
  }

  private String buildToken(Map<String, Object> claims, UserDetails userDetails, long expiration) {
    return Jwts.builder()
        .claims(claims)
        .subject(userDetails.getUsername())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSigningKey())
        .compact();
  }

  // * Bisa melempar io.jsonwebtoken.JwtException (token expired/malformed/signature invalid)
  // * atau IllegalArgumentException (token null/kosong) karena tetap melakukan parsing token.
  // * Caller WAJIB menangkap/membiarkan exception ini naik ke GlobalExceptionHandler.
  public boolean isRefreshToken(String token) {
    String type = extractClaim(token, claims -> claims.get(CLAIM_TOKEN_TYPE, String.class));
    return TOKEN_TYPE_REFRESH.equals(type);
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
