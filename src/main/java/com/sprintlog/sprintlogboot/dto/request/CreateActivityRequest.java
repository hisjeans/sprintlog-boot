package com.sprintlog.sprintlogboot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.Visibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record CreateActivityRequest(
        // 빈 문자열, 공백 문자열 허용 대신 null은 안 됨!
        // 필수값으로 지정하기 위해
        @NotNull(message = "활동 유형(type)은 필수입니다.")
        @JsonProperty("category") // 왠만하면 프론트와 백엔드 맞춰주는 것이 좋지만,
        // 백엔드는 'type'이란 이름으로 올 것을 기대하고 있고 프론트는 'category' 란 이름을 기대하는 불일치 문제 발생
        // 단순 변경 시에는 다른 코드까지 변경되어야 하기 때문에 
        // @JsonProperty 어노테이션 이용해 type-category 짝임을 알려준다
        ActivityCategory type,

        // @NotEmpty: 공백 문자열 허용, 빈 문자열, null은 안 됨!
        // 빈 문자열, 공백문자열 null 모두 안 됨!
        @NotBlank(message = "제목은 비워둘 수 없습니다.")
        String title,

        @Min(value = 1, message = "학습 시간은 1분 이상이어야 합니다.")
        @Max(value = 1440, message = "학습 시간은 하루(1440분)를 넘을 수 없습니다.")
        int minutes,

        @NotNull(message = "공개 여부는 필수입니다.")
        Visibility visibility,

        // 선택값들
        Set<String> tags,
        String instructorName, // LectureLog만 사용
        int completionRate,
        String bookTitle
) {
}
