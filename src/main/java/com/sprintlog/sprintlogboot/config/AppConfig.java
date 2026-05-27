package com.sprintlog.sprintlogboot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// 일반 @Componant로 bean 등록하면 메서드가 특정 상황에서는 동작이 안 될 때도 있음
// 호출할 때마다 달라짐, 매번 새 인스턴스 리턴되는 문제
// => @Configuration, 프록시 항상 동일한 객체 생성하게 해줌
// 이 클래스는 Bean 정의 모음 설정 클래스임을 표시
// 명시적으로 설정 클래스임을 보여줌
// 나중에 property가 여러개가 될 수 있음 그때마다 각각의 component에 붙이는 것 보다 한곳에서 관리해주자
@Configuration
@EnableConfigurationProperties(SprintLogProperties.class)
public class AppConfig {
    // 리턴되는 Clock 객체 bean으로
    // 클래스에 bean 등록하면 클래스 이름으로 등록되고 메서드에 등록하면 메서드 이름으로 등록됨
    // 반환 객체의 타입이 Bean 타입(Clock), 다른 곳에서 Clock clock 으로 주입받으면 이 객체가 들어옴
    @Bean
    public Clock systemClock(){
        return Clock.systemDefaultZone();
    }


}

// LocalDateTime.now();
// - 호출했을 당시의 메서드 가져옴
// - 테스트 환경에서 호출하면 오늘은 통과하는데 내일은 통과하지 못할 수 있음
// - 시간 고정되지 않는 문제
// Clock 객체 의존성으로 받으면 통제 가능
// 외부 통제로 시간 변경 가능
// 외부에서 시간 관리 권장
