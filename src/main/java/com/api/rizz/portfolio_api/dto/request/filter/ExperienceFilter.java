package com.api.rizz.portfolio_api.dto.request.filter;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ExperienceFilter extends CommonFilter {
  private String companyName;
  private String position;
}
