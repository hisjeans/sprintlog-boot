package com.sprintlog.sprintlogboot.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect // 이 클래스가 횡단 관심사
@Component
@Slf4j
public class LoggingAspect {

    // 1. Pointcut (어디서 사용할 것인가?)
    // execution([수식어] 리턴타입 [클래스경로.]메서드이름(파라미터) [예외]) - []는 생략 가능한 문법
    // @Pointcut("execution(* com.codeit.springwebbasic.member.controller.MemberController.*(..))")
    // 특정 클래스에 적용 가능
    // 모든 접근 제한자 허용, 모든 리턴타입 허용, MemberController 안에 있는 모든 메서드를 대상(매개값은 모든 파라미터)
    // @Pointcut("execution(* com.codeit.springwebbasic..*.*(..))")
    // ..: 0개 이상의 하위 패키지를 의미 -> 모든 하위 패키지를 전부 지목하고 싶을 때


//    @Pointcut("execution(* com.codeit.springwebbasic.member.controller.MemberController.*(..))")
//    private void allControllerMethods() {
//        // 위에서 지정한 (어디에?) 라는 메서드 위치에 사전에 지정해야 할 여러 설정, 사전 작업 등을 명시합니다.
//        // @Pointcut을 생략하고, @Around에 바로 execution을 작성해도 됩니다.
//        System.out.println("allControllerMethods 호출!");
//    }

    // 목적: 횡단 관심사를 어느 곳에 적용할지 @Pointcut 문법을 통해 알려준다
    // '부가기능을 어디에 적용할 것인가' 에 대한 대상을 지정
    // 여러 클래스에 전부 적용하거나 특정 메서드에 특정 타입으로도 변환 가능: (ResponseEntity<ActivityResponse>..
    // 메서드 이름 선언 가능, 특정 메서드에서만 동작하도록 메서드 이름 지정 가능
    // 매개변수 선언 방식도 가능
    // 메서드 리턴 타입은 자세하게 작성해야 한다 (org.springframework.http.ResponseEntity..
    // *: 메서드의 반환 타입은 무엇이든 상관 없다, 컨트롤러가 리턴
    // com.sprintlog.sprintlogboot.controller..: 이 패키지 및 그 하위의 모든 패키지(..)에 있는 클래스를 대상으로 한다
    // *(..): 메서드 이름은 무엇이든 상관 없고 *, 매개변수의 개수나 타입도 무엇이든 상관 없다(..)
    @Pointcut("execution(* com.sprintlog.sprintlogboot.controller..*(..))")
    public void controllerLayer() {}

    // ProceeingJoinPoint: AOP가 가로챈 실제 실행될 메서드의 정보를 담고 있는 객체
    // getById 전에 호출되어 ProceedingJoiniPoint 객체 전달
    // 대상 메서드가 실행되기 전, 후 전체를 가로채서 제어할 수 있는 어노테이션
    // 원래 controller 로직 실행 전, 후 제어할 수 있다 - annotation 이용해 전, 후 지정 가능
    // @Around: 전, 후 전부 제어할 수 있는 가장 강력한 제어
    @Around("controllerLayer()")
    public Object logAndMeasure(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        // 메서드 이름 추출 - 짧은 문자열로 달라고 하지 않으면 패키지 경로 전부 노출되기 때문에 메서드 이름만 짧게 호출되도록 toShortString
        // 패키지 경로, 클래스 이름까지 얻고 싶다면 getName()
        Object[] args = joinPoint.getArgs(); // 메서드로 전달된 매개값(인자) 추출

        long start = System.currentTimeMillis();

        log.info("요청 시작: {}, 인자={}", method, Arrays.toString(args));

        Object result = joinPoint.proceed();// 원래 메서드 실행, 핵심 기능 실행, 핵심 메서드 실행되어야 시간 측정할 것
        // ⚠️ AOP 가 가로챈 상태이기 때문에 proceed 호출시켜야 원본 로직이 돌아간다

        long end=System.currentTimeMillis();
        log.info("요청 완료: {} ({}ms)", method, end-start);

        return result; // 원본 메서드가 반환하는 값을 그대로 클라이언트에게 리턴

   }

    // throwing = "ex" : 메서드가 던진 예외를 ex 파라미터로 받는다. (예외가 발생할 때만 실행됨)
    @AfterThrowing(pointcut = "controllerLayer()", throwing = "ex")
    public void afterServiceThrows(JoinPoint joinPoint, Throwable ex) {
        log.warn("[@AfterThrowing] {} 예외 발생: {}",
                joinPoint.getSignature().toShortString(), ex.getMessage());
    } // 나중에 수정할 것


    // @Before: 원본 메서드가 실행되기 직전까지만 실행된다
    // joinPoint.proceed()를 다로 호출하지 않는다

    // @AfterReturning: 메서드 정상 호출 종료 이후 실행할 내용

    // 이 두 개의 기능을 한꺼번에 아우를 수 있는 어노테이션이 @Around

}
