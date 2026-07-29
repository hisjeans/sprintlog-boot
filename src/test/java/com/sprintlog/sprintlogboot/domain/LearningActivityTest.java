package com.sprintlog.sprintlogboot.domain;

import com.sprintlog.sprintlogboot.exception.InvalidActivityException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
// assertThat을 더 사용

class LearningActivityTest {

  LearningActivity activity;

  @BeforeEach // 각각의 테스트 메서드가 실행되기 직전에 무조건 1회 실행된다
  void setUp() { // setUp이 끝난 뒤 테스트 로직 돌아간다
    activity = new LearningActivity(
        ActivityCategory.LECTURE, "테스트 강의", 30, Visibility.PUBLIC,
        "이강사", null, null);
  }

  @Nested
  @DisplayName("생성")
  class Creation {

    @Test
    @DisplayName("정상 값으로 만들면 필드가 그대로 담긴다")
    void 정상_생성() {
      // 여러 값을 *한꺼번에* 검증 — 하나가 틀려도 나머지까지 다 확인해 보고(그룹 검증)
      assertAll( // junit에서 제공
          // Assertj에서 제공
          () -> assertThat(activity.getTitle()).isEqualTo("스프링 강의"),
          () -> assertThat(activity.getMinutes()).isEqualTo(30),
          () -> assertThat(activity.getCategory()).isEqualTo(ActivityCategory.LECTURE),
          () -> assertThat(activity.getVisibility()).isEqualTo(Visibility.PUBLIC)
      );
    }

    @Test
    @DisplayName("제목이 비어 있으면 InvalidActivityException")
    void 빈_제목_예외() {
      assertThatThrownBy(() -> new LearningActivity(
          ActivityCategory.LECTURE, "   ", 30, Visibility.PUBLIC, "이강사", null, null))
          .isInstanceOf(InvalidActivityException.class)
          .hasMessageContaining("제목");
    }

    @Test
    @DisplayName("학습 시간이 0 이하이면 예외")
    void 잘못된_시간_예외() {
      assertThatThrownBy(() -> new LearningActivity(
          ActivityCategory.LECTURE, "제목", 0, Visibility.PUBLIC, "이강사", null, null))
          .isInstanceOf(InvalidActivityException.class);
    }
  }

  // @Nested를 활용해 테스트를 주제별로 묶을 수 있다
  @Nested
  @DisplayName("학습 시간 연장")
  class ExtendStudy{
    // 왠만한 테스트 타입 void, 대부분 테스트 완료되면 끝이기 때문
    // 메서드 이름은 상관 없이 편한대로 작성하면 된다
    // 한글 섞어 짓는 경우도 많다, 팀 컨벤션 따른다
    @Test
    @DisplayName("학습 시간을 연장하면 분이 그만큼 늘어난다.")
    void 늘어남() {
      // given: 테스트 사용할 때 필요한 값 전달하는 구간, 테스트 준비(테스트에 필요한 데이터를 준비하는 구간)

      // 객체 직접 생성 -> extendStudy

      // when: 테스트 실행 (실제 검증하고자 하는 로직이 수행되는 구간)
      activity.extendStudy(25);

      // then: 테스트 검증(테스트 결과를 검증하는 구간, AssertJ에서 제공하는 메서드로 검증)
      Assertions.assertThat(activity.getMinutes()).isEqualTo(55);

    }

    @Test
    @DisplayName("0분 이하로 연장하면 예외가 발생한다.")
    void 음수_예외() {
      // given

      // when
      // activity.extendStudy(-5); 에러 발생할 것, then까지 가보지 못하고 코드 죽을 수 있다 - 합쳐서 사용하자

      // then
      // 익명 객체 형태로 전달
      assertThatThrownBy(() -> activity.extendStudy(-5))
          .isInstanceOf(InvalidActivityException.class) // 예외의 종류 단언 가능
          .hasMessageContaining("1분"); // 에러 메시지에 포함될 문자열 단언 가능

    }
  }
  @Nested
  @DisplayName("태그")
  class Tags {

    @Test
    @DisplayName("추가하면 공백 제거·소문자로 정규화되어 담긴다")
    void 추가_정규화() {
      activity.addTag("  Spring  ");
      assertThat(activity.getTags()).containsExactly("spring"); // spring 반드시 포함되어 있어야 한다
    }

    @Test
    @DisplayName("같은 태그는 대소문자만 달라도 중복 저장되지 않는다")
    void 중복_무시() {
      activity.addTag("java");
      activity.addTag("JAVA");
      assertThat(activity.getTags()).hasSize(1); // 대소문자 다르게 넣더라도 1, 통과한 것
    }

    @Test
    @DisplayName("빈 태그는 예외")
    void 빈_태그_예외() {
      assertThatThrownBy(() -> activity.addTag("   "))
          .isInstanceOf(InvalidActivityException.class);
    }
  }

  @Nested
  @DisplayName("제목·공개여부 변경")
  class Change {

    @Test
    @DisplayName("제목을 바꾸면 반영된다")
    void 제목_변경() {
      activity.changTitle("새 제목");
      assertThat(activity.getTitle()).isEqualTo("새 제목");
    }

    @Test
    @DisplayName("비공개로 바꾸면 PRIVATE 이 된다")
    void 비공개() {
      activity.hideFromPublic();
      assertThat(activity.getVisibility()).isEqualTo(Visibility.PRIVATE);
    }
  }

}
// DB 연결, repository 활성화될 필요 없다
// 이 로직을 main에서 작성하려면 서버를 무조건 켜야 띄워야 한다

