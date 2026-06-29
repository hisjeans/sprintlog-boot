package com.sprintlog.sprintlogboot.repository;

import com.sprintlog.sprintlogboot.domain.LearningActivity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// 직접 인메모리(ArrayList)에 저장하던 방식을 버리고, Spring Data JPA로 교체
public interface ActivityRepository extends JpaRepository<LearningActivity, Long> { // JpaRepository를 상속받아야 JPA 기능 사용 가능

  // Spring Data JPA는 PK를 활용한 조회 기능, 저장, 수정, 삭제는 기본적인 메서드를 제공한다 (구현체가)
  // 이번에는 우리가 PK가 아닌 FK(user_id)를 이용해 조회를 시도하려고 한다
  // 이런 경우에는 직접 메서드를 선언해 주어야 한다 (쿼리 메서드, JPQL 사용 등)
  // findByOwnerId: 'where owner_id = ?' 쿼리로 자동 생성
  // - 컬럼명 사용
  // - 선언만 하면 구현체가 알아서 쿼리 구성
  List<LearningActivity> findByOwnerId(Long ownerId);

//  List<LearningActivity> findByTitle(String title); // 같은 타이틀 중복 가능성 있다면 여러 개 조회될 수 있다
  Optional<LearningActivity> findByTitle(String title); // 하나만 조회할 때는 entity 그대로 받으면 위험할 수 있기 때문에 optional
}
