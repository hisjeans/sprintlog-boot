package com.sprintlog.sprintlogboot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "persistent_logins",
        indexes = @Index(name = "idx_persistent_logins_username", columnList = "username"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersistentLogin {

  @Id // series가 pk
  @Column(name = "series", length = 64)
  private String series;

  @Column(name = "username", nullable = false, length = 100)
  private String username; // 중복될 수 있어 pk 될 수 없다

  @Column(name = "token", nullable = false, length = 64)
  private String token; // 계속 바뀌는 값, pk 되기 어렵다

  @Column(name = "last_used", nullable = false)
  private LocalDateTime lastUsed;

//  // JPA 기본 생성자.
//  protected PersistentLogin() {}

  /** Spring Security 가 준 값을 '그대로' 저장한다(series·token 을 여기서 만들지 않는다). */
  public PersistentLogin(String series, String username, String token, LocalDateTime lastUsed) {
    this.series = series;
    this.username = username;
    this.token = token;
    this.lastUsed = lastUsed;
  }

  /** 자동 로그인마다 Spring 이 준 '새 token' 으로 갱신한다(token 회전 → 탈취 감지의 기반). */
  public void update(String token, LocalDateTime lastUsed) {
    this.token = token;
    this.lastUsed = lastUsed;
  }
}
