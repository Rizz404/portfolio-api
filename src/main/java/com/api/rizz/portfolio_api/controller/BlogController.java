package com.api.rizz.portfolio_api.controller;

import java.util.List;

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

import com.api.rizz.portfolio_api.dto.request.BlogRequest;
import com.api.rizz.portfolio_api.dto.response.BlogResponse;
import com.api.rizz.portfolio_api.service.BlogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blogs")
@RequiredArgsConstructor
public class BlogController {
  final BlogService blogService;

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BlogResponse> createBlogJson(@RequestBody BlogRequest request) {
    // Kita kirim null untuk parameter file
    BlogResponse blogResponse = blogService.createBlog(request, null, null);
    return new ResponseEntity<>(blogResponse, HttpStatus.CREATED);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BlogResponse> createBlogMultipart(
      @RequestPart("data") BlogRequest request,
      @RequestPart(value = "featuredImageFile", required = false) MultipartFile featuredImageFile,
      @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {

    BlogResponse blogResponse = blogService.createBlog(request, featuredImageFile, attachments);
    return new ResponseEntity<>(blogResponse, HttpStatus.CREATED);
  }

  @GetMapping("")
  public ResponseEntity<Object> findAllBlogs(@RequestParam(required = false) String search,
      @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") List<String> sortBy,
      @RequestParam(defaultValue = "desc") List<String> sortDir) {
    Object response = blogService.findAllBlogs(search, cursor, page, size, sortBy, sortDir);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<BlogResponse> findBlogById(@PathVariable("id") Long id) {
    BlogResponse blogResponse = blogService.findBlogById(id);

    return new ResponseEntity<>(blogResponse, HttpStatus.OK);
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BlogResponse> updateBlogJson(
      @PathVariable("id") Long id,
      @RequestBody BlogRequest request) {

    BlogResponse blogResponse = blogService.updateBlog(id, request, null, null);
    return new ResponseEntity<>(blogResponse, HttpStatus.OK);
  }

  // Endpoint untuk update berbasis Multipart
  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BlogResponse> updateBlogMultipart(
      @PathVariable("id") Long id,
      @RequestPart("data") BlogRequest request,
      @RequestPart(value = "featuredImageFile", required = false) MultipartFile featuredImageFile,
      @RequestPart(value = "newAttachments", required = false) List<MultipartFile> newAttachments) {

    BlogResponse blogResponse = blogService.updateBlog(id, request, featuredImageFile, newAttachments);
    return new ResponseEntity<>(blogResponse, HttpStatus.OK);
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteBlog(@PathVariable("id") Long id) {
    blogService.deleteBlog(id);

    return new ResponseEntity<>("Blog with ID: %d deleted".formatted(id), HttpStatus.OK);
  }
}
