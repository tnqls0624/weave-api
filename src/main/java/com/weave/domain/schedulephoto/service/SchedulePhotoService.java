package com.weave.domain.schedulephoto.service;

import com.weave.domain.schedulephoto.dto.SchedulePhotoDto;
import com.weave.domain.schedulephoto.entity.SchedulePhoto;
import com.weave.domain.schedulephoto.repository.SchedulePhotoRepository;
import com.weave.domain.user.entity.User;
import com.weave.domain.user.repository.UserRepository;
import com.weave.global.exception.BusinessException;
import com.weave.global.exception.ErrorCode;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulePhotoService {

  private final S3Client s3Client;
  private final SchedulePhotoRepository schedulePhotoRepository;
  private final UserRepository userRepository;

  @Value("${aws.s3.bucket}")
  private String bucket;

  @Value("${aws.s3.public-base-url:}")
  private String publicBaseUrl;

  @Value("${aws.region:}")
  private String region;

  private static final String UPLOAD_PREFIX = "schedule-photos/";
  private static final SimpleDateFormat dateFormat;

  static {
    dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public List<SchedulePhotoDto> getPhotos(String scheduleId) {
    ObjectId scheduleOid = new ObjectId(scheduleId);
    List<SchedulePhoto> photos = schedulePhotoRepository.findByScheduleIdOrderByUploadedAtDesc(scheduleOid);
    return photos.stream().map(this::toDto).collect(Collectors.toList());
  }

  @Transactional
  public SchedulePhotoDto uploadPhoto(String scheduleId, MultipartFile file, String userId) {
    validateImage(file);

    ObjectId scheduleOid = new ObjectId(scheduleId);
    ObjectId userOid = new ObjectId(userId);

    java.nio.file.Path temp = null;
    try {
      temp = java.nio.file.Files.createTempFile("schedule-photo-", ".bin");
      copyToTemp(file, temp);

      String ext = extensionFromContentType(file.getContentType());
      String key = buildRandomKey(UPLOAD_PREFIX + scheduleId + "/", ext);

      PutObjectRequest putReq = PutObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .contentType(file.getContentType())
          .build();

      s3Client.putObject(putReq, RequestBody.fromFile(temp));

      String url = buildPublicUrl(bucket, key);

      SchedulePhoto photo = SchedulePhoto.builder()
          .scheduleId(scheduleOid)
          .url(url)
          .s3Key(key)
          .uploadedBy(userOid)
          .uploadedAt(new Date())
          .build();

      SchedulePhoto saved = schedulePhotoRepository.save(photo);
      return toDto(saved);

    } catch (IOException e) {
      throw new RuntimeException("파일 처리 중 오류가 발생했습니다.", e);
    } finally {
      if (temp != null) {
        try {
          java.nio.file.Files.deleteIfExists(temp);
        } catch (IOException ignore) {
        }
      }
    }
  }

  @Transactional
  public void deletePhoto(String scheduleId, String photoId, String userId) {
    ObjectId photoOid = new ObjectId(photoId);
    ObjectId userOid = new ObjectId(userId);

    SchedulePhoto photo = schedulePhotoRepository.findById(photoOid)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

    // 본인이 업로드한 사진만 삭제 가능
    if (!photo.getUploadedBy().equals(userOid)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    // S3에서 삭제
    if (StringUtils.isNotBlank(photo.getS3Key())) {
      try {
        DeleteObjectRequest deleteReq = DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(photo.getS3Key())
            .build();
        s3Client.deleteObject(deleteReq);
      } catch (Exception e) {
        log.warn("Failed to delete S3 object: {}", photo.getS3Key(), e);
      }
    }

    schedulePhotoRepository.delete(photo);
  }

  private SchedulePhotoDto toDto(SchedulePhoto photo) {
    String uploaderName = null;
    if (photo.getUploadedBy() != null) {
      User uploader = userRepository.findById(photo.getUploadedBy()).orElse(null);
      if (uploader != null) {
        uploaderName = uploader.getName();
      }
    }

    return SchedulePhotoDto.builder()
        .id(photo.getId().toHexString())
        .url(photo.getUrl())
        .thumbnailUrl(photo.getThumbnailUrl())
        .uploadedBy(photo.getUploadedBy() != null ? photo.getUploadedBy().toHexString() : null)
        .uploadedByName(uploaderName)
        .uploadedAt(photo.getUploadedAt() != null ? dateFormat.format(photo.getUploadedAt()) : null)
        .caption(photo.getCaption())
        .build();
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

  private void copyToTemp(MultipartFile file, java.nio.file.Path temp) throws IOException {
    try (java.io.InputStream in = file.getInputStream();
        java.io.OutputStream out = java.nio.file.Files.newOutputStream(temp)) {
      byte[] buffer = new byte[64 * 1024];
      int n;
      while ((n = in.read(buffer)) != -1) {
        out.write(buffer, 0, n);
      }
    }
  }

  private String extensionFromContentType(String contentType) {
    if (contentType == null) return null;
    return switch (contentType) {
      case MediaType.IMAGE_PNG_VALUE -> "png";
      case MediaType.IMAGE_JPEG_VALUE -> "jpg";
      case MediaType.IMAGE_GIF_VALUE -> "gif";
      case "image/webp" -> "webp";
      default -> null;
    };
  }

  private String buildPublicUrl(String bucket, String key) {
    if (StringUtils.isNotBlank(publicBaseUrl)) {
      return String.format("%s/%s", publicBaseUrl.replaceAll("/$", ""), key);
    }
    if (StringUtils.isNotBlank(region)) {
      return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }
    return String.format("https://%s.s3.amazonaws.com/%s", bucket, key);
  }

  private String buildRandomKey(String prefix, String ext) {
    String cleanPrefix = StringUtils.isBlank(prefix) ? "" : (prefix.endsWith("/") ? prefix : prefix + "/");
    String randomHex = generateRandomHex(8);
    long ts = System.currentTimeMillis();
    String base = cleanPrefix + randomHex + "-" + ts;
    return ext != null ? base + "." + ext : base;
  }

  private String generateRandomHex(int numBytes) {
    byte[] bytes = new byte[numBytes];
    new SecureRandom().nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }
}
