package com.api.rizz.portfolio_api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuccessResponse<T>(String status, String message, T data) {
  // * Constructor pembantu agar tidak perlu selalu menulis "success"
  public SuccessResponse(String message, T data) {
    this("success", message, data);
  }
}
