package com.sprintlog.sprintlogboot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 모든 엔터티가 공통으로 가지는 것 - 식별자(id)와 생성, 수정 시각 - 을 한곳에 모은 상위 클래스
@Getter
@MappedSuperclass // 이 클래스 자체는 테이블이 되지 않는다, 대신 이 클래스를 상속한 엔터티의 테이블에 여기 선언된 컬럼이 합쳐져서 들어간다
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity { // 자체적으로 객체 생성하지 못하게 막아준다

  @Id // 테이블 기본 키 알려줘야 한다, primary key
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  // 숫자 자동 증가 전략 사용하겠다, sequence는 My SQL.. 지원하지 않는다 - 10씩 증가, 숫자 시작 천 부터 시작 약간의 cumstom 필요할 때는 sequence 사용해 값 올려야 하는 것
  private Long id;

  // 생성 시각 - 처음 저장될 때 한 번 채워지고, 이후 바뀌지 않는다
  // insert 되는 시간 자동 채워진다
  @CreatedDate
  @Column(updatable = false)
  private LocalDateTime createdAt;

  // 수정 시각 - 저장될 때마다 현재 시각으로 갱신
  @LastModifiedDate
  private LocalDateTime updatedAt;

}
