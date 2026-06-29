CREATE TABLE blog_attachments (
  id BIGINT PRIMARY KEY,
  blog_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_url VARCHAR(255) NOT NULL,
  -- Contoh: 'application/pdf', 'image/jpeg'
  file_type VARCHAR(100) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_blog FOREIGN KEY (blog_id) REFERENCES blogs (id) ON DELETE CASCADE
);
