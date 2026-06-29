CREATE TABLE uses (
  id BIGINT PRIMARY KEY,
  item_name VARCHAR(255) NOT NULL,
  category VARCHAR(50) NOT NULL CHECK (category IN ('software', 'hardware')),
  logo_url VARCHAR(255),
  -- Galeri foto setup atau detail device
  pictures JSONB,
  reasons TEXT,
  -- Bisa simpan multi-link untuk opsi download atau marketplace
  links JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
