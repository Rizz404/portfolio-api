package com.api.rizz.portfolio_api.controller;

import com.api.rizz.portfolio_api.CursorResponse;
import com.api.rizz.portfolio_api.dto.request.BatchDeleteRequest;
import com.api.rizz.portfolio_api.dto.request.BlogAttachmentRequest;
import com.api.rizz.portfolio_api.dto.response.BlogAttachmentResponse;
import com.api.rizz.portfolio_api.dto.response.CursorInfo;
import com.api.rizz.portfolio_api.dto.response.PagedResponse;
import com.api.rizz.portfolio_api.dto.response.PagingInfo;
import com.api.rizz.portfolio_api.dto.response.SuccessResponse;
import com.api.rizz.portfolio_api.service.BlogAttachmentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/blog-attachments")
@RequiredArgsConstructor
public class BlogAttachmentController {
  final BlogAttachmentService blogAttachmentService;

  @PreAuthorize("isAuthenticated()")
  @PostMapping("")
  public ResponseEntity<SuccessResponse<BlogAttachmentResponse>> createBlogAttachment(
      @Valid @RequestBody BlogAttachmentRequest request) {
    BlogAttachmentResponse blogAttachmentResponse =
        blogAttachmentService.createBlogAttachment(request);

    SuccessResponse<BlogAttachmentResponse> successResponse =
        new SuccessResponse<>("BlogAttachment created", blogAttachmentResponse);
    return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);
  }

  @GetMapping("")
  public ResponseEntity<?> findAllBlogAttachments(
      @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") List<String> sortBy,
      @RequestParam(defaultValue = "desc") List<String> sortDir) {
    Object response =
        blogAttachmentService.findAllBlogAttachments(cursor, page, size, sortBy, sortDir);

    if (response instanceof org.springframework.data.domain.Page<?> pageResult) {
      PagingInfo pagingInfo =
          new PagingInfo(
              (int) pageResult.getTotalElements(),
              pageResult.getSize(),
              pageResult.getNumber() + 1,
              pageResult.getTotalPages(),
              pageResult.hasPrevious(),
              pageResult.hasNext());

      PagedResponse<?> pagedResponse =
          new PagedResponse<>(
              "Successfully retrieved blog attachment list", pageResult.getContent(), pagingInfo);

      return ResponseEntity.ok(pagedResponse);
    } else if (response instanceof java.util.List<?> listResult) {
      @SuppressWarnings("unchecked")
      List<BlogAttachmentResponse> data = (List<BlogAttachmentResponse>) listResult;

      String nextCursor = null;
      boolean hasNextPage = false;

      // * Cek apakah data yang didapat lebih dari size yang diminta (artinya masih ada next page)
      if (data.size() > size) {
        hasNextPage = true;
        // Hapus elemen terakhir (elemen ekstra) agar tidak ikut ke-return ke Frontend
        data.remove(data.size() - 1);
      }

      if (!data.isEmpty()) {
        // Ambil ID dari elemen paling terakhir di list (setelah dipotong) sebagai nextCursor
        nextCursor = String.valueOf(data.get(data.size() - 1).id());
      }

      CursorInfo cursorInfo = new CursorInfo(nextCursor, data.size(), hasNextPage);
      CursorResponse<List<BlogAttachmentResponse>> cursorResponse =
          new CursorResponse<>(
              "Successfully retrieved blog attachment list with cursor", data, cursorInfo);

      return ResponseEntity.ok(cursorResponse);
    }

    return ResponseEntity.internalServerError().build();
  }

  @GetMapping("/{id}")
  public ResponseEntity<SuccessResponse<BlogAttachmentResponse>> findBlogAttachmentById(
      @PathVariable("id") Long id) {
    BlogAttachmentResponse blogAttachmentResponse =
        blogAttachmentService.findBlogAttachmentById(id);

    SuccessResponse<BlogAttachmentResponse> successResponse =
        new SuccessResponse<>("BlogAttachment retrieved", blogAttachmentResponse);
    return ResponseEntity.ok(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{id}")
  public ResponseEntity<SuccessResponse<BlogAttachmentResponse>> updateBlogAttachment(
      @PathVariable("id") Long id, @Valid @RequestBody BlogAttachmentRequest request) {
    BlogAttachmentResponse blogAttachmentResponse =
        blogAttachmentService.updateBlogAttachment(id, request);

    SuccessResponse<BlogAttachmentResponse> successResponse =
        new SuccessResponse<>("BlogAttachment updated", blogAttachmentResponse);
    return ResponseEntity.ok(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<SuccessResponse<String>> deleteBlogAttachment(@PathVariable("id") Long id) {
    blogAttachmentService.deleteBlogAttachment(id);

    SuccessResponse<String> successResponse =
        new SuccessResponse<>(
            "BlogAttachment deleted", "BlogAttachment with ID: %d deleted".formatted(id));
    return ResponseEntity.ok(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/batch")
  public ResponseEntity<SuccessResponse<String>> deleteBlogAttachmentBatch(
      @Valid @RequestBody BatchDeleteRequest request) {
    blogAttachmentService.deleteBlogAttachmentBatch(request.ids());

    SuccessResponse<String> successResponse =
        new SuccessResponse<>(
            "BlogAttachments deleted",
            "%d blog attachment(s) deleted".formatted(request.ids().size()));
    return ResponseEntity.ok(successResponse);
  }
}
