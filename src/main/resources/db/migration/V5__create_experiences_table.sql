CREATE TABLE experiences (
  id BIGINT PRIMARY KEY,
  company_name VARCHAR(255) NOT NULL,
  position VARCHAR(255) NOT NULL,
  description TEXT,
  jobdesk JSONB,
  start_date DATE NOT NULL,
  end_date DATE,
  -- Di-set NULL jika statusnya masih aktif bekerja
  is_current BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
