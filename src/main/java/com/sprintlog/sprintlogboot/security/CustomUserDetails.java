package com.sprintlog.sprintlogboot.security;

import com.sprintlog.sprintlogboot.domain.User;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// Spring Security는 우리의 User 엔티티를 전혀 모른다, UserDetails라는 양식(틀)밖에 모른다
// 우리의 User 정보를 UserDetails라는 양식에 맞춰 포장한 객체가 CustomUserDetails
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

  private final User user;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    // 유저의 권한(Role)을 리턴하는 곳
    // GrantedAuthority 형태로 변환해 저장, 접두어로 ROLE_ 를 붙여 저장
    // 객체 여러 개라면 SimpleGrantedAuthority 여러개 붙여 생성, "ROLE_" 반드시 붙여 저장
    return List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole().name()));
  }

  @Override
  public String getPassword() {
    return user.getPassword();
  }

  @Override
  public String getUsername() {
    return user.getEmail();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // 아래 default 메서드는 반드시 선언할 필요는 없다

  /*
  계정 상태 플래그에 대한 메서드 (계정이 만료됐나? 계정이 잠겼나? 비밀번호가 만료됐나? 계정이 비활성화되었나?)
  나중에 휴먼 계정이나 정지된 사용자, 비밀번호 주기적 변경 기능을 넣으면 그 때 활성화해도 된다
  return을 false 로 주어 잠글 수 있다 true를 주면 통과한다
   */

  @Override
  public boolean isAccountNonExpired() {
    return UserDetails.super.isAccountNonExpired();
  }

  @Override
  public boolean isAccountNonLocked() {
    return UserDetails.super.isAccountNonLocked();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return UserDetails.super.isCredentialsNonExpired();
  }

  @Override
  public boolean isEnabled() {
    return UserDetails.super.isEnabled();
  }

}
