package com.api.rizz.portfolio_api.service;

import com.api.rizz.portfolio_api.dto.request.ExperienceRequest;
import com.api.rizz.portfolio_api.dto.request.ExperienceTranslationRequest;
import com.api.rizz.portfolio_api.dto.request.filter.ExperienceFilter;
import com.api.rizz.portfolio_api.dto.response.ExperienceResponse;
import com.api.rizz.portfolio_api.entity.Experience;
import com.api.rizz.portfolio_api.entity.ExperienceTranslation;
import com.api.rizz.portfolio_api.entity.LanguageCode;
import com.api.rizz.portfolio_api.mapper.ExperienceMapper;
import com.api.rizz.portfolio_api.repository.ExperienceRepository;
import com.api.rizz.portfolio_api.util.QueryFilters;
import com.api.rizz.portfolio_api.util.QuerySorting;
import com.api.rizz.portfolio_api.util.ResourceSort;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // * Otomatis buatin Dependency Injection buat variabel "final"
/** ExperienceService */
public class ExperienceService {
  private final ExperienceRepository experienceRepository;
  private final ExperienceMapper experienceMapper;
  private final SnowflakeGenerator snowflakeGenerator;

  // * Nama cache Redis buat domain experience (lihat CacheConfig). Query dievict semua
  // * (allEntries) tiap ada mutasi (create/update/delete) -- daripada invalidate parsial per
  // * kombinasi filter/sort/page yang gak kebayang jumlahnya.
  private static final String CACHE_NAME = "experiences";

  private List<ExperienceTranslation> buildTranslations(
      List<ExperienceTranslationRequest> requests, Experience experience) {
    List<ExperienceTranslation> translations = new ArrayList<>();
    for (ExperienceTranslationRequest t : requests) {
      translations.add(
          ExperienceTranslation.builder()
              .experience(experience)
              .locale(t.locale())
              .position(t.position())
              .description(t.description())
              .jobdesks(t.jobdesks())
              .build());
    }
    return translations;
  }

  // * Update translations locale yang sama in-place (bukan clear()+addAll()) - clear+addAll bikin
  // * Hibernate insert baris baru SEBELUM delete baris lama di flush yang sama, jadi tabrakan
  // * UNIQUE(experience_id, locale) kalau locale-nya gak berubah (kasus paling umum saat update).
  private void reconcileTranslations(
      Experience experience, List<ExperienceTranslationRequest> requests) {
    List<ExperienceTranslation> existing = experience.getTranslations();
    java.util.Map<LanguageCode, ExperienceTranslation> byLocale = new java.util.HashMap<>();
    for (ExperienceTranslation t : existing) {
      byLocale.put(t.getLocale(), t);
    }

    java.util.Set<LanguageCode> requestedLocales = new java.util.HashSet<>();
    for (ExperienceTranslationRequest r : requests) {
      requestedLocales.add(r.locale());
    }
    existing.removeIf(t -> !requestedLocales.contains(t.getLocale()));

    for (ExperienceTranslationRequest r : requests) {
      ExperienceTranslation match = byLocale.get(r.locale());
      if (match != null) {
        match.setPosition(r.position());
        match.setDescription(r.description());
        match.setJobdesks(r.jobdesks());
      } else {
        existing.add(
            ExperienceTranslation.builder()
                .experience(experience)
                .locale(r.locale())
                .position(r.position())
                .description(r.description())
                .jobdesks(r.jobdesks())
                .build());
      }
    }
  }

  @Transactional
  @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
  public ExperienceResponse createExperience(ExperienceRequest experienceRequest) {
    long newId = snowflakeGenerator.nextId();
    Experience experience = experienceMapper.toEntity(experienceRequest);

    experience.setId(newId);
    experience.setTranslations(buildTranslations(experienceRequest.translations(), experience));

    // * Set timestamp manual karena pakai snowflakes jadi ada write behind pada hibernate
    OffsetDateTime now = OffsetDateTime.now();
    experience.setCreatedAt(now);
    experience.setUpdatedAt(now);

    Experience savedExperience = experienceRepository.save(experience);

    return experienceMapper.toResponse(savedExperience);
  }

