package com.sprintlog.sprintlogboot.config;

// 관련된 키 묶음을 타입 안전한 객체로 받는

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sprintlog")
@Getter
@Setter
public class SprintLogProperties {

    private String welcomeMessage;
    // yml 파일에서 카멜 케이스로 작성했지만 '-' 빼고 작성해야

    private SampleData sampleData=new SampleData();

    // 중첩된 설정 - 내부 클래스를 하나 선언해서 표현
    // sampele-data 안에 들어가있기 때문에
    @Getter @Setter
    public static class SampleData{
        private boolean enabled;
        private int count;
    }


}
