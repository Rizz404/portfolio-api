CREATE TABLE blog_translations (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  blog_id BIGINT NOT NULL REFERENCES blogs (id) ON DELETE CASCADE,
  locale VARCHAR(10) NOT NULL CHECK (locale IN ('en', 'id')),
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (blog_id, locale)
);

-- Backfill data existing sebagai locale 'en' (default/fallback)
INSERT INTO blog_translations (blog_id, locale, title, content, created_at, updated_at)
SELECT id, 'en', title, content, created_at, updated_at
FROM blogs;

ALTER TABLE blogs
DROP COLUMN title,
DROP COLUMN content;
