package com.api.rizz.portfolio_api.util;

import com.api.rizz.portfolio_api.dto.request.filter.CommonFilter;
import com.api.rizz.portfolio_api.entity.LanguageCode;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.jpa.domain.Specification;

/** Builds typed predicates; values are passed to Criteria rather than concatenated into SQL. */
public final class QueryFilters {
  private QueryFilters() {}

  /** Reuse one LEFT JOIN restricted to the current locale for both search and sorting. */
  @SuppressWarnings("unchecked")
  public static <T, R> Join<T, R> currentTranslation(Root<T> root, CriteriaBuilder cb) {
    for (Join<T, ?> join : root.getJoins()) {
      if ("current_translation".equals(join.getAlias())) {
        return (Join<T, R>) join;
      }
    }
    LanguageCode language;
    try {
      language = LanguageCode.valueOf(LocaleContextHolder.getLocale().getLanguage());
    } catch (IllegalArgumentException e) {
      language = LanguageCode.en;
    }
    Join<T, R> translation = root.join("translations", JoinType.LEFT);
    translation.alias("current_translation");
    translation.on(cb.equal(translation.get("locale"), language));
    return translation;
  }

  public static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  public static List<String> values(List<String> rawValues) {
    if (rawValues == null) {
      return List.of();
    }
    return rawValues.stream()
        .filter(java.util.Objects::nonNull)
        .flatMap(value -> Arrays.stream(value.split(",")))
        .map(String::trim)
        .filter(QueryFilters::hasText)
        .distinct()
        .toList();
  }

  public static <E extends Enum<E>> List<E> enums(
      String rawValue, Class<E> type, String parameter) {
    return enums(rawValue == null ? List.of() : List.of(rawValue), type, parameter);
  }

  public static <E extends Enum<E>> List<E> enums(
      List<String> rawValues, Class<E> type, String parameter) {
    return values(rawValues).stream()
        .map(
            value ->
                Arrays.stream(type.getEnumConstants())
                    .filter(constant -> constant.name().equalsIgnoreCase(value))
                    .findFirst()
                    .orElseThrow(
                        () ->
                            new IllegalArgumentException(
                                "Invalid value '%s' for '%s'. Allowed values: %s"
                                    .formatted(
                                        value,
                                        parameter,
                                        Arrays.toString(type.getEnumConstants())))))
        .distinct()
        .toList();
  }

  public static <T extends Comparable<? super T>> void validateRange(
      T from, T to, String parameter) {
    if (from != null && to != null && from.compareTo(to) > 0) {
      throw new IllegalArgumentException(parameter + " lower bound must not exceed upper bound");
    }
  }

  public static <T> Specification<T> common(CommonFilter filter) {
    validateRange(
        filter.getCreatedFrom() == null ? null : filter.getCreatedFrom().toInstant(),
        filter.getCreatedTo() == null ? null : filter.getCreatedTo().toInstant(),
        "createdFrom/createdTo");
    validateRange(
        filter.getUpdatedFrom() == null ? null : filter.getUpdatedFrom().toInstant(),
        filter.getUpdatedTo() == null ? null : filter.getUpdatedTo().toInstant(),
        "updatedFrom/updatedTo");
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (filter.getIds() != null && !filter.getIds().isEmpty()) {
        predicates.add(root.get("id").in(filter.getIds()));
      }
      if (filter.getCreatedFrom() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedFrom()));
      }
      if (filter.getCreatedTo() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedTo()));
      }
      if (filter.getUpdatedFrom() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), filter.getUpdatedFrom()));
      }
      if (filter.getUpdatedTo() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), filter.getUpdatedTo()));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  /** PostgreSQL JSONB: matches any top-level array value or object key. */
  public static Predicate jsonContainsAny(
      Root<?> root, CriteriaBuilder cb, String field, List<String> values) {
    return cb.or(
        values.stream()
            .map(
                value ->
                    cb.isTrue(
                        cb.function(
                            "jsonb_exists", Boolean.class, root.get(field), cb.literal(value))))
            .toArray(Predicate[]::new));
  }

  public static void validatePaging(int page, int size) {
    if (page < 1 || size < 1 || size > 100) {
      throw new IllegalArgumentException("page must be >= 1 and size must be between 1 and 100");
    }
  }
}
