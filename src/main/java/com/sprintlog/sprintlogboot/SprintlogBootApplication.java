package com.sprintlog.sprintlogboot;

import com.sprintlog.sprintlogboot.domain.LectureLog;
import com.sprintlog.sprintlogboot.domain.PracticeLog;
import com.sprintlog.sprintlogboot.domain.ReadingLog;
import com.sprintlog.sprintlogboot.domain.Visibility;
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
            ActivityReportService reportService) {
			// 객체 생성
			// 의존성 주입 DI
			// 스프링이 Bean 등록된 객체 보내줌
			// 메서드가 호출될 때 필요로 하는 객체 스프링이 자동으로 호출




		// 실제로 리턴하는 것이 CommandLineRunner, CommandLineRunner 구현체를 익명 클래스 람다식으로 작성
		// run 메서드를 구현
		return args -> {
			System.out.println();
			System.out.println("==================================================");
			System.out.println("  SprintLog Boot — Bean 시연");
			System.out.println("==================================================");

			// 1. 우리가 만든 Bean 이 컨테이너에 등록됐는지 확인
			System.out.println();
			System.out.println("── 1. com.sprintlog 패키지의 Bean 목록 ──");
			String[] allBeanNames = context.getBeanDefinitionNames(); // 등록된 bean들의 이름을 출력
			for (String name : allBeanNames) {
				Object bean = context.getBean(name);
				String beanClassName = bean.getClass().getName();
				if (beanClassName.startsWith("com.sprintlog")) {
					System.out.println("  • " + name + " → " + beanClassName);
				}
			}

			// 2. DI 가 동작하는지 — Repository 에 활동 추가 후 Dashboard 로 출력
			System.out.println();
			System.out.println("── 2. Repository 에 샘플 활동 3개 추가 ──");
			repository.add(new LectureLog("Spring IoC 컨테이너", 90, Visibility.PUBLIC, "김강사"));
			repository.add(new PracticeLog("Bean 등록 실습", 45, Visibility.PUBLIC, 85));
			repository.add(new ReadingLog("토비의 Spring", 60, Visibility.PRIVATE, "토비의 Spring 3.1"));
			System.out.println("  총 활동 수: " + repository.count() + "개");
			System.out.println("  총 학습 시간: " + repository.getTotalMinutes() + "분");

			// 3. Dashboard 의 카테고리별 그룹화
			System.out.println();
			System.out.println("── 3. Dashboard.groupByCategory() (TreeMap 정렬) ──");
			dashboard.groupByCategory().forEach((category, activities) ->
					System.out.println("  " + category.getLabel() + ": " + activities.size() + "개"));

			// 4. ReportService 로 전체 출력
			System.out.println();
			System.out.println("── 4. ReportService.printAll() (ConsoleActivityPrinter 주입) ──");
			reportService.printAll();

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