package com.sprintlog.sprintlogboot.controller;

import com.sprintlog.sprintlogboot.domain.*;
import com.sprintlog.sprintlogboot.dto.request.UpdateActivityRequest;
import com.sprintlog.sprintlogboot.exception.ActivityNotFoundException;
import com.sprintlog.sprintlogboot.repository.ActivityRepository;
import com.sprintlog.sprintlogboot.dto.request.CreateActivityRequest;
import com.sprintlog.sprintlogboot.service.ActivityDashboard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// 컨트롤러 자체에 공통 url 매핑하는 것이 가능
@RestController// Response body 내장, 메서드 마다 @ResponseBody(json 변환) 일일이 붙일 필요 없게 됨
@Slf4j
@RequiredArgsConstructor
@RequestMapping({"api/v1/activities","/api/activities"})
// 컨트롤러 쪽에 공통 url 매핑, 기본적으로 "/api/activities" 으로 시작하도록 지정, 경로 두 개 설정 가능(버전 명시 앞으로 권장하는 새 버전, 이전 버전)
// 경로 저장하는 react 화면이 깨지지 않기 위해 이전 버전, 새 버전 모두 명시하는 것이 필요하다
// 경로를 둘로 받아 기존의 요청도 컨트롤러가 해결할 수 있도록 한다
@Tag(name = "활동(Activity)", description = "학습 활동 조회, 생성, 수정, 삭제 API") // 자세한 설명 추가
public class ActivityController implements ActivityControllerDocs {
// if) repository, dashboard가 없었다면 직접 작성해야 했을 것`
    private final ActivityRepository repository;
    // ActivityController는 ActivityRepository에게 의존
    // 저장된 정보 불러와 리턴해야 하기 때문
    private final ActivityDashboard dashboard; // 의존성 관계 추가


