package com.api.rizz.portfolio_api.service;

import com.api.rizz.portfolio_api.dto.request.SkillRequest;
import com.api.rizz.portfolio_api.dto.request.SkillTranslationRequest;
import com.api.rizz.portfolio_api.dto.response.SkillResponse;
import com.api.rizz.portfolio_api.entity.LanguageCode;
import com.api.rizz.portfolio_api.entity.Skill;
import com.api.rizz.portfolio_api.entity.SkillTranslation;
import com.api.rizz.portfolio_api.mapper.SkillMapper;
import com.api.rizz.portfolio_api.repository.SkillRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
/** SkillService */
public class SkillService {
  private final SkillRepository skillRepository;
  private final SkillMapper skillMapper;
  private final SnowflakeGenerator snowflakeGenerator;
  private final FileUploadService fileUploadService;

  private static final Set<String> TRANSLATABLE_SORT_FIELDS = Set.of("description");

  // * Nama cache Redis buat domain skill (lihat CacheConfig). Query dievict semua (allEntries)
  // * tiap ada mutasi (create/update/delete) -- daripada invalidate parsial per kombinasi
  // * filter/sort/page yang gak kebayang jumlahnya.
  private static final String CACHE_NAME = "skills";

  private LanguageCode resolveRequestLocale() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    try {
      return LanguageCode.valueOf(lang);
    } catch (IllegalArgumentException e) {
      return LanguageCode.en;
    }
  }

  private List<SkillTranslation> buildTranslations(
      List<SkillTranslationRequest> requests, Skill skill) {
    List<SkillTranslation> translations = new ArrayList<>();
    for (SkillTranslationRequest t : requests) {
      translations.add(
          SkillTranslation.builder()
              .skill(skill)
              .locale(t.locale())
              .description(t.description())
              .build());
    }
    return translations;
  }

  // * Update translations locale yang sama in-place (bukan clear()+addAll()) - clear+addAll bikin
  // * Hibernate insert baris baru SEBELUM delete baris lama di flush yang sama, jadi tabrakan
  // * UNIQUE(skill_id, locale) kalau locale-nya gak berubah (kasus paling umum saat update).
  private void reconcileTranslations(Skill skill, List<SkillTranslationRequest> requests) {
    List<SkillTranslation> existing = skill.getTranslations();
    java.util.Map<LanguageCode, SkillTranslation> byLocale = new java.util.HashMap<>();
    for (SkillTranslation t : existing) {
      byLocale.put(t.getLocale(), t);
    }

    java.util.Set<LanguageCode> requestedLocales = new java.util.HashSet<>();
    for (SkillTranslationRequest r : requests) {
      requestedLocales.add(r.locale());
    }
    existing.removeIf(t -> !requestedLocales.contains(t.getLocale()));

    for (SkillTranslationRequest r : requests) {
      SkillTranslation match = byLocale.get(r.locale());
      if (match != null) {
        match.setDescription(r.description());
      } else {
        existing.add(
            SkillTranslation.builder()
                .skill(skill)
                .locale(r.locale())
                .description(r.description())
                .build());
      }
    }
  }

  @Transactional
  @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
  public SkillResponse createSkill(SkillRequest skillRequest, MultipartFile logoFile) {
    try {
      long newId = snowflakeGenerator.nextId();
      Skill skill = skillMapper.toEntity(skillRequest);

      skill.setId(newId);
      skill.setTranslations(buildTranslations(skillRequest.translations(), skill));

      boolean hasLogoString = skillRequest.logoUrl() != null && !skillRequest.logoUrl().isBlank();
      boolean hasLogoFile = logoFile != null && !logoFile.isEmpty();

      if (hasLogoString && hasLogoFile) {
        // * Gak bisa keduanya
        throw new IllegalArgumentException(
            "Cannot accept both 'logoUrl' string and 'logoFile'. Choose one.");
      }

      if (hasLogoFile) {
        String logoUrl = fileUploadService.uploadFile(logoFile, "portfolio/skills/logo");
        skill.setLogoUrl(logoUrl);
      } else if (hasLogoString) {
        skill.setLogoUrl(skillRequest.logoUrl());
      }

      // * Set timestamp manual karena pakai snowflakes jadi ada write behind pada hibernate
      OffsetDateTime now = OffsetDateTime.now();
      skill.setCreatedAt(now);
      skill.setUpdatedAt(now);

      Skill savedSkill = skillRepository.save(skill);

      return skillMapper.toResponse(savedSkill);
    } catch (IOException e) {
      throw new RuntimeException("Error when communicate with cloudinary: " + e.getMessage(), e);
    }
  }

  // * @Transactional wajib: mapper resolve translations (LAZY @OneToMany) di toResponse(),
  // * butuh session Hibernate masih terbuka; open-in-view=false jadi gak otomatis
  @Transactional(readOnly = true)
  // * Locale ikut jadi bagian key -- response-nya ke-resolve per locale (lihat SkillMapper), jadi
  // * kalau gak ikut dimasukin, request locale ID bisa ke-serve cache hasil locale EN (atau
  // * sebaliknya).
  @Cacheable(
      cacheNames = CACHE_NAME,
      key =
          "T(org.springframework.context.i18n.LocaleContextHolder).getLocale() + ':' + #search"
              + " + ':' + #category + ':' + #cursor + ':' + #page + ':' + #size + ':' + #sortBy"
              + " + ':' + #sortDir")
  public Object findAllSkills(
      String search,
      String category,
      Long cursor,
      int page,
      int size,
      List<String> sortBy,
      List<String> sortDir) {
    Specification<Skill> spec =
        (root, query, cb) -> {
          // * 1. Siapkan Filter (Where Clause Dinamis)
          List<Predicate> predicates = new ArrayList<>();

          // * Kalau ada keyword pencarian di name (name tetap di tabel utama, bukan translatable)
          if (search != null && !search.isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
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

      // * description sekarang ada di tabel terpisah - drop diam-diam alih-alih error
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
      Page<Skill> result = skillRepository.findAll(spec, limitOnly);
      return result.getContent().stream().map(skillMapper::toResponse).toList();
    } else {
      // * LOGIKA OFFSET (Default): Butuh info total halaman dan total data
      // * Kurangi 1 biar gak minus page nya
      int actualPage = page > 0 ? page - 1 : 0;
      Pageable pageable = PageRequest.of(actualPage, size, finalSort);
      Page<Skill> result = skillRepository.findAll(spec, pageable);
      return result.map(skillMapper::toResponse);
    }
  }

  @Transactional(readOnly = true)
  @Cacheable(
      cacheNames = CACHE_NAME,
      key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale() + ':' + #id")
  public SkillResponse findSkillById(Long id) {
    Skill skill =
        skillRepository
            .findById(id)
            .orElseThrow(
                () -> new NoSuchElementException("Skill with ID: %d not found".formatted(id)));

    return skillMapper.toResponse(skill);
  }

  @Transactional
  @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
  public SkillResponse updateSkill(Long id, SkillRequest skillRequest, MultipartFile logoFile) {
    try {
      Skill skill =
          skillRepository
              .findById(id)
              .orElseThrow(
                  () -> new NoSuchElementException("Skill with ID: %d not found".formatted(id)));

      // * Update data entity lama pakai data request baru
      skillMapper.updateEntityFromRequest(skillRequest, skill);

      // * Mutasi in-place, JANGAN setTranslations(newList) - lihat catatan di ProjectService
      reconcileTranslations(skill, skillRequest.translations());

      boolean hasStringUrl = skillRequest.logoUrl() != null && !skillRequest.logoUrl().isBlank();
      boolean hasFile = logoFile != null && !logoFile.isEmpty();

      if (hasStringUrl && hasFile) {
        throw new IllegalArgumentException(
            "Cannot accept both 'logoUrl' string and 'logoFile'. Choose one.");
      }

      if (hasFile) {
        // Hapus file lama di Cloudinary jika ada
        if (skill.getLogoUrl() != null) {
          String oldPublicId = fileUploadService.extractCloudinaryPublicId(skill.getLogoUrl());
          if (oldPublicId != null) {
            try {
              fileUploadService.deleteFile(oldPublicId);
            } catch (Exception ignored) {
            }
          }
        }
        String uploadedUrl = fileUploadService.uploadFile(logoFile, "portfolio/skills/logo");
        skill.setLogoUrl(uploadedUrl);
      } else if (hasStringUrl) {
        skill.setLogoUrl(skillRequest.logoUrl());
      }

      Skill updatedSkill = skillRepository.save(skill);
      return skillMapper.toResponse(updatedSkill);
    } catch (IOException e) {
      throw new RuntimeException("Error during update mutation: " + e.getMessage(), e);
    }
  }

  @Transactional
  @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
  public void deleteSkill(Long id) {
    Skill skill =
        skillRepository
            .findById(id)
            .orElseThrow(
                () -> new NoSuchElementException("Skill with ID: %d not found".formatted(id)));

    if (skill.getLogoUrl() != null) {
      String logoPublicId = fileUploadService.extractCloudinaryPublicId(skill.getLogoUrl());
      if (logoPublicId != null) {
        try {
          fileUploadService.deleteFile(logoPublicId);
        } catch (Exception ignored) {
        }
      }
    }

    skillRepository.deleteById(id);
  }

  @Transactional
  @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
  public void deleteSkillBatch(List<Long> ids) {
    List<Skill> skills = skillRepository.findAllById(ids);

    if (skills.size() != ids.size()) {
      List<Long> foundIds = skills.stream().map(Skill::getId).toList();
      List<Long> missingIds = ids.stream().filter(id -> !foundIds.contains(id)).toList();
      throw new NoSuchElementException("Skill(s) with ID: %s not found".formatted(missingIds));
    }

    for (Skill skill : skills) {
      if (skill.getLogoUrl() != null) {
        String logoPublicId = fileUploadService.extractCloudinaryPublicId(skill.getLogoUrl());
        if (logoPublicId != null) {
          try {
            fileUploadService.deleteFile(logoPublicId);
          } catch (Exception ignored) {
          }
        }
      }
    }

    skillRepository.deleteAll(skills);
  }
}
