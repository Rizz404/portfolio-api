package com.api.rizz.portfolio_api.seeder;

import com.api.rizz.portfolio_api.entity.Use;
import com.api.rizz.portfolio_api.repository.UseRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

/** UseSeeder */
@Slf4j
@Component
@RequiredArgsConstructor
public class UseSeeder {
  private static final int SEED_COUNT = 30;

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
    for (int i = 0; i < SEED_COUNT; i++) {
      Use.Category category =
          Use.Category.values()[faker.random().nextInt(Use.Category.values().length)];

      String reasons =
          String.join("\n\n", faker.lorem().paragraphs(faker.number().numberBetween(1, 3)));

      Use randomUse =
          Use.builder()
              .id(snowflakeGenerator.nextId())
              .itemName(faker.commerce().productName())
              .category(category)
              .logoUrl(faker.internet().image())
              .pictures(randomList(faker.number().numberBetween(1, 5), faker.internet()::image))
              .reasons(reasons)
              .links(randomList(faker.number().numberBetween(1, 3), faker.internet()::url))
              .build();

      useRepository.save(randomUse);
    }
  }

  private List<String> randomList(int count, Supplier<String> generator) {
    List<String> values = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      values.add(generator.get());
    }
    return values;
  }
}
