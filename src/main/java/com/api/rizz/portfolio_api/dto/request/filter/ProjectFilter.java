package com.api.rizz.portfolio_api.dto.request.filter;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProjectFilter extends CommonFilter {
  private String slug;
  private List<String> projectTypes;
  private List<String> linkTypes;
  private List<String> techStack;
}
