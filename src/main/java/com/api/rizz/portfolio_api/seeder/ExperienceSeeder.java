package com.api.rizz.portfolio_api.seeder;

import com.api.rizz.portfolio_api.entity.Experience;
import com.api.rizz.portfolio_api.entity.ExperienceTranslation;
import com.api.rizz.portfolio_api.entity.LanguageCode;
import com.api.rizz.portfolio_api.repository.ExperienceRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

/** ExperienceSeeder */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExperienceSeeder {
  private static final int SEED_COUNT = 30;

  private final ExperienceRepository experienceRepository;
  private final SnowflakeGenerator snowflakeGenerator;
  private final Faker faker = new Faker();

  public void seed() {
    if (experienceRepository.count() == 0) {
      log.info("Seeding data for Experience entity...");
      generateData();
    } else {
      log.info(
          "Experiences table has already have a data. Skipping seeding for experience entity.");
    }
  }

  public void generateData() {
    // * Mundur kronologis dari sekarang biar antar pengalaman tidak saling tumpang tindih
    LocalDate cursor = LocalDate.now();

    for (int i = 0; i < SEED_COUNT; i++) {
      // * Hanya pengalaman paling baru (i == 0) yang boleh berstatus current
      boolean isCurrent = i == 0 && faker.bool().bool();

      int durationMonths = faker.number().numberBetween(3, 30);
      LocalDate endDate = isCurrent ? null : cursor;
      LocalDate startDate = cursor.minusMonths(durationMonths);

      Experience randomExperience =
          Experience.builder()
              .id(snowflakeGenerator.nextId())
              .companyName(faker.company().name())
              .startDate(startDate)
              .endDate(endDate) // * Jika isCurrent true, endDate mutlak null
              .isCurrent(isCurrent)
              .build();

      // * 'en' dan 'id' dibuat untuk SEMUA row biar perubahan i18n langsung kelihatan di frontend
      List<ExperienceTranslation> translations = new ArrayList<>();
      translations.add(
          ExperienceTranslation.builder()
              .experience(randomExperience)
              .locale(LanguageCode.en)
              .position(faker.job().position())
              .description(
                  String.join("\n\n", faker.lorem().paragraphs(faker.number().numberBetween(1, 3))))
              .jobdesks(randomJobdesks())
              .build());

      translations.add(
          ExperienceTranslation.builder()
              .experience(randomExperience)
              .locale(LanguageCode.id)
              .position(faker.job().position())
              .description(
                  String.join("\n\n", faker.lorem().paragraphs(faker.number().numberBetween(1, 3))))
              .jobdesks(randomJobdesks())
              .build());
      randomExperience.setTranslations(translations);

      experienceRepository.save(randomExperience);

      // * Jeda beberapa bulan sebelum pengalaman berikutnya (gap antar kerja)
      cursor = startDate.minusMonths(faker.number().numberBetween(0, 6));
    }
  }

  private List<String> randomJobdesks() {
    int count = faker.number().numberBetween(2, 6);
    List<String> jobdesks = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      jobdesks.add(faker.lorem().sentence(faker.number().numberBetween(6, 14)));
    }
    return jobdesks;
  }
}
