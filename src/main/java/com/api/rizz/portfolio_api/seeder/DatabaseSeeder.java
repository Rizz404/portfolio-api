package com.api.rizz.portfolio_api.seeder;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("dev") // * [ ! ] Eksekusi ini HANYA berjalan jika spring.profiles.active=dev
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {
  private final ProjectSeeder projectSeeder;
  private final ExperienceSeeder experienceSeeder;
  private final BlogSeeder blogSeeder;
  private final UseSeeder useSeeder;

  @Override
  public void run(String... args) throws Exception {
    log.info("Mengeksekusi isolasi seeder untuk environment development...");
    projectSeeder.seed();
    experienceSeeder.seed();
    blogSeeder.seed();
    useSeeder.seed();
    log.info("Seluruh mock data berhasil diinjeksi.");
  }
}
