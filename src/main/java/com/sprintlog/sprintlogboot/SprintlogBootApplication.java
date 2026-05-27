package com.sprintlog.sprintlogboot;

import com.sprintlog.sprintlogboot.domain.*;
import com.sprintlog.sprintlogboot.lifecycle.ImportBatch;
import com.sprintlog.sprintlogboot.printer.ActivityPrinter;
import com.sprintlog.sprintlogboot.repository.ActivityRepository;
import com.sprintlog.sprintlogboot.service.ActivityDashboard;
import com.sprintlog.sprintlogboot.service.ActivityReportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class SprintlogBootApplication {

	public static void main(String[] args) {

		SpringApplication.run(SprintlogBootApplication.class, args);
	}

	// 메서드가 리턴하는 객체를 Bean으로 등록하겠다
	// 내가 직접 만든 클래스는 직접 @Component 등을 사용해 빈 등록하면 되지만,
	// 라이브러러리 등에서 제공하는 객체는 메서드 안에서 객체를 생성한 후에 리턴되는 객체를 Bean으로 등록하기 위해 @Bean 사용
	// CommandLineRunner: Spring Boot가 구동이 완료되고, 딱 한 번 실행되는 코드를 정의할 때 사용하는 인터페이스
	// 주로 초기 데이터 적재나 설정 확인용으로 쓰임
	@Bean
	public CommandLineRunner demonstrateBeans(
            ApplicationContext context, // IOC container
            ActivityRepository repository,
            ActivityDashboard dashboard,
            ActivityReportService reportService,
            ActivityPrinter defaultPrinter, // 전달되는 printer는 condoleprinter가 될 것, compact printer로 바꾸고 싶다면 @Qualifier("compact")해야
            List <ActivityPrinter> allPrinters,
            Map<String, ActivityPrinter> printersByName,
			@Value("${sprintlog.welcome-message}") String welcomeMessage) {
			// 객체 생성
			// 의존성 주입 DI
			// 스프링이 Bean 등록된 객체 보내줌
			// 메서드가 호출될 때 필요로 하는 객체 스프링이 자동으로 호출
			// 맵 전달도 가능
			// 문자 하나라도 틀릴 경우 에러 발생하는 문제



		// 실제로 리턴하는 것이 CommandLineRunner, CommandLineRunner 구현체를 익명 클래스 람다식으로 작성
		// run 메서드를 구현
		return args -> {
			System.out.println();
			System.out.println("==================================================");
			System.out.println("  SprintLog Boot — 외부 설정 시연");
			System.out.println("==================================================");


			System.out.println();
			// System.out.println(welcomeMessage);
			// 숨길 수 있는 방법이 지우는 것 말고 없을까?
			// 예: 데이터 아이디, 비밀번호 같은 값들을 숨기고 싶다, 외부에서 세팅하고 값을 가져오고 싶다
			// => yml 파일과의 연동
			System.out.println("── Repository 상태 (Profile 별 Initializer 가 결정) ──");
			System.out.println("  활동 수: " + repository.count() + "개");
			for (LearningActivity activity : repository.findAll()) {
				defaultPrinter.print(activity);
			}


			System.out.println();
			System.out.println("==================================================");
			System.out.println("  Bean 시연 완료 — 톰캣은 계속 8080 에서 동작 중");
			System.out.println("==================================================");
			System.out.println();
		};
	}

}
// 객체 생성문 없는 이유: Bean으로 등록되어 있기 때문 -> Spring에게 annotation으로 명령을 내려준 것
// 클래스만 있다고 메서드 바로 호출할 수 없음
// 어디에도 "new" 객체 직접 생성한 적 없음

// 자바는 객체의 소멸주기를 garbagecollector가 자동으로 동작
// 가장 쉽게 확인하는 방법, 서버 내리기
// 서버를 내리면 bean들도 전부 세팅되어 있기 때문