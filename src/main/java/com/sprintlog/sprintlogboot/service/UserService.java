package com.sprintlog.sprintlogboot.service;

import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionInformation;
import java.util.List;
import com.sprintlog.sprintlogboot.security.CustomUserDetails;
import com.sprintlog.sprintlogboot.domain.Role;
import com.sprintlog.sprintlogboot.domain.User;
import org.springframework.transaction.annotation.Transactional;

import com.sprintlog.sprintlogboot.dto.request.SignUpRequest;
import com.sprintlog.sprintlogboot.dto.response.UserResponse;
import com.sprintlog.sprintlogboot.exception.BusinessException;
import com.sprintlog.sprintlogboot.exception.ErrorCode;
import com.sprintlog.sprintlogboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
  private final SessionRegistry sessionRegistry;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder; // 회원가입 기능은 spring security가 제공하지 않기 때문에 직접 구현

  public UserResponse register(SignUpRequest request){
    // 1. 이메일 중복 확인 (409 status)
    if (userRepository.existsByEmail(request.email())) {
      throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS); // 여기서 발생한 예외는 컨트롤러에서 잡아서 global exception handler에서 처리
    }

    // 2. 비밀번호는 반드시 암호화해서 저장(평문 저장 금지)
    String hashedPassword = passwordEncoder.encode(request.password());
    // passwordEncoder.matches("원문 비밀번호", "암호화된 비밀번호") -> 패턴 매칭되는지 알려준다, equals로 비교하면 당연히 false, matches로 패턴 일치 여부 비교

    // 3. 저장
    User user = new User(request.nickname(), request.email(), hashedPassword);
    User saved = userRepository.save(user);

    log.info("[USER] 회원가입 완료 - id: {}, email: {}", saved.getId(), saved.getEmail());

    // dto->entity 변환 처리: service vs controller
    // 대중적인 방식은 서비스 계층에서 dto 변환 선호, response entity 변환할 때 연관관계 범위 조회해야 하는 경우 있는데 LAZY
    // Transaction 걸면 JPA 영속성 범위가 transaction으로 잡힌다
    // 컨트롤러 쪽에서 수행하게 되면 나도 모르는 새 다른 엔티티 변환 필요한 경우 있다, 컨트롤러는 요청-응답 담당하는 계층
    // 엔티티 -> DTO 변환하는 로직도 비즈니스 로직에 더 어울리지 않은가? 컨트롤러는 entity를 모르 게 하는 것이 더 안전하지 않나 의견이 대중적
    // 컨트롤러 계층에서 DTO 변환하면 서비스 쪽 재사용성 극대화하기는 하지만 단점 많기 때문에 왠만하면 서비스 계층에서 DTO 변환하는 것이 선호

    return UserResponse.from(user);
  }

  @Transactional
  public UserResponse changeRole(String email, Role newRole) {
    // 권한이 항상 승격만 되지는 않기 때문에 컨트롤러에서 받아서 처리한다고 가정
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    user.changeRole(newRole);

    expireSessionsOf(email);

    User saved = userRepository.save(user);
    return UserResponse.from(saved);
  }

  private void expireSessionsOf(String email) {
    // 세션 레지스트리에 등록된 모든 사용자의 세션 만료
    // 한 계정이 사파리, 핸드폰 여러 곳에 접속된 계정이라면 모든 세션 만료시켜줘야 한다
    for (Object principal : sessionRegistry.getAllPrincipals()) {
      if (principal instanceof CustomUserDetails details
      && details.getUsername().equals(email)){
        List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal,
            false); // 만료된 세션은 포함하지 않고 활성화된 모든 세션 가져오도록
        sessions.forEach(SessionInformation::expireNow); // 지금 바로 만료
      }
    }
  }
}
