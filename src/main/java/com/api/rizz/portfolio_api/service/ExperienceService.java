package com.api.rizz.portfolio_api.service;

import com.api.rizz.portfolio_api.dto.request.ExperienceRequest;
import com.api.rizz.portfolio_api.dto.response.ExperienceResponse;
import com.api.rizz.portfolio_api.entity.Experience;
import com.api.rizz.portfolio_api.mapper.ExperienceMapper;
import com.api.rizz.portfolio_api.repository.ExperienceRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
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

@Service
@RequiredArgsConstructor // * Otomatis buatin Dependency Injection buat variabel "final"
/** ExperienceService */
public class ExperienceService {
  private final ExperienceRepository experienceRepository;
  private final ExperienceMapper experienceMapper;
  private final SnowflakeGenerator snowflakeGenerator;

  @Transactional
  public ExperienceResponse createExperience(ExperienceRequest experienceRequest) {
    long newId = snowflakeGenerator.nextId();
    Experience experience = experienceMapper.toEntity(experienceRequest);

    experience.setId(newId);

    // * Set timestamp manual karena pakai snowflakes jadi ada write behind pada hibernate
    OffsetDateTime now = OffsetDateTime.now();
    experience.setCreatedAt(now);
    experience.setUpdatedAt(now);

    Experience savedExperience = experienceRepository.save(experience);

    return experienceMapper.toResponse(savedExperience);
  }

  public Object findAllExperiences(String search, Boolean isCurrent, LocalDate startDate,
      LocalDate endDate, Long cursor, int page, int size, List<String> sortBy,
      List<String> sortDir) {
    Specification<Experience> spec = (root, query, cb) -> {
      // * 1. Siapkan Filter (Where Clause Dinamis)
      List<Predicate> predicates = new ArrayList<>();

      // * Kalau ada keyword pencarian di company name dan position
      if (search != null && !search.isBlank()) {
        String searchKeyword = "%" + search.toLowerCase() + "%";

        // * cb.or() = Pilih salah satu yang cocok (OR)
        Predicate searchCompanyName = cb.like(cb.lower(root.get("companyName")), searchKeyword);
        Predicate searchPosition = cb.like(cb.lower(root.get("position")), searchKeyword);

        predicates.add(cb.or(searchCompanyName, searchPosition));
      }

      // * Harus kek gini kalo bool
      if (Boolean.TRUE.equals(isCurrent)) {
        predicates.add(cb.equal(root.get("isCurrent"), isCurrent));
      }

      // * Start date end date logic yang sering dipake
      if (startDate != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startDate));
      }
      if (endDate != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), endDate));
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
      Page<Experience> result = experienceRepository.findAll(spec, limitOnly);
      return result.getContent().stream().map(experienceMapper::toResponse).toList();
    } else {
      // * LOGIKA OFFSET (Default): Butuh info total halaman dan total data
      Pageable pageable = PageRequest.of(page, size, finalSort);
      Page<Experience> result = experienceRepository.findAll(spec, pageable);
      return result.map(experienceMapper::toResponse);
    }
  }

  public ExperienceResponse findExperienceById(Long id) {
    Experience experience = experienceRepository.findById(id).orElseThrow(
        () -> new NoSuchElementException("Experience with ID: %d not found".formatted(id)));

    return experienceMapper.toResponse(experience);
  }

  @Transactional
  public ExperienceResponse updateExperience(Long id, ExperienceRequest experienceRequest) {
    Experience experience = experienceRepository.findById(id).orElseThrow(
        () -> new NoSuchElementException("Experience with ID: %d not found".formatted(id)));

    // * Update data entity lama pakai data request baru
    experienceMapper.updateEntityFromRequest(experienceRequest, experience);

    Experience updatedExperience = experienceRepository.save(experience);
    return experienceMapper.toResponse(updatedExperience);
  }

  @Transactional
  public void deleteExperience(Long id) {
    if (!experienceRepository.existsById(id)) {
      throw new NoSuchElementException("Experience with ID: %d not found".formatted(id));
    }

    experienceRepository.deleteById(id);
  }
}
