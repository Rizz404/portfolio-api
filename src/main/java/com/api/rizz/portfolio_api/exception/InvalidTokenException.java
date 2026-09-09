package com.api.rizz.portfolio_api.exception;

/**
 * InvalidTokenException
 *
 * <p>* Dilempar buat kegagalan refresh token yang levelnya "bukan masalah bentuk request" (400),
 * tapi "kredensial/token-nya tidak bisa dipakai" (401) - mis. token-nya access token bukan refresh
 * token, atau user pemiliknya sudah tidak ada. Disatukan sama JwtException (malformed/
 * expired/signature invalid) di bawah status 401 yang sama lewat GlobalExceptionHandler, biar
 * client bisa pakai satu aturan sederhana: 401 dari /auth/refresh -> refresh token gak dipakai
 * lagi, wajib login ulang.
 */
public class InvalidTokenException extends RuntimeException {
  public InvalidTokenException(String message) {
    super(message);
  }
}
