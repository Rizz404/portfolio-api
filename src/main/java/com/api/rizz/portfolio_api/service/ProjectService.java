package com.api.rizz.portfolio_api.service;

import com.api.rizz.portfolio_api.dto.request.ProjectRequest;
import com.api.rizz.portfolio_api.dto.request.ProjectTranslationRequest;
import com.api.rizz.portfolio_api.dto.response.ProjectResponse;
import com.api.rizz.portfolio_api.entity.LanguageCode;
import com.api.rizz.portfolio_api.entity.Project;
import com.api.rizz.portfolio_api.entity.Project.ProjectStatus;
import com.api.rizz.portfolio_api.entity.ProjectTranslation;
import com.api.rizz.portfolio_api.mapper.ProjectMapper;
import com.api.rizz.portfolio_api.repository.ProjectRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
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
/** ProjectService */
public class ProjectService {
  private final ProjectRepository projectRepository;
  private final ProjectMapper projectMapper;
  private final SnowflakeGenerator snowflakeGenerator;
  private final FileUploadService fileUploadService;

  // * Field translatable yang gak bisa di-sort langsung karena kolomnya sekarang ada di tabel
  // * translation (join lewat @OneToMany rapuh untuk Sort/QueryUtils) - di-drop diam-diam dari
  // * sortBy kalau diminta.
  private static final Set<String> TRANSLATABLE_SORT_FIELDS = Set.of("name", "description");

