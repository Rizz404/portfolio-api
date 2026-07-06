package com.api.rizz.portfolio_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.api.rizz.portfolio_api.entity.BlogAttachment;

@Repository
/**
 * BlogAttachmentRepository
 */
public interface BlogAttachmentRepository
    extends JpaRepository<BlogAttachment, Long>, JpaSpecificationExecutor<BlogAttachment> {

}
