package com.sprintlog.sprintlogboot.security;

import com.sprintlog.sprintlogboot.domain.PersistentLogin;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import com.sprintlog.sprintlogboot.repository.PersistentLoginRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
PersistentTokenRepository를 JPA로 직접 구현하는 클래스
Spring의 내장 JdbcTokenRepositoryImpl(row sql로 감춰져 있다, 커스텀 안 되고 정해진 대로 작성해야 한다)
 대신 우리가 4개 메서드를 직접 구현해
토큰이 어떻게 저장, 갱신, 조회, 삭제되는지 전 과정을 JPA로 일관되게 처리

@Component로 빈 등록하면 SecurityConfig의 rememberMe().tokenRepository(...)가 이 구현체를 주입받아 사용한다
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaPersistentTokenRepository implements PersistentTokenRepository {

  private final PersistentLoginRepository repository;

  // 로그인(로그인 유지 체크) 성공 시 1회 호출 - Spring이 준 토큰을 세 행으로 DB에 저장
  @Override
  @Transactional
  public void createNewToken(PersistentRememberMeToken token) {
    // 처음에 로그인했을 때 한 번 호출
    PersistentLogin entity = new PersistentLogin(
        token.getSeries(),
        token.getUsername(),
        token.getTokenValue(),
        toLocalDateTime(token.getDate())
    );
    repository.save(entity);
  }

  // 자동 로그인 성공마다 호출 - Spring이 준 새 토큰으로 회전(series는 그대로)
  // 자동 로그인 성공할 때마다 호출, 토큰 변경
  // 엔티티 자체를 변경하는 것이 아니다
  @Override
  @Transactional
  public void updateToken(String series, String tokenValue, Date lastUsed) {
    repository.findBySeries(series).ifPresentOrElse( // 존재하는 경우, 존재하지 않는 경우 전부 람다식으로 작성
        entity->entity.update(tokenValue, toLocalDateTime(lastUsed)),
        // orElse
        // 참고, 전달되는 값 없기 때문에 "()"
        ()->{
          log.warn("[REMEMBER-ME] 갱신 대상 없음 - series={}", series);
          // 부가 로직 추가로 더 작성 가능
        }
    ); // 있으면 토큰을 갈아주고 없으면 로그를 남겨주는 로직, 데이터베이스 사용하는 토큰만 갈아주면 된다
  }

  // 자동 로그인 시 호출 - 쿠키의 series로 저장된 토큰을 찾아서 Spring 규격 객체(PersistentRememberMeToken)로 돌려준다
  // (없으면 null 주도록 한다)
  @Override
  @Transactional(readOnly = true)
  public PersistentRememberMeToken getTokenForSeries(String seriesId) {
    return repository.findBySeries(seriesId)
        .map(e->new PersistentRememberMeToken(
            e.getUsername(), e.getSeries(), e.getToken(), toDate(e.getLastUsed())
        ))
        .orElse(null); // 없으면 null, map 자체가 optional이기 때문에 이어서 optional로 작성
  }

  // 로그아웃 등에서 그 사용자의 모든 토큰 삭제
  @Override
  @Transactional
  public void removeUserTokens(String username) {
    long count = repository.countByUsername(username);
    repository.deleteByUsername(username);
    log.info("[REMEMBER-ME] 토큰 삭제 - user={}, 삭제 수={}", username, count);
  }

  private static LocalDateTime toLocalDateTime(Date date){
    return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
  }
  // 예전에는 날짜 표현하는 방법이 Date 였지만, Java 8 이후로는 LocalDateTime과 같이 java.time 패키지의 클래스를 사용하기 때문에 바꾸어주는 것
  private static Date toDate(LocalDateTime dateTime){
    return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
  }
}
