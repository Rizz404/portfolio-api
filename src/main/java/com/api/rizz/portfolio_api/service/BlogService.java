package com.api.rizz.portfolio_api.service;

import com.api.rizz.portfolio_api.dto.request.BlogRequest;
import com.api.rizz.portfolio_api.dto.request.BlogTranslationRequest;
import com.api.rizz.portfolio_api.dto.response.BlogResponse;
import com.api.rizz.portfolio_api.entity.Blog;
import com.api.rizz.portfolio_api.entity.BlogAttachment;
import com.api.rizz.portfolio_api.entity.BlogAttachment.FileType;
import com.api.rizz.portfolio_api.entity.BlogTranslation;
import com.api.rizz.portfolio_api.entity.LanguageCode;
import com.api.rizz.portfolio_api.mapper.BlogMapper;
import com.api.rizz.portfolio_api.repository.BlogRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor // * Otomatis buatin Dependency Injection buat variabel "final"
/** BlogService */
public class BlogService {
  private final BlogRepository blogRepository;
  private final BlogMapper blogMapper;
  private final SnowflakeGenerator snowflakeGenerator;
  private final FileUploadService fileUploadService;

  private static final Set<String> TRANSLATABLE_SORT_FIELDS = Set.of("title", "content");

  private LanguageCode resolveRequestLocale() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    try {
      return LanguageCode.valueOf(lang);
    } catch (IllegalArgumentException e) {
      return LanguageCode.en;
    }
  }

  private String enTitle(List<BlogTranslationRequest> requests) {
    return requests.stream()
        .filter(t -> t.locale() == LanguageCode.en)
        .findFirst()
        .map(BlogTranslationRequest::title)
        .orElseThrow(
            () -> new IllegalArgumentException("Default locale (en) translation is required"));
  }

  private List<BlogTranslation> buildTranslations(
      List<BlogTranslationRequest> requests, Blog blog) {
    List<BlogTranslation> translations = new ArrayList<>();
    for (BlogTranslationRequest t : requests) {
      translations.add(
          BlogTranslation.builder()
              .blog(blog)
              .locale(t.locale())
              .title(t.title())
              .content(t.content())
              .build());
    }
    return translations;
  }

  // * Update translations locale yang sama in-place (bukan clear()+addAll()) - clear+addAll bikin
  // * Hibernate insert baris baru SEBELUM delete baris lama di flush yang sama, jadi tabrakan
  // * UNIQUE(blog_id, locale) kalau locale-nya gak berubah (kasus paling umum saat update).
  private void reconcileTranslations(Blog blog, List<BlogTranslationRequest> requests) {
    List<BlogTranslation> existing = blog.getTranslations();
    java.util.Map<LanguageCode, BlogTranslation> byLocale = new java.util.HashMap<>();
    for (BlogTranslation t : existing) {
      byLocale.put(t.getLocale(), t);
    }

    java.util.Set<LanguageCode> requestedLocales = new java.util.HashSet<>();
    for (BlogTranslationRequest r : requests) {
      requestedLocales.add(r.locale());
    }
    existing.removeIf(t -> !requestedLocales.contains(t.getLocale()));

    for (BlogTranslationRequest r : requests) {
      BlogTranslation match = byLocale.get(r.locale());
      if (match != null) {
        match.setTitle(r.title());
        match.setContent(r.content());
      } else {
        existing.add(
            BlogTranslation.builder()
                .blog(blog)
                .locale(r.locale())
                .title(r.title())
                .content(r.content())
                .build());
      }
    }
  }

  // * Dari springframework bukan jakarta Transactional nya
  @Transactional
  public BlogResponse createBlog(
      BlogRequest blogRequest, MultipartFile featuredImage, List<MultipartFile> attachments) {
    try {
      long newId = snowflakeGenerator.nextId();
      // * Slug selalu dibuat dari title locale 'en' (default/fallback), bukan dari locale
      // * request-time, biar slug stabil gak berubah tergantung Accept-Language header
      String generatedSlug =
          enTitle(blogRequest.translations()).toLowerCase().replaceAll("[^a-z0-9]+", "-");
      Blog blog = blogMapper.toEntity(blogRequest);

      blog.setId(newId);
      blog.setSlug(generatedSlug);
      blog.setTranslations(buildTranslations(blogRequest.translations(), blog));

      boolean hasStringUrl =
          blogRequest.featuredImageUrl() != null && !blogRequest.featuredImageUrl().isBlank();
      boolean hasFile = featuredImage != null && !featuredImage.isEmpty();

      if (hasStringUrl && hasFile) {
        // * Gak bisa keduanya
        throw new IllegalArgumentException(
            "Cannot accept both 'featuredImageUrl' string and 'featuredImageFile'. Choose one.");
      }

      if (hasFile) {
        String featuredUrl =
            fileUploadService.uploadFile(featuredImage, "portfolio/blogs/featured");
        blog.setFeaturedImage(featuredUrl);
      } else if (hasStringUrl) {
        blog.setFeaturedImage(blogRequest.featuredImageUrl());
      }

      List<BlogAttachment> attachmentEntities = new ArrayList<>();

      if (attachments != null && !attachments.isEmpty()) {
        for (MultipartFile file : attachments) {
          if (!file.isEmpty()) {
            String fileUrl = fileUploadService.uploadFile(file, "portfolio/blogs/attachments");

            BlogAttachment attachment =
                BlogAttachment.builder()
                    .id(snowflakeGenerator.nextId())
                    .blog(blog)
                    .fileName(file.getOriginalFilename())
                    .fileUrl(fileUrl)
                    .fileType(resolveFileType(file.getContentType()))
                    .build();

            attachmentEntities.add(attachment);
          }
        }
      }
      // * Set timestamp manual karena pakai snowflakes jadi ada write behind pada hibernate
      OffsetDateTime now = OffsetDateTime.now();
      blog.setCreatedAt(now);
      blog.setUpdatedAt(now);

      blog.setBlogAttachments(attachmentEntities);
      Blog savedBlog = blogRepository.save(blog);

      return blogMapper.toResponse(savedBlog);
    } catch (Exception e) {
      throw new RuntimeException("Error when communicate with cloudinary: " + e.getMessage(), e);
    }
  }

  @Transactional(readOnly = true)
  public Object findAllBlogs(
      String search, Long cursor, int page, int size, List<String> sortBy, List<String> sortDir) {
    Specification<Blog> spec =
        (root, query, cb) -> {
          // * 1. Siapkan Filter (Where Clause Dinamis)
          List<Predicate> predicates = new ArrayList<>();

          // * Kalau ada keyword pencarian di title dan content - join ke translation sesuai
          // * locale request (Accept-Language), fallback 'en'
          if (search != null && !search.isBlank()) {
            String searchKeyword = "%" + search.toLowerCase() + "%";
            Join<Blog, BlogTranslation> t = root.join("translations", JoinType.LEFT);

            // * cb.or() = Pilih salah satu yang cocok (OR)
            Predicate searchTitle = cb.like(cb.lower(t.get("title")), searchKeyword);
            Predicate searchContent = cb.like(cb.lower(t.get("content")), searchKeyword);

            predicates.add(cb.equal(t.get("locale"), resolveRequestLocale()));
            predicates.add(cb.or(searchTitle, searchContent));
          }

          // * Kalau pakai Cursor Pagination (Cari ID yang lebih kecil dari cursor)
          if (cursor != null) {
            predicates.add(cb.lessThan(root.get("id"), cursor));
          }
          return cb.and(predicates.toArray(Predicate[]::new));
        };

    // * 2. Siapkan Sorting (Ascending / Descending)
    Sort finalSort = Sort.unsorted();

    for (int i = 0; i < sortBy.size(); i++) {
      String field = sortBy.get(i);

      // * title/content sekarang ada di tabel terpisah - drop diam-diam alih-alih error
      if (TRANSLATABLE_SORT_FIELDS.contains(field)) {
        continue;
      }

      // Jaga-jaga kalau user ngirim sortBy 2 biji, tapi sortDir cuma 1. Kita default
      // ke 'asc'
      String direction = (i < sortDir.size()) ? sortDir.get(i) : "asc";

      // Bikin gerbong saat ini
      Sort currentSort =
          direction.equalsIgnoreCase("desc")
              ? Sort.by(field).descending()
              : Sort.by(field).ascending();

      // Sambungin ke kereta utama pakai .and() !
      finalSort = finalSort.and(currentSort);
    }

    // * 3. Eksekusi Pencarian!
    if (cursor != null) {
      // * LOGIKA CURSOR: Ambil 'size + 1' untuk mengecek apakah masih ada sisa data untuk next page
      Pageable limitOnly = PageRequest.of(0, size + 1, finalSort);
      Page<Blog> result = blogRepository.findAll(spec, limitOnly);
      return result.getContent().stream().map(blogMapper::toResponse).toList();
    } else {
      // * LOGIKA OFFSET (Default): Butuh info total halaman dan total data
      // * Kurangi 1 biar gak minus page nya
      int actualPage = page > 0 ? page - 1 : 0;
      Pageable pageable = PageRequest.of(actualPage, size, finalSort);
      Page<Blog> result = blogRepository.findAll(spec, pageable);
      return result.map(blogMapper::toResponse);
    }
  }

  @Transactional(readOnly = true)
  public BlogResponse findBlogById(Long id) {
    Blog blog =
        blogRepository
            .findById(id)
            .orElseThrow(
                () -> new NoSuchElementException("Blog with ID: %d not found".formatted(id)));

    return blogMapper.toResponse(blog);
  }

  @Transactional
  public BlogResponse updateBlog(
      Long id,
      BlogRequest blogRequest,
      MultipartFile featuredImageFile,
      List<MultipartFile> newAttachments) {
    try {
      Blog blog =
          blogRepository
              .findById(id)
              .orElseThrow(
                  () -> new NoSuchElementException("Blog with ID: %d not found".formatted(id)));

      // * Update data entity lama pakai data request baru
      blogMapper.updateEntityFromRequest(blogRequest, blog);
      blog.setSlug(enTitle(blogRequest.translations()).toLowerCase().replaceAll("[^a-z0-9]+", "-"));

      // * Mutasi in-place, JANGAN setTranslations(newList) - lihat catatan di ProjectService
      reconcileTranslations(blog, blogRequest.translations());

      boolean hasStringUrl =
          blogRequest.featuredImageUrl() != null && !blogRequest.featuredImageUrl().isBlank();
      boolean hasFile = featuredImageFile != null && !featuredImageFile.isEmpty();

      if (hasStringUrl && hasFile) {
        throw new IllegalArgumentException(
            "Cannot accept both 'featuredImageUrl' string and 'featuredImageFile'. Choose one.");
      }

      if (hasFile) {
        // Hapus file lama di Cloudinary jika ada
        if (blog.getFeaturedImage() != null) {
          String oldPublicId = fileUploadService.extractCloudinaryPublicId(blog.getFeaturedImage());
          if (oldPublicId != null) fileUploadService.deleteFile(oldPublicId);
        }
        String uploadedUrl =
            fileUploadService.uploadFile(featuredImageFile, "portfolio/blogs/featured");
        blog.setFeaturedImage(uploadedUrl);
      } else if (hasStringUrl) {
        blog.setFeaturedImage(blogRequest.featuredImageUrl());
      }

      if (newAttachments != null && !newAttachments.isEmpty()) {
        for (MultipartFile file : newAttachments) {
          if (!file.isEmpty()) {
            String fileUrl = fileUploadService.uploadFile(file, "portfolio/blogs/attachments");
            BlogAttachment attachment =
                BlogAttachment.builder()
                    .id(snowflakeGenerator.nextId())
                    .blog(blog)
                    .fileName(file.getOriginalFilename())
                    .fileUrl(fileUrl)
                    .fileType(resolveFileType(file.getContentType()))
                    .build();
            blog.getBlogAttachments().add(attachment); // Tambahkan ke relasi yang sudah ada
          }
        }
      }

      Blog updatedBlog = blogRepository.save(blog);
      return blogMapper.toResponse(updatedBlog);
    } catch (Exception e) {
      throw new RuntimeException("Error during update mutation: " + e.getMessage(), e);
    }
  }

  @Transactional
  public void deleteBlog(Long id) {
    Blog blog =
        blogRepository
            .findById(id)
            .orElseThrow(
                () -> new NoSuchElementException("Blog with ID: %d not found".formatted(id)));

    if (!blogRepository.existsById(id)) {
      throw new NoSuchElementException("Blog with ID: %d not found".formatted(id));
    }

    if (blog.getFeaturedImage() != null) {
      String featuredPublicId =
          fileUploadService.extractCloudinaryPublicId(blog.getFeaturedImage());
      if (featuredPublicId != null) {
        try {
          fileUploadService.deleteFile(featuredPublicId);
        } catch (Exception ignored) {
        }
      }
    }

    if (blog.getBlogAttachments() != null) {
      for (BlogAttachment attachment : blog.getBlogAttachments()) {
        String publicId = fileUploadService.extractCloudinaryPublicId(attachment.getFileUrl());
        if (publicId != null) {
          try {
            fileUploadService.deleteFile(publicId);
          } catch (Exception ignored) {
          }
        }
      }
    }

    blogRepository.deleteById(id);
  }

  // * Kategorikan MIME type upload jadi salah satu dari FileType, biar tidak perlu simpan
  // MIME type mentah
  private FileType resolveFileType(String contentType) {
    if (contentType == null) return FileType.other;

    if (contentType.startsWith("image/")) return FileType.image;
    if (contentType.startsWith("video/")) return FileType.video;
    if (contentType.startsWith("audio/")) return FileType.audio;

    if (contentType.equals("application/zip")
        || contentType.equals("application/x-rar-compressed")
        || contentType.equals("application/x-7z-compressed")
        || contentType.equals("application/gzip")
        || contentType.equals("application/x-tar")) {
      return FileType.archive;
    }

    if (contentType.equals("application/pdf")
        || contentType.equals("application/msword")
        || contentType.startsWith("application/vnd.openxmlformats-officedocument")
        || contentType.startsWith("application/vnd.ms-")
        || contentType.equals("text/plain")
        || contentType.equals("text/markdown")
        || contentType.equals("application/rtf")) {
      return FileType.document;
    }

    return FileType.other;
  }
}
