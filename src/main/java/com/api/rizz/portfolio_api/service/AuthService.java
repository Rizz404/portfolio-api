package com.api.rizz.portfolio_api.service;

import com.api.rizz.portfolio_api.dto.request.LoginRequest;
import com.api.rizz.portfolio_api.dto.request.RefreshTokenRequest;
import com.api.rizz.portfolio_api.dto.request.RegisterRequest;
import com.api.rizz.portfolio_api.dto.response.AuthResponse;
import com.api.rizz.portfolio_api.entity.LanguageCode;
import com.api.rizz.portfolio_api.entity.User;
import com.api.rizz.portfolio_api.entity.User.Role;
import com.api.rizz.portfolio_api.entity.UserTranslation;
import com.api.rizz.portfolio_api.mapper.AuthMapper;
import com.api.rizz.portfolio_api.mapper.UserMapper;
import com.api.rizz.portfolio_api.repository.UserRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // * Otomatis buatin Dependency Injection buat variabel "final"
/** AuthService */
public class AuthService {
  private final UserRepository userRepository;
  private final AuthMapper authMapper;
  private final UserMapper userMapper;
  private final SnowflakeGenerator snowflakeGenerator;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (!request.password().equals(request.confirmPassword())) {
      throw new IllegalArgumentException("Password and Confirm Password do not match");
    }

    if (userRepository.existsByEmail(request.email())) {
      throw new IllegalArgumentException("Email has been used");
    }

    User user = authMapper.toEntity(request);

    user.setId(snowflakeGenerator.nextId());
    user.setPassword(passwordEncoder.encode(request.password()));
    user.setRole(Role.USER);
    user.setProvider(User.AuthProvider.LOCAL);
    // * RegisterRequest gak punya field bio - tetap wajib bikin 1 row locale 'en' (bio null)
    // * biar TranslationResolver ada yang bisa di-resolve pas toResponse()
    user.setTranslations(
        List.of(UserTranslation.builder().user(user).locale(LanguageCode.en).bio(null).build()));

    // * Set timestamp manual karena pakai snowflakes jadi ada write behind pada hibernate
    OffsetDateTime now = OffsetDateTime.now();
    user.setCreatedAt(now);
    user.setUpdatedAt(now);

    User savedUser = userRepository.save(user);

    var token = jwtService.generateToken(user);
    var refreshToken = jwtService.generateRefreshToken(user);

    return new AuthResponse(token, refreshToken, userMapper.toResponse(savedUser));
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    String token = jwtService.generateToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    return new AuthResponse(token, refreshToken, userMapper.toResponse(user));
  }

  // * Menukar refresh token yang masih valid jadi pasangan access+refresh token baru, tanpa
  // * user perlu login ulang pakai password. Refresh token lama otomatis "habis masa pakai"-nya
  // * secara alami begitu expired - karena stateless (bukan DB-backed), tidak ada mekanisme
  // * revoke sebelum expired (mis. saat logout paksa/ganti password); trade-off ini sadar
  // * diambil demi kesederhanaan, lihat catatan di JwtService.
  @Transactional
  public AuthResponse refresh(RefreshTokenRequest request) {
    String refreshToken = request.refreshToken();

    // * Tolak kalau ini access token (atau token rusak/expired) - keduanya dilempar sebagai
    // * JwtException/IllegalArgumentException yang sudah ditangani GlobalExceptionHandler.
    if (!jwtService.isRefreshToken(refreshToken)) {
      throw new IllegalArgumentException("Token is not a refresh token");
    }

    String email = jwtService.extractUsername(refreshToken);
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (!jwtService.isTokenValid(refreshToken, user)) {
      throw new IllegalArgumentException("Refresh token is invalid or expired");
    }

    String newToken = jwtService.generateToken(user);
    String newRefreshToken = jwtService.generateRefreshToken(user);

    return new AuthResponse(newToken, newRefreshToken, userMapper.toResponse(user));
  }
}
