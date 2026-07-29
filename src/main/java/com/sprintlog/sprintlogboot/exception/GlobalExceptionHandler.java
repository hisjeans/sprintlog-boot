package com.sprintlog.sprintlogboot.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 공통 응답 조립 헬퍼 메서드
    private ProblemDetail problem(HttpStatus status, String code, String detail, String title) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setProperty("code", code);
        pd.setProperty("timestamp", Instant.now());
        return pd; // 모든 메서드에 공통으로 이루어지던 작업 분리하기 위함
    }

    // 통합 핸들러 - 우리의 도메인 예외(BusinessException을 상속받은 계열) 전부를 하나로
    // 더이상 핸들러가 status를 하드코딩하지 않는다
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException e) {
        ErrorCode ec = e.getErrorCode();
         // 4xx(클라이언트 문제)는 예상된 상황이니 WARN 레벨로 간결하게
        log.warn("[{}] {}", ec.getCode(), e.getMessage());
        return problem(ec.getStatus(), ec.getCode(), e.getMessage(), ec.getDefaultMessage());
    } // ActivityNotFound, InvalidException 처리

    // 스프링이 던지는 프레임워크 예외이기 때문에 BusinessException 상속은 어렵다
    @ExceptionHandler(MethodArgumentNotValidException.class) // Spring framework 예외 타입 수정 불가능, 각 타입에 맞게 관리 필요
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        // 1. 오류 결과를 담을 Map을 생성 (key: 필드명, value: 에러 메시지)
        Map<String, String> errors=new HashMap<>();

        // 최대한 변수 선언 없이 method chaining 이용해 호출하는 방식
        // 맵에 담지 않고 바로 넣는 방식
        e.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        ProblemDetail pd
                = problem(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_INPUT.getCode(), "요청 본문에 일부 필드가 유효하지 않습니다.", "입력 검증 실패");
        pd.setProperty("errors", errors);
        return pd;
// warn 레벨로 찍을  수 있다
    }

// 하드코딩 삭제 - handleBusiness 가 잡아주기 때문

    // 400 — JSON 자체가 깨졌거나 enum 에 없는 값 등, 요청 본문을 읽지 못함
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_INPUT.getCode(), // 예외 원인이 무엇인지 확인하기 위한 식별자 getCode() 활용
                "요청 본문(JSON)을 읽을 수 없습니다. 형식이나 값을 확인하세요.", "요청 본문 오류");
        // warn 레벨로 찍을 수 있다
    }

    // 400 — 경로 변수·쿼리 파라미터의 타입 불일치(예: /activities/abc). 프레임워크 예외 → C001
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_INPUT.getCode(),
            "요청 값 '" + e.getName() + "' 의 형식이 올바르지 않습니다.", "잘못된 요청 파라미터");
    }

    // 500 — 그 밖의 예상 못 한 오류. 원본 메시지는 로그에만, 클라이언트엔 안전한 문구만
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception e) {
        log.error("예상치 못한 서버 오류", e);
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        return problem(ec.getStatus(), ec.getCode(), ec.getDefaultMessage(), "서버 오류");
    }
}
