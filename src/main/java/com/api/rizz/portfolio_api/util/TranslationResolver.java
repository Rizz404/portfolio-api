package com.api.rizz.portfolio_api.util;

import com.api.rizz.portfolio_api.entity.HasLocale;
import com.api.rizz.portfolio_api.entity.LanguageCode;
import java.util.List;

/**
 * TranslationResolver
 *
 * <p>Utility buat resolve translation row yang sesuai locale yang diminta, dengan fallback. Dipakai
 * oleh semua Mapper (lewat @AfterMapping) dan oleh Service saat build search predicate.
 */
public final class TranslationResolver {

  private TranslationResolver() {}

  /**
   * Resolve translation sesuai prioritas: (1) exact match locale yang diminta, (2) exact match
   * locale fallback, (3) elemen pertama di list sebagai last resort.
   *
   * @throws IllegalStateException kalau list kosong (seharusnya tidak pernah terjadi karena locale
   *     fallback wajib diisi saat create/update).
   */
  public static <T extends HasLocale> T resolve(
      List<T> translations, LanguageCode requested, LanguageCode fallback) {
    if (translations == null || translations.isEmpty()) {
      throw new IllegalStateException("No translation available to resolve");
    }

    return translations.stream()
        .filter(t -> t.getLocale() == requested)
        .findFirst()
        .or(() -> translations.stream().filter(t -> t.getLocale() == fallback).findFirst())
        .orElse(translations.get(0));
  }
}
