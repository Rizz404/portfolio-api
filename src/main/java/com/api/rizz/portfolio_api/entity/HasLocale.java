package com.api.rizz.portfolio_api.entity;

/**
 * HasLocale
 *
 * <p>Dipakai oleh semua entity translation (ProjectTranslation, BlogTranslation, dst) supaya {@link
 * com.api.rizz.portfolio_api.util.TranslationResolver} bisa resolve translation yang sesuai locale
 * request secara generic, tanpa duplikasi logic per entity.
 */
public interface HasLocale {
  LanguageCode getLocale();
}