  // * Resolve locale dari Accept-Language header (via LocaleContextHolder), fallback ke 'en'
  // * kalau bahasanya gak didukung.
  private LanguageCode resolveRequestLocale() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    try {
      return LanguageCode.valueOf(lang);
    } catch (IllegalArgumentException e) {
      return LanguageCode.en;
    }
  }

  private List<ProjectTranslation> buildTranslations(
      List<ProjectTranslationRequest> requests, Project project) {
    List<ProjectTranslation> translations = new ArrayList<>();
    for (ProjectTranslationRequest t : requests) {
      translations.add(
          ProjectTranslation.builder()
              .project(project)
              .locale(t.locale())
              .name(t.name())
              .description(t.description())
              .build());
    }
    return translations;
  }

  // * Update translations locale yang sama in-place (bukan clear()+addAll()) - clear+addAll bikin
  // * Hibernate insert baris baru SEBELUM delete baris lama di flush yang sama, jadi tabrakan
  // * UNIQUE(project_id, locale) kalau locale-nya gak berubah (kasus paling umum saat update).
  private void reconcileTranslations(Project project, List<ProjectTranslationRequest> requests) {
    List<ProjectTranslation> existing = project.getTranslations();
    java.util.Map<LanguageCode, ProjectTranslation> byLocale = new java.util.HashMap<>();
    for (ProjectTranslation t : existing) {
      byLocale.put(t.getLocale(), t);
    }

    java.util.Set<LanguageCode> requestedLocales = new java.util.HashSet<>();
    for (ProjectTranslationRequest r : requests) {
      requestedLocales.add(r.locale());
    }
    // * Locale yang udah gak diminta lagi dihapus (orphanRemoval yang handle DELETE-nya)
    existing.removeIf(t -> !requestedLocales.contains(t.getLocale()));

    for (ProjectTranslationRequest r : requests) {
      ProjectTranslation match = byLocale.get(r.locale());
      if (match != null) {
        match.setName(r.name());
        match.setDescription(r.description());
      } else {
        existing.add(
            ProjectTranslation.builder()
                .project(project)
                .locale(r.locale())
                .name(r.name())
                .description(r.description())
                .build());
      }
    }
  }

  @Transactional
  public ProjectResponse createProject(
      ProjectRequest projectRequest, MultipartFile logoFile, List<MultipartFile> imageFiles) {
    try {
      long newId = snowflakeGenerator.nextId();

      // * Slug selalu dibuat dari nama locale 'en' (default/fallback), bukan dari locale
      // * request-time, biar slug stabil gak berubah tergantung Accept-Language header
      String enName =
          projectRequest.translations().stream()
              .filter(t -> t.locale() == LanguageCode.en)
              .findFirst()
              .map(ProjectTranslationRequest::name)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException("Default locale (en) translation is required"));
      String generatedSlug = enName.toLowerCase().replaceAll("[^a-z0-9]+", "-");
      Project project = projectMapper.toEntity(projectRequest);

      project.setId(newId);
      project.setSlug(generatedSlug);
      project.setTranslations(buildTranslations(projectRequest.translations(), project));

      boolean hasLogoString =
          projectRequest.logoUrl() != null && !projectRequest.logoUrl().isBlank();
      boolean hasLogoFile = logoFile != null && !logoFile.isEmpty();

      if (hasLogoString && hasLogoFile) {
        // * Gak bisa keduanya
        throw new IllegalArgumentException(
            "Cannot accept both 'logoUrl' string and 'logoFileFile'. Choose one.");
      }

      if (hasLogoFile) {
        String logoUrl = fileUploadService.uploadFile(logoFile, "portfolio/projects/logo");
        project.setLogoUrl(logoUrl);
      } else if (hasLogoString) {
        project.setLogoUrl(projectRequest.logoUrl());
      }

      boolean hasImageStrings =
          projectRequest.imageUrls() != null && !projectRequest.imageUrls().isEmpty();
      boolean hasImageFiles = imageFiles != null && !imageFiles.isEmpty();

      if (hasImageFiles) {
        List<String> imageUrls =
            fileUploadService.uploadFiles(imageFiles, "portfolio/projects/image");
        project.setImageUrls(imageUrls);
      } else if (hasImageStrings) {
        project.setImageUrls(projectRequest.imageUrls());
      }

      // * Set timestamp manual karena pakai snowflakes jadi ada write behind pada hibernate
      OffsetDateTime now = OffsetDateTime.now();
      project.setCreatedAt(now);
      project.setUpdatedAt(now);

      Project savedProject = projectRepository.save(project);

      return projectMapper.toResponse(savedProject);
    } catch (Exception e) {
      throw new RuntimeException("Error when communicate with cloudinary: " + e.getMessage(), e);
    }
  }

  // * @Transactional wajib: mapper resolve translations (LAZY @OneToMany) di toResponse(),
  // * butuh session Hibernate masih terbuka; open-in-view=false jadi gak otomatis
  @Transactional(readOnly = true)
  public Object findAllProjects(
      String search,
      String status,
      Long cursor,
      int page,
      int size,
      List<String> sortBy,
      List<String> sortDir) {
    Specification<Project> spec =
        (root, query, cb) -> {
          // * 1. Siapkan Filter (Where Clause Dinamis)
          List<Predicate> predicates = new ArrayList<>();

          // * Kalau ada keyword pencarian di nama project - join ke translation sesuai locale
          // * request (Accept-Language), fallback 'en'. Project yang cuma punya translation 'en'
          // * gak bakal match search pas Accept-Language: id, meski tetap tampil (fallback) kalau
          // * di-fetch by ID - trade-off yang diterima, bukan bug.
          if (search != null && !search.isBlank()) {
            Join<Project, ProjectTranslation> t = root.join("translations", JoinType.LEFT);
            predicates.add(cb.equal(t.get("locale"), resolveRequestLocale()));
            predicates.add(cb.like(cb.lower(t.get("name")), "%" + search.toLowerCase() + "%"));
          }

          // * Kalau mau filter berdasarkan status (active/development)
          if (status != null && !status.isBlank()) {
            predicates.add(cb.equal(root.get("status"), ProjectStatus.valueOf(status)));
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

      // * Field translatable (name/description) sekarang ada di tabel terpisah - sort lewat
      // * @OneToMany join rapuh di Spring Data QueryUtils, jadi di-drop diam-diam alih-alih 500
      if (TRANSLATABLE_SORT_FIELDS.contains(field)) {
        continue;
      }

      // Jaga-jaga kalau project ngirim sortBy 2 biji, tapi sortDir cuma 1. Kita
      // default
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
      Page<Project> result = projectRepository.findAll(spec, limitOnly);
      return result.getContent().stream().map(projectMapper::toResponse).toList();
    } else {
      // * LOGIKA OFFSET (Default): Butuh info total halaman dan total data
      // * Kurangi 1 biar gak minus page nya
      int actualPage = page > 0 ? page - 1 : 0;
      Pageable pageable = PageRequest.of(actualPage, size, finalSort);
      Page<Project> result = projectRepository.findAll(spec, pageable);
      return result.map(projectMapper::toResponse);
    }
  }

  @Transactional(readOnly = true)
  public ProjectResponse findProjectById(Long id) {
    Project project =
        projectRepository
            .findById(id)
            .orElseThrow(
                () -> new NoSuchElementException("Project with ID: %d not found".formatted(id)));

    return projectMapper.toResponse(project);
  }

  @Transactional
  public ProjectResponse updateProject(
      Long id,
      ProjectRequest projectRequest,
      MultipartFile logoFile,
      List<MultipartFile> projectImageFiles) {
    try {
      Project project =
          projectRepository
              .findById(id)
              .orElseThrow(
                  () -> new NoSuchElementException("Project with ID: %d not found".formatted(id)));

      // * Update data entity lama pakai data request baru
      projectMapper.updateEntityFromRequest(projectRequest, project);

      // * PENTING: gak boleh project.setTranslations(newList) - collection ini managed +
      // * orphanRemoval=true, replace reference-nya bikin HibernateException. Reconcile in-place
      // * (update locale yang sama, bukan delete+insert) biar gak tabrakan UNIQUE constraint.
      reconcileTranslations(project, projectRequest.translations());

      boolean hasStringUrl =
          projectRequest.logoUrl() != null && !projectRequest.logoUrl().isBlank();
      boolean hasFile = logoFile != null && !logoFile.isEmpty();

      if (hasStringUrl && hasFile) {
        throw new IllegalArgumentException(
            "Cannot accept both 'logoUrl' string and 'logoFile'. Choose one.");
      }

      if (hasFile) {
        // Hapus file lama di Cloudinary jika ada
        if (project.getLogoUrl() != null) {
          String oldPublicId = fileUploadService.extractCloudinaryPublicId(project.getLogoUrl());
          if (oldPublicId != null) {
            try {
              fileUploadService.deleteFile(oldPublicId);
            } catch (Exception ignored) {
            }
          }
        }
        String uploadedUrl = fileUploadService.uploadFile(logoFile, "portfolio/projects/logo");
        project.setLogoUrl(uploadedUrl);
      } else if (hasStringUrl) {
        project.setLogoUrl(projectRequest.logoUrl());
      }

      if (projectRequest.deletedImageUrls() != null
          && !projectRequest.deletedImageUrls().isEmpty()) {
        // Hapus fisik di Cloudinary
        fileUploadService.deleteFilesByUrls(projectRequest.deletedImageUrls());
        // Hapus string URL dari List Entity Database
        if (project.getImageUrls() != null) {
          project.getImageUrls().removeAll(projectRequest.deletedImageUrls());
        }
      }

      if (projectImageFiles != null && !projectImageFiles.isEmpty()) {
        List<String> newUrls =
            fileUploadService.uploadFiles(projectImageFiles, "portfolio/projects/image");

        // Jaga-jaga jika array di DB masih null
        if (project.getImageUrls() == null) {
          project.setImageUrls(new ArrayList<>());
        }
        // Gabungkan array baru dengan array yang tersisa
        project.getImageUrls().addAll(newUrls);
      }

      Project updatedProject = projectRepository.save(project);
      return projectMapper.toResponse(updatedProject);
    } catch (Exception e) {
      throw new RuntimeException("Error during update mutation: " + e.getMessage(), e);
    }
  }

  @Transactional
  public void deleteProject(Long id) {
    Project project =
        projectRepository
            .findById(id)
            .orElseThrow(
                () -> new NoSuchElementException("Blog with ID: %d not found".formatted(id)));

    if (!projectRepository.existsById(id)) {
      throw new NoSuchElementException("Project with ID: %d not found".formatted(id));
    }

    if (project.getLogoUrl() != null) {
      String logoPublicId = fileUploadService.extractCloudinaryPublicId(project.getLogoUrl());
      if (logoPublicId != null) {
        try {
          fileUploadService.deleteFile(logoPublicId);
        } catch (Exception ignored) {
        }
      }
    }

    if (project.getImageUrls() != null && !project.getImageUrls().isEmpty()) {
      fileUploadService.deleteFilesByUrls(project.getImageUrls());
    }

    projectRepository.deleteById(id);
  }

  @Transactional
  public void deleteProjectBatch(List<Long> ids) {
    // * Ambil semua entity sekaligus dalam 1 query, bukan findById satu-satu
    List<Project> projects = projectRepository.findAllById(ids);

    if (projects.size() != ids.size()) {
      List<Long> foundIds = projects.stream().map(Project::getId).toList();
      List<Long> missingIds = ids.stream().filter(id -> !foundIds.contains(id)).toList();
      throw new NoSuchElementException("Project(s) with ID: %s not found".formatted(missingIds));
    }

    for (Project project : projects) {
      if (project.getLogoUrl() != null) {
        String logoPublicId = fileUploadService.extractCloudinaryPublicId(project.getLogoUrl());
        if (logoPublicId != null) {
          try {
            fileUploadService.deleteFile(logoPublicId);
          } catch (Exception ignored) {
          }
        }
      }

      if (project.getImageUrls() != null && !project.getImageUrls().isEmpty()) {
        fileUploadService.deleteFilesByUrls(project.getImageUrls());
      }
    }

    // * deleteAll (bukan deleteAllInBatch) supaya cascade/orphanRemoval JPA tetap jalan
    projectRepository.deleteAll(projects);
  }
}
