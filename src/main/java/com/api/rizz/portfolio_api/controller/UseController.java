package com.api.rizz.portfolio_api.controller;

import java.util.List;

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

import com.api.rizz.portfolio_api.dto.request.UseRequest;
import com.api.rizz.portfolio_api.dto.response.UseResponse;
import com.api.rizz.portfolio_api.service.UseService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/uses")
@RequiredArgsConstructor
public class UseController {
  final UseService useService;

  @PreAuthorize("isAuthenticated()")
  @PostMapping("")
  public ResponseEntity<UseResponse> createUse(@RequestBody UseRequest request) {
    UseResponse useResponse = useService.createUse(request);

    return new ResponseEntity<>(useResponse, HttpStatus.CREATED);
  }

  @GetMapping("")
  public ResponseEntity<Object> findAllUses(@RequestParam(required = false) String search,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") List<String> sortBy,
      @RequestParam(defaultValue = "desc") List<String> sortDir) {
    Object response = useService.findAllUses(search, category, cursor, page, size,
        sortBy,
        sortDir);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<UseResponse> findUseById(@PathVariable("id") Long id) {
    UseResponse useResponse = useService.findUseById(id);

    return new ResponseEntity<>(useResponse, HttpStatus.OK);
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{id}")
  public ResponseEntity<UseResponse> updateUse(@PathVariable("id") Long id,
      @RequestBody UseRequest request) {
    UseResponse useResponse = useService.updateUse(id, request);

    return new ResponseEntity<>(useResponse, HttpStatus.OK);
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteUse(@PathVariable("id") Long id) {
    useService.deleteUse(id);

    return new ResponseEntity<>("Use with ID: %d deleted".formatted(id), HttpStatus.OK);
  }
}
