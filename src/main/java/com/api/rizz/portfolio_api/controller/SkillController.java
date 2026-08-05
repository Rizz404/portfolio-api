package com.api.rizz.portfolio_api.controller;

import com.api.rizz.portfolio_api.CursorResponse;
import com.api.rizz.portfolio_api.dto.request.SkillRequest;
import com.api.rizz.portfolio_api.dto.response.CursorInfo;
import com.api.rizz.portfolio_api.dto.response.PagedResponse;
import com.api.rizz.portfolio_api.dto.response.PagingInfo;
import com.api.rizz.portfolio_api.dto.response.SkillResponse;
import com.api.rizz.portfolio_api.dto.response.SuccessResponse;
import com.api.rizz.portfolio_api.service.SkillService;
import jakarta.validation.Valid;
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
@RequestMapping("/skills")
@RequiredArgsConstructor
public class SkillController {
  final SkillService skillService;

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SuccessResponse<SkillResponse>> createSkillJson(
      @Valid @RequestBody SkillRequest request) {
    // Kita kirim null untuk parameter file
    SkillResponse skillResponse = skillService.createSkill(request, null);

    SuccessResponse<SkillResponse> successResponse =
        new SuccessResponse<>("Skill created", skillResponse);
    return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SuccessResponse<SkillResponse>> createSkillMultipart(
      @Valid @RequestPart("data") SkillRequest request,
      @RequestPart(value = "logoFile", required = false) MultipartFile logoFile) {

    SkillResponse skillResponse = skillService.createSkill(request, logoFile);

    SuccessResponse<SkillResponse> successResponse =
        new SuccessResponse<>("Skill created", skillResponse);
    return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);
  }

  @GetMapping("")
  public ResponseEntity<?> findAllSkills(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") List<String> sortBy,
      @RequestParam(defaultValue = "desc") List<String> sortDir) {
    Object response =
        skillService.findAllSkills(search, category, cursor, page, size, sortBy, sortDir);

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
              "Successfully retrieved skill list", pageResult.getContent(), pagingInfo);

      return ResponseEntity.ok(pagedResponse);
    } else if (response instanceof java.util.List<?> listResult) {
      @SuppressWarnings("unchecked")
      List<SkillResponse> data = (List<SkillResponse>) listResult;

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
      CursorResponse<List<SkillResponse>> cursorResponse =
          new CursorResponse<>("Successfully retrieved skill list with cursor", data, cursorInfo);

      return ResponseEntity.ok(cursorResponse);
    }

    return ResponseEntity.internalServerError().build();
  }

  @GetMapping("/{id}")
  public ResponseEntity<SuccessResponse<SkillResponse>> findSkillById(@PathVariable("id") Long id) {
    SkillResponse skillResponse = skillService.findSkillById(id);

    SuccessResponse<SkillResponse> successResponse =
        new SuccessResponse<>("Skill retrieved", skillResponse);
    return ResponseEntity.ok(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SuccessResponse<SkillResponse>> updateSkillJson(
      @PathVariable("id") Long id, @Valid @RequestBody SkillRequest request) {

    SkillResponse skillResponse = skillService.updateSkill(id, request, null);

    SuccessResponse<SkillResponse> successResponse =
        new SuccessResponse<>("Skill updated", skillResponse);
    return ResponseEntity.ok(successResponse);
  }

  // Endpoint untuk update berbasis Multipart
  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SuccessResponse<SkillResponse>> updateSkillMultipart(
      @PathVariable("id") Long id,
      @Valid @RequestPart("data") SkillRequest request,
      @RequestPart(value = "logoFile", required = false) MultipartFile logoFile) {

    SkillResponse skillResponse = skillService.updateSkill(id, request, logoFile);

    SuccessResponse<SkillResponse> successResponse =
        new SuccessResponse<>("Skill updated", skillResponse);
    return ResponseEntity.ok(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<SuccessResponse<String>> deleteSkill(@PathVariable("id") Long id) {
    skillService.deleteSkill(id);

    SuccessResponse<String> successResponse =
        new SuccessResponse<>("Skill deleted", "Skill with ID: %d deleted".formatted(id));
    return ResponseEntity.ok(successResponse);
  }
}
