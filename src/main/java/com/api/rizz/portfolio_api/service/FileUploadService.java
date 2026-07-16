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
}
