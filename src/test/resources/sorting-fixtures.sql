-- Supplement the base fixture only for tests of translated ordering and language isolation.
UPDATE project_translations SET name = CASE project_id
  WHEN 10 THEN 'Zulu' WHEN 20 THEN 'alpha' WHEN 30 THEN 'Alpha'
  WHEN 40 THEN 'Beta' WHEN 50 THEN 'gamma' END,
  description = 'foreign-description-' || project_id WHERE locale = 'en';
UPDATE project_translations SET name = 'Zebra', description = 'cherry' WHERE project_id = 10 AND locale = 'id';
INSERT INTO project_translations (project_id, locale, name, description) VALUES
  (20, 'id', 'apel', 'banana'), (30, 'id', 'Apel', NULL), (50, 'id', 'jeruk', 'apple');

UPDATE blog_translations SET title = CASE blog_id WHEN 10 THEN 'Zulu' WHEN 20 THEN 'alpha' ELSE 'Beta' END,
  content = 'English-only article' WHERE locale = 'en';
INSERT INTO blog_translations (blog_id, locale, title, content) VALUES
  (10, 'id', 'Zebra', 'Pisang'), (20, 'id', 'apel', 'Apel');

INSERT INTO experience_translations (experience_id, locale, position, description) VALUES
  (10, 'id', 'Zebra Engineer', 'Pisang'), (20, 'id', 'Apel Engineer', 'Apel');

INSERT INTO skill_translations (skill_id, locale, description) VALUES
  (10, 'id', 'Zebra'), (20, 'id', 'apel');
UPDATE skills SET name = CASE id WHEN 10 THEN 'Zebra' WHEN 20 THEN 'apple' ELSE 'Apple' END;

INSERT INTO use_translations (use_id, locale, reasons) VALUES
  (10, 'id', 'Zebra'), (20, 'id', 'apel');
INSERT INTO user_translations (user_id, locale, bio) VALUES
  (10, 'id', 'Zebra'), (20, 'id', 'apel');
UPDATE users SET place_of_birth = 'Jakarta', address = 'Zebra', phone_number = '900' WHERE id = 10;
UPDATE users SET place_of_birth = 'Bandung', address = 'apple', phone_number = '100' WHERE id = 20;
