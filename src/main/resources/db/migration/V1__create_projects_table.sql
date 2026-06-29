CREATE TABLE projects (
  -- Snowflake ID (64-bit integer)
  id BIGINT PRIMARY KEY,
  slug VARCHAR(255) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  status VARCHAR(50) NOT NULL CHECK (
    status IN (
      'active',
      'inactive',
      'development',
      'maintenance',
      'archived'
    )
  ),
  logo_url VARCHAR(255),
  -- Menggunakan JSONB untuk performa query array gambar yang lebih cepat
  image_urls JSONB,
  -- Menyimpan multi-link (repo, demo, figma) dalam format key-value
  project_links JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
