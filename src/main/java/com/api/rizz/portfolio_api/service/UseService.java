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

import com.api.rizz.portfolio_api.dto.request.UseRequest;
import com.api.rizz.portfolio_api.dto.response.UseResponse;
import com.api.rizz.portfolio_api.entity.Use;
import com.api.rizz.portfolio_api.mapper.UseMapper;
import com.api.rizz.portfolio_api.repository.UseRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // * Otomatis buatin Dependency Injection buat variabel "final"
/**
 * UseService
 */
public class UseService {
  private final UseRepository useRepository;
  private final UseMapper useMapper;
  private final SnowflakeGenerator snowflakeGenerator;

  @Transactional
  public UseResponse createUse(UseRequest useRequest) {
    long newId = snowflakeGenerator.nextId();
    Use use = useMapper.toEntity(useRequest);

    use.setId(newId);

    Use savedUse = useRepository.save(use);

    return useMapper.toResponse(savedUse);
  }

  public Object findAllUses(String search, String category, Long cursor, int page, int size, List<String> sortBy,
      List<String> sortDir) {
    Specification<Use> spec = (root, query, cb) -> {
      // * 1. Siapkan Filter (Where Clause Dinamis)
      List<Predicate> predicates = new ArrayList<>();

      // * Kalau ada keyword pencarian di title dan content
      if (search != null && !search.isBlank()) {
        predicates.add(cb.like(cb.lower(root.get("item_name")), "%" + search.toLowerCase() + "%"));
      }

      // * Kalau mau filter berdasarkan category
      if (category != null && !category.isBlank()) {
        predicates.add(cb.equal(root.get("category"), category));
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
      Page<Use> result = useRepository.findAll(spec, limitOnly);
      return result.getContent().stream().map(useMapper::toResponse).toList();
    } else {
      // * LOGIKA OFFSET (Default): Butuh info total halaman dan total data
      Pageable pageable = PageRequest.of(page, size, finalSort);
      Page<Use> result = useRepository.findAll(spec, pageable);
      return result.map(useMapper::toResponse);
    }
  }

  public UseResponse findUseById(Long id) {
    Use use = useRepository
        .findById(id)
        .orElseThrow(
            () -> new NoSuchElementException("Use with ID: %d not found".formatted(id)));

    return useMapper.toResponse(use);
  }

  @Transactional
  public UseResponse updateUse(Long id, UseRequest useRequest) {
    Use use = useRepository
        .findById(id)
        .orElseThrow(
            () -> new NoSuchElementException("Use with ID: %d not found".formatted(id)));

    // * Update data entity lama pakai data request baru
    useMapper.updateEntityFromRequest(useRequest, use);

    Use updatedUse = useRepository.save(use);
    return useMapper.toResponse(updatedUse);
  }

  @Transactional
  public void deleteUse(Long id) {
    if (!useRepository.existsById(id)) {
      throw new NoSuchElementException("Use with ID: %d not found".formatted(id));
    }

    useRepository.deleteById(id);
  }

}
