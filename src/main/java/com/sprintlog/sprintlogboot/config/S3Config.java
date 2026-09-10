package com.sprintlog.sprintlogboot.config;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@Slf4j
@ConditionalOnProperty(name = "sprintlog.storage", havingValue = "s3")
@EnableConfigurationProperties(S3Properties.class)
public class S3Config { // bean 등록하기 위해, S3 client...는 우리가 만든 게 아니기 때문

  @Bean
  public S3Client s3Client(S3Properties props) {
    S3ClientBuilder builder = S3Client.builder()
        .region(Region.of(props.getRegion())); // region 은 동일하게 서울로 맞춘다

    if (StringUtils.hasText(props.getEndpoint())) { // yaml 파일에서 endpoint를 읽어 왔을 때 값이 존재한다면 -> 테스트
      // endpoint에 값이 있다면 테스트 환경
      builder.endpointOverride(URI.create(props.getEndpoint())) // test url로 endpoint를 설정 - 기본적으로는 s3 "https://s3.ap-northeast-2.amazonaws.com" 을 제공하지만 테스트 시에는 localhost:4566 같은 테스트 url 사용한다
          .forcePathStyle(true) //  테스트 환경에서는 가상 호스팅이 아닌 경로 방식 사용하겠다 강제
          // s3 주소 체계 두 가지 패턴:
          // 1. 가상 호스팅(aws 기본 세팅, 도메인 주소 패턴): 버킷명.s3.amazonaws.com/키
          // 2. 경로 방식: s3.amazonaws.com/버킷명/키
          // 가상 호스팅 방식이 아닌 경로 방식의 url 사용을 강제
          .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create
              ("test", "test"))); // 값을 비우면 에러가 발생하기 때문에 임의로 넣은 것
              // 더미 자격 증명 설정 실제로 AWS에 접속하는 것이 아니기 때문에 아무 값이나 넣어서 형식만 갖추겠다 (아예 없으면 에러 발생)
      log.info("S3Client - 로컬 Mock 사용: {}", props.getEndpoint());
    } else {
      // endpoint에 값이 없다면 진짜 AWS에 요청을 보내야 하는 상황
      builder.forcePathStyle(false) // 가상 호스팅 방식 사용
          // DefaultCredentialsProvider 는 자격 증명을 정해진 순서대로 찾아보는 체인 객체
          // 인증 정보 노출될 위험 적다
          // 1. 환경 변수로 전달된 값이 있는가? (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY) (x)
          // 2. 자바 시스템 프로퍼티 (x)
          // 3. aws-cli를 통해 설정된 프로파일이 존재 하는지 확인 (ㅇ)
          // 4. ECS 태스크 역할 / EC2 인스턴스 프로파일
          .credentialsProvider(DefaultCredentialsProvider.builder().build());
      // 직접 객체 생성하는 방식으로 우회, 싱글톤 패턴 - create가 싱글톤 패턴으로 돌려주는 데 만약 닫혀버리면 다른 객체에서 문제 발생할 수 있으니 우선 권장된 builder 패턴으로 돌려서 사용하자
      log.info("S3Client - 실제 AWS(region={})", props.getRegion());
    }
    return builder.build();
  }

  @Bean
  public S3Presigner s3Presigner(S3Properties props) {
    S3Presigner.Builder builder = S3Presigner.builder()
        .region(Region.of(props.getRegion()));// region 은 동일하게 서울로 맞춘다

    if (StringUtils.hasText(props.getEndpoint())) { // yaml 파일에서 endpoint를 읽어 왔을 때 값이 존재한다면 -> 테스트
      // endpoint에 값이 있다면 테스트 환경
      builder.endpointOverride(URI.create(props.getEndpoint())) // test url로 endpoint를 설정 - 기본적으로는 s3 "https://s3.ap-northeast-2.amazonaws.com" 을 제공하지만 테스트 시에는 localhost:4566 같은 테스트 url 사용한다
          .serviceConfiguration(S3Configuration.builder()
              .pathStyleAccessEnabled(true)
              .build())
          // s3 주소 체계 두 가지 패턴:
          // 1. 가상 호스팅(aws 기본 세팅, 도메인 주소 패턴): 버킷명.s3.amazonaws.com/키
          // 2. 경로 방식: s3.amazonaws.com/버킷명/키
          // 가상 호스팅 방식이 아닌 경로 방식의 url 사용을 강제
          .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create
              ("test", "test"))); // 값을 비우면 에러가 발생하기 때문에 임의로 넣은 것
      // 더미 자격 증명 설정 실제로 AWS에 접속하는 것이 아니기 때문에 아무 값이나 넣어서 형식만 갖추겠다 (아예 없으면 에러 발생)
    } else {
      // endpoint에 값이 없다면 진짜 AWS에 요청을 보내야 하는 상황
      builder.serviceConfiguration(S3Configuration.builder()
                      .pathStyleAccessEnabled(false)
                      .build())
                      .credentialsProvider(DefaultCredentialsProvider.builder().build());
    }
    return builder.build();
  }
}
