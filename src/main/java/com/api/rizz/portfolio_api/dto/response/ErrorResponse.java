package com.api.rizz.portfolio_api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

// * JsonInclude berguna agar field bernilai null tidak ikut di-render ke JSON (mirip omitempty di
// Go)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse<T>(String status, String message, T error) {
  // * Constructor pembantu jika tidak ada detail error
  public ErrorResponse(String message) {
    this("error", message, null);
  }
}
