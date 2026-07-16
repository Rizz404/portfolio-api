package com.api.rizz.portfolio_api.service;

import com.api.rizz.portfolio_api.dto.request.LoginRequest;
import com.api.rizz.portfolio_api.dto.request.RegisterRequest;
import com.api.rizz.portfolio_api.dto.response.AuthResponse;
import com.api.rizz.portfolio_api.entity.User;
import com.api.rizz.portfolio_api.entity.User.Role;
import com.api.rizz.portfolio_api.mapper.AuthMapper;
import com.api.rizz.portfolio_api.mapper.UserMapper;
import com.api.rizz.portfolio_api.repository.UserRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    User savedUser = userRepository.save(user);

    var token = jwtService.generateToken(user);

    return new AuthResponse(token, userMapper.toResponse(savedUser));
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

    return new AuthResponse(token, userMapper.toResponse(user));
  }
}
