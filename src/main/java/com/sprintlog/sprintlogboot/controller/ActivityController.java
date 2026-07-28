package com.sprintlog.sprintlogboot.controller;

import com.sprintlog.sprintlogboot.aspect.LogExecutionTime;
import com.sprintlog.sprintlogboot.domain.*;
import com.sprintlog.sprintlogboot.dto.request.UpdateActivityRequest;
import com.sprintlog.sprintlogboot.dto.response.ActivityResponse;
import com.sprintlog.sprintlogboot.dto.request.CreateActivityRequest;
import com.sprintlog.sprintlogboot.dto.response.AuditLogResponse;
import com.sprintlog.sprintlogboot.dto.response.PagedResponse;
import com.sprintlog.sprintlogboot.dto.response.SliceResponse;
import com.sprintlog.sprintlogboot.service.ActivityDashboard;
import com.sprintlog.sprintlogboot.service.ActivityService;
import com.sprintlog.sprintlogboot.service.FileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

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

    private final ActivityDashboard dashboard; // 의존성 관계 추가, 상태 보여주는 역할
    private final FileService fileService;
    private final ActivityService activityService; // activity 관련 비즈니스 로직 담당
    // 컨트롤러 서비스와는 무관한 역할


    // 모든 활동 목록(페이징)
    @GetMapping // 요청 들어오면 get 메서드 세팅해줄 것
    public ResponseEntity<PagedResponse<ActivityResponse>> getAll(
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long ownerId
    ){
        // 게시판에 처음 들어왔을 때 데이터 전달되지 않을 가능성 크기 때문에 기본 값 설정

        // 기존 EntityModel을 ActivityResponse로 감싸서 전달
        Page<LearningActivity> result
            = activityService.page(sort, page, size, ownerId);

        // 원본 리스트를 꺼낼 때는 getContent를 통해서 꺼낼 수 있다.
        List<ActivityResponse> content = result.getContent().stream()
            .map(ActivityResponse::from)
            .toList();

        // 페이지 정보들까지 함께 담을 수 있는 PagedResponse를 사용해서 응답
        return ResponseEntity.ok().body(new PagedResponse<>(content, result.getNumber(), result.getSize(),
            result.getTotalElements(), result.getTotalPages()));

    }


    @GetMapping("/slice")
    public ResponseEntity<SliceResponse<ActivityResponse>> slice(
        @RequestParam(defaultValue = "PUBLIC") Visibility visibility,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size // 공개된 활동들만 받기 위해 페이징 처리

    ){
        Slice<LearningActivity> result = activityService.sliceByVisibility(visibility,
            page, size);
        List<ActivityResponse> content = result.getContent().stream()
            .map(ActivityResponse::from)
            .toList();// 원본 리스트 포장

        return ResponseEntity.ok().body(new SliceResponse<>(content, result.getNumber(), result.getSize(), result.hasNext()));

    }



    @GetMapping("/{id}")
    @LogExecutionTime
    public ResponseEntity<EntityModel<ActivityResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok().body(toModel(activityService.get(id)));
    }

    // 카테고리별로 그룹화된 활동 목록
    @GetMapping("/dashboard")
    public ResponseEntity<Map<ActivityCategory, List<LearningActivity>>> getDashboard(){
        Map<ActivityCategory, List<LearningActivity>> map = dashboard.groupByCategory();
        return ResponseEntity.ok().body(map);
    }

    // 활동 수 요약 정보 (전체 / 강의 / 실습 / 독서) -> ActivityDashboard
    @RequestMapping(value = "/summary", method = RequestMethod.GET)
    public ResponseEntity<ActivityDashboard.Summary> getSummary(){
        return ResponseEntity.ok().body(dashboard.summarize());
    }

    // --------------------------------------------------------------------------------------------------------------------

    // 변경 작업: -- 생성(POST) / 수정(PUT) / 삭제(DELETE) --
    // 일반적 조회 로직을 위에 두고 생성, 수정, 삭제를 아래에 두는 것 선호

    @PostMapping // 요청 post
    public ResponseEntity<EntityModel<ActivityResponse>> create(
        @Valid @RequestPart("data") CreateActivityRequest request,
        @RequestPart(value = "file", required = false) MultipartFile file // 첨부파일 여러 개 받아야 한다면 list로 받는다
    ){ // @Valid 없으면 CreateActivityController 안에 있는 유효성 검증 동작하지 않는다
        // client는 react로 이루어져 있어 그대로 전달하지 않고 JSON 형태로 전달, createActivityRequest 형태 전달하고 싶은데 변환할 수 있을까
        // Java->Json @ResponseBody
        // JSON->Java @RequestBody - 이제는 파일도 넘어온다 => @RequestPart
        // 요청 본문에 들어있는 JSON을 자바로 변환

        String savedFileName = null; // 기존에 존재하는 변수에 파일명 넣도록 수정
        if(file!=null && !file.isEmpty()){
            savedFileName = fileService.saveFile(file);
        } // 데이터베이스에는 파일명 저장

        LearningActivity saved = activityService.create(request, savedFileName);// 컨트롤러가 DTO를 Entity로 바꾸지 않도록 한다

        // 성공 시 201 Created + Location 헤더(생성된 자원의 주소)를 함께 응답
        URI location = URI.create("/api/activities" + saved.getId()); // 기존과 달리 데이터가 insert될 때, 자동으로 아이디 세팅되기 때문에 변경
        return ResponseEntity.created(location).body(toModel(saved));
    }

    // 활동 수정, 자원 식별은 Path(/{id}) - 수정할 때는 어떤 객체를 변경할 것인지 지목해줘야 하기 때문
    // 변경할 내용은 본문 (UpdatedActivityRequest)
    // 대상이 없으면 404, 있으면 제목, 공개여부 변경하고 200
    @PutMapping("/{id}") // 빌드 수정 어렵기 때문에 바꾼다, 원래는 patchmapping이 어울리기는 하나 이미 빌드 완료된 프론트가 수정 요청을 보낼 때 putmapping 했기 때문에 바꾼 것
    public ResponseEntity<EntityModel<ActivityResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody UpdateActivityRequest request){
        // 컨트롤러의 역할에 맞게 수정
        return ResponseEntity.ok().body(toModel(activityService.update(id, request)));
    }

    // 활동 삭제, 성공 시 본문 없이 204 No Content, 대상이 없으면 404
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){ // 바디에 담는 데이터 없다, 전달하고자 하는 값 없기 때문에 Void 선언
        activityService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- 응답 DTO + HATEOAS 링크 만들기 (필수는 아님) -------------------------------------------------------------------
    private EntityModel<ActivityResponse> toModel(LearningActivity activity){
        // 활동 객체 하나를 응답 DTO + 링크 변환
        long id = activity.getId();
        return EntityModel.of(
                ActivityResponse.from(activity),
                linkTo(methodOn(ActivityController.class).getById(id)).withSelfRel(),
                // HATEOAS에서 제공하는 linkTo 메서드
                // 이 데이터에 상세 정보를 보고 싶다면 getById를 호출해야 하는데 이 메서드를 호출할 수 있는 자신을 참조할 수 있는 링크를 붙여주겠다
                linkTo(ActivityController.class).withRel("activities"), // ActivityController에 activities 요청을 보내면
                linkTo(methodOn(ActivityTagController.class).getTags(id)).withRel("tags") // AcitivityTagController 클래스에 있는 getTags를 "tags" 이름
        );
    }

    @GetMapping("/find") // Restful 하지 못한 url 작성법
    public ResponseEntity<List<ActivityResponse>> find(
        @RequestParam(required = false) ActivityCategory category,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer minMinutes
    ){
        List<ActivityResponse> dtoList=activityService.search(category, keyword, minMinutes);
        return ResponseEntity.ok().body(dtoList);
    }

    @GetMapping("/with-details")
    public ResponseEntity<List<ActivityResponse>> getAllWithDetails(){
        List<ActivityResponse> list = activityService.withDetails().stream()
            .map(a -> ActivityResponse.from(a))
            .toList();

        return ResponseEntity.ok().body(list);
    }



    // 활동 변경 내역 조회(최근순)
    @GetMapping("/history")
    public ResponseEntity<List<AuditLogResponse>> history() { // 조회 로직
        List<AuditLogResponse> list = activityService.history().stream()
            .map(AuditLogResponse::from)
            .toList();
        return ResponseEntity.ok().body(list);

    }

    // 트랜잭션 원자성 시연 - 활동 등록 (활동 저장 + 이력 기록)을 한 트랜잭션
    // 둘 중 하나라도 실패하면 롤백
    // fail이란 값을 false로 주면 둘 중 하나라도 실패하면 롤백되는지 확인하겠다는 의미
    @PostMapping("/demo-atomic")
    public ResponseEntity<String> demoAtomic(@RequestParam(defaultValue = "false") boolean fail) {
        activityService.demoAtomicRegister(fail); // faile = true면 예외를 일부러 발생 -> 롤백
        return ResponseEntity.ok().body("활동과 이력이 한 트랜잭션으로 저장되었습니다.");
    }

    @PostMapping("/demo-propagation")
    public ResponseEntity<String> demoPropagation(@RequestParam(defaultValue = "false") boolean fail) {
        activityService.demoPropagation(fail); // faile = true면 예외를 일부러 발생 -> 롤백
        return ResponseEntity.ok().body("활동 등록을 시도했습니다. (시도 이력은 별도 트랜잭션으로 남습니다.)");
    }

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