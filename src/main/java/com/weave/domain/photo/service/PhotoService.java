package com.weave.domain.photo.service;

import com.weave.domain.photo.dto.PhotoUploadResponseDto;
import com.weave.domain.photo.entity.Photo;
import com.weave.domain.photo.repository.PhotoRepository;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoService {

  private final S3Client s3Client;
  private final PhotoRepository photoRepository;

  @Value("${aws.s3.bucket}")
  private String bucket;

  @Value("${aws.s3.public-base-url:}")
  private String publicBaseUrl; // optional override, else use virtual-hosted-style

  @Value("${aws.s3.upload-prefix:uploads/}")
  private String uploadPrefix;

  @Value("${aws.region:}")
  private String region;

  @Transactional
  public PhotoUploadResponseDto upload(MultipartFile file) {
    long t0 = System.nanoTime();
    validateImage(file);

    // Stream to temp file while hashing to avoid loading into heap
    java.nio.file.Path temp = null;
    String hash;
    try {
      temp = java.nio.file.Files.createTempFile("photo-upload-", ".bin");
      long hashStart = System.nanoTime();
      hash = sha256AndCopyToTemp(file, temp);
      long hashEnd = System.nanoTime();
      log.debug("[PHOTO] hash+spill_to_disk ms={} size={} name={}",
          (hashEnd - hashStart) / 1_000_000.0, file.getSize(), file.getOriginalFilename());
    } catch (IOException e) {
      throw new RuntimeException("임시 파일 생성/쓰기 중 오류가 발생했습니다.", e);
    }

    // Deduplicate by hash
    Photo existing = photoRepository.findByHash(hash).orElse(null);
    log.debug("[PHOTO] db_lookup hash={} existing={}", hash, existing);
    if (existing != null) {
      // cleanup temp file
      try {
        if (temp != null) {
          java.nio.file.Files.deleteIfExists(temp);
        }
      } catch (IOException ignore) {
      }
      return PhotoUploadResponseDto.builder().url(existing.getUrl()).build();
    }

    String ext = extensionFromContentType(file.getContentType());
    String key = buildRandomKey(uploadPrefix, ext);

    try {
      PutObjectRequest putReq = PutObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .contentType(file.getContentType())
          .build();

      log.debug("[PHOTO] s3_put key={} contentType={}", key, file.getContentType());

      long s3Start = System.nanoTime();
      s3Client.putObject(putReq, RequestBody.fromFile(temp));
      long s3End = System.nanoTime();
      if (log.isDebugEnabled()) {
        log.debug("[PHOTO] s3_put ms={}", (s3End - s3Start) / 1_000_000.0);
      }
    } finally {
      // Always try to delete temp file
      try {
        if (temp != null) {
          java.nio.file.Files.deleteIfExists(temp);
        }
      } catch (IOException ignore) {
        log.warn("Failed to delete temporary file: {}", temp, ignore);
      }
    }

    String url = buildPublicUrl(bucket, key);

    Photo saved = Photo.builder()
        .url(url)
        .hash(hash)
        .build();
    saved = photoRepository.save(saved);

    long t1 = System.nanoTime();
    log.debug("[PHOTO] total ms={} size={}", (t1 - t0) / 1_000_000.0, file.getSize());

    log.info("Uploaded photo: {}", saved);

    return PhotoUploadResponseDto.builder().url(saved.getUrl()).build();
  }

  private void validateImage(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("이미지 파일이 비어있습니다.");
    }
    String ct = file.getContentType();
    if (StringUtils.isBlank(ct) || !ct.startsWith("image/")) {
      throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
    }
  }

  private String extensionFromContentType(String contentType) {
    if (contentType == null) {
      return null;
    }
    return switch (contentType) {
      case MediaType.IMAGE_PNG_VALUE -> "png";
      case MediaType.IMAGE_JPEG_VALUE -> "jpg";
      case MediaType.IMAGE_GIF_VALUE -> "gif";
      case "image/webp" -> "webp";
      case "image/avif" -> "avif";
      default -> null;
    };
  }

  private String buildPublicUrl(String bucket, String key) {
    if (StringUtils.isNotBlank(publicBaseUrl)) {
      return String.format("%s/%s", publicBaseUrl.replaceAll("/$", ""), key);
    }
    if (StringUtils.isNotBlank(region)) {
      // Region-specific virtual-hosted-style URL (NestJS 스타일과 유사)
      return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }
    // Default virtual-hosted-style URL (region 미지정 시)
    return String.format("https://%s.s3.amazonaws.com/%s", bucket, key);
  }

  private String buildRandomKey(String prefix, String ext) {
    String cleanPrefix =
        StringUtils.isBlank(prefix) ? "" : (prefix.endsWith("/") ? prefix : prefix + "/");
    String randomHex = generateRandomHex(8); // 8 bytes -> 16 hex chars
    long ts = System.currentTimeMillis();
    String base = cleanPrefix + randomHex + "-" + ts;
    return ext != null ? base + "." + ext : base;
  }

  private String generateRandomHex(int numBytes) {
    byte[] bytes = new byte[numBytes];
    new SecureRandom().nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  private String sha256AndCopyToTemp(MultipartFile file, java.nio.file.Path temp)
      throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
    }

    // Stream read -> hash -> write to temp, to avoid loading entire file into heap
    try (java.io.InputStream in = file.getInputStream();
        java.io.OutputStream out = java.nio.file.Files.newOutputStream(
            temp,
            java.nio.file.StandardOpenOption.WRITE,
            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
      byte[] buffer = new byte[64 * 1024]; // 64KB buffer
      int n;
      while ((n = in.read(buffer)) != -1) {
        digest.update(buffer, 0, n);
        out.write(buffer, 0, n);
      }
      out.flush();
    }

    byte[] hash = digest.digest();
    return HexFormat.of().formatHex(hash);
  }
}
