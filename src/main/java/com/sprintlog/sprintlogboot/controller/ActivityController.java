package com.sprintlog.sprintlogboot.controller;

import com.sprintlog.sprintlogboot.domain.*;
import com.sprintlog.sprintlogboot.dto.request.UpdateActivityRequest;
import com.sprintlog.sprintlogboot.exception.ActivityNotFoundException;
import com.sprintlog.sprintlogboot.repository.ActivityRepository;
import com.sprintlog.sprintlogboot.dto.request.CreateActivityRequest;
import com.sprintlog.sprintlogboot.service.ActivityDashboard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/api/activities") // 컨트롤러 쪽에 공통 url 매핑, 기본적으로 "/api/activities" 으로 시작하도록 지정
public class ActivityController {
// if) repository, dashboard가 없었다면 직접 작성해야 했을 것`
    private final ActivityRepository repository;
    // ActivityController는 ActivityRepository에게 의존
    // 저장된 정보 불러와 리턴해야 하기 때문
    private final ActivityDashboard dashboard; // 의존성 관계 추가

    // @ResponseBody 이 메서드가 리턴하는 문자값이 json이라는 파일 형태로 직접 던져줌
    // 요청에 대한 데이터만 운반할 것
    // 메서드 마다 붙일 필요 없이 @RestController 이용하자
    // /hello 란 요청이 get 메서드 받았을 때
    @GetMapping("/hello")
    public String hello(){
        log.info("ActivityController.hello() 호출!");
        return "home";
        // 사용자 요청 -> Dispatcher Servlet이 먼저 받음
        // -> Handler Adaptor, Handler Mapping이 컨테이너에서 bean, 메서드 찾음
        // -> controller에게 요청 넘김
    }


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

    @GetMapping("/{id}") // "id" <- 1,2,3,4 요청을 보내는 쪽에서 보내는 데이터
    public ResponseEntity<LearningActivity> getById(@PathVariable Long id) { // 메서드 내부에서 id 변수로 사용할 수 있도록 설정
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
        // 대부분 () 생략할 수 있기 때문에 맞춰주는 편
        // 쿼리 파라미터로 보내자
        // 요청과 함께 전달하고자 하는 파라미터: 쿼리파라미터
        // client가 요청을 보낼 때 여러 개의 값을 나열해서 보냈을 때 @RequestParam통해 받을 수 있음
        log.info("RequestParam을 통해 얻어낸 값: {}, {}, {}", tag, name, age);

        List<LearningActivity> list = dashboard.filterByTag(tag);
        return ResponseEntity.ok().body(list);
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
    @PatchMapping("/{id}")
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