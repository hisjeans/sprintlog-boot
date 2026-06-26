package com.sprintlog.sprintlogboot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity // 이 클래스는 JPA가 관리한다, 이 클래슨느 데이터베이스의 한 행(인스턴스)에 정확하게 대응된다
@Table(name = "users") // 언급하지 않으면 클래스 이름과 동일하게 만들어준다
public class User extends BaseEntity {

//  @Id // 테이블 기본 키 알려줘야 한다, primary key
//  @GeneratedValue(strategy = GenerationType.IDENTITY)
//  // 숫자 자동 증가 전략 사용하겠다, sequence는 My SQL.. 지원하지 않는다 - 10씩 증가, 숫자 시작 천 부터 시작 약간의 cumstom 필요할 때는 sequence 사용해 값 올려야 하는 것
//  private Integer userNo; -> extends BaseEntity

  // @Column 속성으로 컬럼 제약 포현(null 가능 여부, 길이)
  @Column(nullable = false, length = 50) // NOT NULL
  private String nickName;

  @Column(nullable = false, unique = true, length = 100)
  // VARCHAR(100) NOT NULL UNIQUE, length를 따로 알려주지 않으면 255로 설정된다, UNIQUE는 얘기하지 않으면 기본값 false
  private String email;

  // user, learningActivity, member, book, loan, author..는 pk->id, createdAt, updatedAt 공통적으로 가질 가능성 높다

  // JPA가 엔티티를 만들 때 사용하는 기본 생성자, 우리가 호출하는 게 아니다
  // 없으면 JPA가 user 객체 생성할 수 없다
  protected User() {}

  // 우리가 실제로 사용하는 생성자
  public User(String nickName, String email) {
    this.nickName = nickName;
    this.email = email;
  }

}
