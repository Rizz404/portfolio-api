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

import com.api.rizz.portfolio_api.dto.request.ProjectRequest;
import com.api.rizz.portfolio_api.dto.response.ProjectResponse;
import com.api.rizz.portfolio_api.entity.Project;
import com.api.rizz.portfolio_api.mapper.ProjectMapper;
import com.api.rizz.portfolio_api.repository.ProjectRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // * Otomatis buatin Dependency Injection buat variabel "final"
/**
 * ProjectService
 */
public class ProjectService {
  private final ProjectRepository projectRepository;
  private final ProjectMapper projectMapper;
  private final SnowflakeGenerator snowflakeGenerator;

  @Transactional
  public ProjectResponse createProject(ProjectRequest projectRequest) {
    long newId = snowflakeGenerator.nextId();
    Project project = projectMapper.toEntity(projectRequest);

    project.setId(newId);

    String generatedSlug = projectRequest.name().toLowerCase().replaceAll("[^a-z0-9]+", "-");

    project.setSlug(generatedSlug);

    Project savedProject = projectRepository.save(project);

    return projectMapper.toResponse(savedProject);
  }

  public Object findAllProjects(String search, String status, Long cursor, int page, int size, List<String> sortBy,
      List<String> sortDir) {
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
    Project project = projectRepository
        .findById(id)
        .orElseThrow(
            () -> new NoSuchElementException("Project with ID: %d not found".formatted(id)));

    return projectMapper.toResponse(project);
  }

  @Transactional
  public ProjectResponse updateProject(Long id, ProjectRequest projectRequest) {
    Project project = projectRepository
        .findById(id)
        .orElseThrow(
            () -> new NoSuchElementException("Project with ID: %d not found".formatted(id)));

    // * Update data entity lama pakai data request baru
    projectMapper.updateEntityFromRequest(projectRequest, project);

    String generatedSlug = projectRequest.name().toLowerCase().replaceAll("[^a-z0-9]+", "-");
    project.setSlug(generatedSlug);

    Project updatedProject = projectRepository.save(project);
    return projectMapper.toResponse(updatedProject);
  }

  @Transactional
  public void deleteProject(Long id) {
    if (!projectRepository.existsById(id)) {
      throw new NoSuchElementException("Project with ID: %d not found".formatted(id));
    }

    projectRepository.deleteById(id);
  }

}
