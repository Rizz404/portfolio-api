package com.api.rizz.portfolio_api.service;

import org.springframework.stereotype.Service;

import com.api.rizz.portfolio_api.entity.Project;
import com.api.rizz.portfolio_api.repository.ProjectRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // * Otomatis buatin Dependency Injection buat variabel "final"
/**
 * ProjectService
 */
public class ProjectService {
  private final ProjectRepository projectRepository;
  private final SnowflakeGenerator snowflakeGenerator;

  @Transactional
  public Project createProject(Project project) {
    long newId = snowflakeGenerator.nextId();

    project.setId(newId);

    return projectRepository.save(project);
  }

}
