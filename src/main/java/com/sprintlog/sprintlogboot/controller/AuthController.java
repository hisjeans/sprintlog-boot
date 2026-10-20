package com.sprintlog.sprintlogboot.controller;

import com.sprintlog.sprintlogboot.dto.response.UserResponse;
import com.sprintlog.sprintlogboot.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import java.util.Map;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  @GetMapping("/whoami")
  public Map<String, Object> whoami(){
    // 1. SecurityContextHolder: 현재 스레드(=지금 이 요청)의 인증 정보 저장소에서 컨텍스트를 꺼낸다
    SecurityContext context= SecurityContextHolder.getContext();

    // 2. SecurityContext: 그 안에 담긴 Authentication을 꺼낸다
    Authentication authentication=context.getAuthentication();

    // 3. Authentication: 누가 무슨 권한, 인증됐는지를 표현한다
    // 4. getAuthorities() 안의 요소 하나하나가 GrantedAuthority - 권한 문자열로 변환
    List<String> authorities=authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();

    return Map.of(
        "name", authentication.getName(),
        "authorities", authorities,
        "authenticated", authentication.isAuthenticated(),
        "authenticationType", authentication.getClass().getSimpleName()
    );
  }


  @GetMapping("/me")
  public UserResponse me(@AuthenticationPrincipal CustomUserDetails principal) {
    return UserResponse.from(principal.getUser());
  } // 당연히 인증되어야 접근 가능, 인증되지 않았을 때 CustomUserDetails null 이기 때문에 반드시 인증 과정 거치도록 한다


  @GetMapping("/csrf-token")
  public ResponseEntity<Object> csrfToken(CsrfToken csrfToken){
    csrfToken.getToken(); // 스프링 미리 토큰 생성하지 않기 때문에 토큰 강제로 생성
    // 내용은 딱히 없고, csrf 토큰을 한번 더 가져오라는 명령을 내린다
    // 이 요청이 들어오는 과정에서 필터가 동작해 자동으로 쿠키를 생성한다
    return ResponseEntity.noContent().build(); // csrf 동작해 자동으로 쿠키 내보내준다
  }
}
