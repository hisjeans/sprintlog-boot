package com.sprintlog.sprintlogboot.service;


import com.sprintlog.sprintlogboot.aspect.LogExecutionTime;
import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.LearningActivity;
import com.sprintlog.sprintlogboot.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

// final로 선언된 필드만 받는 생성자를 자동으로 만들어줌
// 반드시 요구되는 매개값을 받는 생성자를 lombok이 만들어줌
// 코드상으로는 보이지 않지만 컴파일 과정에서 lombok이 final 필드 초기화할 수 있는 생성자 만들어줄 것
// @NoArgsConstructor 매개값이 없는 기본 형태의 생성자도 만들어줄 수 있음
// 현재 만들어줄 수 없는 이유: constructor는 생성자(객체 만드는 역할),
// 기본 생성자는 매개값이 하나도 없는 생성자, 이 모양대로 객체를 생성하면 final 필드를 누가 초기화? - 로직을 쓰면 가능은 하나 이는 기본 생성자 아님
// @AllArgsConstructor 모든 생성자
@Service // 빈 등록 어노테이션, @Component와 기능은 똑같고 계층을 좀 더 명시적으로 표기
@RequiredArgsConstructor
public class ActivityDashboard {

    private final ActivityRepository repository; // to 불변 객체

    // 의존성 자동 주입, ActivityRepository가 ActivityRepository에 의존하고 있는 상황
    // 생성자를 통해 ActivityRepository를 전달 받을 때 컨테이너에서 검색해 주입해줄 것
    // 객체는 spring이 컨테이너 안에 있는 activity repository 꺼내줄 것(DI)
    // 생성자 지움

    //내부 클래스에 static을 붙이는 이유는 메모리 누수를 방지하고 독립성을 가지기 위해
    //static이 없다면 메모리에 있는 객체와 강하게 연결됨 반드시 ActivityDashboard 바깥의 객체가 먼저 생성되어야 summary객체 생성 가능, 강한 연결성
    //Dashboard 없이 Summary만 필요한 상황에서도 같이 생성해야 하는 문제
    //Dashboard가 쓸모가 없어 사라졌는데 garbage collector, summary 삭제되지 않는 문제
    //내부 클래스에 static이 붙으면 ActivityDashboard가 생성되지 않아도 Summary 가 별도로 생성 가능
    //Summary가 바깥의 필드를 자유롭게 생성 가능
    //static을 선언하면 dashboard가 생성되지 않아 바깥 객체가 간섭하지 않게 함
    //배열 직접 건드리지 못하게 함
    //위의 dashboard field 참조하지 못함

    /**
     * 카테고리별 활동 수를 세어 Summary를 만들자.
     *
     */

    //메서드 안에서도 클래스 선언 가능
    @LogExecutionTime // summarize 호출되기 전 start 시간 찍히고 summarize 호출된 후 end 시간이 찍힐 것
    public Summary summarize(){
        //로컬 클래스 선언: 메소드 안에서 클래스 선언, summarize()밖에서는 사용할 수 없음
        class Counter { //summarize 메소드를 호출해야만 counter 클래스 사용 가능
            private int totalCount;
            private int lectureCount;
            private int practiceCount;
            private int readingCount;

            void add(LearningActivity activity) {
                totalCount++; //총개수는 카테고리와 무관하게 올라감
                // getCategory()는 LearningActivity의 public API
                switch (activity.getCategory()) {
                    case LECTURE -> lectureCount++;
                    case PRACTICE -> practiceCount++;
                    case READING -> readingCount++;
                }
            }

            Summary toSummary() { //거의 마지막에 부를 것
                return new Summary(totalCount, lectureCount, practiceCount, readingCount);
                //실제 summary 객체 만들어 return
            }
        }//end Counter class

        //로컬 클래스를 메서드 안에 전달하는 이유, 카테고리 바뀔 때 counter만 수정하면 됨

        Counter counter = new Counter();
        for (LearningActivity activity : repository.findAll()) {
            counter.add(activity);

            //메서드 안에 로컬 클래스 이용해 접근
        }
        return counter.toSummary();

    }//end summarize()


    // 내부 클래스에 static을 붙이는 이유는 메모리 누수를 방지하고 독립성을 가지기 위해
    public static class Summary { //코드 구조상 ActivityDashboard의 summary임을 보여줌
        private final int totalCount;
        private final int lectureCount;
        private final int practiceCount;
        private final int readingCount;


        //command+n 생성자
        public Summary(int totalCount, int lectureCount, int practiceCount, int readingCount) {
            this.totalCount = totalCount;
            this.lectureCount = lectureCount;
            this.practiceCount = practiceCount;
            this.readingCount = readingCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getLectureCount() {
            return lectureCount;
        }

        public int getPracticeCount() {
            return practiceCount;
        }

        public int getReadingCount() {
            return readingCount;
        }
    }


    //카테고리별 그룹화------------------------------------------------------------
    // 카테고리별로 활동(Log)를 그룹화해 Map으로 반환
    public Map<ActivityCategory, List<LearningActivity>> groupByCategory(){
        Map<ActivityCategory, List<LearningActivity>> result=new TreeMap<>();
        // HashMap -> TreeMap으로 변경: 카테고리(enum) 선언 순서대로 정렬되어 출력이 일관됨
        // TreeMap key 값 정렬
        for(LearningActivity activity: repository.findAll()){
            ActivityCategory cat=activity.getCategory(); //key가 될 것

            //해당 카테고리가 Map에 없으면 빈 List를 먼저 만들어 put
            if(!result.containsKey(cat)){
                result.put(cat, new ArrayList<>());
            }

            //카테고리별 리스트를 얻어온 후 리스트에 활동 객체를 add
            List<LearningActivity>list=result.get(cat);
            //map에서 get하면 맵에 매핑된 key 꺼냄
            //key: activity value:
            list.add(activity);

        }
        return result;
    }


    // 모든 활동에서 태그를 모아 알파벳순 정렬 Set으로 반환
    public Set<String> getSortedTagSet(){
        Set<String> tags=new TreeSet<>();
        // 중복 자동 제거, 정렬(!= 일반 hash set 정렬 보장되지 않음)

        for (LearningActivity activity : repository.findAll()) {
            tags.addAll(activity.getTags());
        }
        return Collections.unmodifiableSet(tags);
    }

    //태그 필터링-----------------------------------------------------------------

    public List<LearningActivity> filterByTag(String tag){
        List<LearningActivity> result=new ArrayList<>();
        for (LearningActivity activity : repository.findAll()) {
            if(activity.hasTag(tag)) {
                result.add(activity);
            }
        }
        return Collections.unmodifiableList(result); //외부에서 add, read 불가능
    }
}


