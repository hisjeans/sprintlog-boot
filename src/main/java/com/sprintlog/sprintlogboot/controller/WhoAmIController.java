package com.sprintlog.sprintlogboot.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WhoAmIController {

  @GetMapping("/whoami")
  public Map<String, String> whoami(){
    return Map.of("host", System.getenv().getOrDefault("HOSTNAME", "unknown"), "test", "test");
  } // 변화주어 컨테이너가 이미지에 돌아가는지 확인
}
