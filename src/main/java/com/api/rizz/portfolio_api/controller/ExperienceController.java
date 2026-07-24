package com.api.rizz.portfolio_api.controller;

import com.api.rizz.portfolio_api.CursorResponse;
import com.api.rizz.portfolio_api.dto.request.ExperienceRequest;
import com.api.rizz.portfolio_api.dto.response.CursorInfo;
import com.api.rizz.portfolio_api.dto.response.ExperienceResponse;
import com.api.rizz.portfolio_api.dto.response.PagedResponse;
import com.api.rizz.portfolio_api.dto.response.PagingInfo;
import com.api.rizz.portfolio_api.dto.response.SuccessResponse;
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
  public ResponseEntity<SuccessResponse<ExperienceResponse>> createExperience(
      @RequestBody ExperienceRequest request) {
    ExperienceResponse experienceResponse = experienceService.createExperience(request);

    SuccessResponse<ExperienceResponse> successResponse =
        new SuccessResponse<>("Experience created", experienceResponse);
    return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);
  }

  @GetMapping("")
  public ResponseEntity<?> findAllExperiences(@RequestParam(required = false) String search,
      // * Cuma jadiin string default valuenya untuk bolean jadi bisa di convert ke
      // * string
      @RequestParam(defaultValue = "false") Boolean isCurrent,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) Long cursor, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") List<String> sortBy,
      @RequestParam(defaultValue = "desc") List<String> sortDir) {
    Object response = experienceService.findAllExperiences(search, isCurrent, startDate, endDate,
        cursor, page, size, sortBy, sortDir);

    if (response instanceof org.springframework.data.domain.Page<?> pageResult) {
      PagingInfo pagingInfo = new PagingInfo((int) pageResult.getTotalElements(),
          pageResult.getSize(), pageResult.getNumber() + 1, pageResult.getTotalPages(),
          pageResult.hasPrevious(), pageResult.hasNext());

      PagedResponse<?> pagedResponse = new PagedResponse<>("Berhasil mengambil daftar experience",
          pageResult.getContent(), pagingInfo);

      return ResponseEntity.ok(pagedResponse);
    } else if (response instanceof java.util.List<?> listResult) {
      @SuppressWarnings("unchecked")
      List<ExperienceResponse> data = (List<ExperienceResponse>) listResult;

      String nextCursor = null;
      boolean hasNextPage = false;

      if (!data.isEmpty()) {
        nextCursor = data.get(data.size() - 1).id();
        hasNextPage = data.size() == size;
      }

      CursorInfo cursorInfo = new CursorInfo(nextCursor, hasNextPage, size);
      CursorResponse<List<ExperienceResponse>> cursorResponse = new CursorResponse<>(
          "Berhasil mengambil daftar experience dengan cursor", data, cursorInfo);

      return ResponseEntity.ok(cursorResponse);
    }

    return ResponseEntity.internalServerError().build();
  }

  @GetMapping("/{id}")
  public ResponseEntity<SuccessResponse<ExperienceResponse>> findExperienceById(
      @PathVariable("id") Long id) {
    ExperienceResponse experienceResponse = experienceService.findExperienceById(id);

    SuccessResponse<ExperienceResponse> successResponse =
        new SuccessResponse<>("Experience retrieved", experienceResponse);
    return ResponseEntity.ok(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{id}")
  public ResponseEntity<SuccessResponse<ExperienceResponse>> updateExperience(
      @PathVariable("id") Long id, @RequestBody ExperienceRequest request) {
    ExperienceResponse experienceResponse = experienceService.updateExperience(id, request);

    SuccessResponse<ExperienceResponse> successResponse =
        new SuccessResponse<>("Experience updated", experienceResponse);
    return ResponseEntity.ok(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<SuccessResponse<String>> deleteExperience(@PathVariable("id") Long id) {
    experienceService.deleteExperience(id);

    SuccessResponse<String> successResponse =
        new SuccessResponse<>("Experience deleted", "Experience with ID: %d deleted".formatted(id));
    return ResponseEntity.ok(successResponse);
  }
}
