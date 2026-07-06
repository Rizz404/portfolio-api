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

import com.api.rizz.portfolio_api.dto.request.BlogRequest;
import com.api.rizz.portfolio_api.dto.response.BlogResponse;
import com.api.rizz.portfolio_api.entity.Blog;
import com.api.rizz.portfolio_api.mapper.BlogMapper;
import com.api.rizz.portfolio_api.repository.BlogRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // * Otomatis buatin Dependency Injection buat variabel "final"
/**
 * BlogService
 */
public class BlogService {
  private final BlogRepository blogRepository;
  private final BlogMapper blogMapper;
  private final SnowflakeGenerator snowflakeGenerator;

  @Transactional
  public BlogResponse createBlog(BlogRequest blogRequest) {
    long newId = snowflakeGenerator.nextId();
    Blog blog = blogMapper.toEntity(blogRequest);

    blog.setId(newId);

    String generatedSlug = blogRequest.title().toLowerCase().replaceAll("[^a-z0-9]+", "-");

    blog.setSlug(generatedSlug);

    Blog savedBlog = blogRepository.save(blog);

    return blogMapper.toResponse(savedBlog);
  }

  public Object findAllBlogs(String search, String status, Long cursor, int page, int size, List<String> sortBy,
      List<String> sortDir) {
    Specification<Blog> spec = (root, query, cb) -> {
      // * 1. Siapkan Filter (Where Clause Dinamis)
      List<Predicate> predicates = new ArrayList<>();

      // * Kalau ada keyword pencarian di title dan content
      if (search != null && !search.isBlank()) {

      }

      // * views count terbanyak

      // * Kalau mau filter berdasarkan status (active/development)
      if (status != null && !status.isBlank()) {
        predicates.add(cb.equal(root.get("status"), status));
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
      Sort currentSort = direction.equalsIgnoreCase("desc")
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
    Blog blog = blogRepository
        .findById(id)
        .orElseThrow(
            () -> new NoSuchElementException("Blog with ID: %d not found".formatted(id)));

    return blogMapper.toResponse(blog);
  }

  @Transactional
  public BlogResponse updateBlog(Long id, BlogRequest blogRequest) {
    Blog blog = blogRepository
        .findById(id)
        .orElseThrow(
            () -> new NoSuchElementException("Blog with ID: %d not found".formatted(id)));

    // * Update data entity lama pakai data request baru
    blogMapper.updateEntityFromRequest(blogRequest, blog);

    String generatedSlug = blogRequest.title().toLowerCase().replaceAll("[^a-z0-9]+", "-");
    blog.setSlug(generatedSlug);

    Blog updatedBlog = blogRepository.save(blog);
    return blogMapper.toResponse(updatedBlog);
  }

  @Transactional
  public void deleteBlog(Long id) {
    if (!blogRepository.existsById(id)) {
      throw new NoSuchElementException("Blog with ID: %d not found".formatted(id));
    }

    blogRepository.deleteById(id);
  }

}
