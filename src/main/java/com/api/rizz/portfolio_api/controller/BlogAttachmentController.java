package com.api.rizz.portfolio_api.controller;

import com.api.rizz.portfolio_api.dto.request.BlogAttachmentRequest;
import com.api.rizz.portfolio_api.dto.response.BlogAttachmentResponse;
import com.api.rizz.portfolio_api.service.BlogAttachmentService;
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
  public ResponseEntity<BlogAttachmentResponse> createBlogAttachment(
      @RequestBody BlogAttachmentRequest request) {
    BlogAttachmentResponse blogAttachmentResponse =
        blogAttachmentService.createBlogAttachment(request);

    return new ResponseEntity<>(blogAttachmentResponse, HttpStatus.CREATED);
  }

  @GetMapping("")
  public ResponseEntity<Object> findAllBlogAttachments(
      @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") List<String> sortBy,
      @RequestParam(defaultValue = "desc") List<String> sortDir) {
    Object response =
        blogAttachmentService.findAllBlogAttachments(cursor, page, size, sortBy, sortDir);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<BlogAttachmentResponse> findBlogAttachmentById(
      @PathVariable("id") Long id) {
    BlogAttachmentResponse blogAttachmentResponse =
        blogAttachmentService.findBlogAttachmentById(id);

    return new ResponseEntity<>(blogAttachmentResponse, HttpStatus.OK);
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{id}")
  public ResponseEntity<BlogAttachmentResponse> updateBlogAttachment(
      @PathVariable("id") Long id, @RequestBody BlogAttachmentRequest request) {
    BlogAttachmentResponse blogAttachmentResponse =
        blogAttachmentService.updateBlogAttachment(id, request);

    return new ResponseEntity<>(blogAttachmentResponse, HttpStatus.OK);
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteBlogAttachment(@PathVariable("id") Long id) {
    blogAttachmentService.deleteBlogAttachment(id);

    return new ResponseEntity<>("BlogAttachment with ID: %d deleted".formatted(id), HttpStatus.OK);
  }
}
