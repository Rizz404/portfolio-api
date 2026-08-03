ALTER TABLE projects
  -- Menyimpan array tipe project (frontend, backend, mobile, dll), bisa lebih dari satu
  ADD COLUMN project_types JSONB;
