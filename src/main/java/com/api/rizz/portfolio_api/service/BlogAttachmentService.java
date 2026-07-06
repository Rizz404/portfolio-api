package com.api.rizz.portfolio_api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.api.rizz.portfolio_api.dto.request.BlogAttachmentRequest;
import com.api.rizz.portfolio_api.dto.response.BlogAttachmentResponse;
import com.api.rizz.portfolio_api.entity.BlogAttachment;
import com.api.rizz.portfolio_api.mapper.BlogAttachmentMapper;
import com.api.rizz.portfolio_api.repository.BlogAttachmentRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // * Otomatis buatin Dependency Injection buat variabel "final"
/**
 * BlogAttachmentService
 */
public class BlogAttachmentService {
  private final BlogAttachmentRepository blogAttachmentRepository;
  private final BlogAttachmentMapper blogAttachmentMapper;
  private final SnowflakeGenerator snowflakeGenerator;

  @Transactional
  public BlogAttachmentResponse createBlogAttachment(BlogAttachmentRequest blogAttachmentRequest) {
    long newId = snowflakeGenerator.nextId();
    BlogAttachment blogAttachment = blogAttachmentMapper.toEntity(blogAttachmentRequest);

    blogAttachment.setId(newId);

    BlogAttachment savedBlogAttachment = blogAttachmentRepository.save(blogAttachment);

    return blogAttachmentMapper.toResponse(savedBlogAttachment);
  }

  public Object findAllBlogAttachments(Long cursor, int page, int size, String sortBy,
      String sortDir) {
    Specification<BlogAttachment> spec = (root, query, cb) -> {
      // * 1. Siapkan Filter (Where Clause Dinamis)
      List<Predicate> predicates = new ArrayList<>();

      // * Kalau pakai Cursor Pagination (Cari ID yang lebih kecil dari cursor)
      if (cursor != null) {
        predicates.add(cb.lessThan(root.get("id"), cursor));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };

    // * 2. Siapkan Sorting (Ascending / Descending)
    Sort sort = sortDir.equalsIgnoreCase("asc")
        ? Sort.by(sortBy).ascending()
        : Sort.by(sortBy).descending();

    // * 3. Eksekusi Pencarian!
    if (cursor != null) {
      // * LOGIKA CURSOR: Biasanya gak butuh info total halaman, cukup ambil 'size'
      // * datanya aja
      Pageable limitOnly = PageRequest.of(0, size, sort);
      Page<BlogAttachment> result = blogAttachmentRepository.findAll(spec, limitOnly);
      return result.getContent().stream().map(blogAttachmentMapper::toResponse).toList();
    } else {
      // * LOGIKA OFFSET (Default): Butuh info total halaman dan total data
      Pageable pageable = PageRequest.of(page, size, sort);
      Page<BlogAttachment> result = blogAttachmentRepository.findAll(spec, pageable);
      return result.map(blogAttachmentMapper::toResponse);
    }
  }

  public BlogAttachmentResponse findBlogAttachmentById(Long id) {
    BlogAttachment blogAttachment = blogAttachmentRepository
        .findById(id)
        .orElseThrow(
            () -> new NoSuchElementException("BlogAttachment with ID: %d not found".formatted(id)));

    return blogAttachmentMapper.toResponse(blogAttachment);
  }

  @Transactional
  public BlogAttachmentResponse updateBlogAttachment(Long id, BlogAttachmentRequest blogAttachmentRequest) {
    BlogAttachment blogAttachment = blogAttachmentRepository
        .findById(id)
        .orElseThrow(
            () -> new NoSuchElementException("BlogAttachment with ID: %d not found".formatted(id)));

    BlogAttachment updatedBlogAttachment = blogAttachmentRepository.save(blogAttachment);
    return blogAttachmentMapper.toResponse(updatedBlogAttachment);
  }

  @Transactional
  public void deleteBlogAttachment(Long id) {
    if (!blogAttachmentRepository.existsById(id)) {
      throw new NoSuchElementException("BlogAttachment with ID: %d not found".formatted(id));
    }

    blogAttachmentRepository.deleteById(id);
  }

}
