package com.sprintlog.sprintlogboot.repository;

import com.sprintlog.sprintlogboot.domain.PersistentLogin;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

// Remember-me 영구 토큰을 다루는 JPA 레포지토리
public interface PersistentLoginRepository extends JpaRepository<PersistentLogin, String> {

  // findById로도 가능하지만, pk가 id가 아닌 경우 의미가 드러나게 명시적으로 선언(가독성)
  Optional<PersistentLogin> findBySeries(String series);

  // 특정 사용자의 활성 토큰 수(로그아웃/삭제 시 로그 판단용)
  long countByUsername(String username);

  // 로그아웃, 비밀번호 변경 등에서 그 사용자의 모든 토큰을 삭제 (자동 로그인 완전 해제)
  // 기존에 자동 로그인 중이라 하더라도 비밀번호가 바뀌면 자동 로그인이 풀리도록 해야 한다, 그때 사용자의 모든 토큰 삭제
  @Modifying // jpql 아닌 다른 쿼리 사용할 때는 반드시 @Modifying 붙여야 한다
  @Query("DELETE from PersistentLogin p WHERE p.username=:username")
  void deleteByUsername(String username);


}
