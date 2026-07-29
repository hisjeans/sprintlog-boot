package com.sprintlog.sprintlogboot.repository;

import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.LearningActivity;
import com.sprintlog.sprintlogboot.domain.Visibility;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

//@SpringBootTest -> 내가 등록한, Spring Boot에서 사용하는 모든 빈들이 로딩되어 컨테이너에 세팅된다, 모든 빈들 로딩되어 속도 느려진다
@DataJpaTest // JPA 계층 관련 빈만 로딩(Service, Controller, Component는 로딩되지 않는다)
class ActivityRepositoryTest {

  @Autowired // 테스트 환경에서는 생성자 의존성 주입을 사용할 수 없어 @Autowired 직접 주입해야 한다
  ActivityRepository repository; // 인터페이스는 직접 만들었지만, 구현체는 JPA가 제공하는 것
  // 테스트 클래스는 객체 생성되지 않기 때문에 @RequiredArgsConstructor 가 생성되지 않는다
  // 테스트 클래스는 Bean 등록되지 않고, 자동 생성되지 않는다
  // => Autowired: ActivityRepository 가질 수 있는 Bean 검색

  @Autowired
  TestEntityManager em; // EntityManager - JPA에서 중요한 영속성 컨텍스트(persistence context), 관리
  // Test 환경에서 사용할 수 있는 test manager 직접 주입받겠다


  @BeforeEach
  void setUp() {
    // 테스트 전, 영속성 컨텍스트에 네 개 세팅하면 조회 로직이 select가 나가지 않고 영속성 컨텍스트가 바로 조회된다
    persist(ActivityCategory.LECTURE, "Spring Boot 입문", 90, Visibility.PUBLIC, "spring", "java");
    persist(ActivityCategory.LECTURE, "JPA 심화", 120, Visibility.PUBLIC, "spring", "jpa");
    persist(ActivityCategory.READING, "클린 코드", 60, Visibility.PRIVATE, "clean");
    persist(ActivityCategory.PRACTICE, "알고리즘 연습", 45, Visibility.PUBLIC);

    // 더미데이터 세팅 후에 진행될 테스트를 조금 더 깔끔하게 진행하기 위해 TestEntityManager로 영속성 컨텍스트 직접 제어
    em.flush(); // 강제 수행, insert 쿼리 무조건 나간다, 영속성 컨텍스트에 영속된 엔티티들을 강제로 밀어내기 -> INSERT
    em.clear(); // 깔끔하게 비운다, 테스트할 때 영속성 컨텍스트 재사용하지 않도록 한다, 영속성 컨텍스트 비우기
    // 테스트 환경 확실히 하기 위함
  }

  private void persist(ActivityCategory category, String title, int minutes, Visibility visibility,
      String... tags) {
    LearningActivity activity = new LearningActivity(category, title, minutes, visibility, null,
        null, null);
    for (String tag : tags) {
      activity.addTag(tag);
    }
    em.persist(activity);
  }
}