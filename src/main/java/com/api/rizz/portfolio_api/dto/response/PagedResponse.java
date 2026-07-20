package com.api.rizz.portfolio_api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PagedResponse<T>(String status, String message, T data, PagingInfo pagination) {
  public PagedResponse(String message, T data, PagingInfo pagination) {
    this("success", message, data, pagination);
  }
}
