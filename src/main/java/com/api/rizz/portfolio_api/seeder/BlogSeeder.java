package com.api.rizz.portfolio_api.seeder;

import com.api.rizz.portfolio_api.entity.Blog;
import com.api.rizz.portfolio_api.entity.BlogAttachment;
import com.api.rizz.portfolio_api.entity.BlogAttachment.AttachmentType;
import com.api.rizz.portfolio_api.repository.BlogRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

/** BlogSeeder */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlogSeeder {
  private static final int SEED_COUNT = 30;

  // * Ekstensi disamakan dengan attachmentType biar fileType-nya akurat, bukan acak sembarangan
  private static final Map<AttachmentType, List<String>> EXTENSIONS_BY_TYPE =
      Map.of(
          AttachmentType.image, List.of("jpg", "png", "webp"),
          AttachmentType.document, List.of("pdf", "docx", "md"),
          AttachmentType.video, List.of("mp4", "webm"),
          AttachmentType.audio, List.of("mp3", "wav"),
          AttachmentType.archive, List.of("zip", "tar.gz"),
          AttachmentType.other, List.of("bin", "dat"));

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
    for (int i = 0; i < SEED_COUNT; i++) {
      String title = randomTitle();
      // [ ! ] Injeksi random hex untuk menggaransi keunikan constraint slug
      String uniqueSlug =
          title.toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + faker.random().hex(6);

      // * Gabung list paragraf dengan newline, bukan List.toString() biar kurung siku tidak ikut
      String content =
          String.join("\n\n", faker.lorem().paragraphs(faker.number().numberBetween(6, 14)));

      Blog randomBlog =
          Blog.builder()
              .id(snowflakeGenerator.nextId())
              .title(title)
              .slug(uniqueSlug)
              .content(content)
              .featuredImage(faker.internet().image())
              .viewsCount(faker.number().numberBetween(10, 5000))
              .likesCount(faker.number().numberBetween(0, 1000))
              .dislikesCount(faker.number().numberBetween(0, 100))
              .isPublished(faker.random().nextInt(100) < 80) // * Mayoritas blog berstatus published
              .build();

      randomBlog.setBlogAttachments(randomAttachments(randomBlog));

      blogRepository.save(randomBlog);
    }
  }

  private String randomTitle() {
    return switch (faker.random().nextInt(3)) {
      case 0 -> faker.book().title();
      case 1 ->
          capitalize(faker.hacker().ingverb())
              + " the "
              + faker.hacker().adjective()
              + " "
              + faker.hacker().noun();
      default -> faker.commerce().productName() + ": " + faker.company().catchPhrase();
    };
  }

  private String capitalize(String text) {
    return text.substring(0, 1).toUpperCase() + text.substring(1);
  }

  private List<BlogAttachment> randomAttachments(Blog blog) {
    List<BlogAttachment> attachments = new ArrayList<>();
    int attachmentCount =
        faker.number().numberBetween(0, 5); // * Ada blog tanpa attachment sama sekali

    for (int j = 0; j < attachmentCount; j++) {
      AttachmentType type =
          AttachmentType.values()[faker.random().nextInt(AttachmentType.values().length)];
      List<String> extensions = EXTENSIONS_BY_TYPE.get(type);
      String extension = extensions.get(faker.random().nextInt(extensions.size()));

      attachments.add(
          BlogAttachment.builder()
              .id(snowflakeGenerator.nextId())
              .blog(blog) // Penetapan referensi absolut ke induk
              .fileName(faker.file().fileName())
              .fileUrl(faker.internet().url())
              .fileType(extension)
              .attachmentType(type)
              .build());
    }

    return attachments;
  }
}
