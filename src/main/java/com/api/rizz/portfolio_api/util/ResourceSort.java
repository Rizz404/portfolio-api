package com.api.rizz.portfolio_api.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** Public sort names mapped to scalar entity or current-language translation fields. */
public enum ResourceSort {
  PROJECT(text("slug"), value("status"), translated("name"), translated("description")),
  BLOG(
      text("slug"),
      value("isPublished"),
      value("viewsCount"),
      value("likesCount"),
      value("dislikesCount"),
      translated("title"),
      translated("content")),
  EXPERIENCE(
      text("companyName"),
      value("startDate"),
      value("endDate"),
      value("isCurrent"),
      translated("position"),
      translated("description")),
  SKILL(text("name"), value("category"), text("logoUrl"), translated("description")),
  USE(text("itemName"), value("category"), text("logoUrl"), translated("reasons")),
  USER(
      text("nickname"),
      text("fullName"),
      text("email"),
      value("role"),
      value("provider"),
      value("gender"),
      value("dateOfBirth"),
      text("placeOfBirth"),
      text("address"),
      text("phoneNumber"),
      translated("bio")),
  BLOG_ATTACHMENT(
      text("fileName"),
      text("fileUrl"),
      value("fileType"),
      new Field("blogId", "blog.id", false, false));

  public record Field(String name, String path, boolean text, boolean translated) {}

  private final Map<String, Field> fields;

  ResourceSort(Field... additionalFields) {
    Map<String, Field> registry = new LinkedHashMap<>();
    for (String name : new String[] {"id", "createdAt", "updatedAt"}) {
      registry.put(name, value(name));
    }
    for (Field field : additionalFields) {
      registry.put(field.name(), field);
    }
    fields = java.util.Collections.unmodifiableMap(registry);
  }

  public Field field(String name) {
    Field field = fields.get(name);
    if (field == null) {
      throw new IllegalArgumentException(
          "Unsupported sortBy field '%s'. Allowed fields: %s".formatted(name, fields.keySet()));
    }
    return field;
  }

  private static Field text(String name) {
    return new Field(name, name, true, false);
  }

  private static Field value(String name) {
    return new Field(name, name, false, false);
  }

  private static Field translated(String name) {
    return new Field(name, name, true, true);
  }
}
