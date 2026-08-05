CREATE TABLE project_translations (
  -- Bukan Snowflake: baris translation bukan resource API standalone, selalu dibuat
  -- satu transaksi dengan parent-nya.
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  project_id BIGINT NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
  locale VARCHAR(10) NOT NULL CHECK (locale IN ('en', 'id')),
  name VARCHAR(255) NOT NULL,
  description TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (project_id, locale)
);

-- Backfill data existing sebagai locale 'en' (default/fallback)
INSERT INTO project_translations (project_id, locale, name, description, created_at, updated_at)
SELECT id, 'en', name, description, created_at, updated_at
FROM projects;

ALTER TABLE projects
DROP COLUMN name,
DROP COLUMN description;
