package com.sprintlog.sprintlogboot.service;

import com.sprintlog.sprintlogboot.config.S3Properties;
import com.sprintlog.sprintlogboot.exception.FileStorageException;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@Slf4j
@ConditionalOnProperty(name = "sprintlog.storage", havingValue = "s3") // yaml에 이렇게 작성되어 있으면 빈으로 등록시키겠다는 의미
@RequiredArgsConstructor
public class S3Service implements FileStorage{

  // FileService와 동일한 허용 확장자 화이트리스트
  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
      ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg",   // 이미지
      ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",  // 문서
      ".txt", ".md", ".csv", ".json"                              // 텍스트
  );

  private final S3Client s3;
  private final S3Presigner presigner;
  private final S3Properties props;

  @Override
  public String saveFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("파일이 비어있습니다.");
    } // null 체크

    // 원본 파일명 정규화(null·traversal 안전) 후 확장자만 추출.
    String originalFilename = StringUtils.cleanPath(
        file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename());
    int dotIndex = originalFilename.lastIndexOf('.');
    String extension = (dotIndex >= 0) ? originalFilename.substring(dotIndex).toLowerCase() : "";

    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new IllegalArgumentException(
          "허용되지 않는 파일 형식입니다: '" + extension + "' (허용: " + ALLOWED_EXTENSIONS + ")");
    }

    String key = UUID.randomUUID().toString().replace("-", "") + extension;

    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(props.getBucket())
        .key(key) // 파일명, 객체 키 값 세팅
        .contentType(file.getContentType())
        .build();// request dto로 생각하면 된다

    try {
      s3.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
      log.info("S3 업로드 완료: {} (원본: {}, 크기: {} bytes", key, originalFilename, file.getSize());
      return key; // 로컬 저장소처럼 파일명 리턴
    } catch (IOException e) {
      throw new FileStorageException("S3 업로드 실패: "+originalFilename, e);
    } // checked 예외, try-catch 강제하는 예외이기 때문에 throws 메서드에 붙이는 대신 try-catch

  } // s3 저장소에 저장, 키 값


  // 비공개 객체에 대한 presigned GET URL을 생성, 이 URL을 가진 사람은 정해진 시간 동안만
  // 그 객체 하나를 볼 수 있다 (버킷 자체는 비공개 유지)
  @Override
  public String getFileUrl(String storedName) {
    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(
            props.getPresignMinutes())) // 분으로 세팅, 5분 - 메서드에 따라 seconds, hours, minutes, days... 달라진다(최대 시간은 7일, 그 이상 늘릴 수 없다)
        .getObjectRequest(GetObjectRequest.builder()
            .bucket(props.getBucket())
            .key(storedName)
            .build())
        .build();
    return presigner.presignGetObject(presignRequest).url().toString(); // 요청을 보내면 응답 결과가 오는데 presignGetObject 리턴해주는 객체 타입이 PresignedGetObjectRequest

  }

  // 응답 헤더에 Content-Disposition에 attachment; 를 작성하면 브라우저로 응답을 하는 것이 아닌
  // 다운로드로 응답하게 된다
  // 클라이언트가 다운로드 요청을 보내면 S3에게 다운로드 가능한 URL을 받아서 응답하고, 클라이언트는 해당 URL로 redirect해서 다운로드를 S3에게 직접 요청
  @Override
  public String getDownloadUrl(String storedName) {// url을 받기 때문에 getFileUrl과 대부분 로직 동일, responseContentDisposition만 추가
    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(
            props.getPresignMinutes())) // 분으로 세팅, 5분 - 메서드에 따라 seconds, hours, minutes, days... 달라진다(최대 시간은 7일, 그 이상 늘릴 수 없다)
        .getObjectRequest(GetObjectRequest.builder()
            .bucket(props.getBucket())
            .key(storedName)
            .responseContentDisposition("attachment; filename=\"" + storedName + "\"") // "\" 문자열 (예: filename="test.png" - contentdisposition의 header에 이 문자열을 넣으려는 것, attachment - 다운로드 형식으로 응답)
            .build())
        .build();
    return presigner.presignGetObject(presignRequest).url().toString(); // 요청을 보내면 응답 결과가 오는데 presignGetObject 리턴해주는 객체 타입이 PresignedGetObjectRequest
    // 사용자 다운로드 -> WAS <-다운로드 가능한 임시 URL-> AWS S3
    // WAS - 임시 URL -> 클라이언트 -Redirect-> S3 - 직접 download 제공 -> 클라이언트
    // 우리 서버 WAS는 중개 역할한다, 다운로드 직접하는 게 아니라 임시 다운로드 URL 제공하고 요청은 클라이언트가 직접 한다, 서버 부담 적다, 비용 및 시간 절약
    // 만일 직접 한다면? 클라이언트 -1번 게시물 사진-> WAS -사진-> AWS S3 -MultipartFile-> WAS -> client - 클라이언트 요청이 많아질수록 계속 파일 받아 응답해야 하고, 용량, 시간 부담, 서버 과부하 가능성까지

  }

  @Override
  public void deleteFile(String storedName) {// 객체 지우는 경우 - 예: 이미지 품고 있는 활동 객체 삭제
    if (storedName == null || storedName.isBlank()) {
      return;
    }
    s3.deleteObject(DeleteObjectRequest.builder()
        .bucket(props.getBucket())
        .key(storedName)
        .build());
    log.info("S3 객체 삭제: {}", storedName);

  }
}
