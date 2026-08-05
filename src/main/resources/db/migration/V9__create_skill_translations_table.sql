CREATE TABLE skill_translations (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  skill_id BIGINT NOT NULL REFERENCES skills (id) ON DELETE CASCADE,
  locale VARCHAR(10) NOT NULL CHECK (locale IN ('en', 'id')),
  description TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (skill_id, locale)
);

-- Backfill data existing sebagai locale 'en' (default/fallback)
INSERT INTO skill_translations (skill_id, locale, description, created_at, updated_at)
SELECT id, 'en', description, created_at, updated_at
FROM skills;

ALTER TABLE skills
DROP COLUMN description;
