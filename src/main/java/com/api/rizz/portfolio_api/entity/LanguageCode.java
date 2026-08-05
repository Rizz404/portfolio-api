package com.api.rizz.portfolio_api.entity;

/**
 * LanguageCode
 *
 * <p>Enum locale yang didukung untuk konten multi-language (name/title/description/dll).
 *
 * <p>Sengaja dibuat top-level (bukan nested di dalam satu entity seperti enum lain di project ini,
 * mis. {@link Project.ProjectStatus}) karena dipakai lintas banyak entity translation
 * (ProjectTranslation, BlogTranslation, SkillTranslation, ExperienceTranslation, UseTranslation,
 * UserTranslation) yang tidak saling memiliki satu sama lain — nesting di salah satu entity akan
 * memaksa entity translation lain import dari entity yang tidak relevan.
 */
public enum LanguageCode {
  en,
  id
}
