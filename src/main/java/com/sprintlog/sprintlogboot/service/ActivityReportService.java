package com.sprintlog.sprintlogboot.service;

import com.sprintlog.sprintlogboot.domain.LearningActivity;
import com.sprintlog.sprintlogboot.policy.Reviewable;
import com.sprintlog.sprintlogboot.printer.ActivityPrinter;
import com.sprintlog.sprintlogboot.repository.ActivityRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityReportService {

    private final ActivityPrinter printer;
    private final ActivityRepository repository;

    public ActivityReportService(@Qualifier("console") ActivityPrinter printer, ActivityRepository repository){
        if(printer==null){
            throw new IllegalArgumentException("출력 도구는 반드시 필요합니다");
        }
        if (repository==null){
            throw new IllegalArgumentException("Repository는 null일 수 없습니다.");
        }
        this.printer=printer;
        this.repository=repository;
    }

    // 저장된 모든 활동 출력
    public void printAll(){
        for (LearningActivity activity : repository.findAll()) {
            printer.print(activity);
        }
    }

    // 복습이 필요한 활동만 출력
    public void printNeedsReview(){
        List<LearningActivity> activities=repository.findAll();
        for (LearningActivity activity : activities) {
            if(activity instanceof Reviewable r && r.needsReview()){
                // Reviewable 인터페이스 타입 가질 수 있는지, r의 needsReview 호출했을 때 복습 필요한 활동 맞는지
                r.printReviewTarget();
            }
        }
    }
}
