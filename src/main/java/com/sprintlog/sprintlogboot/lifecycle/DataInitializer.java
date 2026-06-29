package com.sprintlog.sprintlogboot.lifecycle;

import com.sprintlog.sprintlogboot.config.SprintLogProperties;
import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.LearningActivity;
import com.sprintlog.sprintlogboot.domain.User;
import com.sprintlog.sprintlogboot.domain.Visibility;
import com.sprintlog.sprintlogboot.repository.ActivityRepository;
import com.sprintlog.sprintlogboot.repository.UserRepository;
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
    // 우리가 직접 UserRepository 빈 등록은 하지 않았지만, Spring Data JPA가 이미 구현체를 빈으로 등록해 놓았다
    private final UserRepository userRepository;

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

        if (repository.count() == 0) {
            repository.save(new LearningActivity(
                ActivityCategory.LECTURE, "Spring Bean Scope", 90, Visibility.PUBLIC, "이강사", null, null));
            repository.save(new LearningActivity(
                ActivityCategory.PRACTICE, "@PostConstruct 실습", 60, Visibility.PUBLIC, null, 85, null));
            repository.save(new LearningActivity(
                ActivityCategory.READING, "스프링 인 액션", 75, Visibility.PUBLIC, null, null, "스프링 인 액션 5판"));
            repository.save(new LearningActivity(
                ActivityCategory.LECTURE, "Prototype vs Singleton", 45, Visibility.PRIVATE, "이강사", null, null));
        }
        log.info("[lifecycle] 샘플 데이터 적재 완료 — 총 {}개", repository.count());

        if (userRepository.count()==0){ // Select Count(*) from users
            User sion = new User("오시온", "sion@icloud.com");
            userRepository.save(sion);
            // 직접 생성한 엔티티가 상속관계까지 있다면 포함, 생성할 때 선언했던 user 타입
            // 저장된 객체 리턴 가능
            User saved = userRepository.save(new User("구정모", "jungmo@gmail.com"));
            log.info("[lifecycle] User 저장 완료 - saved id={}, createdAt={}"
                , saved.getId(), saved.getCreatedAt());
        }

        log.info("[lifecycle] DB 사용자 수: {}명", userRepository.count());
    }

    // 객체 소멸하기 전에 자동으로 호출
    @PreDestroy
    public void shutdown() {
        log.info("[lifecycle] @PreDestroy — DataInitializer 가 종료 정리를 합니다.");
        // getTotalMinutes 대체할 메소드 없기 때문에 삭제
    }
}
