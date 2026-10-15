package com.sprintlog.sprintlogboot.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/*
OncePerRequestFilter - 한 요청당 한 번만 필터가 실행되도록 보장
 */
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    long startedAt = System.currentTimeMillis();
    try {
      // 다음 필터(→ 최종적으로 컨트롤러)로 요청을 넘긴다. 이 호출을 빼먹으면 요청이 여기서 멈춘다.
      filterChain.doFilter(request, response);
    } finally {
      // 요청이 다 처리되고 돌아온 뒤(응답 직전) 로깅한다 — 상태코드·소요시간을 알 수 있는 시점.
      long tookMs = System.currentTimeMillis() - startedAt;
      log.info("[AUDIT] {} {} -> {} ({}ms)",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          tookMs);

    }
  }

}
// 요청이 들어올 때마다 스레드 생성되는 게 아니라 기존 스레드 재사용
// 코어 스레드 같은 경우는 사라지지 않고 기존에 traceId 있으면 재사용한다
// 응답이 나가고 난 뒤에 MDC.remove(TRACE_ID)로 반드시 지워야 한다 왜냐하면 다음 요청이 남의 번호를 달고 로그를 찍을 수 있기 때문이다