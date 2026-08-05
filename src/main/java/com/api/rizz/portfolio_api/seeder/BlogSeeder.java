package com.api.rizz.portfolio_api.seeder;

import com.api.rizz.portfolio_api.entity.Blog;
import com.api.rizz.portfolio_api.entity.BlogAttachment;
import com.api.rizz.portfolio_api.entity.BlogAttachment.FileType;
import com.api.rizz.portfolio_api.entity.BlogTranslation;
import com.api.rizz.portfolio_api.entity.LanguageCode;
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

  // * Ekstensi disamakan dengan fileType biar nama filenya masuk akal, bukan acak sembarangan
  private static final Map<FileType, List<String>> EXTENSIONS_BY_TYPE =
      Map.of(
          FileType.image, List.of("jpg", "png", "webp"),
          FileType.document, List.of("pdf", "docx", "md"),
          FileType.video, List.of("mp4", "webm"),
          FileType.audio, List.of("mp3", "wav"),
          FileType.archive, List.of("zip", "tar.gz"),
          FileType.other, List.of("bin", "dat"));

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
              .slug(uniqueSlug)
              .featuredImage(faker.internet().image())
              .viewsCount(faker.number().numberBetween(10, 5000))
              .likesCount(faker.number().numberBetween(0, 1000))
              .dislikesCount(faker.number().numberBetween(0, 100))
              .isPublished(faker.random().nextInt(100) < 80) // * Mayoritas blog berstatus published
              .build();

      // * 'en' wajib ada di semua row. 'id' cuma di sebagian data biar fallback path ke-cover
      List<BlogTranslation> translations = new ArrayList<>();
      translations.add(
          BlogTranslation.builder()
              .blog(randomBlog)
              .locale(LanguageCode.en)
              .title(title)
              .content(content)
              .build());

      if (i % 2 == 0) {
        String contentId =
            String.join("\n\n", faker.lorem().paragraphs(faker.number().numberBetween(6, 14)));
        translations.add(
            BlogTranslation.builder()
                .blog(randomBlog)
                .locale(LanguageCode.id)
                .title(title)
                .content(contentId)
                .build());
      }
      randomBlog.setTranslations(translations);

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
      FileType type = FileType.values()[faker.random().nextInt(FileType.values().length)];
      List<String> extensions = EXTENSIONS_BY_TYPE.get(type);
      String extension = extensions.get(faker.random().nextInt(extensions.size()));

      attachments.add(
          BlogAttachment.builder()
              .id(snowflakeGenerator.nextId())
              .blog(blog) // Penetapan referensi absolut ke induk
              .fileName(faker.lorem().word() + "." + extension)
              .fileUrl(faker.internet().url())
              .fileType(type)
              .build());
    }

    return attachments;
  }
}