  // * @Transactional wajib: mapper resolve translations (LAZY @OneToMany) di toResponse(),
  // * butuh session Hibernate masih terbuka; open-in-view=false jadi gak otomatis
  @Transactional(readOnly = true)
  @Cacheable(
      cacheNames = CACHE_NAME,
      key =
          "'list:v4:' + T(org.springframework.context.i18n.LocaleContextHolder).getLocale() + ':' + #search"
              + " + ':' + #isCurrent + ':' + #startDate + ':' + #endDate + ':' + #cursor + ':'"
              + " + #page + ':' + #size + ':' + #sortBy + ':' + #sortDir + ':' + #filter")
  public Object findAllExperiences(
      String search,
      Boolean isCurrent,
      LocalDate startDate,
      LocalDate endDate,
      Long cursor,
      int page,
      int size,
      String sortBy,
      String sortDir,
      ExperienceFilter filter) {
    QueryFilters.validatePaging(page, size);
    QuerySorting.Plan sorting = QuerySorting.plan(ResourceSort.EXPERIENCE, cursor, sortBy, sortDir);
    QueryFilters.validateRange(startDate, endDate, "startDate/endDate");
    Specification<Experience> spec =
        (root, query, cb) -> {
          // * 1. Siapkan Filter (Where Clause Dinamis)
          List<Predicate> predicates = new ArrayList<>();

          // * Company name bisa cocok langsung; position hanya pada bahasa request.
          if (search != null && !search.isBlank()) {
            String searchKeyword = "%" + search.toLowerCase(java.util.Locale.ROOT) + "%";
            Join<Experience, ExperienceTranslation> t = QueryFilters.currentTranslation(root, cb);

            // * cb.or() = Pilih salah satu yang cocok (OR)
            Predicate searchCompanyName = cb.like(cb.lower(root.get("companyName")), searchKeyword);
            Predicate searchPosition = cb.like(cb.lower(t.get("position")), searchKeyword);

            predicates.add(cb.or(searchCompanyName, searchPosition));
          }

          // * Harus kek gini kalo bool
          if (isCurrent != null) {
            predicates.add(cb.equal(root.get("isCurrent"), isCurrent));
          }

          if (QueryFilters.hasText(filter.getCompanyName())) {
            predicates.add(
                cb.like(
                    cb.lower(root.get("companyName")),
                    "%" + filter.getCompanyName().toLowerCase(java.util.Locale.ROOT) + "%"));
          }
          if (QueryFilters.hasText(filter.getPosition())) {
            Join<Experience, ExperienceTranslation> positionTranslation =
                QueryFilters.currentTranslation(root, cb);
            predicates.add(
                cb.like(
                    cb.lower(positionTranslation.get("position")),
                    "%" + filter.getPosition().toLowerCase(java.util.Locale.ROOT) + "%"));
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

    spec = spec.and(QueryFilters.common(filter)).and(sorting.specification());

    // * 3. Eksekusi Pencarian!
    if (cursor != null) {
      // * LOGIKA CURSOR: Ambil 'size + 1' untuk mengecek apakah masih ada sisa data untuk next page
      Pageable limitOnly = PageRequest.of(0, size + 1);
      Page<Experience> result = experienceRepository.findAll(spec, limitOnly);
      return result.getContent().stream().map(experienceMapper::toResponse).toList();
    } else {
      // * LOGIKA OFFSET (Default): Butuh info total halaman dan total data
      // * Kurangi 1 biar gak minus page nya
      int actualPage = page > 0 ? page - 1 : 0;
      Pageable pageable = PageRequest.of(actualPage, size);
      Page<Experience> result = experienceRepository.findAll(spec, pageable);
      return result.map(experienceMapper::toResponse);
    }
  }

  @Transactional(readOnly = true)
  @Cacheable(
      cacheNames = CACHE_NAME,
      key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale() + ':' + #id")
  public ExperienceResponse findExperienceById(Long id) {
    Experience experience =
        experienceRepository
            .findById(id)
            .orElseThrow(
                () -> new NoSuchElementException("Experience with ID: %d not found".formatted(id)));

    return experienceMapper.toResponse(experience);
  }

  @Transactional
  @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
  public ExperienceResponse updateExperience(Long id, ExperienceRequest experienceRequest) {
    Experience experience =
        experienceRepository
            .findById(id)
            .orElseThrow(
                () -> new NoSuchElementException("Experience with ID: %d not found".formatted(id)));

    // * Update data entity lama pakai data request baru
    experienceMapper.updateEntityFromRequest(experienceRequest, experience);

    // * Mutasi in-place, JANGAN setTranslations(newList) - lihat catatan di ProjectService
    reconcileTranslations(experience, experienceRequest.translations());

    Experience updatedExperience = experienceRepository.save(experience);
    return experienceMapper.toResponse(updatedExperience);
  }

  @Transactional
  @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
  public void deleteExperience(Long id) {
    if (!experienceRepository.existsById(id)) {
      throw new NoSuchElementException("Experience with ID: %d not found".formatted(id));
    }

    experienceRepository.deleteById(id);
  }

  @Transactional
  @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
  public void deleteExperienceBatch(List<Long> ids) {
    List<Experience> experiences = experienceRepository.findAllById(ids);

    if (experiences.size() != ids.size()) {
      List<Long> foundIds = experiences.stream().map(Experience::getId).toList();
      List<Long> missingIds = ids.stream().filter(id -> !foundIds.contains(id)).toList();
      throw new NoSuchElementException("Experience(s) with ID: %s not found".formatted(missingIds));
    }

    experienceRepository.deleteAll(experiences);
  }
}