    // 모든 활동 목록(페이징)
    @GetMapping // 요청 들어오면 get 메서드 세팅해줄 것
    public ResponseEntity<List<LearningActivity>> getAll(
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        // 게시판에 처음 들어왔을 때 데이터 전달되지 않을 가능성 크기 때문에 기본 값 설정

        Comparator<LearningActivity> comparator=switch (sort){
            case "minutes" -> Comparator.comparingInt(LearningActivity::getMinutes);
            case "title" -> Comparator.comparing(LearningActivity::getTitle);
            default -> Comparator.comparing(LearningActivity::getId); // "minutes", "title" 아닌 이상한 값이 들어오더라도 -> id로 비교
        };
        // 자바 이해하지 못할 수 있기 때문에 리스트를 json으로 변환해줘야 함
        // => @ResponseBody 리턴하고자하는 리스트를 json으로 변환해 리턴해주는 역할

        List<LearningActivity> list = repository.findAll().stream()
                .sorted(comparator)
                .skip(page*size) // 0페이지면 0개 건너뛰고 size개(첫번째 페이지는 스킵하지 않는다), 1페이지면 size개 건너뛰고 size개
                .limit(size)
                .toList();

        return ResponseEntity.ok().body(list); // 바로 list로 리턴되지 않고 ResponseEntity로 감쌌다 생각
    }

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
        @Parameter(description = "활동 식별자", example = "1") @PathVariable Long id) { // 메서드 내부에서 id 변수로 사용할 수 있도록 설정
        LearningActivity activity = repository.findFirst(a -> a.getId() == id)
                .orElseThrow(() -> new ActivityNotFoundException(id));
        return ResponseEntity.ok().body(activity);
    }

    // 카테고리별로 그룹화된 활동 목록
    @GetMapping("/dashboard")
    public ResponseEntity<Map<ActivityCategory, List<LearningActivity>>> getDashboard(){
        Map<ActivityCategory, List<LearningActivity>> map = dashboard.groupByCategory();
        return ResponseEntity.ok().body(map);
    }

    // 활동 수 요약 정보 (전체 / 강의 / 실습 / 독서)
    @GetMapping("/summary")
    public ResponseEntity<ActivityDashboard.Summary> getSummary(){
        return ResponseEntity.ok().body(dashboard.summarize());
    }

    // 태그로 활동을 필터링
    @GetMapping("/search")
    public ResponseEntity<List<LearningActivity>> searchByTag(@RequestParam String tag, // 변수명이 쿼리파라미터 이름과 동일하기 때문에 () 생략 가능
                                                              @RequestParam("name") String name,
                                                              @RequestParam("age") int age){
        // Required false 없다면 필수값이라는 것
        // 대부분 () 생략할 수 있기 때문에 맞춰주는 편
        // 쿼리 파라미터로 보내자
        // 요청과 함께 전달하고자 하는 파라미터: 쿼리파라미터
        // client가 요청을 보낼 때 여러 개의 값을 나열해서 보냈을 때 @RequestParam통해 받을 수 있음
        log.info("RequestParam을 통해 얻어낸 값: {}, {}, {}", tag, name, age);

        List<LearningActivity> list = dashboard.filterByTag(tag);
        return ResponseEntity.ok()
                .header("Deprecation", "true") // 곧 없어질 엔드 포인트
                .header("Sunset", "Thu, 31 Dec 2026 23:59:59 GMT") // 이때까지만 유효하다
                .header("Link", // 대체를 할 수 있는 url은 headerValues
                        "<https://docs.sprintlog.example/guides/migration#search>; rel=\"deprecation\"")
                .body(list);
    }

    // -- 생성(POST) / 수정(PUT) / 삭제(DELETE) --

    @PostMapping // 요청 post
    public ResponseEntity<LearningActivity> create(@Valid @RequestBody CreateActivityRequest request){ // @Valid 없으면 CreateActivityController 안에 있는 유효성 검증 동작하지 않는다
        // client는 react로 이루어져 있어 그대로 전달하지 않고 JSON 형태로 전달, createActivityRequest 형태 전달하고 싶은데 변환할 수 있을까
        // Java->Json @ResponseBody
        // JSON->Java @RequestBody
        // 요청 본문에 들어있는 JSON을 자바로 변환
        LearningActivity activity=toActivity(request);
        repository.add(activity); // 원래는 서비스가 담당

        // 성공 시 201 Created + Location 헤더(생성된 자원의 주소)를 함께 응답
        URI location = URI.create("/api/activities" + activity.getId());
        return ResponseEntity.created(location).body(activity);
    }

    // 활동 수정, 자원 식별은 Path(/{id}) - 수정할 때는 어떤 객체를 변경할 것인지 지목해줘야 하기 때문
    // 변경할 내용은 본문 (UpdatedActivityRequest)
    // 대상이 없으면 404, 있으면 제목, 공개여부 변경하고 200
    @PutMapping("/{id}") // 빌드 수정 어렵기 때문에 바꾼다, 원래는 patchmapping이 어울리기는 하나 이미 빌드 완료된 프론트가 수정 요청을 보낼 때 putmapping 했기 때문에 바꾼 것
    public ResponseEntity<LearningActivity> update(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateActivityRequest request){
        LearningActivity activity = repository.findFirst(a -> a.getId() == id)
                .orElseThrow(() -> new ActivityNotFoundException(id));

        activity.changTitle(request.title());
        if (request.visibility()==Visibility.PUBLIC){
            activity.openToPublic();
        } else {
            activity.hideFromPublic();
        }
        repository.update(activity);
        return ResponseEntity.ok().body(activity);

    }

    // 활동 삭제, 성공 시 본문 없이 204 No Content, 대상이 없으면 404
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){ // 바디에 담는 데이터 없다, 전달하고자 하는 값 없기 때문에 Void 선언
        if (!repository.removeById(id)){ // false -> 해당 아이디를 찾지 못했다
            throw new ActivityNotFoundException(id);

        }
        return ResponseEntity.noContent().build();
    }

    // valid 검사 후 부르기 때문에 @valid 또 부를 필요 없다
    private LearningActivity toActivity(CreateActivityRequest request) {
        LearningActivity activity=switch (request.type()){
            case LECTURE -> new LectureLog(request.title(), request.minutes(), request.visibility(), request.instructorName());
            case PRACTICE -> new PracticeLog(request.title(), request.minutes(), request.visibility(), request.completionRate());
            case READING -> new ReadingLog(request.title(), request.minutes(), request.visibility(), request.bookTitle());
        };
        if (request.tags()!=null){
            request.tags().forEach(activity::addTag);
        }
        return activity;
    } // 나중에 서비스 로직으로 옮길 것
}
// 예전 방식
// 요즈음은 순수 html 방식 사용 적음
// client -요청-> WAS(가지고 있는 html로 사용자에게 응답) - 이는 사용자가 PC안의 browser를 이용했을 때는 유효, HTML, CSS, JS 에서는 유효
// 문제는 우리 서버로 요청을 보내는 client가 PC뿐만이 아니게 됨, 스마트폰, 태블릿... 모바일 시대가 오게 된 것
// 물론 모바일에서도 크롬 브라우저, 사파리 사용 가능 - 문제 없음
// Android, iOS 앱을 통한 요청이 들어오게 된 것
// 기존 서버를 유지하면서 모바일까지 대응할 수 없을까?
// Android - Java, 요즈음은 Kotlin 사용(구글이 밀어주기 시작)
// iOS - Swift
// html로 응답을 줄 수 밖에 없는데 모바일 애플리케이션은 html을 이해할 수 없음
// html은 브라우저에서만 해석이 가능한 것, 모바일 애플리케이션에서 해석할 수 있는 능력이 없음
// 요즈음은 모바일 시장이 훨씬 큼, 실제로 모바일 애플리케이션만 존재하는 서비스 많아짐
// html로만 응답을 주면 안 된다
// => 화면 구성에 필요한 데이터만 전달해주자‼️
// React, Vue 등장
// 데이터 제공해 모바일에서 알아서 구성하도록 함
// 자바 스프링에서 가공한 데이터를 iOS에서 이해하지 못하는 문제 발생
// => 공통 포맷 JSON으로 전달해 해결
// Json -> JS -> Rendering html
// Json -> Swift -> Rendering