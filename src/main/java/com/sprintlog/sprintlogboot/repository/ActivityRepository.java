package com.sprintlog.sprintlogboot.repository;


import com.sprintlog.sprintlogboot.domain.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Repository // 이 클래스는 Repository 역할을 하는 클래스고, Bean으로 등록해줘
public class ActivityRepository {

    private final List<LearningActivity> storage = new ArrayList<>();

    public void add(LearningActivity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("저장할 활동은 null일 수 없습니다.");
        }
        storage.add(activity);
    }

    public void update(LearningActivity activity){
        if (activity==null){
            throw new IllegalArgumentException("수정할 활동은 null일 수 없습니다."); // 사실 서비스 단에서 검증해야 하는 부분, 레파지토리 단에서는 이루어지지 않지만 현재 서비스가 없으므로 추가함
        }
        storage.remove(activity);
        storage.add(activity);
        // set도 가능은 하나 인덱스를 찾아야 하는 번거로움 때문에 remove로 기존 객체 삭제 요청 후 다시 add 요청하자
    }

    // 저장된 모든 활동을 반환한다.
    public List<LearningActivity> findAll() {
        return Collections.unmodifiableList(storage);
    }

    // 조건에 맞는 활동'들'만 골라 반환한다.
    public List<LearningActivity> filter(Predicate<LearningActivity> predicate) {
        List<LearningActivity> result = new ArrayList<>();
        for (LearningActivity activity : storage) {
            if (predicate.test(activity)) {
                result.add(activity);
            }
        }
        return result;
    }

    // 조건에 맞는 첫 번째 활동을 골라 반환한다.
    public Optional<LearningActivity> findFirst(Predicate<LearningActivity> predicate) {
        for (LearningActivity activity : storage) {
            if (predicate.test(activity)) {
                return Optional.of(activity);
            }
        }
        return Optional.empty();
    }

    // 저장한 활동 수를 반환한다.
    public int count() {
        return storage.size();
    }

    // 저장된 모든 활동의 총 학습 시간(분)을 반환한다.
    public int getTotalMinutes() {
        int total = 0;
        for (LearningActivity activity : storage) {
            total += activity.getMinutes(); // LearningActivity가 LearningActivity의 자식이기 때문에 getMinutes() 호출 가능
        }
        return total;
    }


    // removeIf: 조건에 맞는 객체를 리스트에서 삭제 후 true를 리턴, 해당 id를 가진 활동이 없다면 false를 리턴
    public boolean removeById(Long id) {
        return storage.removeIf(activity -> activity.getId() == id);
        // 조건식 단 한 번도 동작하지 않으면 false return
    }
}










