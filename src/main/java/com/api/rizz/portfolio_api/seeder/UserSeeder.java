package com.api.rizz.portfolio_api.seeder;

import com.api.rizz.portfolio_api.entity.LanguageCode;
import com.api.rizz.portfolio_api.entity.User;
import com.api.rizz.portfolio_api.entity.User.AuthProvider;
import com.api.rizz.portfolio_api.entity.User.Gender;
import com.api.rizz.portfolio_api.entity.User.Role;
import com.api.rizz.portfolio_api.entity.UserTranslation;
import com.api.rizz.portfolio_api.repository.UserRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** UserSeeder */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSeeder {
  private static final int SEED_COUNT = 30;
  private static final String DEFAULT_PASSWORD = "password123";

  private final UserRepository userRepository;
  private final SnowflakeGenerator snowflakeGenerator;
  private final PasswordEncoder passwordEncoder;
  private final Faker faker = new Faker();

  public void seed() {
    if (userRepository.count() == 0) {
      log.info("Seeding data for User entity...");
      generateData();
    } else {
      log.info("Users table has already have a data. Skipping seeding for user entity.");
    }
  }

  public void generateData() {
    // * Akun admin dengan kredensial tetap biar bisa dipakai login saat development
    User admin =
        User.builder()
            .id(snowflakeGenerator.nextId())
            .nickname("admin")
            .fullName("Portfolio Admin")
            .email("admin@portfolio.dev")
            .password(passwordEncoder.encode(DEFAULT_PASSWORD))
            .role(Role.ADMIN)
            .provider(AuthProvider.LOCAL)
            .profilePict(faker.internet().image())
            .placeOfBirth(faker.address().city())
            .dateOfBirth(randomBirthDate())
            .gender(Gender.PREFER_NOT_TO_SAY)
            .phoneNumber(faker.phoneNumber().phoneNumber())
            .address(faker.address().fullAddress())
            .build();
    admin.setTranslations(
        List.of(
            UserTranslation.builder()
                .user(admin)
                .locale(LanguageCode.en)
                .bio(faker.lorem().paragraph(2))
                .build(),
            UserTranslation.builder()
                .user(admin)
                .locale(LanguageCode.id)
                .bio(faker.lorem().paragraph(2))
                .build()));
    userRepository.save(admin);

    for (int i = 0; i < SEED_COUNT - 1; i++) {
      AuthProvider provider =
          faker.random().nextInt(100) < 80 ? AuthProvider.LOCAL : AuthProvider.GITHUB;
      // * User dari OAuth (GITHUB) tidak punya password lokal
      String password =
          provider == AuthProvider.LOCAL ? passwordEncoder.encode(DEFAULT_PASSWORD) : null;

      // * Injeksi digit random biar email tidak collide dengan constraint unique
      String email =
          faker.internet().username()
              + faker.number().digits(4)
              + "@"
              + faker.internet().domainName();

      String bio =
          String.join("\n\n", faker.lorem().paragraphs(faker.number().numberBetween(1, 3)));

      User randomUser =
          User.builder()
              .id(snowflakeGenerator.nextId())
              .nickname(faker.internet().username())
              .fullName(faker.name().fullName())
              .email(email)
              .password(password)
              .role(Role.USER)
              .provider(provider)
              .profilePict(faker.internet().image())
              .placeOfBirth(faker.address().city())
              .dateOfBirth(randomBirthDate())
              .gender(Gender.values()[faker.random().nextInt(Gender.values().length)])
              .phoneNumber(faker.phoneNumber().phoneNumber())
              .address(faker.address().fullAddress())
              .build();

      // * 'en' dan 'id' dibuat untuk SEMUA row biar perubahan i18n langsung kelihatan di frontend
      List<UserTranslation> translations = new ArrayList<>();
      translations.add(
          UserTranslation.builder().user(randomUser).locale(LanguageCode.en).bio(bio).build());

      String bioId =
          String.join("\n\n", faker.lorem().paragraphs(faker.number().numberBetween(1, 3)));
      translations.add(
          UserTranslation.builder().user(randomUser).locale(LanguageCode.id).bio(bioId).build());
      randomUser.setTranslations(translations);

      userRepository.save(randomUser);
    }
  }

  private LocalDate randomBirthDate() {
    return LocalDate.now()
        .minusYears(faker.number().numberBetween(18, 60))
        .minusDays(faker.number().numberBetween(0, 365));
  }
}
