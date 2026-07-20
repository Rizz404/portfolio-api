package com.api.rizz.portfolio_api.controller;

import com.api.rizz.portfolio_api.dto.request.LoginRequest;
import com.api.rizz.portfolio_api.dto.request.RegisterRequest;
import com.api.rizz.portfolio_api.dto.response.AuthResponse;
import com.api.rizz.portfolio_api.dto.response.SuccessResponse;
import com.api.rizz.portfolio_api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
  final AuthService authService;

  // * Jangan dihiraukan dulu soal role soalnya ini kan web portfolio
  @PostMapping("/register")
  public ResponseEntity<SuccessResponse<AuthResponse>> register(
      @RequestBody RegisterRequest request) {
    AuthResponse authResponse = authService.register(request);

    SuccessResponse<AuthResponse> successResponse =
        new SuccessResponse<>("User registered", authResponse);

    return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);
  }

  @PostMapping("/login")
  public ResponseEntity<SuccessResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
    AuthResponse authResponse = authService.login(request);

    SuccessResponse<AuthResponse> successResponse =
        new SuccessResponse<>("User logged", authResponse);

    return ResponseEntity.ok(successResponse);
  }
}
