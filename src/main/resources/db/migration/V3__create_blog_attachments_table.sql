CREATE TABLE blog_attachments (
  id BIGINT PRIMARY KEY,
  blog_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_url VARCHAR(255) NOT NULL,
  attachment_type VARCHAR(50) NOT NULL DEFAULT 'other' CHECK (
    attachment_type IN (
      'image',
      'document',
      'video',
      'audio',
      'archive',
      'other'
    )
  ),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_blog FOREIGN KEY (blog_id) REFERENCES blogs (id) ON DELETE CASCADE
);
