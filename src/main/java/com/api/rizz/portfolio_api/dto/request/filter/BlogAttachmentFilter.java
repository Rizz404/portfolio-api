package com.api.rizz.portfolio_api.dto.request.filter;

import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BlogAttachmentFilter extends CommonFilter {
  private String search;
  @Positive private Long blogId;
  private List<String> fileType;
}
