package com.sprintlog.sprintlogboot.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthorizationEventListener {

  @EventListener
  public void onDenied(AuthorizationDeniedEvent<?> event){
    // 메서드 매개변수로 무엇을 받는지에 따라 구분
    // AuthorizationDeniedEvent<?> event 실패 권한
    // 인가 실패했을 때 자동으로 호출되는 listener

    String who = event.getAuthentication().get() != null
        ? event.getAuthentication().get().getName() // 최종적 이메일 얻어낸다
        : "anonymous"; // 인증 안 된 사용자로 문자열 채워넣는다

    log.warn("[AUTHZ] 인가 거부 - 사용자: {}, 결정: {}", who, event.getAuthorizationResult());
  }
}
