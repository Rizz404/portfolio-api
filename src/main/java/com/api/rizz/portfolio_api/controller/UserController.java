package com.api.rizz.portfolio_api.controller;

import com.api.rizz.portfolio_api.dto.request.UserRequest;
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
  public ResponseEntity<UserResponse> createUserJson(@RequestBody UserRequest request) {
    // Kita kirim null untuk parameter file
    UserResponse userResponse = userService.createUser(request, null);
    return new ResponseEntity<>(userResponse, HttpStatus.CREATED);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UserResponse> createUserMultipart(@RequestPart("data") UserRequest request,
      @RequestPart(value = "profilePictFile", required = false) MultipartFile profilePictFile) {

    UserResponse userResponse = userService.createUser(request, profilePictFile);
    return new ResponseEntity<>(userResponse, HttpStatus.CREATED);
  }

  @GetMapping("")
  public ResponseEntity<Object> findAllUsers(@RequestParam(required = false) String search,
      @RequestParam(required = false) String role, @RequestParam(required = false) String provider,
      @RequestParam(required = false) String gender, @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") List<String> sortBy,
      @RequestParam(defaultValue = "desc") List<String> sortDir) {
    Object response = userService.findAllUsers(search, role, provider, gender, cursor, page, size,
        sortBy, sortDir);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> findUserById(@PathVariable("id") Long id) {
    UserResponse userResponse = userService.findUserById(id);

    return new ResponseEntity<>(userResponse, HttpStatus.OK);
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UserResponse> updateUserJson(@PathVariable("id") Long id,
      @RequestBody UserRequest request) {

    UserResponse userResponse = userService.updateUser(id, request, null);
    return new ResponseEntity<>(userResponse, HttpStatus.OK);
  }

  // Endpoint untuk update berbasis Multipart
  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UserResponse> updateUserMultipart(@PathVariable("id") Long id,
      @RequestPart("data") UserRequest request,
      @RequestPart(value = "profilePictFile", required = false) MultipartFile profilePictFile) {

    UserResponse userResponse = userService.updateUser(id, request, profilePictFile);
    return new ResponseEntity<>(userResponse, HttpStatus.OK);
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteUser(@PathVariable("id") Long id) {
    userService.deleteUser(id);

    return new ResponseEntity<>("User with ID: %d deleted".formatted(id), HttpStatus.OK);
  }
}
