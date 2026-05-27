package com.sprintlog.sprintlogboot.domain;


import com.sprintlog.sprintlogboot.policy.Reviewable;
import com.sprintlog.sprintlogboot.policy.Shareable;

import java.io.Serializable;


public class PracticeLog extends LearningActivity implements Reviewable, Shareable, Serializable {

    private static final long serialVersionUID=1L;// 'L' - Long 타입 알려줌
    // 자바는 그냥 정수를 int 취급
    //' l' 도 가능하지만 숫자 1과 헷갈릴 수 있기 때문에 'L' 권장
    // 이 파일의 클래스 구조가 현재 클래스와 같은지에 대한 버전 키 검사용 필드


    private static final int MINIMUM_COMPLETION_RATE=70;


    private int completionRate; //PracticLog만 가지는 고유한 필드 1개 사용 위치

    public PracticeLog(String title, int minutes, Visibility visibility, int completionRate) {
        super(title, minutes, visibility, ActivityCategory.PRACTICE);
        //completionRate는 PracticeLog만 받을 수 있는 정보
        this.completionRate=normalizeCompletionRate(completionRate);

    }


    @Override
    public boolean needsReview() {
        return getCategory().isShortStudy(getMinutes())||completionRate<MINIMUM_COMPLETION_RATE;

    }

    @Override
    public void printReviewTarget(){
        System.out.println("[복습 권장] " + getTitle() + " (완료율: " + completionRate + "%)");
    }

    public int getCompletionRate() {

        return completionRate;
    }

    private int normalizeCompletionRate(int completionRate){
        if(completionRate<0){
            return 0;
        }

        if(completionRate>100){
            return 100;
        }

        return completionRate;
    }


    @Override
    public boolean canShare() {
        return isPublicActivity();
    }

    @Override
    public String getSharTitle() {
        return getTitle();
    }
    //오버로딩!=오버라이딩

    @Override
    public String getActivityType(){
        return "실습";
    }

    @Override
    public String getDetailText(){
        return "완료율"+completionRate+"%";
    }
}
