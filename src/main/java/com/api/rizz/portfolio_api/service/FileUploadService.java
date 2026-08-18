package com.api.rizz.portfolio_api.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
/** FileUploadService */
public class FileUploadService {

  private final Cloudinary cloudinary;

  // * Batasan upload: bisa diatur lewat application.properties, ada default kalau gak di-set.
  @Value("${app.upload.max-file-size-mb:10}")
  private long maxFileSizeMb;

  @Value("${app.upload.max-files-per-request:10}")
  private int maxFilesPerRequest;

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) return;

    long maxBytes = maxFileSizeMb * 1024L * 1024L;
    if (file.getSize() > maxBytes) {
      throw new IllegalArgumentException(
          "File '"
              + file.getOriginalFilename()
              + "' exceeds the maximum allowed size of "
              + maxFileSizeMb
              + "MB");
    }
  }

  public String uploadFile(MultipartFile file, String folderName) throws IOException {
    validateFile(file);

    Map<String, Object> options =
        ObjectUtils.asMap(
            "folder", folderName,
            "use_filename", true,
            "unique_filename", true);

    Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
    return uploadResult.get("secure_url").toString();
  }

  public List<String> uploadFiles(List<MultipartFile> files, String folderName) throws IOException {
    if (files == null || files.isEmpty()) return new ArrayList<>();

    long nonEmptyCount = files.stream().filter(f -> f != null && !f.isEmpty()).count();
    if (nonEmptyCount > maxFilesPerRequest) {
      throw new IllegalArgumentException(
          "Cannot upload more than "
              + maxFilesPerRequest
              + " files in a single request (got "
              + nonEmptyCount
              + ")");
    }

    // * Validasi semua file duluan sebelum ada satu pun yang di-upload ke Cloudinary, biar kalau
    // * ada file di tengah/akhir list yang melanggar batas ukuran, gak ada upload "nyangkut" yang
    // * sudah kepalang terkirim tapi entity-nya gagal disimpan.
    for (MultipartFile file : files) {
      validateFile(file);
    }

    List<String> fileUrls = new ArrayList<>();
    for (MultipartFile file : files) {
      if (file != null && !file.isEmpty()) {
        String fileUrl = uploadFile(file, folderName);
        fileUrls.add(fileUrl);
      }
    }
    return fileUrls;
  }

  public Map<?, ?> deleteFile(String publicId) throws IOException {
    return cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
  }

  public String extractCloudinaryPublicId(String fileUrl) {
    if (fileUrl == null || !fileUrl.contains("/upload/")) return null;
    try {
      String afterUpload = fileUrl.split("/upload/")[1];
      String withoutVersion = afterUpload.substring(afterUpload.indexOf("/") + 1);
      return withoutVersion.substring(0, withoutVersion.lastIndexOf("."));
    } catch (Exception e) {
      return null; // Fallback jika regex parsing gagal
    }
  }

  // * Fitur baru: Menghapus banyak file sekaligus berdasarkan List URL-nya
  public void deleteFilesByUrls(List<String> fileUrls) {
    if (fileUrls == null || fileUrls.isEmpty()) return;

    for (String url : fileUrls) {
      String publicId = extractCloudinaryPublicId(url);
      if (publicId != null) {
        try {
          deleteFile(publicId);
        } catch (Exception e) {
          // Log error, tapi biarkan loop berlanjut ke file berikutnya
          System.err.println("Failed to remove cloudinary file: " + publicId);
        }
      }
    }
  }
}
