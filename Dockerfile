
# --- 1. build 스테이지 ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon
# Docker는 빌드할 때마다 레이어 캐시를 만든다 이전 빌드와 파일 내용이 바뀌지 않았다면 이전에 저장한 레이어 캐시를 그대로 재사용
# build.gradle 거의 바뀌지 않지만 src 코드 넣는 공간은 굉장히 자주 바뀐다
# 빈도수 체크했을 때 src >>변경 훨씬 많다>> build.gradle
# 자주 바뀌지 않는 건(예: dependencies 설정) 재사용해 빌드 속도 올리기 위한 용도로 미리 세팅
# gradle 관련 설정이나 build.gradle에 있는 의존성 라이브러리 목록은 잘 바뀌지 않는 영역
# "chmod" 권한 부여
# 밑에 있는 폴더와 파일을 미리 카피를 해 놓고, gradlew에게 의존성 라이브러리를 미리 다운로드해서 레이어 캐시를 저장
# --no-daemon: Docker 이미지를 빌드하는 컨테이너는 일회성이므로 메모리 누수 위험이 있는 gradle daemon 프로세스를 켜지 않도록 설정

# 소스코드 복사 후 실행 가능한 jar 빌드
COPY src ./src
# 의존성 라이브러리를 많이 사용할수록 위에 미리 써놓지 않으면 계속 생성해야 하는데 이미지를 재빌드할 때 재사용할 수 있다
RUN ./gradlew clean build -x test
# 원래라면 빌드 과정에서 테스트 진행, 현재는 시간상 생략
# -x: 제외
# 개발 과정에서 테스트를 이미지 빌드에서 제외 가능
# 왠만하면 "-x test" 는 작성하지 않는 것 권장
# 여기까지 수행되었다면 jar 파일이 나올 것



# --- 2. run 스테이지 ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
# jre 를 사용해 용량 더 줄인다

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
# 불필요한 패키지 설치 막아준다
# 네트워크 요청 도구인 curl 설치(리눅스의 postman 도구 느낌), 불필요한 패키지 설치를 막고 패키지 목록 임시 파일들을 전부 다 삭제해 이미지 용량을 줄인다
# curl 설치 이유: 헬스체크용

COPY --from=build /workspace/build/libs/sprintlog-boot-0.0.1-SNAPSHOT.jar app.jar
# build 스테이지에서 만든 jar를 카피해 app.jar 이름으로 붙여넣기

ENV TZ=Asia/Seoul
# 타임존 설정 - 설정하지 않으면 UTC로 설정된다(영국 시간대와 동일해진다)
RUN apk add --no-cache tzdata  # alpine 버전 사용 시 필요


ENV SPRING_PROFILES_ACTIVE=prod
# 운영 프로파일로 실행하기 위해 환경변수 값을 prod로 전달

EXPOSE 8080 9090
# 포트 노출, 프로젝트 앱 서버 sprintlog app: 8080, actuator: 9090 사용 중이다 이를 노출시키겠다는 의미
# 이렇게 세팅해도 실제로 노출되지는 않는다 - EXPOSE는 문서용 키워드
# 실제로 노출시키려면 컨테이너 생성 시에 -p를 사용해 노출시켜야 한다

HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
  CMD curl -fsS http://localhost:9090/management/health || exit 1
# Actuator 헬스체크(별도 포트 9090, base-path /management). 부팅 시간 고려해 start-period 여유.
# --start-period=60s: 60초 뒤에 실행, 서버는 아직 켜지지 않았고, 서버가 켜지는 데 시간 오래 걸린다, 60초 뒤에 헬스 체크 시작
# --interval=15s: 요청을 15초에 한 번씩 보내겠다
# --timeout=3s: 3초 이내에 응답이 와야 건강하다 판단
# --retries=5: 만일 3초 이내 응답을 보냈는데 오지 않았다, 재요청 횟수, 건강하지 않은 상태, 요청 제대로 처리할 수 없는 상태 - 서버 내린다, 그 재시도 횟수
# 응답 오지 않으면 서버를 아예 내려버리겠다

ENTRYPOINT ["java", "-jar", "app.jar"]
# CMD, RUN은 기본 실행 명령을 의미, 컨테이너 실행 시에 다른 명령어가 주어지면 그 명령어로 대체된다
# ENTRYPOINT는 반드시 실행되어야 할 명령어를 의미 다른 명령어로 대체되지 않는다
# 스프링 부트는 무조건 -jar 옵션으로 실행되어야 하기 때문에 강조의 의미로 ENTRYPOINT 선언 - 절대 바꾸면 안 된다는 의미 강조하기 위해
