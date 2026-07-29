package com.sprintlog.sprintlogboot.exception;

// 이 예외는 애초에 예외 처리를 강제하기 위한 타입이라 BusinessException을 상속하기에는 적합하지 않다
public class ActivityArchiveException extends Exception { // 롤백이 안 된다는 것 확인하기 위해 Exception 상속

  public ActivityArchiveException(String message) {
    super(message);
  }
}
