package com.sprintlog.sprintlogboot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

@Schema(description = "활동 생성 요청 본문") // swagger 제공하는 schema 붙여 설명
public record CreateActivityRequest(
        // 빈 문자열, 공백 문자열 허용 대신 null은 안 됨!
        // 필수값으로 지정하기 위해
        @Schema(description = "활동 유형", examples = "LECTURE", requiredMode = Schema.RequiredMode.REQUIRED) // 활동 유형 반드시 들어가야 하는 값, 필수 표시
        @NotNull(message = "활동 유형(type)은 필수입니다.")
        @JsonProperty("category") // 왠만하면 프론트와 백엔드 맞춰주는 것이 좋지만,
        // 백엔드는 'type'이란 이름으로 올 것을 기대하고 있고 프론트는 'category' 란 이름을 기대하는 불일치 문제 발생
        // 단순 변경 시에는 다른 코드까지 변경되어야 하기 때문에 
        // @JsonProperty 어노테이션 이용해 type-category 짝임을 알려준다
        ActivityCategory type,

        // @NotEmpty: 공백 문자열 허용, 빈 문자열, null은 안 됨!
        // 빈 문자열, 공백문자열 null 모두 안 됨!
        @Schema(description = "학습 제목", examples = "Spring Bean Scope", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "제목은 비워둘 수 없습니다.")
        String title,

        @Schema(description = "학습 시간(분, 1~1440)", examples = "Spring Bean Scope", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "학습 시간은 1분 이상이어야 합니다.")
        @Max(value = 1440, message = "학습 시간은 하루(1440분)를 넘을 수 없습니다.")
        int minutes,

        @Schema(description = "공개 여부", examples = "PUBLIC", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "공개 여부는 필수입니다.")
        Visibility visibility,

        // 선택값들
        @Schema(description = "태그 목록(선택)", examples = "[\"Spring\", \"Java\"]") // "\" 추가해 문자열 표현하기 위해 쓴 것임을 표시
        Set<String> tags,
        @Schema(description = "강사 이름 (type=LECTURE 일 때)", examples = "이강사")
        String instructorName, // LectureLog만 사용
        @Schema(description = "완료율 % (type=PRACTICE 일 때)", examples = "85")
        int completionRate,
        @Schema(description = "책 제목 (type=READING 일 때)", examples = "스프링 인 액션")

        String bookTitle
) {
}
// DTO에도 Swagger 문법 사용 가능하나 선호하지 않는 경우도 있다
// DTO 마다 어노테이션을 붙이는 것에 대해 피로함을 느끼는 경우도 있다
// validation이 어느 정도 그 역할을 수행하기 때문에 Swagger 문법 사용하지 않을 수도 있는 것
