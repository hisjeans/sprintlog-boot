package com.sprintlog.sprintlogboot.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.*;
import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.LearningActivity;
import com.sprintlog.sprintlogboot.domain.Visibility;
import com.sprintlog.sprintlogboot.dto.request.CreateActivityRequest;
import com.sprintlog.sprintlogboot.dto.request.UpdateActivityRequest;
import com.sprintlog.sprintlogboot.exception.ActivityNotFoundException;
import com.sprintlog.sprintlogboot.repository.ActivityRepository;
import com.sprintlog.sprintlogboot.repository.AuditLogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityService 슬라이스 테스트 (Mockito)")
class ActivityServiceTest {

//  @Autowired 로딩을 하지 않아 ActivityRepository 타입 빈이 없다고 표시하는 것
  // ActivityService의 기능을 테스트하는 것이기에, 나머지 의존객체는 가짜로 채워 넣는다
  @Mock // ActivityRepository는 가짜 객체로 쓰일 것
  ActivityRepository repository;
  @Mock AuditLogRepository auditLogRepository;
  @Mock AuditService auditService;

  // 서비스의 create는 timer가 걸려 있음 -> MeterRegistry의 timer는 실제로 동작해야 한다 (Mock 안 됨)
  // @Spy를 걸어서 실제 기능이 동작할 수 있는 객체로 둔다 - 일부 기능은 동작할수 있게끔 처리
  @Spy MeterRegistry meterRegistry=new SimpleMeterRegistry();
  // @Mock 쓰면 안 된다 - meterRegistry가 가짜 객체이면 timer도 없기 때문에 create 메서드 자체가 동작하지 않는다

  @InjectMocks // 가짜 객체 주입
  ActivityService service; // 메서드 부르기 위해 추가
  // 가짜 객체들을 주입받은 Service 객체를 사용

  private LearningActivity sample() {
    return new LearningActivity(ActivityCategory.LECTURE, "옛 제목", 30, Visibility.PRIVATE, "이강사", null, null);
  }

  @Nested
  @DisplayName("조회(get)")
  class Get {

    @Test
    @DisplayName("있으면 그 활동을 돌려준다.")
    void 있으면_반환() {
      // given
      LearningActivity activity = sample();
//      repository.findById(1L); 현재 repository는 가짜 객체, 비어 있기 때문에 아무것도 줄 수 없다
      when(repository.findById(1L)).thenReturn(Optional.of(activity));
      // 서비스가 1번 활동 조회 요청하면 activity 반환

      // when & then
      assertThat(service.get(1L)).isSameAs(activity);
      // 가짜 객체가 주입된 service InjectMocks, 샘플 return 하도록
    }

    @Test
    @DisplayName("없으면 ActivityNotFoundException 발생")
    void 없으면_예외() {
      // given
      when(repository.findById(999L)).thenReturn(Optional.empty()); // 비어 있는 객체 리턴

      // when & then
      assertThatThrownBy(()->service.get(999L)).isInstanceOf(ActivityNotFoundException.class);
      // 예외 발생 -> 타입은 ActivityNotFoundException
    }
  }

  @Nested
  @DisplayName("수정(update)")
  class Update {

    @Test
    @DisplayName("제목, 공개여부를 바꾸고 저장한다.")
    void 정상_수정() {
      // given
      LearningActivity activity = sample();
//      when(repository.findById(1L)).thenReturn(Optional.of(activity)); 기본 Mockito 방식
      given(repository.findById(1L)).willReturn(Optional.of(activity)); // BDD 방식의 Mockito 방식
      given(repository.save(any(LearningActivity.class))).willReturn(activity); // any(): 무엇이든
      // when
      service.update(1L, new UpdateActivityRequest("새 제목", Visibility.PUBLIC));

      // then
      assertThat(activity.getTitle()).isEqualTo("새 제목"); // 상태 검증
      assertThat(activity.getVisibility()).isEqualTo(Visibility.PUBLIC); // 상태 검증
      verify(repository).save(activity); // repository에 save가 호출되면서 activity 받았는지 행위를 검증하는 것
      // 행위 검증(서비스가 협력자를 제대로 불렀니?)
      // 명시적으로 save 부르는 것 선호하지만, 안 부를 수도 있다
      // 서비스가 update하고 나서의 결과에 대한 검증, 서비스가 어떠한 행위를 했는지 까지도 검증받고 싶다
    }

    @Test
    @DisplayName("대상이 없다면 예외 - 저장은 일어나지 않는다")
    void 없으면_변경없음() {
      // given
      given(repository.findById(999L)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(()->service.update(999L, new UpdateActivityRequest("x", Visibility.PUBLIC)))
          .isInstanceOf(ActivityNotFoundException.class);
      verify(repository, never()).save(any()); // save 는 절대 불리지 않았을 것
      // 없는 것을 수정하려면 save 호출되기 전에 미리 처리되었을 것
    }
  }

  @Nested
  @DisplayName("삭제(delete)")
  class Delete{

    @Test
    @DisplayName("존재하면 그 id로 삭제한다. (existsById 확인)")
    void 정상_삭제() {
      // given
      given(repository.existsById(1L)).willReturn(true);

      // when
      service.delete(1L); // void 메서드, 돌려받는 게 없다 - 상태 검증 필요 없다, 행위 검증만

      // then
      verify(repository).deleteById(1L);

    }

