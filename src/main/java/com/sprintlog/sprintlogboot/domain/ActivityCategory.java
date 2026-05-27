package com.sprintlog.sprintlogboot.domain;

public enum ActivityCategory {

    LECTURE("강의", 60),
    PRACTICE("실습", 60),
    READING("독서", 45);
    //활동을 복습시간 레이블과 함께 추가적인 정보 저장하려는 목적

    private final String label;
    private final int reviewThresholdMinutes;

    ActivityCategory(String label, int reviewThresholdMinutes) {
        this.label = label;
        this.reviewThresholdMinutes = reviewThresholdMinutes;
    }

    public String getLabel() { return label; }

    public boolean isShortStudy(int minutes) {
        return minutes < reviewThresholdMinutes;
    }
}
