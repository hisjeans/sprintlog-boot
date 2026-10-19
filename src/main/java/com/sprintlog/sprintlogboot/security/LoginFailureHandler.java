package com.sprintlog.sprintlogboot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void onAuthenticationFailure(HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException exception) throws IOException, ServletException { // 로그인 실패했을 때 동작하기 때문에 예외 객체 전달받는다

    ProblemDetail pd = ProblemDetail.forStatusAndDetail(
        HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."
        // (참고) 요즈음은 직접적으로 알려주지 않는다
        // 이메일 존재하지 않는다는 게 어떻게 보면 보안상 취약점이 될 수 있기 때문
    );
    pd.setProperty("code", "AUTH_LOGIN_FAILED");

    // 응답 내보내는 방식은 LoginSuccessHandler와 같다
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE); // 문제 상황에서 리턴되는 JSON 타입
    response.setCharacterEncoding("UTF-8"); // 응답 객체에 한글 깨짐 방지 위해
    objectMapper.writeValue(response.getWriter(), pd);
  }
}
