package com.sprintlog.sprintlogboot.dto.response;

import com.sprintlog.sprintlogboot.domain.ActivityAuditLog;
import java.time.LocalDateTime;

public record AuditLogResponse(
    long id,
    String action,
    String detail,
    LocalDateTime at
) { // 엔티티를 화면단에 그대로 노출하지 않고 필요한 정보만 보여준다

  // 객체 생성 없이도 부를 수 있도록
  public static AuditLogResponse from(ActivityAuditLog log){
    return new AuditLogResponse(
        log.getId(),
        log.getAction(),
        log.getDetail(),
        log.getCreatedAt()
    );
  }
}
