package com.api.rizz.portfolio_api.controller;

import com.api.rizz.portfolio_api.CursorResponse;
import com.api.rizz.portfolio_api.dto.request.UserRequest;
import com.api.rizz.portfolio_api.dto.response.CursorInfo;
import com.api.rizz.portfolio_api.dto.response.PagedResponse;
import com.api.rizz.portfolio_api.dto.response.PagingInfo;
import com.api.rizz.portfolio_api.dto.response.SuccessResponse;
import com.api.rizz.portfolio_api.dto.response.UserResponse;
import com.api.rizz.portfolio_api.service.UserService;
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
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
  final UserService userService;

  // * Jangan dihiraukan dulu soal role soalnya ini kan web portfolio
  @PreAuthorize("hasRole('USER', 'ADMIN')")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SuccessResponse<UserResponse>> createUserJson(
      @RequestBody UserRequest request) {
    // Kita kirim null untuk parameter file
    UserResponse userResponse = userService.createUser(request, null);

    SuccessResponse<UserResponse> successResponse =
        new SuccessResponse<>("User created", userResponse);
    return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SuccessResponse<UserResponse>> createUserMultipart(
      @RequestPart("data") UserRequest request,
      @RequestPart(value = "profilePictFile", required = false) MultipartFile profilePictFile) {

    UserResponse userResponse = userService.createUser(request, profilePictFile);

    SuccessResponse<UserResponse> successResponse =
        new SuccessResponse<>("User created", userResponse);
    return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);
  }

  @GetMapping("")
  public ResponseEntity<?> findAllUsers(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) String provider,
      @RequestParam(required = false) String gender,
      @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") List<String> sortBy,
      @RequestParam(defaultValue = "desc") List<String> sortDir) {
    Object response =
        userService.findAllUsers(
            search, role, provider, gender, cursor, page, size, sortBy, sortDir);

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
              "Berhasil mengambil daftar user", pageResult.getContent(), pagingInfo);

      return ResponseEntity.ok(pagedResponse);
    } else if (response instanceof java.util.List<?> listResult) {
      @SuppressWarnings("unchecked")
      List<UserResponse> data = (List<UserResponse>) listResult;

      Long nextCursor = null;
      boolean hasNextPage = false;

      if (!data.isEmpty()) {
        nextCursor = Long.valueOf(data.get(data.size() - 1).id());
        hasNextPage = data.size() == size;
      }

      CursorInfo cursorInfo = new CursorInfo(nextCursor, hasNextPage, size);
      CursorResponse<List<UserResponse>> cursorResponse =
          new CursorResponse<>("Berhasil mengambil daftar user dengan cursor", data, cursorInfo);

      return ResponseEntity.ok(cursorResponse);
    }

    return ResponseEntity.internalServerError().build();
  }

  @GetMapping("/{id}")
  public ResponseEntity<SuccessResponse<UserResponse>> findUserById(@PathVariable("id") Long id) {
    UserResponse userResponse = userService.findUserById(id);

    SuccessResponse<UserResponse> successResponse =
        new SuccessResponse<>("User retrieved", userResponse);
    return ResponseEntity.ok(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SuccessResponse<UserResponse>> updateUserJson(
      @PathVariable("id") Long id, @RequestBody UserRequest request) {

    UserResponse userResponse = userService.updateUser(id, request, null);

    SuccessResponse<UserResponse> successResponse =
        new SuccessResponse<>("User updated", userResponse);
    return ResponseEntity.ok(successResponse);
  }

  // Endpoint untuk update berbasis Multipart
  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SuccessResponse<UserResponse>> updateUserMultipart(
      @PathVariable("id") Long id,
      @RequestPart("data") UserRequest request,
      @RequestPart(value = "profilePictFile", required = false) MultipartFile profilePictFile) {

    UserResponse userResponse = userService.updateUser(id, request, profilePictFile);

    SuccessResponse<UserResponse> successResponse =
        new SuccessResponse<>("User updated", userResponse);
    return ResponseEntity.ok(successResponse);
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<SuccessResponse<String>> deleteUser(@PathVariable("id") Long id) {
    userService.deleteUser(id);

    SuccessResponse<String> successResponse =
        new SuccessResponse<>("User deleted", "User with ID: %d deleted".formatted(id));
    return ResponseEntity.ok(successResponse);
  }
}
