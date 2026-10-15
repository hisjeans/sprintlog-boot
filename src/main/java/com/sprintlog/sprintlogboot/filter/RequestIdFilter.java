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

@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {

  private static final String REQUEST_ID_HEADER = "X-Request-ID";
  private static final String MDC_REQUEST_ID_KEY = "requestId";

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    // 1. Request ID 생성 또는 추출
    String requestId = request.getHeader(REQUEST_ID_HEADER); // 요청 객체에서 "X-Request-ID" 헤더
    if (requestId == null || requestId.isEmpty()) {
      requestId = UUID.randomUUID().toString(); // 클라이언트 쪽에서 각각의 구분할 수 있는 아이디를 보내주면 그걸 꺼내고, 그렇지 않으면 랜덤한 고유식별자 꺼내주겠다
    }

    // 2. MDC에 저장 (로그에서 사용)
    // 각각의 스레드마다 고유한 컨텍스트 정보 저장해 돕는, 사용자의 요청이 쏟아졌을 때 각각의 요청이 어떤 요청인지 구분할 수 있도록 돕는 것
    MDC.put(MDC_REQUEST_ID_KEY, requestId);

    // 3. Response 헤더에 추가
    response.setHeader(REQUEST_ID_HEADER, requestId);

    try {
      log.info("Request ID: {} - Started", requestId);
      filterChain.doFilter(request, response);
    } finally {
      log.info("Request ID: {} - Completed", requestId);
      // 4. MDC 정리 (메모리 누수 방지)
      MDC.clear();
    }
  }
}
