package com.sprintlog.sprintlogboot.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Retention: 어노테이션의 유효기간
// RetentionPolicy.SOURCE: 대표적 오버라이드, 실제로 프로그램이 동작할 때는 사라진다 - 바이트 코드 변환될 때
// .CLASS: 바이트 코드로 변환될 때는 남아있지만 프로그램 동작할 때는 사라진다
// .RUNTIME: 실행할 때도 남아있어야 한다
// AOP는 app이 실행되는 도중에 동적으로 특정 메서드를 가로채서 기능을 추가하기 때문에 RUNTIME으로 선언
// 어노테이션을 붙일 수 있는 위치는 메서드다
// ElementType.FIELD: 변수에 붙일 수 있는 어노테이션
// PARAMETER: 매개변수에 붙일 수 있는 어노테이션
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogExecutionTime {
}
