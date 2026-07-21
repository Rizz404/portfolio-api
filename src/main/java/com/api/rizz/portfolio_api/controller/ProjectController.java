package com.api.rizz.portfolio_api.controller;

import com.api.rizz.portfolio_api.CursorResponse;
import com.api.rizz.portfolio_api.dto.request.ProjectRequest;
import com.api.rizz.portfolio_api.dto.response.CursorInfo;
import com.api.rizz.portfolio_api.dto.response.PagedResponse;
import com.api.rizz.portfolio_api.dto.response.PagingInfo;
import com.api.rizz.portfolio_api.dto.response.ProjectResponse;
import com.api.rizz.portfolio_api.dto.response.SuccessResponse;
import com.api.rizz.portfolio_api.service.ProjectService;
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
@RequestMapping("/projects")
@RequiredArgsConstructor
/** ProjectController */
public class ProjectController {
  final ProjectService projectService;

  // * String kosong = /projects bisa juga @PostMapping doang kalo
  // * @PostMapping("/") = /projects/
  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SuccessResponse<ProjectResponse>> createProjectJson(
      @RequestBody ProjectRequest request) {
    // Kita kirim null untuk parameter file
    ProjectResponse projectResponse = projectService.createProject(request, null, null);

    SuccessResponse<ProjectResponse> successResponse =
        new SuccessResponse<>("Project created", projectResponse);
    return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SuccessResponse<ProjectResponse>> createProjectMultipart(
      @RequestPart("data") ProjectRequest request,
      @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
      @RequestPart(value = "imageFiles", required = false) List<MultipartFile> imageFiles) {

    ProjectResponse projectResponse = projectService.createProject(request, logoFile, imageFiles);

    SuccessResponse<ProjectResponse> successResponse =
        new SuccessResponse<>("Project created", projectResponse);
    return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);
  }

  @GetMapping("")
  public ResponseEntity<?> findAllProjects(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") List<String> sortBy,
      @RequestParam(defaultValue = "desc") List<String> sortDir) {
    Object response =
        projectService.findAllProjects(search, status, cursor, page, size, sortBy, sortDir);

    if (response instanceof org.springframework.data.domain.Page<?> pageResult) {
      // * Spring Data Page dimulai dari 0, kita +1 agar lebih lazim untuk Frontend
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
              "Berhasil mengambil daftar project", pageResult.getContent(), pagingInfo);

      return ResponseEntity.ok(pagedResponse);
    } else if (response instanceof java.util.List<?> listResult) {

      @SuppressWarnings("unchecked")
      List<ProjectResponse> data = (List<ProjectResponse>) listResult;

      Long nextCursor = null;
      boolean hasNextPage = false;

      if (!data.isEmpty()) {
        // Ambil ID dari elemen paling terakhir sebagai nextCursor (di-parse ke Long)
        nextCursor = Long.valueOf(data.get(data.size() - 1).id());
        // kemungkinan masih ada sisa data di database
        hasNextPage = data.size() == size;
      }

      CursorInfo cursorInfo = new CursorInfo(nextCursor, hasNextPage, size);
      CursorResponse<List<ProjectResponse>> cursorResponse =
          new CursorResponse<>("Berhasil mengambil daftar project dengan cursor", data, cursorInfo);

      return ResponseEntity.ok(cursorResponse);
    }

    return ResponseEntity.internalServerError().build();
  }

  @GetMapping("/{id}")
  public ResponseEntity<SuccessResponse<ProjectResponse>> findProjectById(
      @PathVariable("id") Long id) {
    ProjectResponse projectResponse = projectService.findProjectById(id);

    SuccessResponse<ProjectResponse> successResponse =
        new SuccessResponse<>("Project retrieved", projectResponse);
    return ResponseEntity.ok(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SuccessResponse<ProjectResponse>> updateProjectJson(
      @PathVariable("id") Long id, @RequestBody ProjectRequest request) {

    ProjectResponse projectResponse = projectService.updateProject(id, request, null, null);

    SuccessResponse<ProjectResponse> successResponse =
        new SuccessResponse<>("Project updated", projectResponse);
    return ResponseEntity.ok(successResponse);
  }

  // Endpoint untuk update berbasis Multipart
  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SuccessResponse<ProjectResponse>> updateProjectMultipart(
      @PathVariable("id") Long id,
      @RequestPart("data") ProjectRequest request,
      @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
      @RequestPart(value = "newImageFiles", required = false) List<MultipartFile> newImageFiles) {

    ProjectResponse projectResponse =
        projectService.updateProject(id, request, logoFile, newImageFiles);

    SuccessResponse<ProjectResponse> successResponse =
        new SuccessResponse<>("Project updated", projectResponse);
    return ResponseEntity.ok(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<SuccessResponse<String>> deleteProject(@PathVariable("id") Long id) {
    projectService.deleteProject(id);

    SuccessResponse<String> successResponse =
        new SuccessResponse<>("Project deleted", "Project with ID: %d deleted".formatted(id));
    return ResponseEntity.ok(successResponse);
  }
}
