package com.sprintlog.sprintlogboot;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SprintlogBootApplication {

	public static void main(String[] args) {

		SpringApplication.run(SprintlogBootApplication.class, args);
	}

}
// 객체 생성문 없는 이유: Bean으로 등록되어 있기 때문 -> Spring에게 annotation으로 명령을 내려준 것
// 클래스만 있다고 메서드 바로 호출할 수 없음
// 어디에도 "new" 객체 직접 생성한 적 없음

// 자바는 객체의 소멸주기를 garbagecollector가 자동으로 동작
// 가장 쉽게 확인하는 방법, 서버 내리기
// 서버를 내리면 bean들도 전부 세팅되어 있기 때문