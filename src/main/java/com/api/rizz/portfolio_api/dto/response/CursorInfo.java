package com.api.rizz.portfolio_api.dto.response;

public record CursorInfo(Long nextCursor, boolean hasNextPage, int perPage) {}
