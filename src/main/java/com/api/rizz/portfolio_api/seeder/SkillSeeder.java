package com.api.rizz.portfolio_api.seeder;

import com.api.rizz.portfolio_api.entity.Skill;
import com.api.rizz.portfolio_api.repository.SkillRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

/** SkillSeeder */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillSeeder {

  // * Data realistis per kategori, biar gak asal random pakai Faker commerce
  private static final Map<Skill.SkillCategory, List<String>> SKILL_POOL =
      Map.of(
          Skill.SkillCategory.programming_language,
          List.of(
              "Java",
              "JavaScript",
              "TypeScript",
              "Python",
              "Go",
              "Rust",
              "Kotlin",
              "PHP",
              "C#",
              "Ruby"),
          Skill.SkillCategory.framework,
          List.of(
              "Spring Boot",
              "React",
              "Vue.js",
              "Laravel",
              "Django",
              "Express.js",
              "Next.js",
              "Flutter",
              "Angular",
              "NestJS"),
          Skill.SkillCategory.database,
          List.of(
              "PostgreSQL",
              "MySQL",
              "MongoDB",
              "Redis",
              "SQLite",
              "Oracle",
              "Cassandra",
              "Elasticsearch"),
          Skill.SkillCategory.tool,
          List.of("Docker", "Git", "Kubernetes", "Jenkins", "Postman", "Figma", "Nginx", "Webpack"),
          Skill.SkillCategory.other,
          List.of("REST API", "GraphQL", "CI/CD", "Agile", "Unit Testing", "System Design"));

  private final SkillRepository skillRepository;
  private final SnowflakeGenerator snowflakeGenerator;
  private final Faker faker = new Faker();

  public void seed() {
    if (skillRepository.count() == 0) {
      log.info("Seeding data for Skill entity...");
      generateData();
    } else {
      log.info("Skills table has already have a data. Skipping seeding for skill entity.");
    }
  }

  public void generateData() {
    SKILL_POOL.forEach(
        (category, names) -> {
          for (String name : names) {
            boolean hasDescription = faker.bool().bool();

            Skill randomSkill =
                Skill.builder()
                    .id(snowflakeGenerator.nextId())
                    .name(name)
                    .category(category)
                    .logoUrl(faker.internet().image())
                    .description(hasDescription ? faker.lorem().sentence(15) : null)
                    .build();

            skillRepository.save(randomSkill);
          }
        });
  }
}
