package com.api.rizz.portfolio_api.repository;

import com.api.rizz.portfolio_api.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
/** ProjectRepository */
public interface ProjectRepository
    extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {}

// JpaSpecificationExecutor, Spring Boot otomatis bikinin fungsi pencarian super
// canggih di balik layar
