CREATE TABLE experience_translations (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  experience_id BIGINT NOT NULL REFERENCES experiences (id) ON DELETE CASCADE,
  locale VARCHAR(10) NOT NULL CHECK (locale IN ('en', 'id')),
  position VARCHAR(255) NOT NULL,
  description TEXT,
  jobdesk JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (experience_id, locale)
);

-- Backfill data existing sebagai locale 'en' (default/fallback)
INSERT INTO experience_translations (experience_id, locale, position, description, jobdesk, created_at, updated_at)
SELECT id, 'en', position, description, jobdesk, created_at, updated_at
FROM experiences;

ALTER TABLE experiences
DROP COLUMN position,
DROP COLUMN description,
DROP COLUMN jobdesk;
