package com.api.rizz.portfolio_api.repository;

import com.api.rizz.portfolio_api.entity.Experience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
/** ExperienceRepository */
public interface ExperienceRepository
    extends JpaRepository<Experience, Long>, JpaSpecificationExecutor<Experience> {}
