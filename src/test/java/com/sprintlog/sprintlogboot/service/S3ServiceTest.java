package com.sprintlog.sprintlogboot.service;

import static org.assertj.core.api.Assertions.*;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import com.sprintlog.sprintlogboot.config.S3Config;
import com.sprintlog.sprintlogboot.config.S3Properties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@DisplayName("S3Service 테스트 (S3Mock)")
class S3ServiceTest {

  static final String BUCKET = "sprintlog-uploads-hisjeans";

  static S3MockContainer s3Mock; // 진짜 S3처럼 행동하는 로컬 서버 컨테이너
  static S3Service s3Service; // 테스트 주체
  static S3Client s3Client; // AWS 대신 S3MockContainer와 소통할 것

  // @ BeforeAll은 모든 테스트 전에 한번만 실행되는 메서드
  // 인스턴스가 여러 개 만들어지는 구조에서 "한 번만"을 보장하려면 static 메서드로 선언하는 것이 좋다
  // static 메서드로 선언해야 한다 - @BeforeAll - 각각의 변수들, 그 안의 내용들도 static으로 선언하는 것이 필요, 고정된 기능
  @BeforeAll // 처음 테스트 돌릴 때 한 번 동작, 이후 테스트 여러 번 돌린다 해도 동작하지 않는다
  static void startMock() {
    // 로컬 가짜 S3 컨테이너 기동
    s3Mock = new S3MockContainer("latest");
    s3Mock.start();

    // 운영 코드와 *동일한* S3Config 로 클라이언트를 만든다(엔드포인트만 목으로).
    // 원래 S3Properties는 yml에 있는 값을 끌고와서 세팅하는데
    // 테스트 환경에서는 우리가 직접 세팅해 s3Client와 S3Presigner에게 전달
    S3Properties props = new S3Properties(); // S3Properties는 static 아니기 때문에 new로 선언 - 메서드 안에서 생성하고 사라져도 상관 없기 때문
    props.setBucket(BUCKET);
    props.setRegion("ap-northeast-2");
    props.setEndpoint(s3Mock.getHttpEndpoint()); // 목 주소(가짜 컨테이너의 주소 받는다)
    props.setPresignMinutes(10);

    S3Config config = new S3Config();
    s3Client = config.s3Client(props);
    S3Presigner presigner = config.s3Presigner(props);

    // 위에서 생성한 여러 객체들을 S3Service에게 직접 전달해 의존성 주입
    s3Service = new S3Service(s3Client, presigner, props);

    // 버킷 생성(실제 배포 전엔 콘솔/IaC 로 만들지만, 테스트에선 SDK 로 준비)
    s3Client.createBucket(b -> b.bucket(BUCKET));
  }

  @AfterAll // 전체 테스트 끝난 뒤 한 번만 동작
  static void stopMock() {
    if (s3Mock != null) s3Mock.stop();
  }

  @Test
  @DisplayName("업로드 -> presigned URL로 실제 GET -> 같은 바이트가 돌아온다")
  void 업로드_후_presigned_URL_로_내려받는다() throws IOException, InterruptedException {
    // given
    byte[] content = "SprintLog S3 통합 테스트 이미지 바이트".getBytes(StandardCharsets.UTF_8); // byte 배열만 준비되면 되기 때문
    MockMultipartFile file
        = new MockMultipartFile("file", "proof.png", "image/png", content); // 테스트에서 사용하는 가짜 형태 만든다
    // when & then
    String key = s3Service.saveFile(file); // 업로드 후 저장되면 파일명이 온다, 업로드 -> 저장 키(UUID.png)
    assertThat(key).endsWith(".png");

    // presigned GET URL 생성
    String url = s3Service.getFileUrl(key);
    assertThat(url).contains(key).contains("X-Amz-Signature");

    // 위에서 받은 URL로 실제 HTTP GET 요청 보내기
    HttpResponse<byte[]> response = HttpClient.newHttpClient().send( // 에러 발생하면 클래스는 어차피 실패이기 때문에 try-catch 대신 메서드에 throws 붙인다
        HttpRequest.newBuilder(URI.create(url)).build(),
        HttpResponse.BodyHandlers.ofByteArray()
    );

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo(content); // body에 들어있는 내부 데이터 전부 동일한지 테스트 통과했다면 그대로 전달받았다는 의미 될 것
  }

  @Test
  @DisplayName("허용되지 않는 확장자는 업로드가 거부된다")
  void 허용안된_확장자_거부() {
    // given
    MockMultipartFile evil // 잘못된 파일
        = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "x".getBytes()); // 테스트에서 사용하는 가짜 형태 만든다
    // 현재 확장자 exe 허용하지 않고 있다

    // when & then
    assertThatThrownBy(()->s3Service.saveFile(evil))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("허용되지 않은 파일");

  }

  @Test
  @DisplayName("삭제하면 객체가 사라진다.")
  void 삭제하면_사라진다() {
    // given
    byte[] content = "지울 파일".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile file = new MockMultipartFile(
        "file", "temp.txt", "text/plain", content
    );

    // when
    String key = s3Service.saveFile(file);
    s3Service.deleteFile(key);

    // then
    // 삭제 후 s3Client로 직접 버킷에 접근해 key값으로 Object를 꺼내려 하면 NoSuchKeyException이 발생할 것
    assertThatThrownBy(()->s3Client.getObject(b->b.bucket(BUCKET).key(key)))
        .isInstanceOf(NoSuchKeyException.class);


  }
}