package com.sprintlog.sprintlogboot;

import com.sprintlog.sprintlogboot.domain.LectureLog;
import com.sprintlog.sprintlogboot.domain.PracticeLog;
import com.sprintlog.sprintlogboot.domain.ReadingLog;
import com.sprintlog.sprintlogboot.domain.Visibility;
import com.sprintlog.sprintlogboot.lifecycle.ImportBatch;
import com.sprintlog.sprintlogboot.printer.ActivityPrinter;
import com.sprintlog.sprintlogboot.repository.ActivityRepository;
import com.sprintlog.sprintlogboot.service.ActivityDashboard;
import com.sprintlog.sprintlogboot.service.ActivityReportService;
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
            List <ActivityPrinter> allPrinters,
            Map<String, ActivityPrinter> printersByName) {
			// 객체 생성
			// 의존성 주입 DI
			// 스프링이 Bean 등록된 객체 보내줌
			// 메서드가 호출될 때 필요로 하는 객체 스프링이 자동으로 호출
			// 맵 전달도 가능



		// 실제로 리턴하는 것이 CommandLineRunner, CommandLineRunner 구현체를 익명 클래스 람다식으로 작성
		// run 메서드를 구현
		return args -> {
			System.out.println();
			System.out.println("==================================================");
			System.out.println("  SprintLog Boot — Bean Scope 생명주기 시연");
			System.out.println("==================================================");

			// 1. DataInitializer 의 @PostConstruct 가 이미 실행됐는지 확인
			//    (이 메서드 실행 시점엔 이미 샘플 데이터가 있어야 함)
			System.out.println();
			System.out.println("── 1. CommandLineRunner 시작 시점의 Repository 상태 ──");
			System.out.println("  활동 수: " + repository.count() + "개 (← DataInitializer 가 미리 적재)");
			// repository singleton


			// 2. Singleton 검증 — 같은 ActivityRepository 를 두 번 꺼내면 동일 인스턴스인가?
			System.out.println();
			System.out.println("── 2. Singleton 검증 — ActivityRepository ──");
			ActivityRepository repo1 = context.getBean(ActivityRepository.class);
			ActivityRepository repo2 = context.getBean(ActivityRepository.class);
			System.out.println("  repo1 == repo2 ? " + (repo1 == repo2));
			System.out.println("  repo1.hashCode(): " + repo1.hashCode());
			System.out.println("  repo2.hashCode(): " + repo2.hashCode());
			// 같은 객체를 두 번 꺼내면 동일한 인스턴스일까?
			System.out.println("	Parameter repo: "+repository.hashCode());
			// 서버 실행시키면 주소값 모두 같음
			// ActivityRepository bean이 하나 등록되어 있고
			// 필요할 때마다 객체를 생성하는 게 아니라 spring이 초기화될 때 생성된 bean이 계속 사용되고 있는 것 확인 가능

			// 3. Prototype 검증 — ImportBatch 를 두 번 꺼내면 서로 다른 인스턴스일까?
			System.out.println();
			System.out.println("── 3. Prototype 검증 — ImportBatch ──");
			ImportBatch batch1 = context.getBean(ImportBatch.class);
			// 살짝 시간 차이를 두고 두 번째 인스턴스 생성
			Thread.sleep(10);
			ImportBatch batch2 = context.getBean(ImportBatch.class);
			System.out.println("  batch1: " + batch1);
			System.out.println("  batch2: " + batch2);
			System.out.println("  batch1 == batch2 ? " + (batch1 == batch2));


			/*
			5. 모든 구현체 한꺼번에 받기

			System.out.println();
			System.out.println("주입된 인스턴스 수: "+allPrinters.size());
			for (ActivityPrinter p : allPrinters) {
				System.out.println("   -"+p.getClass().getSimpleName());
			}


			// 6. Map<String, 객체>: Bean 이름 + 인스턴스 매핑
			System.out.println();
			printersByName.forEach((name, printer)->
					System.out.println("    -"+name+": "+printer.getClass().getSimpleName()));
 		*/

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