package com.sprintlog.sprintlogboot.config;

import java.util.List;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import com.sprintlog.sprintlogboot.filter.RequestIdFilter;
import com.sprintlog.sprintlogboot.filter.RequestLoggingFilter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import com.sprintlog.sprintlogboot.security.LoginFailureHandler;
import com.sprintlog.sprintlogboot.security.LoginSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity // Spring Boot 자동 설정으로 생략 가능한 경우도 있지만 쓰는 것 관례, 웹 보안 설정 클래스임을 명확하게 나타내기 위해 명시
@EnableMethodSecurity // 있어야 서비스의 @PreAuthorize가 동작한다
public class SecurityConfig {

  /*
  SecurityFilterChain - Spring Security의 요청 처리 규칙을 정의하는 빈
  이 빈이 있으면 Spring boot의 기본 자동 설정 대신 '우리 규칙'이 적용된다
  */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http,
      // AuthenticationEntryPoint, AccessDeniedHandler는 밑에 빈 등록 로직이 작성되어 있으므로,
      // securityFilterChain이 호출될 때 등록된 빈이 전달되도록 세팅
      AuthenticationEntryPoint restAuthenticationEntryPoint,
      AccessDeniedHandler restAccessDeniedHandler,
      PersistentTokenRepository persistentTokenRepository,
      UserDetailsService userDetailsService,
      AuthenticationSuccessHandler loginSuccessHandler,
      AuthenticationFailureHandler loginFailureHandler
      ) throws Exception {
    http
        // csrf 나인것처럼 요청 보냈을 때, 쿠키 실어보낼 일 없다, 요청 헤더에 토큰 값 넣어 전달
        // 세션 쿠키 기반 -> csrf on, 토큰 기반 사용자 정보 유지 -> csrf off
        // 대부분 REST API 토큰 기반 인증 방식이므로 csrf off, 세션/폼 기반 인증 방식은 csrf on
        // REST API는 브라우저 세션 폼이 아니라 클라이언트가 직접 요청하므로 지금 단계에서는 CSRF 보호를 끈다 (세션/폼 기반으로 넘어갈 때 다시 다룬다)
        .csrf(csrf->csrf.disable()) // Customize 인터페이스 구현한 빈 전달 - 클래스 직접 만들지 않고 람다식으로 선언

        // CORS (교차 출처 자원 공유) 다른 출처의 브라우저 요청을 허용
        // Customizer.withDefaults(): 등록된 빈 중 CorsConfigurationSource 타입의 빈이 있다면 기본 적용하겠다 
        .cors(Customizer.withDefaults())

        // XSS 방어를 돕는 보안 응답 헤더 - Content-Security-Policy
        // default-src 'self' = 기본적으로 같은 출처의 리소스만 로드 허용 -> 외부 악성 스크립트 주입 완화
        .headers(headers -> headers // 익명 객체로 전달
            .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
        )

        // 서버로 들어오는 요청 중 어떤 요청을 허용할 것인가에 대한 설정
        // 이 안에서 경로별 인증 및 권한 체크 진행 가능
        .authorizeHttpRequests(auth->auth
                // "**": /api/v1/admin/user(admin 뒤에 경로 하나), /api/v1/admin/temp/board(admin 하위 경로 두 칸) 하위 경로 몇 칸이든 가능
                // "*": 하위 경로 하나 허용
                .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll() // 원래는 전체를 잠그고 허용할 몇 가지 방식만 열어주는 것이 보안상 안전하지만, 현재는 예외적으로 허용
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/me/**").hasRole("USER")
                // ActivityController가 @RequestMapping({"api/v1/activities","/api/activities"}) 선언되어 있어 두 경로 모두 허용
                // 사전에 로그인된 것만 허용
                .requestMatchers(HttpMethod.POST, "/api/v1/activities/**", "/api/activities/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/activities/**", "/api/activities/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/activities/**", "/api/activities/**").authenticated()
                .requestMatchers("api/v1/auth/me").authenticated()
                //.authenticated() // 모든 요청은 인증된 사용자만 접근 가능, 로그인 안 한 상태라면 무조건 이 단계에서 막힌다
                .anyRequest().permitAll() // permitAll 로 설정되어 있어도 들어오는 사람 누구인지에 대한 로직은 정상적으로 들어간다, 나중에 인가 로직 추가
        )
        // 필터 단에서 발생한 커스텀 예외 처리 등록
        .sessionManagement(session->session
            //.sessionCreationPolicy(SessionCreationPolicy.STATELESS)// jwt는 세션 사용하지 않는다
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // 로그인 성공하면 세션 필요
            .invalidSessionUrl("/login.html?expired")
            // 세션 ID만 변경하고 세션 객체는 그대로 유지, 보통 많이 사용하는 것
            // 예전 세션으로 로그인하면 새로운 세션으로 로그인 진행된다
            .sessionFixation(fixation->fixation.changeSessionId())
//            .sessionFixation(fixation->fixation.migrateSession()) // 새 세션을 생성해 기존 세션의 모든 속성을 복사한 뒤 기존 세션을 무효화
//            .sessionFixation(fixation->fixation.newSession()) // 새 세션을 생성, 기존 데이터는 유지되지 않음
//            .sessionFixation(fixation->fixation.none()) // 사용하지 말 것, 아무것도 하지 않는다

            // 동시성 관련 설정은 이 블록 안에서 작성한다
                .sessionConcurrency(concurrency->concurrency
                    .maximumSessions(1) // 한 사용자 당 최대 세션 수, 1개로 제한했다=동시 로그인 절대 불가능하게 하겠다는 의미
                    .maxSessionsPreventsLogin(false) // false: 새 로그인 시 이전 세션 만료, true: 이미 로그인 되어 있다면 새 로그인 차단
                    .expiredUrl("/login.html?expired") // 동시성 세션 안에서 만료된 세션(이전 세션) 어디로 보낼지
                    // .invalidSession: 진짜 세션 만료되었을 때 설정 != expiredUrl: 동시 요청 만료된 세션, 이전 세션이 만료되었을 때 설정
                    // 경로가 같다고 해서 같은 의미가 아니다, expiredUrl은 동시성 세션 만료 시 이동할 경로, invalidSessionUrl은 진짜 세션 만료 시 이동할 경로
                )
        ) // 커스텀하는 인터페이스 기반 익명 객체 생성

        .exceptionHandling(ex-> ex
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler)
            ) // 익명 객체 이용해 등록, customizer 인터페이스 구현한 익명 객체 방식으로 전달
        // Customize 인터페이스 구현한 객체 전달 - 추상 메서드 1, 즉석 람다식으로 전달 가능
        // HTTP Basic 인증을 켠다 (Authorization 헤더에 Basic < email:password(Base64)> 형식으로 전달되면
        // DaoAuthenticationProvider를 통해 로그인 검증을 수행하고 SecurityContext에 인증을 채운다
        // 매 요청마다 자격증명을 실어 보내는 무상태(stateless) 방식
        .httpBasic(Customizer.withDefaults()) // disables 걸면 사용하지 않겠다

        // 폼 로그인 (세션 기반) 켠다
        // 한 번 로그인하면 서버가 세션을 만들고 JSESSIONID 쿠키를 발급한다
        // 그 이후의 요청은 그 쿠키만으로 인증 유지된다 - 상태 유지(stateful) 방식
        .formLogin(form->form
            .loginProcessingUrl("/login") // 로그인 처리되는 url, 폼이 POST 처리되는 URL(Spring이 가로챈다, 우리가 직접 만들지 않는다)
            .successHandler(loginSuccessHandler)
            .failureHandler(loginFailureHandler)
            .permitAll() // 로그인 요청을 누구나 접근 가능, 로그인은 인증 받지 않은 사람들이 들어오는 곳이기 때문
        ) // 람다식 익명 객체 설정
        .rememberMe(remember->remember
            .key("sprintlog-rememberme-secret-key") // 토큰 서명에 사용하는 비밀 키, 운영 레벨에서는 당연히 노출되면 안 된다, 값 바뀌면 모든 레벨 무효화, 운영 레벨에서는 환경 변수로 노출하는 것을 권장한다
            .rememberMeParameter("remember-me") // 로그인 폼의 checkbox name과 불일치
            // checkbox 같은 경우 사용자가 화면 보고 체크하는데 요청이 들어왔을 때 remember-me 파라미터 확인, 있으면 동작, 없으면 동작시키지 않는다
            .tokenValiditySeconds(60 * 60 * 24 * 14) // 곱하기 연산으로 작성하는 것이 가독성 좋다, 기간 임의로 설정한 것, 14일
            .tokenRepository(persistentTokenRepository) // 영구 토큰을 저장/조회할 곳
            .userDetailsService(userDetailsService) // rememberme 쿠키 자체 유효할 경우 쿠키를 찾는다, 쿠키가 유효할 때 email로 사용자를 다시 로드하는 다리 역할 객체 
        )

        .logout(logout->logout
            .logoutUrl("/logout") // Spring Security가 알아서 인식, POST /logout 으로 로그아웃
            .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)) // 204, 참고 로그아웃 핸들러는 FailureHandler 없다
            .invalidateHttpSession(true) // 세션 무효화(기본값이지만 명시 선호)
            .deleteCookies("JSESSIONID", "remember-me") // 세션 쿠키 삭제, 자동 로그인 쿠키도 삭제
        ) // 사용자의 정보를 서버가 영원히 유지할 수 없다, 사용자가 원할 때는 연결 끊어줄 수 있어야 한다
        
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

  // 미인증(401) 응답을 ProblemDetail JSON으로 커스텀할 수 있는 객체
  @Bean
  AuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
    return (request, response, authException)->
        writeProblem(objectMapper, response, HttpStatus.UNAUTHORIZED, "AUTH_401", "인증이 필요합니다. 로그인 후 다시 시도하세요.");
  }


  // 권한 부족(403) 응답을 ProblemDetail JSON으로 커스텀할 수 있는 객체
  @Bean
  AccessDeniedHandler restAccessDeniedHandler(ObjectMapper objectMapper) {
    return (request, response, deniedException)->
        writeProblem(objectMapper, response, HttpStatus.FORBIDDEN, "AUTH_403", "이 작업을 수행할 권한이 없습니다.");

  }


  /** 401/403 공통 — ProblemDetail 을 JㅁSON 으로 직접 응답 본문에 쓴다. */
  private static void writeProblem(ObjectMapper objectMapper, HttpServletResponse response,
      HttpStatus status, String code, String detail) throws IOException {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setProperty("code", code);
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), pd);
  }

  // 서블릿 컨테이너의 세션 생성/소멸을 Spring 이벤트로 발행
  // 동시 세션 제어에서도 세션 개수를 추적하려면 이 publisher가 필요하다
  // 이벤트 발행, 톰캣 서버가 세션 만들고 없앨 때 이벤트 연결해주는 역할, 있어야 우리 listener가 이벤트 받을 수 있다
  @Bean
  HttpSessionEventPublisher httpSessionEventPublisher(){
    return new HttpSessionEventPublisher();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(){
    CorsConfiguration config=new CorsConfiguration();
    // 허용할 출처(운영에서는 실제 프론트 도메인 주소가 들어간다)
    config.setAllowedOrigins(List.of("http://localhost:63342", "http://localhost:3000")); // 허용할 출처 설정
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); // CORS 정책 걸리지 않게 할 요청 방식
    // OPTIONS 브라우저 쪽 설정
    config.setAllowedHeaders(List.of("*")); // 전부 허용
    config.setAllowCredentials(true); // 쿠키 허용, 자격 증명

    // 위에서 만든 규칙을 모든 URL 경로에 적용
    UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);

    return source;
  }

  @Bean
  AuthenticationSuccessHandler loginSuccessHandler(ObjectMapper objectMapper) {
    return new LoginSuccessHandler(objectMapper);
  }

  @Bean
  AuthenticationFailureHandler loginFailureHandler(ObjectMapper objectMapper) {
    return new LoginFailureHandler(objectMapper);
  }

}
