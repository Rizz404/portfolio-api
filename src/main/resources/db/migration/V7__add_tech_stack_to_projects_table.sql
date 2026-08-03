ALTER TABLE projects
  -- Menyimpan tech stack dalam format key-value (nama tech -> logo URL)
  ADD COLUMN tech_stack JSONB;
