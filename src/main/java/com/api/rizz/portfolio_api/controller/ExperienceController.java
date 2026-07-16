package com.api.rizz.portfolio_api.controller;

import com.api.rizz.portfolio_api.dto.request.ExperienceRequest;
import com.api.rizz.portfolio_api.dto.response.ExperienceResponse;
import com.api.rizz.portfolio_api.service.ExperienceService;
import java.time.LocalDate;
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
@RequestMapping("/experiences")
@RequiredArgsConstructor
public class ExperienceController {
  final ExperienceService experienceService;

  @PreAuthorize("isAuthenticated()")
  @PostMapping("")
  public ResponseEntity<ExperienceResponse> createExperience(
      @RequestBody ExperienceRequest request) {
    ExperienceResponse experienceResponse = experienceService.createExperience(request);

    return new ResponseEntity<>(experienceResponse, HttpStatus.CREATED);
  }

  @GetMapping("")
  public ResponseEntity<Object> findAllExperiences(
      @RequestParam(required = false) String search,
      // * Cuma jadiin string default valuenya untuk bolean jadi bisa di convert ke
      // * string
      @RequestParam(defaultValue = "false") Boolean isCurrent,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") List<String> sortBy,
      @RequestParam(defaultValue = "desc") List<String> sortDir) {
    Object response =
        experienceService.findAllExperiences(
            search, isCurrent, startDate, endDate, cursor, page, size, sortBy, sortDir);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<ExperienceResponse> findExperienceById(@PathVariable("id") Long id) {
    ExperienceResponse experienceResponse = experienceService.findExperienceById(id);

    return new ResponseEntity<>(experienceResponse, HttpStatus.OK);
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{id}")
  public ResponseEntity<ExperienceResponse> updateExperience(
      @PathVariable("id") Long id, @RequestBody ExperienceRequest request) {
    ExperienceResponse experienceResponse = experienceService.updateExperience(id, request);

    return new ResponseEntity<>(experienceResponse, HttpStatus.OK);
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteExperience(@PathVariable("id") Long id) {
    experienceService.deleteExperience(id);

    return new ResponseEntity<>("Experience with ID: %d deleted".formatted(id), HttpStatus.OK);
  }
}