    @Test
    @DisplayName("없으면 예외 - 삭제는 일어나지 않는다.")
    void 없으면_삭제안함() {
      // given
      given(repository.existsById(999L)).willReturn(false);

      // when & then
      assertThatThrownBy(()->service.delete(999L)).isInstanceOf(ActivityNotFoundException.class);
      verify(repository, never()).deleteById(any()); // deleteById의 무엇이 들어가든 절대 들어갈 일 없다

    }
  }

  @Nested
  @DisplayName("저장된 '내용' 검증 - ArgumentCaptor")
  class SavedContent {

    @Test
    @DisplayName("수정 시 저장되는 엔터티의 제목은 요청대로 바뀌어 있다.")
    void 저장내용_검증() {
      // given
      LearningActivity activity = sample();
      given(repository.findById(1L)).willReturn(Optional.of(activity)); // BDD 방식의 Mockito 방식
      given(repository.save(any(LearningActivity.class))).willReturn(activity); // any(): 무엇이든

      // when
      service.update(1L, new UpdateActivityRequest("바뀐 제목", Visibility.PUBLIC));
      // ArgumentCaptor는 행위를 검증하는 verify를 시행할 때 호출하는 메서드의 매개값을 붙잡아 활용할 수 있게 해 준다
      // update에서는 사실 ArgumentCaptor를 사용할 필요는 없다 -> 샘플 데이터를 직접 생성해 전달하기 때문에
      // 똑같은 객체를 수정해 확인하면 끝
      // create에서는 비교 대상이 없기 때문에 전달된 값을 검증하기 위해 ArgumentCaptor가 필요
      ArgumentCaptor<LearningActivity> captor = ArgumentCaptor.forClass(
          LearningActivity.class);

      // then
      verify(repository).save(captor.capture());
      assertThat(captor.getValue().getTitle()).isEqualTo("바뀐 제목");

    }
  }
  
  @Nested
  @DisplayName("생성(create) - 내부에서 만든 엔터티를 ArgumentCaptor로 검증")
  class Create{

    @Test
    @DisplayName("요청을 매핑해 저장한다 - 저장되는 엔터티의 필드가 요청과 일치")
    void 요청을_매핑해_저장한다() {
      // given
      CreateActivityRequest request = new CreateActivityRequest(
          ActivityCategory.LECTURE, "새 강의", 45, Visibility.PUBLIC,
          null, null, "이 강사", null, null
      );

      // willReturn 은 리턴할 객체를 직접 명시해 주어야 한다
      // create()를 테스트할 때는 이 테스트 메서드 안에 리턴해줄 Entity가 없다
//      given(repository.save(any(LearningActivity.class))).willReturn(???);
      // 서비스가 repository의 save를 호출하면 호출한 시점에 전달받은 그 인자(엔터티)를 그대로 돌려주어라 -> willAnswer
      given(repository.save(any(LearningActivity.class))).willAnswer(inv->inv.getArgument(0)); // save 메서드 호출될 때 리턴되는 첫 번째 인자값 돌려줄 것

      // when
      service.create(request, null); // 이미지 저장 안 되면 null로 저장
      ArgumentCaptor<LearningActivity> captor = ArgumentCaptor.forClass(LearningActivity.class); // 매개값을 captor

      // then
      verify(repository).save(captor.capture()); // willAnswer 가 돌려준 엔티티를 argument captor가 붙잡음
      LearningActivity saved = captor.getValue(); // 붙잡은 Entity를 돌려준다

      // 내가 전달한 DTO의 값으로 Entity가 잘 매핑 되었는지를 단언을 통해 검증
      assertThat(saved.getTitle()).isEqualTo("새 강의");
      assertThat(saved.getCategory()).isEqualTo(ActivityCategory.LECTURE); // request.title() 로 비교해도 된다
      assertThat(saved.getMinutes()).isEqualTo(45);
      assertThat(saved.getInstructorName()).isEqualTo("이 강사");
    } // 테스트는 DTO 통해 생성되고 있다 -> save 되는 첫 번째 매개값을 돌려주고(willAnswer-ArgumentCaptor) 값 일치하는지 확인
  }

  @Nested
  @DisplayName("void 협력자 다루기 - willThrow")
  class VoidCollabolator{

    @Test
    @DisplayName("삭제 협력자가 실패하면 예외가 전파된다.")
    void 삭제_실패_전파() {
      // given
      // given(repository.deleteById(1L)).willThrow(new RuntimeException("DB 오류")); - deleteById가 void, given 절 안에 인자로 들어갈 수 없다
      // => 순서를 뒤집어서 해결하자
      given(repository.existsById(1L)).willReturn(true);
      // deleteById를 호출하면서 1L을 주면 예외를 일부러 발생 시키겠다
      willThrow(new RuntimeException("DB 오류")).given(repository).deleteById(1L);
      // 실제로는 예외가 발생하지 않지만 일부로 데이터베이스 예외 발생한다

      // when & then
      assertThatThrownBy(()->service.delete(1L)).isInstanceOf(RuntimeException.class);
    }
  }
}