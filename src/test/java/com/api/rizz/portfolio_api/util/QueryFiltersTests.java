package com.api.rizz.portfolio_api.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api.rizz.portfolio_api.dto.request.filter.CommonFilter;
import com.api.rizz.portfolio_api.entity.Project.ProjectStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class QueryFiltersTests {
  @Test
  void acceptsMixedCommaSeparatedAndRepeatedEnumsWithoutDuplicates() {
    assertThat(
            QueryFilters.enums(
                List.of("ACTIVE,development", "active", " "), ProjectStatus.class, "status"))
        .containsExactly(ProjectStatus.active, ProjectStatus.development);
  }

  @Test
  void rejectsAnInvalidMemberInsteadOfDroppingItFromAValidList() {
    assertThatThrownBy(() -> QueryFilters.enums("active,bogus", ProjectStatus.class, "status"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("bogus", "status", "archived");
  }

  @Test
  void treatsEquivalentTimestampOffsetsAsTheSameInstant() {
    CommonFilter filter = new CommonFilter();
    filter.setCreatedFrom(OffsetDateTime.parse("2026-01-01T07:00:00+07:00"));
    filter.setCreatedTo(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
    assertThatCode(() -> QueryFilters.common(filter)).doesNotThrowAnyException();
  }

  @Test
  void usesIdOrderForIdCursorsAndAddsAnOffsetTieBreaker() {
    Set<String> fields = Set.of("id", "createdAt");
    assertThat(QueryFilters.sort(100L, List.of("createdAt"), List.of("asc"), fields, Set.of()))
        .isEqualTo(Sort.by("id").descending());
    assertThat(QueryFilters.sort(null, List.of("createdAt"), List.of("asc"), fields, Set.of()))
        .isEqualTo(Sort.by("createdAt").ascending().and(Sort.by("id").descending()));
  }

  @Test
  void rejectsUnsupportedSortingBeforeExecutingAQuery() {
    assertThatThrownBy(
            () ->
                QueryFilters.sort(
                    null, List.of("password"), List.of("asc"), Set.of("id"), Set.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("password");
    assertThatThrownBy(
            () ->
                QueryFilters.sort(null, List.of("id"), List.of("invalid"), Set.of("id"), Set.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sortDir");
  }
}
