package com.sprintlog.sprintlogboot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintlog.sprintlogboot.dto.response.UserResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final ObjectMapper objectMapper; // 잭슨 라이브러리 json 형태로 응답 내보낼 수 있다
  // 잭슨 데이터 라이브러리 이미 갖고 있다
  // RequestBody, ResponseBody 어노테이션이 jackson 라이브러리가 제공한다

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, // 요청 객체, 응답 객체, 인증 객체 전달받는다
      HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    // 인증에 성공하면 principal을 받을 수 있는데 principal에 우리 CustomUserDetatils가 담겨 있다 -> 도메인 User 꺼내서 활용 가능
    CustomUserDetails principal= (CustomUserDetails) authentication.getPrincipal();
    UserResponse body=UserResponse.from(principal.getUser());

    response.setStatus(HttpStatus.OK.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE); // 문자열로 직접 써도 된다 "application/json"
    response.setCharacterEncoding("UTF-8"); // 응답 객체에 한글 깨짐 방지 위해
    objectMapper.writeValue(response.getWriter(), body);
  }
}
