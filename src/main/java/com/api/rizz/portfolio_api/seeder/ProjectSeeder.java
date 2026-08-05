package com.api.rizz.portfolio_api.seeder;

import com.api.rizz.portfolio_api.entity.LanguageCode;
import com.api.rizz.portfolio_api.entity.Project;
import com.api.rizz.portfolio_api.entity.Project.LinkType;
import com.api.rizz.portfolio_api.entity.Project.ProjectStatus;
import com.api.rizz.portfolio_api.entity.Project.ProjectType;
import com.api.rizz.portfolio_api.entity.ProjectTranslation;
import com.api.rizz.portfolio_api.repository.ProjectRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
  private static final int SEED_COUNT = 30;

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
    for (int i = 0; i < SEED_COUNT; i++) {
      String appName = faker.app().name();
      // * Pool nama app di faker terbatas, injeksi hex biar slug unique tidak collide saat count
      // besar
      String uniqueSlug =
          appName.toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + faker.random().hex(6);

      String description =
          String.join("\n\n", faker.lorem().paragraphs(faker.number().numberBetween(1, 4)));

      Project randomProject =
          Project.builder()
              .id(snowflakeGenerator.nextId())
              .slug(uniqueSlug)
              .status(ProjectStatus.values()[faker.random().nextInt(ProjectStatus.values().length)])
              .logoUrl(faker.internet().image())
              .imageUrls(randomImageUrls())
              .projectLinks(randomProjectLinks())
              .techStack(randomTechStack())
              .projectTypes(randomProjectTypes())
              .build();

      // * 'en' dan 'id' dibuat untuk SEMUA row biar perubahan i18n langsung kelihatan di frontend
      List<ProjectTranslation> translations = new ArrayList<>();
      translations.add(
          ProjectTranslation.builder()
              .project(randomProject)
              .locale(LanguageCode.en)
              .name(appName)
              .description(description)
              .build());

      String descriptionId =
          String.join("\n\n", faker.lorem().paragraphs(faker.number().numberBetween(1, 4)));
      translations.add(
          ProjectTranslation.builder()
              .project(randomProject)
              .locale(LanguageCode.id)
              .name(appName)
              .description(descriptionId)
              .build());
      randomProject.setTranslations(translations);

      projectRepository.save(randomProject);
    }
  }

  private List<String> randomImageUrls() {
    int count = faker.number().numberBetween(1, 5);
    List<String> images = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      images.add(faker.internet().image());
    }
    return images;
  }

  private Map<LinkType, String> randomProjectLinks() {
    List<LinkType> shuffled = new ArrayList<>(List.of(LinkType.values()));
    Collections.shuffle(shuffled);
    int count = faker.number().numberBetween(1, 5);

    Map<LinkType, String> links = new LinkedHashMap<>();
    for (LinkType type : shuffled.subList(0, count)) {
      links.put(type, faker.internet().url());
    }
    return links;
  }

  private Map<String, String> randomTechStack() {
    int count = faker.number().numberBetween(2, 6);
    Map<String, String> techStack = new LinkedHashMap<>();
    for (int i = 0; i < count; i++) {
      techStack.put(faker.programmingLanguage().name(), faker.internet().image());
    }
    return techStack;
  }

  private List<ProjectType> randomProjectTypes() {
    List<ProjectType> shuffled = new ArrayList<>(List.of(ProjectType.values()));
    Collections.shuffle(shuffled);
    int count = faker.number().numberBetween(1, 4);
    return new ArrayList<>(shuffled.subList(0, count));
  }
}
