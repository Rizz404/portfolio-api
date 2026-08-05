CREATE TABLE user_translations (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  locale VARCHAR(10) NOT NULL CHECK (locale IN ('en', 'id')),
  bio TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, locale)
);

-- Backfill data existing sebagai locale 'en' (default/fallback)
INSERT INTO user_translations (user_id, locale, bio, created_at, updated_at)
SELECT id, 'en', bio, created_at, updated_at
FROM users;

ALTER TABLE users
DROP COLUMN bio;
