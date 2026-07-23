package com.sprintlog.sprintlogboot.dto.response;

import java.util.List;

public record PagedResponse<T> (
    List<ActivityResponse> content, // 이번 페이지의 데이터
    int page, // 현재 페이지 번호
    int size, // 페이지의 데이터 개수
    long totalElements, // 전체 데이터 개수
    int totalPages // 전체 페이지 수
){

}
