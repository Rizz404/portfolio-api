package com.api.rizz.portfolio_api.controller;

import com.api.rizz.portfolio_api.dto.request.ProjectRequest;
import com.api.rizz.portfolio_api.dto.response.ProjectResponse;
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
  public ResponseEntity<ProjectResponse> createProjectJson(@RequestBody ProjectRequest request) {
    // Kita kirim null untuk parameter file
    ProjectResponse projectResponse = projectService.createProject(request, null, null);
    return new ResponseEntity<>(projectResponse, HttpStatus.CREATED);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ProjectResponse> createProjectMultipart(
      @RequestPart("data") ProjectRequest request,
      @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
      @RequestPart(value = "imageFiles", required = false) List<MultipartFile> imageFiles) {

    ProjectResponse projectResponse = projectService.createProject(request, logoFile, imageFiles);
    return new ResponseEntity<>(projectResponse, HttpStatus.CREATED);
  }

  @GetMapping("")
  public ResponseEntity<Object> findAllProjects(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") List<String> sortBy,
      @RequestParam(defaultValue = "desc") List<String> sortDir) {
    Object response =
        projectService.findAllProjects(search, status, cursor, page, size, sortBy, sortDir);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<ProjectResponse> findProjectById(@PathVariable("id") Long id) {
    ProjectResponse projectResponse = projectService.findProjectById(id);

    return new ResponseEntity<>(projectResponse, HttpStatus.OK);
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ProjectResponse> updateProjectJson(
      @PathVariable("id") Long id, @RequestBody ProjectRequest request) {

    ProjectResponse projectResponse = projectService.updateProject(id, request, null, null);
    return new ResponseEntity<>(projectResponse, HttpStatus.OK);
  }

  // Endpoint untuk update berbasis Multipart
  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ProjectResponse> updateProjectMultipart(
      @PathVariable("id") Long id,
      @RequestPart("data") ProjectRequest request,
      @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
      @RequestPart(value = "newImageFiles", required = false) List<MultipartFile> newImageFiles) {

    ProjectResponse projectResponse =
        projectService.updateProject(id, request, logoFile, newImageFiles);
    return new ResponseEntity<>(projectResponse, HttpStatus.OK);
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteProject(@PathVariable("id") Long id) {
    projectService.deleteProject(id);

    return new ResponseEntity<>("Project with ID: %d deleted".formatted(id), HttpStatus.OK);
  }
}
