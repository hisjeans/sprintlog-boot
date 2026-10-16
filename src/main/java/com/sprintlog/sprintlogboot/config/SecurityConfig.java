package com.sprintlog.sprintlogboot.config;

import com.sprintlog.sprintlogboot.filter.RequestIdFilter;
import com.sprintlog.sprintlogboot.filter.RequestLoggingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity // Spring Boot 자동 설정으로 생략 가능한 경우도 있지만 쓰는 것 관례, 웹 보안 설정 클래스임을 명확하게 나타내기 위해 명시
public class SecurityConfig {

  /*
  SecurityFilterChain - Spring Security의 요청 처리 규칙을 정의하는 빈
  이 빈이 있으면 Spring boot의 기본 자동 설정 대신 '우리 규칙'이 적용된다
  */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // csrf 나인것처럼 요청 보냈을 때, 쿠키 실어보낼 일 없다, 요청 헤더에 토큰 값 넣어 전달
        // 세션 쿠키 기반 -> csrf on, 토큰 기반 사용자 정보 유지 -> csrf off
        // 대부분 REST API 토큰 기반 인증 방식이므로 csrf off, 세션/폼 기반 인증 방식은 csrf on
        // REST API는 브라우저 세션 폼이 아니라 클라이언트가 직접 요청하므로 지금 단계에서는 CSRF 보호를 끈다 (세션/폼 기반으로 넘어갈 때 다시 다룬다)
        .csrf(csrf->csrf.disable()) // Customize 인터페이스 구현한 빈 전달 - 클래스 직접 만들지 않고 람다식으로 선언

        // 서버로 들어오는 요청 중 어떤 요청을 허용할 것인가에 대한 설정
        // 이 안에서 경로별 인증 및 권한 체크 진행 가능
        .authorizeHttpRequests(auth->auth
                // "**": /api/v1/admin/user(admin 뒤에 경로 하나), /api/v1/admin/temp/board(admin 하위 경로 두 칸) 하위 경로 몇 칸이든 가능
                // "*": 하위 경로 하나 허용
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/me/**").hasRole("USER")
                // ActivityController가 @RequestMapping({"api/v1/activities","/api/activities"}) 선언되어 있어 두 경로 모두 허용
                // 사전에 로그인된 것만 허용
                .anyRequest().permitAll() // permitAll 로 설정되어 있어도 들어오는 사람 누구인지에 대한 로직은 정상적으로 들어간다, 나중에 인가 로직 추가
        )
        // Customize 인터페이스 구현한 객체 전달 - 추상 메서드 1, 즉석 람다식으로 전달 가능
        .httpBasic(Customizer.withDefaults())
        .addFilterBefore(new RequestIdFilter(), UsernamePasswordAuthenticationFilter.class)
        // RequestLoggingFilter 객체를 생성해 필터로 추가할텐데 UsernamePasswordAuthenticationFilter 전에 끼워넣겠다
        // 로그인 필터 처리 전 앞 쪽에 끼워넣었다 생각
        // RequestLoggingFilter 전부 요청 기록하기 위해 만든 것, 인증 전 로깅 먼저
        .addFilterAfter(new RequestLoggingFilter(), RequestIdFilter.class);
    return http.build();
  }



  /*
  DB에 비밀번호 저장 시 암호화 필요, 비밀번호 암호화 및 암호화 패턴 매칭 기능
  BCryptPasswordEncoder - 비밀번호를 단방향 해시로 인코딩 / 검증하는 빈
  평문 저장 금지 원칙을 코드로 실현할 준비물
  */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /*
  static인 이유: 이 계층 빈은 보안 인프라가 초기화되는 설정 단계에 확실히 잡혀야 한다
  권한 계층 설정은 static으로 선언하면 다른 빈의 조기 초기화 부작용 없이 이를 보장받을 수 있다
  다른 빈이 먼저 초기화되어 이 계층이 설정되는 것을 방지
  static initializer 정적 초기화, role 권한 계층부터 먼저 세팅되고 나서 나머지 계층 선언
  나중에 컨트롤러에 @PreAuthorize(메서드 보안) 도입 시 static을 붙이지 않으면 동작하지 않는 사례가 있다
  */
  @Bean
  static RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.withDefaultRolePrefix()
        .role("ADMIN").implies("USER")
        .build();
  }
}
