package com.api.rizz.portfolio_api.exception;

import com.api.rizz.portfolio_api.dto.response.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse<Map<String, String>>> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    log.warn("Request validation failed - Path: {}", request.getRequestURI());

    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
    }

    ErrorResponse<Map<String, String>> response =
        new ErrorResponse<>("error", "Validation failed", fieldErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ErrorResponse<String>> handleNotFoundException(
      NoSuchElementException ex, HttpServletRequest request) {
    log.warn("Data not found: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponse<String> response =
        new ErrorResponse<>("error", ex.getMessage(), "Path: " + request.getRequestURI());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse<String>> handleIllegalArgumentException(
      IllegalArgumentException ex, HttpServletRequest request) {
    log.warn("Validation failed: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponse<String> response = new ErrorResponse<>("error", ex.getMessage(), null);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse<String>> handleAuthenticationException(
      AuthenticationException ex, HttpServletRequest request) {
    // * Meng-cover BadCredentialsException & UsernameNotFoundException dari proses login
    // * (AuthService.login -> authenticationManager.authenticate()). Pesan sengaja digeneralisir
    // * (tidak membedakan "email tidak ada" vs "password salah") biar tidak bocorin info akun ke
    // * client, detail asli tetap di-log di server.
    log.warn("Authentication failed: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponse<String> response =
        new ErrorResponse<>("error", "Invalid email or password", null);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse<String>> handleAccessDeniedException(
      AccessDeniedException ex, HttpServletRequest request) {
    // * Meng-cover kegagalan @PreAuthorize (termasuk AuthorizationDeniedException di Spring
    // * Security 6, yang merupakan subclass dari AccessDeniedException) - baik karena belum
    // * login sama sekali maupun karena role/permission tidak cukup.
    log.warn("Access denied: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponse<String> response =
        new ErrorResponse<>("error", "You don't have permission to access this resource", null);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse<String>> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException ex, HttpServletRequest request) {
    // * Ditangkap di level servlet (spring.servlet.multipart.max-file-size /
    // * max-request-size) sebelum request sempat sampai ke validasi custom di
    // * FileUploadService, misalnya kalau total ukuran request-nya sudah kelewatan duluan.
    log.warn("Upload size exceeded - Path: {}", request.getRequestURI());

    ErrorResponse<String> response =
        new ErrorResponse<>("error", "Uploaded file(s) exceed the maximum allowed size", null);
    return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(response);
  }

  @ExceptionHandler(JwtException.class)
  public ResponseEntity<ErrorResponse<String>> handleJwtException(
      JwtException ex, HttpServletRequest request) {
    // * Jaring pengaman: dalam kondisi normal JwtAuthFilter sudah menangkap JwtException
    // * duluan sebelum request sampai ke controller. Handler ini hanya jaga-jaga kalau ada
    // * pemanggilan JwtService lain di masa depan yang lupa menangani exception-nya sendiri.
    log.warn("Invalid JWT: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponse<String> response = new ErrorResponse<>("error", "Invalid or expired token", null);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse<String>> handleGlobalException(
      Exception ex, HttpServletRequest request) {
    log.error("A system error occurred at path: {}", request.getRequestURI(), ex);

    ErrorResponse<String> response =
        new ErrorResponse<>(
            "error", "An error occurred on the server. Please try again later.", null);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}
