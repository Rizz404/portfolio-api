CREATE TABLE use_translations (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  use_id BIGINT NOT NULL REFERENCES uses (id) ON DELETE CASCADE,
  locale VARCHAR(10) NOT NULL CHECK (locale IN ('en', 'id')),
  reasons TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (use_id, locale)
);

-- Backfill data existing sebagai locale 'en' (default/fallback)
INSERT INTO use_translations (use_id, locale, reasons, created_at, updated_at)
SELECT id, 'en', reasons, created_at, updated_at
FROM uses;

ALTER TABLE uses
DROP COLUMN reasons;
