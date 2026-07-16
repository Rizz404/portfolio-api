package com.api.rizz.portfolio_api.seeder;

import com.api.rizz.portfolio_api.entity.Blog;
import com.api.rizz.portfolio_api.entity.BlogAttachment;
import com.api.rizz.portfolio_api.repository.BlogRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

/** BlogSeeder */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlogSeeder {
  private final BlogRepository blogRepository;
  private final SnowflakeGenerator snowflakeGenerator;
  private final Faker faker = new Faker();

  public void seed() {
    if (blogRepository.count() == 0) {
      log.info("Seeding data for Blog entity...");
      generateData();
    } else {
      log.info("Blogs table has already have a data. Skipping seeding for blog entity.");
    }
  }

  public void generateData() {
    for (int i = 0; i <= 10; i++) {
      String title = faker.book().title();
      // [ ! ] Injeksi random hex untuk menggaransi keunikan constraint slug
      String uniqueSlug =
          title.toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + faker.random().hex(6);
      Blog randomBlog =
          Blog.builder()
              .id(snowflakeGenerator.nextId())
              .title(title)
              .slug(uniqueSlug)
              .content(faker.lorem().paragraphs(4).toString()) // Konversi List ke String
              .featuredImage(faker.internet().image())
              .viewsCount(faker.number().numberBetween(10, 1000))
              .likesCount(faker.number().numberBetween(5, 500))
              .dislikesCount(faker.number().numberBetween(0, 50))
              .isPublished(faker.bool().bool())
              .build();

      List<BlogAttachment> attachments = new ArrayList<>();
      for (int j = 0; j < 2; j++) {
        BlogAttachment attachment =
            BlogAttachment.builder()
                .id(snowflakeGenerator.nextId())
                .blog(randomBlog) // Penetapan referensi absolut ke induk
                .fileName(faker.file().fileName())
                .fileUrl(faker.internet().url())
                .fileType(faker.file().extension())
                .build();
        attachments.add(attachment);
      }

      randomBlog.setBlogAttachments(attachments);

      blogRepository.save(randomBlog);
    }
  }
}
