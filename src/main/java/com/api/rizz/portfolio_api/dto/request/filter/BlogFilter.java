package com.api.rizz.portfolio_api.dto.request.filter;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BlogFilter extends CommonFilter {
  private String slug;
  private Boolean isPublished;
  @PositiveOrZero private Integer minViews;
  @PositiveOrZero private Integer maxViews;
}
