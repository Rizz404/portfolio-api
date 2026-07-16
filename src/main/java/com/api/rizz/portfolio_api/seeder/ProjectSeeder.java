package com.api.rizz.portfolio_api.seeder;

import com.api.rizz.portfolio_api.entity.Project;
import com.api.rizz.portfolio_api.repository.ProjectRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

/** ProjectSeeder */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectSeeder {
  private final ProjectRepository projectRepository;
  private final SnowflakeGenerator snowflakeGenerator;
  private final Faker faker = new Faker();

  public void seed() {
    if (projectRepository.count() == 0) {
      log.info("Seeding data for Project entity...");
      generateData();
    } else {
      log.info("Projects table has already have a data. Skipping seeding for project entity.");
    }
  }

  public void generateData() {
    for (int i = 0; i <= 10; i++) {
      String appName = faker.app().name();
      Project randomProject =
          Project.builder()
              .id(snowflakeGenerator.nextId())
              .name(appName)
              .slug(appName.toLowerCase().replaceAll("[^a-z0-9]+", "-"))
              .description(faker.lorem().paragraph(2))
              .status("active")
              .logoUrl(faker.internet().image())
              .imageUrls(List.of(faker.internet().image()))
              .projectLinks(Map.of("demo", faker.internet().url()))
              .build();

      projectRepository.save(randomProject);
    }
  }
}
