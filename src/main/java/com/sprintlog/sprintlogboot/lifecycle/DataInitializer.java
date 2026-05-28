package com.sprintlog.sprintlogboot.lifecycle;

import com.sprintlog.sprintlogboot.config.SprintLogProperties;
import com.sprintlog.sprintlogboot.domain.LectureLog;
import com.sprintlog.sprintlogboot.domain.PracticeLog;
import com.sprintlog.sprintlogboot.domain.ReadingLog;
import com.sprintlog.sprintlogboot.domain.Visibility;
import com.sprintlog.sprintlogboot.repository.ActivityRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j // log 라는 이름으로 SLF4J 로거 자동 생성
public class DataInitializer {

    private final ActivityRepository repository;
    private final SprintLogProperties properties;


    // 생성자 삭제 - 로직 자체가 전달받아 필드에 세팅하는 것밖에 없기 때문에 lombok으로 대체 가능

    // 객체 생성될 때 자동으로 생성되면 좋겠다
    // 객체가 건설되고 난 후 자동으로 호출
    // 주입된 의존성 객체를 가지고 무언가 해야 할 로직을 작성
    @PostConstruct
    public void loadSampleData() {

        log.info("[lifecycle] @PostConstruct — {}", properties.getWelcomeMessage()); // 로그 객체 제공하는 메서드에서는 '{}' 사용 <- ', ' 값 전달 작성
        System.out.println("테스트"); // 문자열, 변수값 연결하기 위해 '+' 사용
        // log.info >>정보>> System.out.println, log.info는 파일로 저장할 수 있음

        if (!properties.getSampleData().isEnabled()){
            log.info("[lifecycle] sample-data.enabled=false - 적재 건너뜀!");
            return;
        }

        log.info("[lifecycle] @PostConstruct — DataInitializer 가 샘플 데이터를 적재합니다.");

        repository.add(new LectureLog("Spring Bean Scope", 90, Visibility.PUBLIC, "이강사"));
        repository.add(new PracticeLog("@PostConstruct 실습", 60, Visibility.PUBLIC, 85));
        repository.add(new ReadingLog("스프링 인 액션", 75, Visibility.PUBLIC, "스프링 인 액션 5판"));
        repository.add(new LectureLog("Prototype vs Singleton", 45, Visibility.PRIVATE, "이강사"));

        log.info("[lifecycle] 샘플 데이터 적재 완료 — 총 {}개", repository.count());

    }

    // 객체 소멸하기 전에 자동으로 호출
    @PreDestroy
    public void shutdown() {
        log.info("[lifecycle] @PreDestroy — DataInitializer 가 종료 정리를 합니다.");
        log.info("[lifecycle] 최종 활동 수: {}개, 총 학습 시간: {}분"
                , repository.count(), repository.getTotalMinutes());

    }
}
