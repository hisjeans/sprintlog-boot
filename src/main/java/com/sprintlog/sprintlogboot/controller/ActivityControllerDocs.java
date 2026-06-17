package com.sprintlog.sprintlogboot.controller;

import com.sprintlog.sprintlogboot.domain.*;
import com.sprintlog.sprintlogboot.dto.request.CreateActivityRequest;
import com.sprintlog.sprintlogboot.dto.request.UpdateActivityRequest;
import com.sprintlog.sprintlogboot.exception.ActivityNotFoundException;
import com.sprintlog.sprintlogboot.service.ActivityDashboard;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// Swagger 전용 인터페이스를 하나 선언해 비즈니스 로직과 문서화 로직 분리
// 기존 컨트롤러는 본연의 역할에 집중
public interface ActivityControllerDocs {
    // 문서화, 역할 분리, 본문은 컨트롤러에 있으니 swagger 관련만 적겠다
// 모든 활동 목록(페이징)
    @GetMapping // 요청 들어오면 get 메서드 세팅해줄 것
    @Operation(summary = "활동 목록 조회",
            description = "정렬(sort), 페이지(page), 크기(size) 쿼리파라미터로 활동 목록을 가볍게(요약) 반환한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공(요약 목록)")
    public ResponseEntity<List<LearningActivity>> getAll(
            @Parameter(description = "정렬 기준", example = "id",
                    schema = @Schema(allowableValues = {"id", "minutes", "title"}))
            @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 화면에 보여질 데이터 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ); // 컨트롤러 있기 때문에 body 삭제

    @Operation(summary = "활동 단건 조회",
            description = "id로 활동 하나를 상세하게 반환한다. 없으면 404(ProblemDetail)")
    // 단건 조회는 응답 형태 2가지 정상/비정상 -> 배열 형태로 표시
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "해당 id의 활동이 없음",
                    content = @Content(
                            mediaType = "application/json", // 404에러가 발생했을 때 json 응답
                            schema=@Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                            "type":
                                            "about:blank",
                                            "title": "활동을 찾을 수 없음",
                                            "status": 404,
                                            "detail": "활동을 찾을 수 없습니다. id=xxx",
                                            "instance": "/api/activities/xxx",
                                            "timestamp": "2026-06-12T01:38:25.989279Z"
                                            }
                                            """
                            )
                    ))
    })
    @GetMapping("/{id}") // "id" <- 1,2,3,4 요청을 보내는 쪽에서 보내는 데이터
    public ResponseEntity<LearningActivity> getById(
            @Parameter(description = "활동 식별자", example = "1") @PathVariable Long id);

    // 카테고리별로 그룹화된 활동 목록
    @GetMapping("/dashboard")
    public ResponseEntity<Map<ActivityCategory, List<LearningActivity>>> getDashboard();

    // 활동 수 요약 정보 (전체 / 강의 / 실습 / 독서)
    @Hidden // 숨길 수도 있다
    @GetMapping("/summary")
    public ResponseEntity<ActivityDashboard.Summary> getSummary();

    // 태그로 활동을 필터링
    @GetMapping("/search")
    public ResponseEntity<List<LearningActivity>> searchByTag(@RequestParam String tag, // 변수명이 쿼리파라미터 이름과 동일하기 때문에 () 생략 가능
                                                              @RequestParam("name") String name,
                                                              @RequestParam("age") int age);

    // -- 생성(POST) / 수정(PUT) / 삭제(DELETE) --

    @PostMapping // 요청 post
    public ResponseEntity<LearningActivity> create(@Valid @RequestBody CreateActivityRequest request);


    // 활동 수정, 자원 식별은 Path(/{id}) - 수정할 때는 어떤 객체를 변경할 것인지 지목해줘야 하기 때문
    // 변경할 내용은 본문 (UpdatedActivityRequest)
    // 대상이 없으면 404, 있으면 제목, 공개여부 변경하고 200
    @PutMapping("/{id}") // 빌드 수정 어렵기 때문에 바꾼다, 원래는 patchmapping이 어울리기는 하나 이미 빌드 완료된 프론트가 수정 요청을 보낼 때 putmapping 했기 때문에 바꾼 것
    public ResponseEntity<LearningActivity> update(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateActivityRequest request);

    // 활동 삭제, 성공 시 본문 없이 204 No Content, 대상이 없으면 404
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id);


}
