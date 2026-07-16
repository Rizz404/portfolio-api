package com.api.rizz.portfolio_api.seeder;

import com.api.rizz.portfolio_api.entity.Experience;
import com.api.rizz.portfolio_api.repository.ExperienceRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import java.time.LocalDate;
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
    for (int i = 0; i <= 10; i++) {
      // Kalkulasi temporal logis: start date di masa lalu, end date setelah start
      // date
      LocalDate startDate = LocalDate.now().minusMonths(faker.number().numberBetween(12, 60));
      LocalDate endDate = startDate.plusMonths(faker.number().numberBetween(1, 11));
      boolean isCurrent = faker.bool().bool();

      Experience randomExperience =
          Experience.builder()
              .id(snowflakeGenerator.nextId())
              .companyName(faker.company().name())
              .position(faker.job().position())
              .description(faker.lorem().paragraph(2))
              .jobdesks(List.of(faker.job().field()))
              .startDate(startDate)
              .endDate(isCurrent ? null : endDate) // * Jika isCurrent true, endDate mutlak null
              .isCurrent(isCurrent)
              .build();

      experienceRepository.save(randomExperience);
    }
  }
}
