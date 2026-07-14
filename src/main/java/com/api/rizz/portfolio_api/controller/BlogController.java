package com.api.rizz.portfolio_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.rizz.portfolio_api.dto.request.BlogRequest;
import com.api.rizz.portfolio_api.dto.response.BlogResponse;
import com.api.rizz.portfolio_api.service.BlogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blogs")
@RequiredArgsConstructor
public class BlogController {
  final BlogService blogService;

  @PostMapping("")
  public ResponseEntity<BlogResponse> createBlog(@RequestBody BlogRequest request) {
    BlogResponse blogResponse = blogService.createBlog(request);

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

  @PatchMapping("/{id}")
  public ResponseEntity<BlogResponse> updateBlog(@PathVariable("id") Long id, @RequestBody BlogRequest request) {
    BlogResponse blogResponse = blogService.updateBlog(id, request);

    return new ResponseEntity<>(blogResponse, HttpStatus.OK);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteBlog(@PathVariable("id") Long id) {
    blogService.deleteBlog(id);

    return new ResponseEntity<>("Blog with ID: %d deleted".formatted(id), HttpStatus.OK);
  }
}
