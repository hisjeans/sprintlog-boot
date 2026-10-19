package com.sprintlog.sprintlogboot.repository;

import com.sprintlog.sprintlogboot.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Spring-Data-JPA는 인터페이스마나 선언해 놓으면 구현체는 자동으로 만들어 준다
// JpaRepository 인터페이스를 상속만 받으면 구현체가 알아서 세팅된다
// 제네릭에는 <엔티티 타입, PK 타입>
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email); // 이메일로 유저 조회, Optional은 null 처리 편하게 해주는 객체

  boolean existsByEmail(String email); // 이 이메일을 가진 사용자가 존재하는지 여부만 간단히 조회, 쿼리 메서드로 작성

}
// 현재 findById로 회원 찾을 수 없다 사용자 가입할 때 이메일로 가입하기 때문에 findByEmail