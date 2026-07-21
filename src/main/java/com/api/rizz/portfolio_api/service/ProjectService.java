package com.api.rizz.portfolio_api.service;

import com.api.rizz.portfolio_api.dto.request.ProjectRequest;
import com.api.rizz.portfolio_api.dto.response.ProjectResponse;
import com.api.rizz.portfolio_api.entity.Project;
import com.api.rizz.portfolio_api.mapper.ProjectMapper;
import com.api.rizz.portfolio_api.repository.ProjectRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
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
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor // * Otomatis buatin Dependency Injection buat variabel "final"
/** ProjectService */
public class ProjectService {
  private final ProjectRepository projectRepository;
  private final ProjectMapper projectMapper;
  private final SnowflakeGenerator snowflakeGenerator;
  private final FileUploadService fileUploadService;

  @Transactional
  public ProjectResponse createProject(ProjectRequest projectRequest, MultipartFile logoFile,
      List<MultipartFile> imageFiles) {
    try {
      long newId = snowflakeGenerator.nextId();
      String generatedSlug = projectRequest.name().toLowerCase().replaceAll("[^a-z0-9]+", "-");
      Project project = projectMapper.toEntity(projectRequest);

      project.setId(newId);
      project.setSlug(generatedSlug);

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

  public Object findAllProjects(String search, String status, Long cursor, int page, int size,
      List<String> sortBy, List<String> sortDir) {
    Specification<Project> spec = (root, query, cb) -> {
      // * 1. Siapkan Filter (Where Clause Dinamis)
      List<Predicate> predicates = new ArrayList<>();

      // * Kalau ada keyword pencarian di nama project
      if (search != null && !search.isBlank()) {
        predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
      }

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

      // Jaga-jaga kalau project ngirim sortBy 2 biji, tapi sortDir cuma 1. Kita
      // default
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
      Page<Project> result = projectRepository.findAll(spec, limitOnly);
      return result.getContent().stream().map(projectMapper::toResponse).toList();
    } else {
      // * LOGIKA OFFSET (Default): Butuh info total halaman dan total data
      Pageable pageable = PageRequest.of(page, size, finalSort);
      Page<Project> result = projectRepository.findAll(spec, pageable);
      return result.map(projectMapper::toResponse);
    }
  }

  public ProjectResponse findProjectById(Long id) {
    Project project = projectRepository.findById(id).orElseThrow(
        () -> new NoSuchElementException("Project with ID: %d not found".formatted(id)));

    return projectMapper.toResponse(project);
  }

  @Transactional
  public ProjectResponse updateProject(Long id, ProjectRequest projectRequest,
      MultipartFile logoFile, List<MultipartFile> projectImageFiles) {
    try {
      Project project = projectRepository.findById(id).orElseThrow(
          () -> new NoSuchElementException("Project with ID: %d not found".formatted(id)));

      // * Update data entity lama pakai data request baru
      projectMapper.updateEntityFromRequest(projectRequest, project);

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
          if (oldPublicId != null)
            fileUploadService.deleteFile(oldPublicId);
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
    Project project = projectRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Blog with ID: %d not found".formatted(id)));

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
}
