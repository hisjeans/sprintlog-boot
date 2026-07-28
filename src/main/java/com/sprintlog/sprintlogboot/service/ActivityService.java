package com.sprintlog.sprintlogboot.service;

import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.LearningActivity;
import com.sprintlog.sprintlogboot.domain.Visibility;
import com.sprintlog.sprintlogboot.dto.request.CreateActivityRequest;
import com.sprintlog.sprintlogboot.dto.request.UpdateActivityRequest;
import com.sprintlog.sprintlogboot.dto.response.ActivityResponse;
import com.sprintlog.sprintlogboot.exception.ActivityNotFoundException;
import com.sprintlog.sprintlogboot.repository.ActivityRepository;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
// 클래스 레벨에 @Transactional을 설정하면 모든 메서드가 readOnly 트랜잭션을 가지게 된다
public class ActivityService {

  private final ActivityRepository repository; // 데이터베이스 연동 위한

  // 사실 조회 기능에는 굳이 필요 없지만 읽기 전용으로 save, delete 동작 막아주는 역할, 무조건 조회밖에 안 되도록 강제할 수 있다
  // 영속성 컨텍스트 범위 지정 가능
//  @Transactional(readOnly = true)
  public List<ActivityResponse> search(ActivityCategory category, String keyword, Integer minMinutes){

    if (category!=null){
      return convertToDtoList(repository.findByCategory(category)); // 바로 repository에서 리턴하면 안 된다
    }
    if (keyword!=null && !keyword.isBlank()){
      return convertToDtoList(repository.findByTitleContainingIgnoreCase(keyword));
    }
    if (minMinutes!=null){
      return convertToDtoList(repository.findByMinutesGreaterThanEqual(minMinutes));
    }
    return convertToDtoList(repository.findAll());
  }

  private List<ActivityResponse> convertToDtoList(List<LearningActivity> list){
    return list.stream()
        .map(a->ActivityResponse.from(a))
        .toList();
  }

  public Page<LearningActivity> page(String sort, int page, int size, Long ownerId) {

    // 기존에는 정렬 기준을 Comparator로 지정했는데, JPA에서 제공하는 페이징 기능을 사용하기 위해
    // Sort 타입으로 정렬 기준을 지정
    Sort sortBy = switch (sort) {
      case "minutes" -> Sort.by(Sort.Direction.DESC, "minutes");
      case "title" -> Sort.by("title");
      default -> Sort.by("id");
    };


    // 페이지 정보를 담을 객체 생성 (Pageable)
    // 여기서는 페이지 번호가 zero-based임. 1페이지를 0으로 취급.
    Pageable pageable = PageRequest.of(page - 1, size, sortBy);

    return (ownerId != null)
        ? repository.findByOwnerId(ownerId, pageable)
        : repository.findAll(pageable);
  }

  public LearningActivity get(Long id) {
    return repository.findById(id)
        .orElseThrow(()->new ActivityNotFoundException(id));
  }

  //   public void create(final CreateActivityRequest request, String savedFileName) { - 서비스가 요청과 함께 넘어온 데이터를 자체적으로 가공하지 못하도록 처리할 수 있다
  @Transactional
  public LearningActivity create(CreateActivityRequest request, String savedFileName) {
    LearningActivity activity = toActivity(request);
    activity.attachFile(savedFileName);
    return repository.save(activity);
  }
  // if) 세세한 것까지 세팅하고 싶다면 생성자 이용해 직접 만든다



  // private 클래스 안에서만 사용하는 것이므로 아래쪽에 배치하는 것이 일반적
  // 평탄화 후 — 하위 타입 생성 switch 가 사라졌다.
  //   종류(type)와 종류별 필드를 그대로 단일 생성자에 넘기면 된다(엔티티가 category 로 구분).
  // switch문 삭제, 일괄적으로 LearningActivity 처리
  private LearningActivity toActivity(CreateActivityRequest request) {
    LearningActivity activity = new LearningActivity(
        request.type(), request.title(), request.minutes(), request.visibility(),
        request.instructorName(), request.completionRate(), request.bookTitle());

    if (request.tags() != null) {
      request.tags().forEach(activity::addTag);
    }
    return activity;
  }

  // readOnly가 아닌 트랜잭션이 필요한 경우 메서드에 직접 붙여주면 된다
  @Transactional // 메서드 레벨에 트랜잭션을 걸면 클래스 레벨보다 더 우선시된다
  public LearningActivity update(Long id, @Valid UpdateActivityRequest request) {
    LearningActivity activity = repository.findById(id)
        .orElseThrow(() -> new ActivityNotFoundException(id)); // id로 수정하고자 하는 객체 먼저 조회

    activity.changTitle(request.title());
    if (request.visibility()== Visibility.PUBLIC){
      activity.openToPublic();
    } else {
      activity.hideFromPublic();
    }
    // JPA가 적용된 상태에서의 update는 findById로 조회해 온 Entity를 setter로 변경
    // 변경 후 명시적으로 save() 호출하면 영속성 컨텍스트의 변경 감지(dirty checking)에 의해 update 쿼리가 날아간다
    // save를 부르지 않아도 findById로 조회했을 때 조회된 내용과 복사본을 두는데 setter 수정된 내용과 복사본 사이 차이가 있다면 update가 날라간다 - transaction이 끝났을 때, 더이상 수정될 내용은 없다고 entity가 판단
    return repository.save(activity);// update문이 나갈 것
  }

  @Transactional
  public void delete(Long id) {
    if (!repository.existsById(id)){ // 해당 id에 대한 데이터 존재 여부 확인(T/F)
      throw new ActivityNotFoundException(id);
    }
    repository.deleteById(id);
  }

  public Slice<LearningActivity> sliceByVisibility(Visibility visibility, int page, int size) {
    PageRequest pageable = PageRequest.of(page, size, Sort.by("id"));
    return repository.findByVisibility(visibility, pageable);
  }

  @Transactional
  public List<LearningActivity> withDetails() {
    return repository.findAllWithDetails();
  }

}
