package com.api.rizz.portfolio_api.service;

import com.api.rizz.portfolio_api.dto.request.BlogAttachmentRequest;
import com.api.rizz.portfolio_api.dto.response.BlogAttachmentResponse;
import com.api.rizz.portfolio_api.entity.BlogAttachment;
import com.api.rizz.portfolio_api.mapper.BlogAttachmentMapper;
import com.api.rizz.portfolio_api.repository.BlogAttachmentRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
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

@Service
@RequiredArgsConstructor // * Otomatis buatin Dependency Injection buat variabel "final"
/** BlogAttachmentService */
public class BlogAttachmentService {
  private final BlogAttachmentRepository blogAttachmentRepository;
  private final BlogAttachmentMapper blogAttachmentMapper;
  private final SnowflakeGenerator snowflakeGenerator;
  private final FileUploadService fileUploadService;

  @Transactional
  public BlogAttachmentResponse createBlogAttachment(BlogAttachmentRequest blogAttachmentRequest) {
    long newId = snowflakeGenerator.nextId();
    BlogAttachment blogAttachment = blogAttachmentMapper.toEntity(blogAttachmentRequest);

    blogAttachment.setId(newId);

    BlogAttachment savedBlogAttachment = blogAttachmentRepository.save(blogAttachment);

    return blogAttachmentMapper.toResponse(savedBlogAttachment);
  }

  public Object findAllBlogAttachments(
      Long cursor, int page, int size, List<String> sortBy, List<String> sortDir) {
    Specification<BlogAttachment> spec =
        (root, query, cb) -> {
          // * 1. Siapkan Filter (Where Clause Dinamis)
          List<Predicate> predicates = new ArrayList<>();

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
      Sort currentSort =
          direction.equalsIgnoreCase("desc")
              ? Sort.by(field).descending()
              : Sort.by(field).ascending();

      // Sambungin ke kereta utama pakai .and() !
      finalSort = finalSort.and(currentSort);
    }

    // * 3. Eksekusi Pencarian!
    if (cursor != null) {
      // * LOGIKA CURSOR: Biasanya gak butuh info total halaman, cukup ambil 'size'
      // * datanya aja
      Pageable limitOnly = PageRequest.of(0, size, finalSort);
      Page<BlogAttachment> result = blogAttachmentRepository.findAll(spec, limitOnly);
      return result.getContent().stream().map(blogAttachmentMapper::toResponse).toList();
    } else {
      // * LOGIKA OFFSET (Default): Butuh info total halaman dan total data
      Pageable pageable = PageRequest.of(page, size, finalSort);
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
}
