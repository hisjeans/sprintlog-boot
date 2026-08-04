package com.sprintlog.sprintlogboot.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.LearningActivity;
import com.sprintlog.sprintlogboot.domain.Visibility;
import com.sprintlog.sprintlogboot.dto.request.UpdateActivityRequest;
import com.sprintlog.sprintlogboot.exception.ActivityNotFoundException;
import com.sprintlog.sprintlogboot.service.ActivityDashboard;
import com.sprintlog.sprintlogboot.service.ActivityService;
import com.sprintlog.sprintlogboot.service.FileService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(ActivityController.class)
@DisplayName("ActivityController 웹 계층 테스트")
class ActivityControllerTest {

  @Autowired
  MockMvc mvc; // 진짜 서버를 띄우지 않고도 HTTP 요청과 응답을 가짜로 시뮬레이션 해주는 스프링의 도구

  // 의존 관계가 있는 객체들은 모두 가짜로 채우자 - 웹 계층만 집중
  @MockitoBean
  ActivityService service; // 가짜

  @MockitoBean
  ActivityDashboard dashboard;

  @MockitoBean
  FileService fileService;

  private LearningActivity sample;

  @BeforeEach
  void setUp() {
    sample = new LearningActivity(
        ActivityCategory.LECTURE, "스프링 강의", 30, Visibility.PUBLIC, "이강사", null, null);
    // id 는 원래 DB 가 부여하지만, 여기선 서비스가 가짜라 직접 심어 응답 JSON 의 id 를 확인할 수 있게 한다.
    ReflectionTestUtils.setField(sample, "id", 1L);
    // LearningActivity 아이디는 @GeneratedValue로 데이터베이스에 insert될 때 id 자동 증가 전략 사용
    // 문제는 데이터베이스 사용하지 않기 때문에 테스트 클래스에서 사용하는 아이디가 항상 null
    // set id라는 로직이 현재 없다, LearningActivity는 @Setter 없다, 테스트를 위해 setId() 열어놓는 것은 적합하지 않다
    // 아이디는 현재 pk, not null+unique, 중복, null 모두 안 된다 - 테스트 위해 set 열어놓는 건 적합하지 않다
    // 나중에 아이디가 겹치게 되면 데이터 전부 꼬이고 문제 발생할 것 - 현재 테스트 LearningActivity에 아이디 채울 방법이 없다
    // => ReflectionTestUtils 테스트에서만 일부적으로 활용해주는 객체
    // runtime의 private이라는 접근 제한자 무시하고 직접 값을 부여해주는 역할, main에서는 절대 사용하면 안 된다 - OCP, 단일책임원칙 전부 무시하고 들어가기 때문
    // 거의 테스트 전용 클래스로 생각
  }

  // Get - 조회, Post - 생성, Put/Patch - 수정... 방식 적용해보자
  @Nested
  @DisplayName("GET /{id}")
  class GetById {

