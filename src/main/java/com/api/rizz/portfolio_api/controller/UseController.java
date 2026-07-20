package com.api.rizz.portfolio_api.controller;

import com.api.rizz.portfolio_api.CursorResponse;
import com.api.rizz.portfolio_api.dto.request.UseRequest;
import com.api.rizz.portfolio_api.dto.response.CursorInfo;
import com.api.rizz.portfolio_api.dto.response.PagedResponse;
import com.api.rizz.portfolio_api.dto.response.PagingInfo;
import com.api.rizz.portfolio_api.dto.response.SuccessResponse;
import com.api.rizz.portfolio_api.dto.response.UseResponse;
import com.api.rizz.portfolio_api.service.UseService;
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
@RequestMapping("/uses")
@RequiredArgsConstructor
public class UseController {
  final UseService useService;

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SuccessResponse<UseResponse>> createUseJson(
      @RequestBody UseRequest request) {
    // Kita kirim null untuk parameter file
    UseResponse useResponse = useService.createUse(request, null, null);

    SuccessResponse<UseResponse> successResponse =
        new SuccessResponse<>("Use created", useResponse);
    return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SuccessResponse<UseResponse>> createUseMultipart(
      @RequestPart("data") UseRequest request,
      @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
      @RequestPart(value = "pictureFiles", required = false) List<MultipartFile> pictureFiles) {

    UseResponse useResponse = useService.createUse(request, logoFile, pictureFiles);

    SuccessResponse<UseResponse> successResponse =
        new SuccessResponse<>("Use created", useResponse);
    return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);
  }

  @GetMapping("")
  public ResponseEntity<?> findAllUses(@RequestParam(required = false) String search,
      @RequestParam(required = false) String category, @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") List<String> sortBy,
      @RequestParam(defaultValue = "desc") List<String> sortDir) {
    Object response = useService.findAllUses(search, category, cursor, page, size, sortBy, sortDir);

    if (response instanceof org.springframework.data.domain.Page<?> pageResult) {
      PagingInfo pagingInfo = new PagingInfo((int) pageResult.getTotalElements(),
          pageResult.getSize(), pageResult.getNumber() + 1, pageResult.getTotalPages(),
          pageResult.hasPrevious(), pageResult.hasNext());

      PagedResponse<?> pagedResponse =
          new PagedResponse<>("Berhasil mengambil daftar use", pageResult.getContent(), pagingInfo);

      return ResponseEntity.ok(pagedResponse);
    } else if (response instanceof java.util.List<?> listResult) {
      @SuppressWarnings("unchecked")
      List<UseResponse> data = (List<UseResponse>) listResult;

      Long nextCursor = null;
      boolean hasNextPage = false;

      if (!data.isEmpty()) {
        nextCursor = Long.valueOf(data.get(data.size() - 1).id());
        hasNextPage = data.size() == size;
      }

      CursorInfo cursorInfo = new CursorInfo(nextCursor, hasNextPage, size);
      CursorResponse<List<UseResponse>> cursorResponse =
          new CursorResponse<>("Berhasil mengambil daftar use dengan cursor", data, cursorInfo);

      return ResponseEntity.ok(cursorResponse);
    }

    return ResponseEntity.internalServerError().build();
  }

  @GetMapping("/{id}")
  public ResponseEntity<SuccessResponse<UseResponse>> findUseById(@PathVariable("id") Long id) {
    UseResponse useResponse = useService.findUseById(id);

    SuccessResponse<UseResponse> successResponse =
        new SuccessResponse<>("Use retrieved", useResponse);
    return ResponseEntity.ok(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SuccessResponse<UseResponse>> updateUseJson(@PathVariable("id") Long id,
      @RequestBody UseRequest request) {

    UseResponse useResponse = useService.updateUse(id, request, null, null);

    SuccessResponse<UseResponse> successResponse =
        new SuccessResponse<>("Use updated", useResponse);
    return ResponseEntity.ok(successResponse);
  }

  // Endpoint untuk update berbasis Multipart
  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SuccessResponse<UseResponse>> updateUseMultipart(
      @PathVariable("id") Long id, @RequestPart("data") UseRequest request,
      @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
      @RequestPart(value = "newPictureFiles",
          required = false) List<MultipartFile> newPictureFiles) {

    UseResponse useResponse = useService.updateUse(id, request, logoFile, newPictureFiles);

    SuccessResponse<UseResponse> successResponse =
        new SuccessResponse<>("Use updated", useResponse);
    return ResponseEntity.ok(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<SuccessResponse<String>> deleteUse(@PathVariable("id") Long id) {
    useService.deleteUse(id);

    SuccessResponse<String> successResponse =
        new SuccessResponse<>("Use deleted", "Use with ID: %d deleted".formatted(id));
    return ResponseEntity.ok(successResponse);
  }
}
