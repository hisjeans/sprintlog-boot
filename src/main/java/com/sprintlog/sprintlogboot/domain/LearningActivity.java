package com.sprintlog.sprintlogboot.domain;

import com.sprintlog.sprintlogboot.exception.InvalidActivityException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;

import java.util.Set;

//객체 생성을 위한 설계도 클래스에는 main 메서드를 작성하지 않음
@Getter
@Entity
@Table(name = "activities")
public class LearningActivity extends BaseEntity{ // 자식 클래스 제거 후 entity로 사용하기 위해 abstract 제거

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int minutes;

    // Enum을 DB에 어떻게 넣을지를 정의 (STRING: 상수를 문자열로 변환 >>직관적>> ORDINAL: 상수의 순서 숫자로 변환, 사용하기는 하나 나중에 데이터베이스에 이미 들어가 있는 1번들 바꿔야 하는 문제 발생 가능)
    // - PUBLIC, PRIVATE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityCategory category;

    // 자식 객체 지우기 때문에 작성 필요, 해당 종료일 때만 채ㅜ어진다
    @Column(length = 50)
    private String instructorName; // LECTURE 전용

    private Integer completionRate; // PRACTICE

    @Column(length = 200)
    private String bookTitle; // READING

    @Column(length = 100)
    private String attachmentFileName; // 첨부 파일의 파일명(UUID), 필수가 아니기 때문에 null을 허용
    // 데이터베이스에는 이름만 저장하겠다, 실제 데이터는 로컬에 저장하겠다

    // 컬렉션 자료형을 별도의 테이블로 매핑, 테이블 이름은 activity_tags, 활동 테이블과 조인할 수 있는 외래 키 이름은 activity_id
    // ElementCollection: 활동 객체를 조회할 때 tag의 조회 방식
    // FetchType.EAGER: 활동 객체 조회 시 무조건 tags를 조인해서 같이 가져온다 (그렇게 선호하지는 않음)
    // FetchType.LAZY: 활동 객체 조회 시 일단 tags는 안 가져온다 내가 직접 tags를 지목하면 그때 select 통해 가져온다
    // 실무는 LAZY를 더 선호, 필요할 때 가져오는 것을 더 효율적으로 생각하기 때문, 명확하지 않을 때나 기본은 LAZY로 설정하고 필요한 것만 EAGER 사용
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "activity_tags", joinColumns = @JoinColumn(name = "activity_id"))
    // join되는 외래 키 "activity_id" hash set 기반 컬렉션 요청
    @Column(name = "tag")
    // 여러 개의 값은 분리 필요, 컬렌션 자료형 분리, 활동 테이블과 join할 수 있는 외래 키 이름 activity id 지정하겠다고 JPA에게 알려주는 것
    private Set<String> tags=new HashSet<>();

    // JPA가 사용하는 생성자를 protected로 선언 (없으면 JPA가 조회한 내용을 객체로 변환 x)
    protected LearningActivity() {}

    public LearningActivity(ActivityCategory category, String title, int minutes, Visibility visibility,
        String instructorName, Integer completionRate, String bookTitle) { // instructorName, completionRate, bookTitle 값 처리 다르게 할 것
        validateTitle(title);
        validateMinutes(minutes);
        this.category = category;
        this.title = title.trim(); // 좌우 공백 제거
        this.minutes = minutes;
        this.visibility = visibility;
        this.instructorName = normalizeInstructorName(category, instructorName); // 이전의 LectureLog, PracticeLog가 가지고 있던 로직을 가져올 것
        this.completionRate = normalizeCompletionRate(completionRate);
        this.bookTitle = bookTitle;
    }

    // 첨부 파일명을 활동 객체에 추가 (평범한 setter)
    // DB에는 파일명만, 실제 파일은 디스크에 저장
    public void attachFile(String savedFileName){
        this.attachmentFileName=savedFileName;
    }

    /**
     * 태그를 추가한다. 공백은 제거하고, 소문자로 저장한다.
     * 중복 태그는 무시한다 (Set의 특성)
     */
    public void addTag(String tag){
        if(tag==null||tag.isBlank()){
            throw new InvalidActivityException("태그는 비워둘 수 없습니다.");
        }
        tags.add(tag.trim().toLowerCase()); //<-> toUpperCase
    }

    // 등록된 태그를 제거한다
    public boolean removeTag(String tag){
        if (tag==null||tag.isBlank()){
            return false;
        }
        return tags.remove(tag.trim().toLowerCase());
    }

    /** 등록된 태그 목록을 읽기 전용으로 반환한다.*/
    public Set<String> getTags(){
        return Collections.unmodifiableSet(tags);
    }

    /**
     * 해당 태그가 등록되어 있는지 확인한다.
     */
    public boolean hasTag(String tag){
        if(tag==null) return false;
        return tags.contains(tag.trim().toLowerCase());
    }


    public void extendStudy(int additionalMinutes) {
        if (additionalMinutes <= 0) {
            throw new InvalidActivityException("추가 학습 시간은 1분 이상이어야 합니다 입력값은 "
                    + additionalMinutes);
        }
        this.minutes += additionalMinutes;
        //방어로직을 메소드에 작성 가능
    }
    //setTitle, setvisibility

    public void changTitle(String newTitle) {
        //전달받은 title을 변경받는 것에 집중
        validateTitle(newTitle); //새로운 타이틀 검증
        this.title = newTitle;
    }

    //메서드 생성 to 역할 분명
    //정보 은닉은 외부에 공개할 필요 없는 내용 숨김
    //to 문자열 return, string 타입으로 변환
    //이 메서드는 외부에서 알 필요 없고, 호출할 일도 없기 때문에 private으로 설정
    //이 클래스 안에서만 사용할 수 있도록 범위를 지정, 객체 내부에서만 쓰는 규칙
    private void validateTitle(String newTitle) {
        if (newTitle == null || newTitle.isBlank()) {
            //||_OR는 좌항이 논리식이면 우항도 논리식이어야
            //null이거나 공백이라면 true
            //and 논리연산자: 둘 중 하나라도 false->false, 모두 true이어야
            //and 대표적인 사례: 로그인(ID, pw 모두 true이어야)
            throw new InvalidActivityException("학습 제목은 비워둘 수 없습니다.");
        }

    }

    private void validateMinutes(int newMinutes) {
        if (newMinutes <= 0) {
            throw new InvalidActivityException("학습 시간은 1분 이상이어야 합니다. 입력값: " + newMinutes);
        }
    }

    public void openToPublic() {
        this.visibility = Visibility.PUBLIC; //boolean 타입이 아니기 때문에 변경, 선언되어 있는 클래스의 이름으로 값 참조

    }

    public void hideFromPublic() {
        this.visibility = Visibility.PRIVATE; //true, false 보다 문맥상 명확

    }

    // 이전 LectureLog 의 정규화 로직을 흡수: 강의인데 강사명이 비면 "강사 미정".
    private static String normalizeInstructorName(ActivityCategory category, String instructorName) {
        if (category == ActivityCategory.LECTURE && (instructorName == null || instructorName.isBlank())) {
            return "강사 미정";
        }
        return instructorName;
    }

    // 이전 PracticeLog 의 정규화 로직을 흡수: 완료율은 0~100 범위로 보정(없으면 null 유지).
    private static Integer normalizeCompletionRate(Integer completionRate) {
        if (completionRate == null) {
            return null;
        }
        if (completionRate < 0) {
            return 0;
        }
        if (completionRate > 100) {
            return 100;
        }
        return completionRate;
    }
}

