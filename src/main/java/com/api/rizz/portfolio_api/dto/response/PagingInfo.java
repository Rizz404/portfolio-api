package com.api.rizz.portfolio_api.dto.response;

public record PagingInfo(int total, int perPage, int currentPage, int totalPages,
    boolean hasPrevPage, boolean hasNextPage) {
}
