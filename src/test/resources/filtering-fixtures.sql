INSERT INTO projects (id, slug, status, project_types, project_links, tech_stack, created_at, updated_at) VALUES
  (10, 'portfolio-api', 'active', '["backend","api"]', '{"github":"https://example.com/repo"}', '{"Spring":"https://example.com/logo"}', '2026-01-01T00:00:00Z', '2026-02-01T00:00:00Z'),
  (20, 'portfolio-ui', 'development', '["frontend"]', '{"website":"https://example.com"}', '{"Vue":"https://example.com/logo"}', '2026-01-02T00:00:00Z', '2026-02-02T00:00:00Z'),
  (30, 'legacy-api', 'active', '["backend"]', '{"gitlab":"https://example.com/repo"}', '{"Kotlin":"https://example.com/logo"}', '2025-01-01T00:00:00Z', '2025-02-01T00:00:00Z'),
  (40, 'fullstack', 'active', '["fullstack"]', '{"github":"https://example.com/repo"}', '{"Spring":"https://example.com/logo"}', '2026-01-03T00:00:00Z', '2026-02-03T00:00:00Z'),
  (50, 'empty-json', 'inactive', NULL, NULL, NULL, '2026-01-04T00:00:00Z', '2026-02-04T00:00:00Z');
INSERT INTO project_translations (project_id, locale, name) VALUES
  (10, 'en', 'Portfolio API'), (10, 'id', 'API Portofolio'),
  (20, 'en', 'Portfolio UI'), (30, 'en', 'Legacy API'),
  (40, 'en', 'Fullstack'), (50, 'en', 'Empty JSON');

INSERT INTO skills (id, name, category) VALUES
  (10, 'Java', 'programming_language'), (20, 'Spring', 'framework'), (30, 'PostgreSQL', 'database');
INSERT INTO skill_translations (skill_id, locale, description) VALUES
  (10, 'en', 'Java'), (20, 'en', 'Spring'), (30, 'en', 'PostgreSQL');

INSERT INTO uses (id, item_name, category) VALUES
  (10, 'Editor', 'software'), (20, 'Keyboard', 'hardware'), (30, 'Terminal', 'software');
INSERT INTO use_translations (use_id, locale, reasons) VALUES
  (10, 'en', 'Editing'), (20, 'en', 'Typing'), (30, 'en', 'Commands');

INSERT INTO blogs (id, slug, is_published, views_count) VALUES
  (10, 'published', true, 100), (20, 'draft', false, 5), (30, 'another-draft', false, 20);
INSERT INTO blog_translations (blog_id, locale, title, content) VALUES
  (10, 'en', 'Published post', 'Content'), (20, 'en', 'Draft post', 'Content'), (30, 'en', 'Another draft', 'Content');
INSERT INTO blog_attachments (id, blog_id, file_name, file_url, file_type) VALUES
  (10, 10, 'cover.png', 'https://example.com/cover.png', 'image'),
  (20, 10, 'guide.pdf', 'https://example.com/guide.pdf', 'document'),
  (30, 20, 'cover.mp4', 'https://example.com/cover.mp4', 'video');

INSERT INTO experiences (id, company_name, start_date, end_date, is_current) VALUES
  (10, 'Acme', '2020-01-01', NULL, true),
  (20, 'Acme', '2021-01-01', '2023-12-31', false),
  (30, 'Other', '2018-01-01', '2019-12-31', false);
INSERT INTO experience_translations (experience_id, locale, position) VALUES
  (10, 'en', 'Backend Engineer'), (20, 'en', 'Frontend Engineer'), (30, 'en', 'Designer');

INSERT INTO users (id, nickname, email, role, provider, gender, date_of_birth) VALUES
  (10, 'alice', 'alice@example.com', 'ADMIN', 'LOCAL', 'FEMALE', '1990-01-01'),
  (20, 'bob', 'bob@example.com', 'USER', 'GITHUB', 'MALE', '2000-01-01'),
  (30, 'chris', 'chris@example.com', 'USER', 'LOCAL', 'OTHER', '1995-01-01');
INSERT INTO user_translations (user_id, locale, bio) VALUES
  (10, 'en', 'Alice'), (20, 'en', 'Bob'), (30, 'en', 'Chris');
