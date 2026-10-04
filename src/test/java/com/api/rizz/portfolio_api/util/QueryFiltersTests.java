package com.api.rizz.portfolio_api.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api.rizz.portfolio_api.dto.request.filter.CommonFilter;
import com.api.rizz.portfolio_api.entity.Project.ProjectStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort.Direction;

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
  void defaultsToIdOrderForCursorsAndCreatedDateForPages() {
    assertThat(QuerySorting.plan(ResourceSort.PROJECT, 100L, null, null))
        .isEqualTo(new QuerySorting.Plan(ResourceSort.PROJECT.field("id"), Direction.DESC));
    assertThat(QuerySorting.plan(ResourceSort.PROJECT, null, null, null))
        .isEqualTo(new QuerySorting.Plan(ResourceSort.PROJECT.field("createdAt"), Direction.DESC));
    assertThat(QuerySorting.plan(ResourceSort.PROJECT, null, "name", "asc"))
        .isEqualTo(new QuerySorting.Plan(ResourceSort.PROJECT.field("name"), Direction.ASC));
  }

  @Test
  void rejectsUnsupportedSortingBeforeExecutingAQuery() {
    assertThatThrownBy(() -> QuerySorting.plan(ResourceSort.USER, null, "password", "asc"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("password");
    assertThatThrownBy(() -> QuerySorting.plan(ResourceSort.PROJECT, null, "id", "invalid"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sortDir");
  }

  @Test
  void rejectsMultipleSortValuesAndCursorConflicts() {
    assertThatThrownBy(() -> QuerySorting.plan(ResourceSort.PROJECT, null, "name,status", "asc"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sortBy", "one value");
    assertThatThrownBy(() -> QuerySorting.plan(ResourceSort.PROJECT, null, "name", "asc,desc"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sortDir", "one value");
    assertThatThrownBy(() -> QuerySorting.plan(ResourceSort.PROJECT, 100L, "name", "asc"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cursor pagination");
  }
}
