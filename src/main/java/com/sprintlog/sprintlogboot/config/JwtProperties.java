package com.sprintlog.sprintlogboot.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("sprintlog.jwt")
public class JwtProperties {

  private String secret;

  private Duration accessTokenValidity=Duration.ofMinutes(30); // yml 에 작성되지 않았을 때는 30 기본값, yml에 작성되어 있다면 그 값을 따른다
  // 초는 's' 사용

  private String issuer="sprintlog"; // yml에 작성된 값 우선, yml에 작성되어 있지 않다면 "sprintlog" 사용

}
