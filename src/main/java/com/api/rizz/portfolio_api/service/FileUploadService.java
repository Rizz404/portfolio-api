package com.api.rizz.portfolio_api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
/**
 * FileUploadService
 */
public class FileUploadService {

  private final Cloudinary cloudinary;

  public String uploadFile(MultipartFile file, String folderName) throws IOException {
    Map<String, Object> options = ObjectUtils.asMap(
        "folder", folderName,
        "use_filename", true,
        "unique_filename", true);

    Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
    return uploadResult.get("secure_url").toString();
  }

  public List<String> uploadFiles(List<MultipartFile> files, String folderName) throws IOException {
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
    if (fileUrl == null || !fileUrl.contains("/upload/"))
      return null;
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
    if (fileUrls == null || fileUrls.isEmpty())
      return;

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
