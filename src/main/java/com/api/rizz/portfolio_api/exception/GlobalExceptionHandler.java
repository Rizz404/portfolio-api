package com.api.rizz.portfolio_api.exception;

import com.api.rizz.portfolio_api.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ErrorResponse<String>> handleNotFoundException(
      NoSuchElementException ex, HttpServletRequest request) {
    log.warn("Data tidak ditemukan: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponse<String> response =
        new ErrorResponse<>("error", ex.getMessage(), "Path: " + request.getRequestURI());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse<String>> handleIllegalArgumentException(
      IllegalArgumentException ex, HttpServletRequest request) {
    log.warn("Validasi gagal: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponse<String> response = new ErrorResponse<>("error", ex.getMessage(), null);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse<String>> handleGlobalException(
      Exception ex, HttpServletRequest request) {
    log.error("Terjadi kesalahan sistem di path: {}", request.getRequestURI(), ex);

    ErrorResponse<String> response =
        new ErrorResponse<>(
            "error", "Terjadi kesalahan pada server. Silakan coba beberapa saat lagi.", null);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}
