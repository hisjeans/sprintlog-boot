package com.sprintlog.sprintlogboot.security;

import com.sprintlog.sprintlogboot.config.JwtProperties;
import com.sprintlog.sprintlogboot.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/*
이 클래스가 JWT의 전부를 담당한다 (생성/검증/추출까지 전부 다 진행하게 될 것)
 */
@Component
@Slf4j
public class JwtProvider {

  public static final String CLAIM_ROLE="role"; // 역할 담는 것을 "role" 로 고정, 오타가 나거나 틀릴 수 있기 때문에 static final로 고정
  private final JwtProperties jwtProperties;
  private final Clock clock; // 날짜를 선언할 때 AppConfig Clock 주입 받을 것
  // Clock 통해 시간 고정할 수 있기 때문에 사용, Datetime 고정 불가능
  private final SecretKey secretKey; // 서명용으로 사용할 수 있는 객체 형태로 바꿔 놓았다 생각

  public JwtProvider(JwtProperties jwtProperties, Clock clock) {
    this.jwtProperties = jwtProperties;
    this.clock = clock;
    this.secretKey = Keys.hmacShaKeyFor(
        jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
  } // 문자열 비밀 키를 대칭 키 방식의 secretKey 로 바꿔주는
  // 비밀키 값 짧게 세팅하면 예외 발생해 동작할 수 없다, 32byte 이상으로 세팅해야 한다

  // Access Token 생성
  // 페이로드: sub(사용자 이름), role(역할), iss(발급자), iat(발급 시간), exp(만료 시간) 이 대표적인 claim, 민감 정보 절대 넣지 않는다
  public String createAccessToken(String username, Role role) { // username - 우리는 email로 작성했다
    // 토큰 생성하는 자체가 로그인 인증 과정이 끝난 것
    Instant now=clock.instant();
    Instant expiry = now.plus(jwtProperties.getAccessTokenValidity());

    return Jwts.builder()
        .subject(username)
        // 여러 개의 커스텀 claim을 넣을 때는 .claims(Map으로 포장)
        // .claims(Map.of(CLAIM_ROLE, role.name(), "address", "seoul", "phone", "010-1234-5678"))
        .claim(CLAIM_ROLE, role.name())
        .issuer(jwtProperties.getIssuer()).issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(secretKey, SIG.HS256) // 서명 지정
        // 알고리즘을 HS256으로 명시 고정 - 토큰 헤더의 alg를 믿지 않고 서버가 정한다
        .compact(); // 객체로 리턴하는 게 아니라 builder로 토큰 짜맞춘 다음 문자열로 압축해서 리턴
  }


  // 토큰을 검증하고 페이로드(Claims)를 돌려준다
  // 나중에 jwt filter를 하나 씌워서 이 요청이 들어가도록 할 것
  public Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey) // 서명 확인 - 토큰 안 서명이 비어 있으면 아예 거부된다+제대로 된 서명인지 확인, 서명 검증 실패하면 예외 발생
        .requireIssuer(jwtProperties.getIssuer()) // 발급자가 일치하는지, 토큰 발행 주체 검증
        // 만료(exp) 판정도 주입받은 시계 기준 - 발급과 검증이 같은 시계를 쓴다 (테스트에서 시간 제어 가능)
        .clock(()-> Date.from(clock.instant())) // 만일 만료되어 있으면 예외 발생
        .build()
        .parseSignedClaims(token) // 토큰 원본 값을 주면서 페이로드(Claims)를 추출
        .getPayload();
  }

  /**
   * 유효 여부만 빠르게 판단하는 편의 메서드 (예외 → false로 남겨주는, 필수 아니다).
   * 어떤 이유로 거부됐는지는 로그로 남긴다.
   * parseClaims 공통적으로 거친다
   * 왜냐하면 디코딩은 누구나 할 수 있다, 검증 없이 읽으면 위조 토큰 그대로 읽어버리는 문제 발생할 수 있기 때문에 parseClaims 호출해 서명 비교, 만료 여부, 발급자 일치 여부까지 확인하도록 한다
   */
  public boolean isValid(String token) {
    try {
      parseClaims(token);
      return true; // 서명이 아예 존재하지 않거나/위조되었거나/유효기간이 지났거나/발급자가 다르거나 등등의 이유로 예외 발생하면 catch로 빠진다
    } catch (ExpiredJwtException e) { // 토큰 만료되었을 때 발생하는 예외
      log.warn("[JWT] 만료된 토큰: {}", e.getMessage());
    } catch (JwtException | IllegalArgumentException e) { // 서명 위조되었거나 유효하지 않은 경우
      log.warn("[JWT] 유효하지 않은 토큰: {}", e.getMessage());
    } // 문제가 없으면 true, 하나라도 문제가 있으면 false return
    return false;
  }

  /** 검증을 통과한 토큰에서 사용자 이름(sub) 추출. */
  public String getUsername(String token) {
    return parseClaims(token).getSubject();
  }

  // 이미 검증된 Claims에서 역할 추출
  public Role getRole(Claims claims) {
    return Role.valueOf(claims.get(CLAIM_ROLE, String.class));
  }
}





