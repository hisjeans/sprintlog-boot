package com.sprintlog.sprintlogboot.repository;

import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.LearningActivity;
import com.sprintlog.sprintlogboot.domain.Visibility;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  // 종류별로 조회: WHERE category = ?
  List<LearningActivity> findByCategory(ActivityCategory category);

  // 제목에 키워드 포함(대소문자 무시): WHERE title LIKE lower('%키워드%');
  // 대소문자 무시 - title과 키워드를 일괄로 대문자로 맞춰주거나 소문자로 맞춰줄 것
  // findByTitle - 정확히 title과 일치해야 한다
  List<LearningActivity> findByTitleContainingIgnoreCase(String keyword);

  // 학습 시간이 기준 이상: WHERE minutes >= ?
  // findByMinutesGreater - 초과
  List<LearningActivity> findByMinutesGreaterThanEqual(int minutes);
  List<LearningActivity> findByMinutesLessThanEqual(int minutes);

  // 두 조건 AND + 정렬: WHERE category = ? AND visibility = ? ORDER BY minutes DESC
  List<LearningActivity> findByCategoryAndVisibilityOrderByMinutesDesc(ActivityCategory category, Visibility visibility);

  // 개수 세기: SELECT COUNT(*) FROM activities WHERE category = ?
  // 쿼리 메서드는 이처럼 간단하게 주로 사용
  long countByCategory(ActivityCategory category);

  // 쿼리 메서드의 키워드를 모두 외울 필요는 전혀 없다 조금만 길어져도 잘 안 쓴다 (조건이 많아지거나 JOIN이 들어가거나)
  // 간단한 조회문을 빠르게 만들 때 (PK가 아닌 컬럼을 조건식으로 쓸 때)

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  // JPQL과 Native Query
  // Native Query: 생 SQL
  // JPQL: 이 SQL을 JPQL 방식으로 작성
  // 기존의 테이블명과 컬럼명으로 작성되는 SQL과는 달리 JPQL은 엔티티 클래스명과 클래스에 있는 필드명으로 쿼리 작성
  // 일반 쿼리 SELECT * FROM activities WHERE minutes > ? ORDER BY minutes DESC
  @Query("SELECT a FROM LearningActivity a WHERE a.minutes >= :min ORDER BY a.minutes DESC") // *->별칭, 모두 조회
  List<LearningActivity> findLongActivities(@Param("min") int min); // 쿼리 메서드 아닌 이름 마음대로 지정
  // 테이블 자체를 entity 필드명으로 작성, ?에 들어가는 값은 min

  @Query("SELECT a FROM LearningActivity a JOIN a.tags t WHERE t=:tag")
  List<LearningActivity> findByTag(@Param("tag") String tag);

  // SELECT a.title, a.minutes, a.category 또한 가능
  @Query("SELECT a FROM LearningActivity a WHERE a.category=?1 AND a.visibility=?2 ORDER BY a.minutes DESC")
  List<LearningActivity> findCategoryAndVisibility(ActivityCategory category, Visibility visibility);

  // Native Query: 순수 SQL을 그대로 작성, DB에 종속적이라 표준 기능으로 안 될 때만 사용
  // 특정 데이터베이스 전용 함수 같은 것들을 써야 할 때, (JPA는 ANSI 표준 문법만 제공)
  @Query(value = "SELECT * FROM activities WHERE minutes >= ? ORDER BY minutes DESC", nativeQuery = true)
  List<LearningActivity> findLongActivitiesNative(@Param("min")int min);

  @Modifying // SELECT 아니면 무조건 붙여야 한다❕JPQL은 기본 SELECT 기반으로 동작
  @Query("DELETE FROM LearningActivity a WHERE a.title = ?1 AND a.category = ?2")
  void deleteByTitleAndCategoryWithJPQL(String title, ActivityCategory category); // 보통 삭제는 하나만 지정해 삭제하는 경우 많다 단순 연습용
}
