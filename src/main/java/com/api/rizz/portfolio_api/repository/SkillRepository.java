package com.api.rizz.portfolio_api.repository;

import com.api.rizz.portfolio_api.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
/** SkillRepository */
public interface SkillRepository
    extends JpaRepository<Skill, Long>, JpaSpecificationExecutor<Skill> {}
