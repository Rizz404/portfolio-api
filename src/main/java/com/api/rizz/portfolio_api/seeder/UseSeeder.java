package com.api.rizz.portfolio_api.seeder;

import java.util.List;

import org.springframework.stereotype.Component;

import com.api.rizz.portfolio_api.entity.Use;
import com.api.rizz.portfolio_api.repository.UseRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;

/**
 * UseSeeder
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UseSeeder {
  private final UseRepository useRepository;
  private final SnowflakeGenerator snowflakeGenerator;
  private final Faker faker = new Faker();

  public void seed() {
    if (useRepository.count() == 0) {
      log.info("Seeding data for Use entity...");
      generateData();
    } else {
      log.info("Uses table has already have a data. Skipping seeding for use entity.");
    }
  }

  public void generateData() {
    for (int i = 0; i <= 10; i++) {
      Use.Category category = faker.bool().bool() ? Use.Category.software : Use.Category.hardware;

      Use randomUse = Use.builder()
          .id(snowflakeGenerator.nextId())
          .itemName(faker.commerce().productName())
          .category(category)
          .logoUrl(faker.internet().image())
          .pictures(List.of(faker.internet().image(), faker.internet().image()))
          .reasons(faker.lorem().paragraph(1))
          .links(List.of(faker.internet().url()))
          .build();

      useRepository.save(randomUse);
    }

  }

}
