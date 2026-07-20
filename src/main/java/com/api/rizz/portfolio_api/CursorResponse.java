package com.api.rizz.portfolio_api;

import com.api.rizz.portfolio_api.dto.response.CursorInfo;

public record CursorResponse<T>(String status, String message, T data, CursorInfo cursor) {
  public CursorResponse(String message, T data, CursorInfo cursor) {
    this("success", message, data, cursor);
  }
}