    @Test
    @DisplayName("존재하면 200 + JSON 본문(id, title)")
    void 존재하면_200() throws Exception {
      // given
      given(service.get(1L)).willReturn(sample);
      // 가짜 객체에 get 1 리턴

      // when & then
      mvc.perform(get("/api/v1/activities/1")) // "localhost8080" 작성할 필요는 없다, 클래스 내 테스트이기 때문
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1)) // json 전용 주소, "$" json 내부 접속 기호
          // mock mvc 통해 get 방식으로 위 요청 보내면 200 OK가 올 것 기대
          .andExpect(jsonPath("$.title").value("스프링 강의"))
          .andExpect(jsonPath("$._link.self").exists()); // HATEOAS 링크는 존재한다

    }

    @Test
    @DisplayName("없으면 예외 → 404 + ProblemDetail(code A001)")
    void 없으면_404() throws Exception {
      given(service.get(999L)).willThrow(new ActivityNotFoundException(999L));

      mvc.perform(get("/api/v1/activities/999"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.code").value("A001"));   // ErrorCode 가 실린다
    }

    @Test
    @DisplayName("GET (목록) - 페이징 파라미터가 전달되면 페이지에 해당하는 목록이 전달된다.")
    void 목록_페이징_조회() throws Exception {
      // given
      Page<LearningActivity> page=new PageImpl<>(List.of(sample), PageRequest.of(0, 20), 1);
      given(service.page(eq("id"), eq(1), eq(5), eq(null))).willReturn(page); // eq 이용하면 매개값도 정확히 일치해야 한다

      // when & then
      mvc.perform(get("/api/v1/activities")
          .param("sort", "id") // 메서드 하나에 두 개의 값, name, values
          .param("page", "1")
          .param("size", "5"))
          .andExpect(status().isOk());
      // ActivityController getAll 요청
    }
  }

  @Nested
  @DisplayName("POST (생성) - multipart(data + 선택 file)")
  class Create {

    @Test
    @DisplayName("data 만 보내도 201 + Location (file 은 선택)")
    void 정상이면_201() throws Exception {
      given(service.create(any(), any())).willReturn(sample);

      // 가짜 multiPartFile - required=false - 필요하지 않으면 안 보낼 수 있다, 파일 보내지 않아도 정상 등록되는지 확인해볼 것
      MockMultipartFile data = new MockMultipartFile("data", "data.json",
          MediaType.APPLICATION_JSON_VALUE,
          """
              {"category":"LECTURE","title":"스프링 강의","minutes":30,"visibility":"PUBLIC","instructorName":"이강사"}
              """.getBytes());

      mvc.perform(multipart("/api/v1/activities").file(data))
          .andExpect(status().isCreated())
          .andExpect(header().string("Location", "/api/activities/1"))
          .andExpect(jsonPath("$.title").value("스프링 강의"));

      verify(fileService, never()).saveFile(any());   // 파일이 없으면 저장도 안 함, saveFile 절대 불리지 않았을 것
    }


    @Test
    @DisplayName("data + file 이면 201, 파일은 FileService로 저장된다.")
    void 파일첨부_201() throws Exception {
      given(service.create(any(), any())).willReturn(sample);
      given(fileService.saveFile(any())).willReturn("saved-uuid.png"); // 저장했다 치고 파일명 반환(가짜)

      // 가짜 multiPartFile - required=false - 필요하지 않으면 안 보낼 수 있다, 파일 보내지 않아도 정상 등록되는지 확인해볼 것
      MockMultipartFile data = new MockMultipartFile("data", "data.json",
          MediaType.APPLICATION_JSON_VALUE,
          """
              {"category":"LECTURE","title":"스프링 강의","minutes":30,"visibility":"PUBLIC","instructorName":"이강사"}
              """.getBytes());

      // 가자 이미지 데이터 추가 (실제 파일을 첨부할 필요는 전혀 없습니다. 컨트롤러가 그걸 신경쓰지 않고, FileService도 가짜입니다.)
      MockMultipartFile file = new MockMultipartFile("file", "proof.png",
          MediaType.IMAGE_PNG_VALUE, "이미지-바이트-데이터".getBytes()); // 가짜 이미지 데이터 추가

      mvc.perform(multipart("/api/v1/activities").file(data).file(file))
          .andExpect(status().isCreated())
          .andExpect(header().string("Location", "/api/activities/1"))
          .andExpect(jsonPath("$.title").value("스프링 강의"));

      verify(fileService).saveFile(any());   // 파일 파트가 FileService에게 넘어갔나?


    }


    @Test
    @DisplayName("제목이 비면 검증 실패 -> 400 + ProblemDetail(errors)")
    void 검증실패면_400() throws Exception {
      // bean @Validation 동작되는지 확인하기 위함

      // 가짜 multiPartFile - required=false - 필요하지 않으면 안 보낼 수 있다, 파일 보내지 않아도 정상 등록되는지 확인해볼 것
      MockMultipartFile data = new MockMultipartFile("data", "data.json",
          MediaType.APPLICATION_JSON_VALUE,
          """
              {"category":"LECTURE","title":"","minutes":30,"visibility":"PUBLIC","instructorName":"이강사"}
              """.getBytes());

      mvc.perform(multipart("/api/v1/activities").file(data))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("C001"))
          .andExpect(jsonPath("$.errors").exists());

      verify(service, never()).create(any(), any());   // 검증에서 막혀서 서비스까지 못 간다
    }
  }

  @Nested
  @DisplayName("수정(PUT)")
  class Update {

    @Test
    @DisplayName("존재하면 200 + 바뀐 값")
    void 수정_200() throws Exception {
      // update controller - PUT, id, UpdateActivityRequest(title, visibility)
      // 실제 수정은 서비스, 수정되었을 때 바뀐 값이 온다는 건 컨트롤러가 제어할 수 없다

      // given
      ReflectionTestUtils.setField(sample, "title", "새 제목");
      given(service.update(eq(1L), any(UpdateActivityRequest.class))).willReturn(sample);

      // when & then
      mvc.perform(put("/api/v1/activities/1")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"title\":\"새 제목\",\"visibility\":\"PUBLIC\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title").value("새 제목"));
    }


    @Test
    @DisplayName("없으면 404 + ProblemDetail(A001)")
    void 없으면_404() throws Exception {

      // given
      given(service.update(eq(999L), any(UpdateActivityRequest.class))).willThrow(
          ActivityNotFoundException.class);

      // when & then

      mvc.perform(patch("/api/v1/activities/999")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"title\": \"x\",\"visibility\": \"PUBLIC\"}"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("S.code").value("A001"));

      }
    }


    @Nested
    @DisplayName("DELETE /{id} (삭제)")
    class Delete {

      @Test
      @DisplayName("성공하면 204 (본문 없음)")
      void 삭제_204() throws Exception {
        mvc.perform(delete("/api/v1/activities/1"))
            .andExpect(status().isNoContent());

        verify(service).delete(1L);
      }

      @Test
      @DisplayName("없으면 404 — void 협력자라 willThrow 로 스텁") // 반대로 뒤집자
      void 없으면_404() throws Exception {
        // ControllerService delete - void
        willThrow(new ActivityNotFoundException(999L)).given(service).delete(999L);
        mvc.perform(delete("api/v1/activities/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("A001"));
      }
    }

}