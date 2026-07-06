package com.api.rizz.portfolio_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.api.rizz.portfolio_api.entity.Use;

@Repository
/**
 * UseRepository
 */
public interface UseRepository extends JpaRepository<Use, Long>, JpaSpecificationExecutor<Use> {

}
