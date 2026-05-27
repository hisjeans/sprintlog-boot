package com.sprintlog.sprintlogboot.domain;

public enum Visibility {
    PUBLIC("공개",true),
    PRIVATE("비공개", false);//enum의 생성자 호출문
    //new LectureLog("title", 40, Visibility, PUBLIC,...) 외부에서 상수가 호출되었을 때 객체 생성
    //문자열은 실수할 확률이 높기 때문에 판단 기준으로 부적합
    //Visibility 타입 문법적 규제로 타입 안전성, 코드 간결성
    //public 공개, private 비공개 상수 타입, boolean타입 같이 관리하고 싶다면
    //enum도 일종의 클래스
    private final String label;
    private final boolean shareable;
    //final 변수의 값에 대한 마지막 변경
    //final 변수 반드시 초기화해야
    //private final String label="야호"; - 이렇게 설정하면 모든 label이 "야호"
    //라벨로 선언하자

    Visibility(String label, boolean shareable){
        this.label=label;
        this.shareable=shareable;
    }
    //label, shareable 외부에서 값 가져올 수 있도록 getter, setter
    public String getLabel(){
        return label;
    }

    public boolean isShareable() {
        return shareable;
    }
}

