package com.api.rizz.portfolio_api.util;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;

/** Validates sorting once and applies database ordering without Pageable overriding it. */
public final class QuerySorting {
  private QuerySorting() {}

  public record Plan(ResourceSort.Field field, Direction direction) {
    public <T> Specification<T> specification() {
      return (root, query, cb) -> {
        Join<T, ?> translation =
            field.translated() ? QueryFilters.currentTranslation(root, cb) : null;
        // A translation sort includes only resources translated into the request language.
        Predicate languageFilter =
            field.translated() ? cb.isNotNull(translation.get("id")) : cb.conjunction();
        // The same language filter must apply to count queries, without adding ORDER BY.
        if (query != null
            && query.getResultType() != Long.class
            && query.getResultType() != long.class) {
          List<jakarta.persistence.criteria.Order> criteriaOrders = new ArrayList<>();
          Path<?> path = field.translated() ? translation.get(field.path()) : root;
          if (!field.translated()) {
            for (String component : field.path().split("\\.")) {
              path = path.get(component);
            }
          }
          Expression<?> expression = field.text() ? cb.lower(path.as(String.class)) : path;
          // Explicit NULLS LAST for both directions, including nullable dates and text.
          criteriaOrders.add(
              cb.asc(cb.<Integer>selectCase().when(cb.isNull(path), 1).otherwise(0)));
          criteriaOrders.add(direction.isAscending() ? cb.asc(expression) : cb.desc(expression));
          // Internal tie breaker keeps pagination stable when the selected values are equal.
          if (!field.name().equals("id")) {
            criteriaOrders.add(cb.desc(root.get("id")));
          }
          query.orderBy(criteriaOrders);
        }
        return languageFilter;
      };
    }
  }

  public static Plan plan(ResourceSort resource, Long cursor, String sortBy, String sortDir) {
    String name = singleValue(sortBy, "sortBy", cursor == null ? "createdAt" : "id");
    String direction = singleValue(sortDir, "sortDir", "desc");
    if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
      throw new IllegalArgumentException("sortDir must be asc or desc");
    }
    ResourceSort.Field field = resource.field(name);
    Direction parsedDirection = Direction.valueOf(direction.toUpperCase(Locale.ROOT));
    if (cursor != null && (!name.equals("id") || parsedDirection != Direction.DESC)) {
      throw new IllegalArgumentException(
          "Cursor pagination only supports sortBy=id&sortDir=desc. Use page pagination for custom sorting.");
    }
    return new Plan(field, parsedDirection);
  }

  private static String singleValue(String raw, String parameter, String defaultValue) {
    if (raw == null) {
      return defaultValue;
    }
    String value = raw.trim();
    if (value.isEmpty() || value.contains(",")) {
      throw new IllegalArgumentException(parameter + " must contain exactly one value");
    }
    return value;
  }
}
