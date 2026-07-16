package com.api.rizz.portfolio_api.repository;

import com.api.rizz.portfolio_api.entity.Use;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
/** UseRepository */
public interface UseRepository extends JpaRepository<Use, Long>, JpaSpecificationExecutor<Use> {}
