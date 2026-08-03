ALTER TABLE blog_attachments
  -- Kategori jenis file (image, document, video, dll), terpisah dari file_type (mime type asli)
  ADD COLUMN attachment_type VARCHAR(50) NOT NULL DEFAULT 'other' CHECK (
    attachment_type IN ('image', 'document', 'video', 'audio', 'archive', 'other')
  );
