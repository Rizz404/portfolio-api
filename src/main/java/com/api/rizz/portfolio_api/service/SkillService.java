package com.api.rizz.portfolio_api.service;

import com.api.rizz.portfolio_api.dto.request.SkillRequest;
import com.api.rizz.portfolio_api.dto.response.SkillResponse;
import com.api.rizz.portfolio_api.entity.Skill;
import com.api.rizz.portfolio_api.mapper.SkillMapper;
import com.api.rizz.portfolio_api.repository.SkillRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import jakarta.persistence.criteria.Predicate;
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

  @Transactional
  public SkillResponse createSkill(SkillRequest skillRequest, MultipartFile logoFile) {
    try {
      long newId = snowflakeGenerator.nextId();
      Skill skill = skillMapper.toEntity(skillRequest);

      skill.setId(newId);

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
    } catch (Exception e) {
      throw new RuntimeException("Error when communicate with cloudinary: " + e.getMessage(), e);
    }
  }

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

          // * Kalau ada keyword pencarian di name
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

  public SkillResponse findSkillById(Long id) {
    Skill skill =
        skillRepository
            .findById(id)
            .orElseThrow(
                () -> new NoSuchElementException("Skill with ID: %d not found".formatted(id)));

    return skillMapper.toResponse(skill);
  }

  @Transactional
  public SkillResponse updateSkill(Long id, SkillRequest skillRequest, MultipartFile logoFile) {
    try {
      Skill skill =
          skillRepository
              .findById(id)
              .orElseThrow(
                  () -> new NoSuchElementException("Skill with ID: %d not found".formatted(id)));

      // * Update data entity lama pakai data request baru
      skillMapper.updateEntityFromRequest(skillRequest, skill);

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
          if (oldPublicId != null) fileUploadService.deleteFile(oldPublicId);
        }
        String uploadedUrl = fileUploadService.uploadFile(logoFile, "portfolio/skills/logo");
        skill.setLogoUrl(uploadedUrl);
      } else if (hasStringUrl) {
        skill.setLogoUrl(skillRequest.logoUrl());
      }

      Skill updatedSkill = skillRepository.save(skill);
      return skillMapper.toResponse(updatedSkill);
    } catch (Exception e) {
      throw new RuntimeException("Error during update mutation: " + e.getMessage(), e);
    }
  }

  @Transactional
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
}
