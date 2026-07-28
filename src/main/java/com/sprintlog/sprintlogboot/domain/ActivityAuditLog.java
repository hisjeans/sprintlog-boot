package com.sprintlog.sprintlogboot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자를 protected로 선언(JPA가 사용하는 생성자) - 외부에서 생성하지 못하도록
public class ActivityAuditLog extends BaseEntity { // 활동 이력 감시 추적

  // 어떤 작업이었는지 - "CREATE", "UPDATE", "DELETE"
  // enum으로 선언하는 것이 더 적합하나 시간관계상 우선 아래처럼 선언
  @Column(nullable = false, length = 20)
  private String action; // 어떤 작업

  // 상세 - 예: "활동 생성: Spring Bean Scope"
  @Column(nullable = false, length = 300)
  private String detail; // 상세 정보


//  protected ActivityAuditLog() {}

  // @Builder 사용 가능
  public ActivityAuditLog(String action, String detail) {
    this.action = action;
    this.detail = detail;
  }
}
