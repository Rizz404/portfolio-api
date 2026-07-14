package com.api.rizz.portfolio_api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.api.rizz.portfolio_api.dto.request.UserRequest;
import com.api.rizz.portfolio_api.dto.response.UserResponse;
import com.api.rizz.portfolio_api.entity.User;
import com.api.rizz.portfolio_api.mapper.UserMapper;
import com.api.rizz.portfolio_api.repository.UserRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // * Otomatis buatin Dependency Injection buat variabel "final"
/**
 * UserService
 */
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final SnowflakeGenerator snowflakeGenerator;

  @Transactional
  public UserResponse createUser(UserRequest userRequest) {
    long newId = snowflakeGenerator.nextId();
    User user = userMapper.toEntity(userRequest);

    user.setId(newId);

    User savedUser = userRepository.save(user);

    return userMapper.toResponse(savedUser);
  }

  public Object findAllUsers(String search, String role, String provider, String gender, Long cursor, int page,
      int size, List<String> sortBy,
      List<String> sortDir) {
    Specification<User> spec = (root, query, cb) -> {
      // * 1. Siapkan Filter (Where Clauser Dinamis)
      List<Predicate> predicates = new ArrayList<>();

      if (search != null && !search.isBlank()) {
        String searchKeyword = "%" + search.toLowerCase() + "%";

        // * cb.or() = Pilih salah satu yang cocok (OR)
        Predicate searchEmail = cb.like(cb.lower(root.get("email")), searchKeyword);
        Predicate searchNickname = cb.like(cb.lower(root.get("nickname")), searchKeyword);
        Predicate searchFullname = cb.like(cb.lower(root.get("fullName")), searchKeyword);

        predicates.add(cb.or(searchEmail, searchNickname, searchFullname));
      }

      // * Kalau mau filter berdasarkan role
      if (role != null && !role.isBlank()) {
        predicates.add(cb.equal(root.get("role"), role));
      }

      // * Kalau mau filter berdasarkan provider
      if (provider != null && !provider.isBlank()) {
        predicates.add(cb.equal(root.get("provider"), provider));
      }

      // * Kalau mau filter berdasarkan gender
      if (gender != null && !gender.isBlank()) {
        predicates.add(cb.equal(root.get("gender"), gender));
      }

      // * Kalau pakai Cursor Pagination (Cari ID yang lebih kecil dari cursor)
      if (cursor != null) {
        predicates.add(cb.lessThan(root.get("id"), cursor));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };

    // * 2. Siapkan Sorting (Ascending / Descending)
    Sort finalSort = Sort.unsorted();

    for (int i = 0; i < sortBy.size(); i++) {
      String field = sortBy.get(i);

      // Jaga-jaga kalau userr ngirim sortBy 2 biji, tapi sortDir cuma 1. Kita default
      // ke 'asc'
      String direction = (i < sortDir.size()) ? sortDir.get(i) : "asc";

      // Bikin gerbong saat ini
      Sort currentSort = direction.equalsIgnoreCase("desc")
          ? Sort.by(field).descending()
          : Sort.by(field).ascending();

      // Sambungin ke kereta utama pakai .and() !
      finalSort = finalSort.and(currentSort);
    }

    // * 3. Eksekusi Pencarian!
    if (cursor != null) {
      // * LOGIKA CURSOR: Biasanya gak butuh info total halaman, cukup ambil 'size'
      // * datanya aja
      Pageable limitOnly = PageRequest.of(0, size, finalSort);
      Page<User> result = userRepository.findAll(spec, limitOnly);
      return result.getContent().stream().map(userMapper::toResponse).toList();
    } else {
      // * LOGIKA OFFSET (Default): Butuh info total halaman dan total data
      Pageable pageable = PageRequest.of(page, size, finalSort);
      Page<User> result = userRepository.findAll(spec, pageable);
      return result.map(userMapper::toResponse);
    }
  }

  public UserResponse findUserById(Long id) {
    User user = userRepository
        .findById(id)
        .orElseThrow(
            () -> new NoSuchElementException("User with ID: %d not found".formatted(id)));

    return userMapper.toResponse(user);
  }

  @Transactional
  public UserResponse updateUser(Long id, UserRequest userRequest) {
    User user = userRepository
        .findById(id)
        .orElseThrow(
            () -> new NoSuchElementException("User with ID: %d not found".formatted(id)));

    // * Update data entity lama pakai data request baru
    userMapper.updateEntityFromRequest(userRequest, user);

    User updatedUser = userRepository.save(user);
    return userMapper.toResponse(updatedUser);
  }

  @Transactional
  public void deleteUser(Long id) {
    if (!userRepository.existsById(id)) {
      throw new NoSuchElementException("User with ID: %d not found".formatted(id));
    }

    userRepository.deleteById(id);
  }

}
