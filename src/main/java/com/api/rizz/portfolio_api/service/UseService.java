package com.api.rizz.portfolio_api.service;

import com.api.rizz.portfolio_api.dto.request.UseRequest;
import com.api.rizz.portfolio_api.dto.response.UseResponse;
import com.api.rizz.portfolio_api.entity.Use;
import com.api.rizz.portfolio_api.mapper.UseMapper;
import com.api.rizz.portfolio_api.repository.UseRepository;
import com.api.rizz.portfolio_api.util.SnowflakeGenerator;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor // * Otomatis buatin Dependency Injection buat variabel "final"
/** UseService */
public class UseService {
  private final UseRepository useRepository;
  private final UseMapper useMapper;
  private final SnowflakeGenerator snowflakeGenerator;
  private final FileUploadService fileUploadService;

  @Transactional
  public UseResponse createUse(
      UseRequest useRequest, MultipartFile logoFile, List<MultipartFile> pictureFiles) {
    try {
      long newId = snowflakeGenerator.nextId();
      Use use = useMapper.toEntity(useRequest);

      use.setId(newId);

      boolean hasLogoString = useRequest.logoUrl() != null && !useRequest.logoUrl().isBlank();
      boolean hasLogoFile = logoFile != null && !logoFile.isEmpty();

      if (hasLogoString && hasLogoFile) {
        // * Gak bisa keduanya
        throw new IllegalArgumentException(
            "Cannot accept both 'logoUrl' string and 'logoFileFile'. Choose one.");
      }

      if (hasLogoFile) {
        String logoUrl = fileUploadService.uploadFile(logoFile, "portfolio/uses/logo");
        use.setLogoUrl(logoUrl);
      } else if (hasLogoString) {
        use.setLogoUrl(useRequest.logoUrl());
      }

      boolean hasImageStrings = useRequest.pictures() != null && !useRequest.pictures().isEmpty();
      boolean hasImageFiles = pictureFiles != null && !pictureFiles.isEmpty();

      if (hasImageFiles) {
        List<String> pictures =
            fileUploadService.uploadFiles(pictureFiles, "portfolio/uses/picture");
        use.setPictures(pictures);
      } else if (hasImageStrings) {
        use.setPictures(useRequest.pictures());
      }

      Use savedUse = useRepository.save(use);

      return useMapper.toResponse(savedUse);
    } catch (Exception e) {
      throw new RuntimeException("Error when communicate with cloudinary: " + e.getMessage(), e);
    }
  }

  public Object findAllUses(
      String search,
      String category,
      Long cursor,
      int page,
      int size,
      List<String> sortBy,
      List<String> sortDir) {
    Specification<Use> spec =
        (root, query, cb) -> {
          // * 1. Siapkan Filter (Where Clause Dinamis)
          List<Predicate> predicates = new ArrayList<>();

          // * Kalau ada keyword pencarian di title dan content
          if (search != null && !search.isBlank()) {
            predicates.add(
                cb.like(cb.lower(root.get("itemName")), "%" + search.toLowerCase() + "%"));
          }

          // * Kalau mau filter berdasarkan category
          if (category != null && !category.isBlank()) {
            predicates.add(cb.equal(root.get("category"), category));
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

      // Jaga-jaga kalau user ngirim sortBy 2 biji, tapi sortDir cuma 1. Kita default
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
      // * LOGIKA CURSOR: Biasanya gak butuh info total halaman, cukup ambil 'size'
      // * datanya aja
      Pageable limitOnly = PageRequest.of(0, size, finalSort);
      Page<Use> result = useRepository.findAll(spec, limitOnly);
      return result.getContent().stream().map(useMapper::toResponse).toList();
    } else {
      // * LOGIKA OFFSET (Default): Butuh info total halaman dan total data
      Pageable pageable = PageRequest.of(page, size, finalSort);
      Page<Use> result = useRepository.findAll(spec, pageable);
      return result.map(useMapper::toResponse);
    }
  }

  public UseResponse findUseById(Long id) {
    Use use =
        useRepository
            .findById(id)
            .orElseThrow(
                () -> new NoSuchElementException("Use with ID: %d not found".formatted(id)));

    return useMapper.toResponse(use);
  }

  @Transactional
  public UseResponse updateUse(
      Long id, UseRequest useRequest, MultipartFile logoFile, List<MultipartFile> pictures) {
    try {
      Use use =
          useRepository
              .findById(id)
              .orElseThrow(
                  () -> new NoSuchElementException("Use with ID: %d not found".formatted(id)));

      // * Update data entity lama pakai data request baru
      useMapper.updateEntityFromRequest(useRequest, use);

      boolean hasStringUrl = useRequest.logoUrl() != null && !useRequest.logoUrl().isBlank();
      boolean hasFile = logoFile != null && !logoFile.isEmpty();

      if (hasStringUrl && hasFile) {
        throw new IllegalArgumentException(
            "Cannot accept both 'logoUrl' string and 'logoFile'. Choose one.");
      }

      if (hasFile) {
        // Hapus file lama di Cloudinary jika ada
        if (use.getLogoUrl() != null) {
          String oldPublicId = fileUploadService.extractCloudinaryPublicId(use.getLogoUrl());
          if (oldPublicId != null) fileUploadService.deleteFile(oldPublicId);
        }
        String uploadedUrl = fileUploadService.uploadFile(logoFile, "portfolio/uses/logo");
        use.setLogoUrl(uploadedUrl);
      } else if (hasStringUrl) {
        use.setLogoUrl(useRequest.logoUrl());
      }

      if (useRequest.deletedPictures() != null && !useRequest.deletedPictures().isEmpty()) {
        fileUploadService.deleteFilesByUrls(useRequest.deletedPictures());
        if (use.getPictures() != null) {
          use.getPictures().removeAll(useRequest.deletedPictures());
        }
      }

      if (pictures != null && !pictures.isEmpty()) {
        List<String> newUrls = fileUploadService.uploadFiles(pictures, "portfolio/uses/picture");
        if (use.getPictures() == null) use.setPictures(new ArrayList<>());
        use.getPictures().addAll(newUrls);
      }

      Use updatedUse = useRepository.save(use);
      return useMapper.toResponse(updatedUse);
    } catch (Exception e) {
      throw new RuntimeException("Error during update mutation: " + e.getMessage(), e);
    }
  }

  @Transactional
  public void deleteUse(Long id) {
    Use use =
        useRepository
            .findById(id)
            .orElseThrow(
                () -> new NoSuchElementException("Blog with ID: %d not found".formatted(id)));

    if (!useRepository.existsById(id)) {
      throw new NoSuchElementException("Use with ID: %d not found".formatted(id));
    }

    if (use.getLogoUrl() != null) {
      String logoPublicId = fileUploadService.extractCloudinaryPublicId(use.getLogoUrl());
      if (logoPublicId != null) {
        try {
          fileUploadService.deleteFile(logoPublicId);
        } catch (Exception ignored) {
        }
      }
    }

    if (use.getPictures() != null && !use.getPictures().isEmpty()) {
      fileUploadService.deleteFilesByUrls(use.getPictures());
    }

    useRepository.deleteById(id);
  }
}
