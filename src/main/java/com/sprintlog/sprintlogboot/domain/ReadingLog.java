package com.sprintlog.sprintlogboot.domain;


import com.sprintlog.sprintlogboot.policy.Reviewable;
import com.sprintlog.sprintlogboot.policy.Shareable;

import java.io.Serializable;


public class ReadingLog extends LearningActivity implements Reviewable, Shareable, Serializable {    //추상클래스
    //부모가 가진 껍데기 메서드를 자식이 구현하지 않으면 에러->문법적으로 강음

    private static final long serialVersionUID=1L;// 'L' - Long 타입 알려줌
    // 자바는 그냥 정수를 int 취급
    //' l' 도 가능하지만 숫자 1과 헷갈릴 수 있기 때문에 'L' 권장
    // 이 파일의 클래스 구조가 현재 클래스와 같은지에 대한 버전 키 검사용 필드



    private String bookTitle;

    public ReadingLog(String title, int minutes, Visibility visibility, String bookTitle){
        super(title, minutes, visibility, ActivityCategory.READING);
        this.bookTitle=bookTitle;

    }


    @Override
    public boolean needsReview() {
        return getCategory().isShortStudy(getMinutes());
    }

    @Override
    public void printReviewTarget() {
        System.out.println("[복습 권장] " + getTitle() + " (" + bookTitle + ")");
    }

    @Override
    public boolean canShare() {
        return isPublicActivity();
    }

    @Override
    public String getSharTitle() {
        return getTitle();
    }

    //역할 담당하는 것이 인터페이스
    //인터페이스는 다중상속 가능 클래스는 단일 상속만 가능

    @Override
    public String getActivityType(){
        return "독서";
    }

    @Override
    public String getDetailText(){
        return "책: "+bookTitle;
    }

    public String getBookTitle(){
        return bookTitle;
    }
}
