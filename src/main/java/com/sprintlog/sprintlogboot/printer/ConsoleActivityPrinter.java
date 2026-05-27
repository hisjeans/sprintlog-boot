package com.sprintlog.sprintlogboot.printer;

import com.sprintlog.sprintlogboot.domain.LearningActivity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// spring container에 bean으로 등록
// ReportBuilder <- ActivityPrinter 필요 인지, console printer 스프링이 자동으로 찾아 생성
@Component("console")
@Primary
public class ConsoleActivityPrinter implements ActivityPrinter {

    @Override
    public void print(LearningActivity activity){
        System.out.println(
                "[" + activity.getActivityType() + "]"
                        + " #" + activity.getId()
                        + " " + activity.getTitle()
                        + " - " + activity.getMinutes() + "분"
                        + " - " + activity.getDetailText()
                        + " - " + activity.getVisibilityText() + " 🙏"
        );
    }
}
