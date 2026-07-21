package com.api.rizz.portfolio_api.service;

import com.api.rizz.portfolio_api.dto.request.BlogRequest;
import com.api.rizz.portfolio_api.dto.response.BlogResponse;
import com.api.rizz.portfolio_api.entity.Blog;
import com.api.rizz.portfolio_api.entity.BlogAttachment;
import com.api.rizz.portfolio_api.mapper.BlogMapper;
import com.api.rizz.portfolio_api.repository.BlogRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor // * Otomatis buatin Dependency Injection buat variabel "final"
/** BlogService */
public class BlogService {
  private final BlogRepository blogRepository;
  private final BlogMapper blogMapper;
  private final SnowflakeGenerator snowflakeGenerator;
  private final FileUploadService fileUploadService;

  @Transactional
  public BlogResponse createBlog(BlogRequest blogRequest, MultipartFile featuredImage,
      List<MultipartFile> attachments) {
    try {
      long newId = snowflakeGenerator.nextId();
      String generatedSlug = blogRequest.title().toLowerCase().replaceAll("[^a-z0-9]+", "-");
      Blog blog = blogMapper.toEntity(blogRequest);

      blog.setId(newId);
      blog.setSlug(generatedSlug);

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

            BlogAttachment attachment = BlogAttachment.builder().id(snowflakeGenerator.nextId())
                .blog(blog).fileName(file.getOriginalFilename()).fileUrl(fileUrl)
                .fileType(file.getContentType()).build();

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

  public Object findAllBlogs(String search, Long cursor, int page, int size, List<String> sortBy,
      List<String> sortDir) {
    Specification<Blog> spec = (root, query, cb) -> {
      // * 1. Siapkan Filter (Where Clause Dinamis)
      List<Predicate> predicates = new ArrayList<>();

      // * Kalau ada keyword pencarian di title dan content
      if (search != null && !search.isBlank()) {
        String searchKeyword = "%" + search.toLowerCase() + "%";

        // * cb.or() = Pilih salah satu yang cocok (OR)
        Predicate searchTitle = cb.like(cb.lower(root.get("title")), searchKeyword);
        Predicate searchContent = cb.like(cb.lower(root.get("content")), searchKeyword);

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

      // Jaga-jaga kalau user ngirim sortBy 2 biji, tapi sortDir cuma 1. Kita default
      // ke 'asc'
      String direction = (i < sortDir.size()) ? sortDir.get(i) : "asc";

      // Bikin gerbong saat ini
      Sort currentSort = direction.equalsIgnoreCase("desc") ? Sort.by(field).descending()
          : Sort.by(field).ascending();

      // Sambungin ke kereta utama pakai .and() !
      finalSort = finalSort.and(currentSort);
    }

    // * 3. Eksekusi Pencarian!
    if (cursor != null) {
      // * LOGIKA CURSOR: Biasanya gak butuh info total halaman, cukup ambil 'size'
      // * datanya aja
      Pageable limitOnly = PageRequest.of(0, size, finalSort);
      Page<Blog> result = blogRepository.findAll(spec, limitOnly);
      return result.getContent().stream().map(blogMapper::toResponse).toList();
    } else {
      // * LOGIKA OFFSET (Default): Butuh info total halaman dan total data
      Pageable pageable = PageRequest.of(page, size, finalSort);
      Page<Blog> result = blogRepository.findAll(spec, pageable);
      return result.map(blogMapper::toResponse);
    }
  }

  public BlogResponse findBlogById(Long id) {
    Blog blog = blogRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Blog with ID: %d not found".formatted(id)));

    return blogMapper.toResponse(blog);
  }

  @Transactional
  public BlogResponse updateBlog(Long id, BlogRequest blogRequest, MultipartFile featuredImageFile,
      List<MultipartFile> newAttachments) {
    try {
      Blog blog = blogRepository.findById(id).orElseThrow(
          () -> new NoSuchElementException("Blog with ID: %d not found".formatted(id)));

      // * Update data entity lama pakai data request baru
      blogMapper.updateEntityFromRequest(blogRequest, blog);
      blog.setSlug(blogRequest.title().toLowerCase().replaceAll("[^a-z0-9]+", "-"));

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
          if (oldPublicId != null)
            fileUploadService.deleteFile(oldPublicId);
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
            BlogAttachment attachment = BlogAttachment.builder().id(snowflakeGenerator.nextId())
                .blog(blog).fileName(file.getOriginalFilename()).fileUrl(fileUrl)
                .fileType(file.getContentType()).build();
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
    Blog blog = blogRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Blog with ID: %d not found".formatted(id)));

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
}
