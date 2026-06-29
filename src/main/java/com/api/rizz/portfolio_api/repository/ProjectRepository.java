package com.api.rizz.portfolio_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api.rizz.portfolio_api.entity.Project;

@Repository
/**
 * ProjectRepository
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

}
