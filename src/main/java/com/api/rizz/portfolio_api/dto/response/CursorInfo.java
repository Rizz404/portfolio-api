package com.api.rizz.portfolio_api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

public record CursorInfo(@JsonFormat(shape = Shape.STRING) String nextCursor, boolean hasNextPage,
    int perPage) {
}
