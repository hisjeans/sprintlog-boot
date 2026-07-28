package com.sprintlog.sprintlogboot.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
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

  // 연관관계의 주인이 아는 User는 mappedBy 속성을 세팅해준다
  // "owner" 값을 세팅하는 건, 연관관계의 주인 쪽 필드가 User다 라는 것을 얘기해 주는 것
  // 여기에 선언된 활동 리스트는 실제 DB에는 존재하지 않는 데이터, 이건 JPA가 연관관계를 보고 만들어주는 가상의 컬럼
  // 부모가 자식의 생명주기를 100% 제어하고 싶을 때 cascade와 orphanRemoval을 같이 걸어주는 경우가 흔하다
  @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true) // 연관관계의 진짜 주인은 LearningActivity의 owner가 진짜 주인, 고아 객체는 삭제하는 것이 true
  @JsonIgnore
  private List<LearningActivity> activities=new ArrayList<>();

      /*
        # CascadeType.PERSIST
        엔터티를 영속화할 때, 연관된 하위 엔터티도 함께 영속화 한다.
        연관 관계의 주인은 Employee. Department의 list는 가상 데이터.
        주인이 아닌 부모가 자식의 생명 주기에 영향을 주고 싶을 때 사용. (저장)

        ## CascadeType.ALL
        모든 Cascade를 적용한다.
        거의 대부분 CascadeType 쓴다면 ALL 한다

        # CascadeType.MERGE
        엔티티 상태를 병합(Merge)할 때, 연관된 하위 엔티티도 모두 병합한다.
        부모를 수정해서 반영하면 그 안에 있는 자식도 함께 반영됩니다.

        # CascadeType.REMOVE
        엔티티를 제거할 때, 연관된 하위 엔티티도 모두 제거한다.

        CascadeType.DETACH
        영속성 컨텍스트에서 엔티티 제거
        부모 엔티티를 detach()(Entity Manager) 수행하면, 연관 하위 엔티티도 detach()상태가 되어 변경 사항을 반영하지 않는다.

        CascadeType.REFRESH (Entity Manager)
        상위 엔티티를 새로고침(Refresh)할 때, 연관된 하위 엔티티도 모두 새로고침한다.

     */

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
