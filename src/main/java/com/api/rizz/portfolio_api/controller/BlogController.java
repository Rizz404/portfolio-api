package com.api.rizz.portfolio_api.controller;

import com.api.rizz.portfolio_api.CursorResponse;
import com.api.rizz.portfolio_api.dto.request.BlogRequest;
import com.api.rizz.portfolio_api.dto.response.BlogResponse;
import com.api.rizz.portfolio_api.dto.response.CursorInfo;
import com.api.rizz.portfolio_api.dto.response.PagedResponse;
import com.api.rizz.portfolio_api.dto.response.PagingInfo;
import com.api.rizz.portfolio_api.dto.response.SuccessResponse;
import com.api.rizz.portfolio_api.service.BlogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/blogs")
@RequiredArgsConstructor
public class BlogController {
  final BlogService blogService;

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SuccessResponse<BlogResponse>> createBlogJson(
      @RequestBody BlogRequest request) {
    // Kita kirim null untuk parameter file
    BlogResponse blogResponse = blogService.createBlog(request, null, null);

    SuccessResponse<BlogResponse> successResponse =
        new SuccessResponse<>("Blog created", blogResponse);
    return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SuccessResponse<BlogResponse>> createBlogMultipart(
      @RequestPart("data") BlogRequest request,
      @RequestPart(value = "featuredImageFile", required = false) MultipartFile featuredImageFile,
      @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {

    BlogResponse blogResponse = blogService.createBlog(request, featuredImageFile, attachments);

    SuccessResponse<BlogResponse> successResponse =
        new SuccessResponse<>("Blog created", blogResponse);
    return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);
  }

  @GetMapping("")
  public ResponseEntity<?> findAllBlogs(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") List<String> sortBy,
      @RequestParam(defaultValue = "desc") List<String> sortDir) {
    Object response = blogService.findAllBlogs(search, cursor, page, size, sortBy, sortDir);

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
              "Berhasil mengambil daftar blog", pageResult.getContent(), pagingInfo);

      return ResponseEntity.ok(pagedResponse);
    } else if (response instanceof java.util.List<?> listResult) {
      @SuppressWarnings("unchecked")
      List<BlogResponse> data = (List<BlogResponse>) listResult;

      Long nextCursor = null;
      boolean hasNextPage = false;

      if (!data.isEmpty()) {
        nextCursor = Long.valueOf(data.get(data.size() - 1).id());
        hasNextPage = data.size() == size;
      }

      CursorInfo cursorInfo = new CursorInfo(nextCursor, hasNextPage, size);
      CursorResponse<List<BlogResponse>> cursorResponse =
          new CursorResponse<>("Berhasil mengambil daftar blog dengan cursor", data, cursorInfo);

      return ResponseEntity.ok(cursorResponse);
    }

    return ResponseEntity.internalServerError().build();
  }

  @GetMapping("/{id}")
  public ResponseEntity<SuccessResponse<BlogResponse>> findBlogById(@PathVariable("id") Long id) {
    BlogResponse blogResponse = blogService.findBlogById(id);

    SuccessResponse<BlogResponse> successResponse =
        new SuccessResponse<>("Blog retrieved", blogResponse);
    return ResponseEntity.ok(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SuccessResponse<BlogResponse>> updateBlogJson(
      @PathVariable("id") Long id, @RequestBody BlogRequest request) {

    BlogResponse blogResponse = blogService.updateBlog(id, request, null, null);

    SuccessResponse<BlogResponse> successResponse =
        new SuccessResponse<>("Blog updated", blogResponse);
    return ResponseEntity.ok(successResponse);
  }

  // Endpoint untuk update berbasis Multipart
  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SuccessResponse<BlogResponse>> updateBlogMultipart(
      @PathVariable("id") Long id,
      @RequestPart("data") BlogRequest request,
      @RequestPart(value = "featuredImageFile", required = false) MultipartFile featuredImageFile,
      @RequestPart(value = "newAttachments", required = false) List<MultipartFile> newAttachments) {

    BlogResponse blogResponse =
        blogService.updateBlog(id, request, featuredImageFile, newAttachments);

    SuccessResponse<BlogResponse> successResponse =
        new SuccessResponse<>("Blog updated", blogResponse);
    return ResponseEntity.ok(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<SuccessResponse<String>> deleteBlog(@PathVariable("id") Long id) {
    blogService.deleteBlog(id);

    SuccessResponse<String> successResponse =
        new SuccessResponse<>("Blog deleted", "Blog with ID: %d deleted".formatted(id));
    return ResponseEntity.ok(successResponse);
  }
}
