package com.api.rizz.portfolio_api.service;

import com.api.rizz.portfolio_api.dto.request.UserRequest;
import com.api.rizz.portfolio_api.dto.request.UserTranslationRequest;
import com.api.rizz.portfolio_api.dto.response.UserResponse;
import com.api.rizz.portfolio_api.entity.LanguageCode;
import com.api.rizz.portfolio_api.entity.User;
import com.api.rizz.portfolio_api.entity.UserTranslation;
import com.api.rizz.portfolio_api.mapper.UserMapper;
import com.api.rizz.portfolio_api.repository.UserRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor // * Otomatis buatin Dependency Injection buat variabel "final"
/** UserService */
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final SnowflakeGenerator snowflakeGenerator;
  private final FileUploadService fileUploadService;

  private static final Set<String> TRANSLATABLE_SORT_FIELDS = Set.of("bio");

  private List<UserTranslation> buildTranslations(
      List<UserTranslationRequest> requests, User user) {
    List<UserTranslation> translations = new ArrayList<>();
    for (UserTranslationRequest t : requests) {
      translations.add(
          UserTranslation.builder().user(user).locale(t.locale()).bio(t.bio()).build());
    }
    return translations;
  }

  // * Update translations locale yang sama in-place (bukan clear()+addAll()) - clear+addAll bikin
  // * Hibernate insert baris baru SEBELUM delete baris lama di flush yang sama, jadi tabrakan
  // * UNIQUE(user_id, locale) kalau locale-nya gak berubah (kasus paling umum saat update).
  private void reconcileTranslations(User user, List<UserTranslationRequest> requests) {
    List<UserTranslation> existing = user.getTranslations();
    java.util.Map<LanguageCode, UserTranslation> byLocale = new java.util.HashMap<>();
    for (UserTranslation t : existing) {
      byLocale.put(t.getLocale(), t);
    }

    java.util.Set<LanguageCode> requestedLocales = new java.util.HashSet<>();
    for (UserTranslationRequest r : requests) {
      requestedLocales.add(r.locale());
    }
    existing.removeIf(t -> !requestedLocales.contains(t.getLocale()));

    for (UserTranslationRequest r : requests) {
      UserTranslation match = byLocale.get(r.locale());
      if (match != null) {
        match.setBio(r.bio());
      } else {
        existing.add(UserTranslation.builder().user(user).locale(r.locale()).bio(r.bio()).build());
      }
    }
  }

  @Transactional
  public UserResponse createUser(UserRequest userRequest, MultipartFile profilePictFile) {
    try {
      long newId = snowflakeGenerator.nextId();
      User user = userMapper.toEntity(userRequest);

      user.setId(newId);
      user.setTranslations(buildTranslations(userRequest.translations(), user));

      boolean hasStringUrl =
          userRequest.profilePictUrl() != null && !userRequest.profilePictUrl().isBlank();
      boolean hasFile = profilePictFile != null && !profilePictFile.isEmpty();

      if (hasStringUrl && hasFile) {
        // * Gak bisa keduanya
        throw new IllegalArgumentException(
            "Cannot accept both 'profilePictUrl' string and 'profilePictFile'. Choose one.");
      }

      if (hasFile) {
        String profileUrl =
            fileUploadService.uploadFile(profilePictFile, "portfolio/users/profile");
        user.setProfilePict(profileUrl);
      } else if (hasStringUrl) {
        user.setProfilePict(userRequest.profilePictUrl());
      }

      // * Set timestamp manual karena pakai snowflakes jadi ada write behind pada hibernate
      OffsetDateTime now = OffsetDateTime.now();
      user.setCreatedAt(now);
      user.setUpdatedAt(now);

      User savedUser = userRepository.save(user);

      return userMapper.toResponse(savedUser);
    } catch (Exception e) {
      throw new RuntimeException("Error during create mutation: " + e.getMessage(), e);
    }
  }

  // * @Transactional wajib: mapper resolve translations (LAZY @OneToMany) di toResponse(),
  // * butuh session Hibernate masih terbuka; open-in-view=false jadi gak otomatis
  @Transactional(readOnly = true)
  public Object findAllUsers(
      String search,
      String role,
      String provider,
      String gender,
      Long cursor,
      int page,
      int size,
      List<String> sortBy,
      List<String> sortDir) {
    Specification<User> spec =
        (root, query, cb) -> {
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

      // * bio sekarang ada di tabel terpisah - drop diam-diam alih-alih error
      if (TRANSLATABLE_SORT_FIELDS.contains(field)) {
        continue;
      }

      // Jaga-jaga kalau userr ngirim sortBy 2 biji, tapi sortDir cuma 1. Kita default
      // ke 'asc'
      String direction = (i < sortDir.size()) ? sortDir.get(i) : "asc";

      // Bikin gerbong saat ini
      Sort currentSort =
          direction.equalsIgnoreCase("desc")
              ? Sort.by(field).descending()
              : Sort.by(field).ascending();

      // Sambungin ke kereta utama pakai .and() !
      finalSort = finalSort.and(currentSort);
    }

    // * 3. Eksekusi Pencarian!
    if (cursor != null) {
      // * LOGIKA CURSOR: Ambil 'size + 1' untuk mengecek apakah masih ada sisa data untuk next page
      Pageable limitOnly = PageRequest.of(0, size + 1, finalSort);
      Page<User> result = userRepository.findAll(spec, limitOnly);
      return result.getContent().stream().map(userMapper::toResponse).toList();
    } else {
      // * LOGIKA OFFSET (Default): Butuh info total halaman dan total data
      // * Kurangi 1 biar gak minus page nya
      int actualPage = page > 0 ? page - 1 : 0;
      Pageable pageable = PageRequest.of(actualPage, size, finalSort);
      Page<User> result = userRepository.findAll(spec, pageable);
      return result.map(userMapper::toResponse);
    }
  }

  @Transactional(readOnly = true)
  public UserResponse findUserById(Long id) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(
                () -> new NoSuchElementException("User with ID: %d not found".formatted(id)));

    return userMapper.toResponse(user);
  }

  @Transactional
  public UserResponse updateUser(Long id, UserRequest userRequest, MultipartFile profilePictFile) {
    return updateUser(id, userRequest, profilePictFile, false);
  }

  // * Dipakai endpoint self-service (/users/me) - role & provider dikunci ke nilai lama
  // * supaya user tidak bisa eskalasi role dirinya sendiri jadi ADMIN lewat body request.
  @Transactional
  public UserResponse updateCurrentUser(
      Long id, UserRequest userRequest, MultipartFile profilePictFile) {
    return updateUser(id, userRequest, profilePictFile, true);
  }

  private UserResponse updateUser(
      Long id, UserRequest userRequest, MultipartFile profilePictFile, boolean restrictSelfFields) {
    try {
      User user =
          userRepository
              .findById(id)
              .orElseThrow(
                  () -> new NoSuchElementException("User with ID: %d not found".formatted(id)));

      User.Role previousRole = user.getRole();
      User.AuthProvider previousProvider = user.getProvider();

      // * Update data entity lama pakai data request baru
      userMapper.updateEntityFromRequest(userRequest, user);

      if (restrictSelfFields) {
        // * Field privileged dikembalikan ke nilai lama, apapun yang dikirim di request
        user.setRole(previousRole);
        user.setProvider(previousProvider);
      }

      // * Mutasi in-place, JANGAN setTranslations(newList) - lihat catatan di ProjectService
      reconcileTranslations(user, userRequest.translations());

      boolean hasStringUrl =
          userRequest.profilePictUrl() != null && !userRequest.profilePictUrl().isBlank();
      boolean hasFile = profilePictFile != null && !profilePictFile.isEmpty();

      if (hasStringUrl && hasFile) {
        throw new IllegalArgumentException(
            "Cannot accept both 'profilePictUrl' string and 'profilePictFile'. Choose one.");
      }

      if (hasFile) {
        // Hapus file lama di Cloudinary jika ada
        if (user.getProfilePict() != null) {
          String oldPublicId = fileUploadService.extractCloudinaryPublicId(user.getProfilePict());
          if (oldPublicId != null) fileUploadService.deleteFile(oldPublicId);
        }
        String uploadedUrl =
            fileUploadService.uploadFile(profilePictFile, "portfolio/users/profilePict");
        user.setProfilePict(uploadedUrl);
      } else if (hasStringUrl) {
        user.setProfilePict(userRequest.profilePictUrl());
      }

      User updatedUser = userRepository.save(user);
      return userMapper.toResponse(updatedUser);
    } catch (Exception e) {
      throw new RuntimeException("Error during update mutation: " + e.getMessage(), e);
    }
  }

  @Transactional
  public void deleteUser(Long id) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(
                () -> new NoSuchElementException("User with ID: %d not found".formatted(id)));

    if (!userRepository.existsById(id)) {
      throw new NoSuchElementException("User with ID: %d not found".formatted(id));
    }

    if (user.getProfilePict() != null) {
      String profilePictPublicId =
          fileUploadService.extractCloudinaryPublicId(user.getProfilePict());
      if (profilePictPublicId != null) {
        try {
          fileUploadService.deleteFile(profilePictPublicId);
        } catch (Exception ignored) {
        }
      }
    }

    userRepository.deleteById(id);
  }
}
