package com.api.rizz.portfolio_api.service;

import com.api.rizz.portfolio_api.dto.request.BlogAttachmentRequest;
import com.api.rizz.portfolio_api.dto.request.filter.BlogAttachmentFilter;
import com.api.rizz.portfolio_api.dto.response.BlogAttachmentResponse;
import com.api.rizz.portfolio_api.entity.BlogAttachment;
import com.api.rizz.portfolio_api.mapper.BlogAttachmentMapper;
import com.api.rizz.portfolio_api.repository.BlogAttachmentRepository;
import com.api.rizz.portfolio_api.util.QueryFilters;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // * Otomatis buatin Dependency Injection buat variabel "final"
/** BlogAttachmentService */
public class BlogAttachmentService {
  private final BlogAttachmentRepository blogAttachmentRepository;
  private final BlogAttachmentMapper blogAttachmentMapper;
  private final SnowflakeGenerator snowflakeGenerator;
  private final FileUploadService fileUploadService;

  // * BlogResponse (di-cache di BlogService dengan cache name "blogs") ikut nyimpen daftar
  // * attachment-nya -- jadi mutasi attachment lewat service ini juga wajib evict cache "blogs",
  // * bukan cuma yang di BlogService sendiri, biar gak nyisain data attachment yang basi.
  @Transactional
  @CacheEvict(cacheNames = "blogs", allEntries = true)
  public BlogAttachmentResponse createBlogAttachment(BlogAttachmentRequest blogAttachmentRequest) {
    long newId = snowflakeGenerator.nextId();
    BlogAttachment blogAttachment = blogAttachmentMapper.toEntity(blogAttachmentRequest);

    blogAttachment.setId(newId);

    // * Set timestamp manual karena pakai snowflakes jadi ada write behind pada hibernate
    OffsetDateTime now = OffsetDateTime.now();
    blogAttachment.setCreatedAt(now);
    blogAttachment.setUpdatedAt(now);

    BlogAttachment savedBlogAttachment = blogAttachmentRepository.save(blogAttachment);

    return blogAttachmentMapper.toResponse(savedBlogAttachment);
  }

  @Transactional(readOnly = true)
  public Object findAllBlogAttachments(
      Long cursor,
      int page,
      int size,
      List<String> sortBy,
      List<String> sortDir,
      BlogAttachmentFilter filter) {
    QueryFilters.validatePaging(page, size);
    List<BlogAttachment.FileType> fileTypes =
        QueryFilters.enums(filter.getFileType(), BlogAttachment.FileType.class, "fileType");
    Specification<BlogAttachment> spec =
        (root, query, cb) -> {
          // * 1. Siapkan Filter (Where Clause Dinamis)
          List<Predicate> predicates = new ArrayList<>();

          if (QueryFilters.hasText(filter.getSearch())) {
            predicates.add(
                cb.like(
                    cb.lower(root.get("fileName")),
                    "%" + filter.getSearch().toLowerCase(java.util.Locale.ROOT) + "%"));
          }
          if (filter.getBlogId() != null) {
            predicates.add(cb.equal(root.get("blog").get("id"), filter.getBlogId()));
          }
          if (!fileTypes.isEmpty()) {
            predicates.add(root.get("fileType").in(fileTypes));
          }

          // * Kalau pakai Cursor Pagination (Cari ID yang lebih kecil dari cursor)
          if (cursor != null) {
            predicates.add(cb.lessThan(root.get("id"), cursor));
          }
          return cb.and(predicates.toArray(Predicate[]::new));
        };

    spec = spec.and(QueryFilters.common(filter));

    // * 2. Siapkan Sorting (Ascending / Descending)
    Sort finalSort =
        QueryFilters.sort(
            cursor,
            sortBy,
            sortDir,
            Set.of("id", "fileName", "fileUrl", "fileType", "createdAt", "updatedAt"),
            Set.of());

    // * 3. Eksekusi Pencarian!
    if (cursor != null) {
      // * LOGIKA CURSOR: Ambil 'size + 1' untuk mengecek apakah masih ada sisa data untuk next page
      Pageable limitOnly = PageRequest.of(0, size + 1, finalSort);
      Page<BlogAttachment> result = blogAttachmentRepository.findAll(spec, limitOnly);
      return result.getContent().stream().map(blogAttachmentMapper::toResponse).toList();
    } else {
      // * LOGIKA OFFSET (Default): Butuh info total halaman dan total data
      // * Kurangi 1 biar gak minus page nya
      int actualPage = page > 0 ? page - 1 : 0;
      Pageable pageable = PageRequest.of(actualPage, size, finalSort);
      Page<BlogAttachment> result = blogAttachmentRepository.findAll(spec, pageable);
      return result.map(blogAttachmentMapper::toResponse);
    }
  }

  public BlogAttachmentResponse findBlogAttachmentById(Long id) {
    BlogAttachment blogAttachment =
        blogAttachmentRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "BlogAttachment with ID: %d not found".formatted(id)));

    return blogAttachmentMapper.toResponse(blogAttachment);
  }

  @Transactional
  @CacheEvict(cacheNames = "blogs", allEntries = true)
  public BlogAttachmentResponse updateBlogAttachment(
      Long id, BlogAttachmentRequest blogAttachmentRequest) {
    BlogAttachment blogAttachment =
        blogAttachmentRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "BlogAttachment with ID: %d not found".formatted(id)));

    // * Update data entity lama pakai data request baru
    blogAttachmentMapper.updateEntityFromRequest(blogAttachmentRequest, blogAttachment);

    BlogAttachment updatedBlogAttachment = blogAttachmentRepository.save(blogAttachment);
    return blogAttachmentMapper.toResponse(updatedBlogAttachment);
  }

  @Transactional
  @CacheEvict(cacheNames = "blogs", allEntries = true)
  public void deleteBlogAttachment(Long id) {
    BlogAttachment attachment =
        blogAttachmentRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "BlogAttachment with ID: %d not found".formatted(id)));

    // Hapus fisik di Cloudinary
    String publicId = fileUploadService.extractCloudinaryPublicId(attachment.getFileUrl());
    if (publicId != null) {
      try {
        fileUploadService.deleteFile(publicId);
      } catch (Exception ignored) {
      }
    }

    // Hapus dari DB
    blogAttachmentRepository.deleteById(id);
  }

  @Transactional
  @CacheEvict(cacheNames = "blogs", allEntries = true)
  public void deleteBlogAttachmentBatch(List<Long> ids) {
    List<BlogAttachment> attachments = blogAttachmentRepository.findAllById(ids);

    if (attachments.size() != ids.size()) {
      List<Long> foundIds = attachments.stream().map(BlogAttachment::getId).toList();
      List<Long> missingIds = ids.stream().filter(id -> !foundIds.contains(id)).toList();
      throw new NoSuchElementException(
          "BlogAttachment(s) with ID: %s not found".formatted(missingIds));
    }

    for (BlogAttachment attachment : attachments) {
      String publicId = fileUploadService.extractCloudinaryPublicId(attachment.getFileUrl());
      if (publicId != null) {
        try {
          fileUploadService.deleteFile(publicId);
        } catch (Exception ignored) {
        }
      }
    }

    blogAttachmentRepository.deleteAll(attachments);
  }
}
