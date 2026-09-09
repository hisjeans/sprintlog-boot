package com.sprintlog.sprintlogboot.domain;

public class WeeklyGoal {

  // 목표치가 정해지면 변경하지 않을 것이기 때문에 final로 선언
  private final int targetMinutes; // 이번 주 목표 학습 시간(분)

  public WeeklyGoal(int targetMinutes) {
    if (targetMinutes <= 0) {
      throw new IllegalArgumentException("주간 목표 시간은 1분 이상이어야 합니다.");
    }
    this.targetMinutes=targetMinutes;
  }

  public int achievementRate(int studiedMinutes) {
    //return (int) Math.round(studiedMinutes * 100.0 / targetMinutes); // 반올림 사용 위해 Math.roud() 부른다
    // 여기까지 green 단계(테스트 통과할 만큼만 구축)/refactor 단계라는 의견 있다

    if (studiedMinutes <= 0) return 0; // 100% 넘게 계산되는 로직 필요
    int rate = (int) Math.round(studiedMinutes * 100.0 / targetMinutes);
    return Math.min(rate, 100); // rate vs 100 중 작은 값 return - if문 사용 가능은 하나 지저분해질 수 있기 때문
  }


  public boolean isAchieved(int studiedMinutes) {
    return studiedMinutes >= targetMinutes;
  }

  public int remainingMinutes(int studiedMinutes) { // 달성을 위해 목표까지 남은 공부시간 알려준다
    return Math.max(0, this.targetMinutes-studiedMinutes); // targetMinutes: 60, studiedMinutes: 90 -> (0, -30)
  }
}
