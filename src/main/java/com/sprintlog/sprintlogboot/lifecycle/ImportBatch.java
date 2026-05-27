package com.sprintlog.sprintlogboot.lifecycle;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
// @Scope("prototype") 으로도 선언가능하나 문자열 오타 가능성
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
// 수명주기, 범위 지정
// getBean() 생성될 때마다 매번 새 객체 지정
// 프로토타입은 정말 필요할 때만 사용, 빈도 적음
// 대부분 bean 등록 객체는 singleton 많이 사용
public class ImportBatch {

    private final String batchId;
    private final LocalDateTime startedAt;

    public ImportBatch() {
        this.batchId = UUID.randomUUID().toString().substring(0,8);
        this.startedAt = LocalDateTime.now();
    }

    public String getBatchId() {
        return batchId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    @Override
    public String toString() {
        return "ImportBatch{" +
                "batchId='" + batchId + '\'' +
                ", startedAt=" + startedAt +
                '}';
    }
}
