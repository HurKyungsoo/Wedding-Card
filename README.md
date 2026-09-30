# 💌 Wedding Card — 모바일 청첩장 서비스

카카오 로그인 후 직접 청첩장을 만들고, 링크·QR·카카오톡으로 하객에게 공유할 수 있는 **모바일 청첩장 제작/게시 웹 서비스**입니다.
편집기에서 수정하는 내용이 실시간 미리보기로 반영되며, 게시하면 `/w/{slug}` 주소의 공개 청첩장이 만들어집니다.

## ✨ 주요 기능

**청첩장 제작**
- 카카오 OAuth2 로그인, 내 청첩장 목록/생성/삭제 (`/my`)
- 편집기 + **라이브 미리보기**, 자동저장 및 게시
- 테마 선택 (기본 / Our story / Getting Married / 아치형 / Forever), 글꼴·색상·사진 효과 설정
- 섹션별 표시 on/off 및 드래그로 순서 변경 (인사말, 달력, D-Day, 혼주 정보, 갤러리, 지도, 계좌, RSVP 등)
- 사진 업로드 (갤러리 그리드/슬라이드형)

**하객용 청첩장**
- D-Day 카운트다운, 캘린더 표시 및 `.ics` 일정 저장
- 카카오맵 기반 예식장 위치/약도 안내 (검색 API는 서버 프록시 경유)
- 신랑·신부측 계좌 정보 복사
- **RSVP(참석 여부 응답)**, **방명록**
- 공유하기: 카카오톡 / 링크 복사 / 문자 / QR 코드

**운영**
- 일별 방문 로그 수집
- 슈퍼 관리자 대시보드 (`/superadmin`): 방문 추이, 테마 비율, 가입 추이, RSVP 집계 (Chart.js)

## 🛠 기술 스택

| 구분 | 사용 기술 |
|---|---|
| Backend | Java 21, Spring Boot 3.3.0 (Web, Data JPA, Security, OAuth2 Client) |
| View | Thymeleaf, Vanilla JS, CSS |
| DB | H2 (로컬, 파일 기반) / PostgreSQL (prod) |
| 외부 API | Kakao Login, Kakao Maps, Kakao JS SDK |
| Build / CI·CD | Maven, Docker, GitHub Actions (self-hosted runner + systemd 배포) |
| Infra | AWS Lightsail (Ubuntu 22.04) |

## 📁 프로젝트 구조

```
src/main/java/com/example/weddingexam
├── account/      # 계좌 정보
├── config/       # 데모 데이터 초기화
├── controller/   # 인증, 청첩장(공개/편집), 슈퍼관리자
├── dto/          # Wedding DTO / Entity
├── guestbook/    # 방명록
├── rsvp/         # 참석 여부 응답
├── security/     # SecurityConfig, 카카오 OAuth2 사용자 서비스
├── service/      # Wedding 서비스/리포지토리
├── user/         # 사용자
└── viewlog/      # 방문 로그
src/main/resources
├── templates/    # invitation, admin/edit, my/list, superadmin, login ...
└── static/       # css, js, images
```

## 🚀 로컬 실행

### 요구사항
- JDK 21, Maven 3.9+
- [Kakao Developers](https://developers.kakao.com) 앱 (REST API 키, JavaScript 키)

### 1. 환경변수 설정

| 변수 | 설명 |
|---|---|
| `KAKAO_REST_API_KEY` | 카카오 REST API 키 (로그인 client-id, 지도 검색) |
| `KAKAO_CLIENT_SECRET` | 카카오 로그인 Client Secret (활성화한 경우) |
| `KAKAO_MAP_APPKEY` | 카카오맵 JavaScript 키 |
| `H2_CONSOLE_ENABLED` | 로컬에서 H2 콘솔이 필요할 때만 `true` (기본 `false`) |

또는 `src/main/resources/application-secret.properties`에 값을 넣어두면 자동으로 로드됩니다 (`.gitignore` 대상, **커밋 금지**).

### 2. 카카오 개발자 콘솔 설정
- **Redirect URI**: `http://localhost:8080/login/oauth2/code/kakao`
- **Web 사이트 도메인**: `http://localhost:8080` (지도 SDK 사용을 위해 필요, Redirect URI와 별개 설정)

### 3. 실행

```bash
mvn spring-boot:run
# 또는
mvn clean package && java -jar target/weddingexam-0.0.1-SNAPSHOT.jar
```

http://localhost:8080 접속. 로컬 DB는 `./data/wedding-db.mv.db`에 저장됩니다.

### 테스트

```bash
mvn test
```

## 🐳 Docker

```bash
docker build -t wedding-card .
docker run -p 8080:8080 --env-file .env wedding-card
```

## ☁️ 배포

- `main` 브랜치에 push 하면 GitHub Actions가 빌드·테스트 후 jar를 아티팩트로 업로드합니다.
- 서버에 설치된 **self-hosted runner**가 jar를 받아 `weddingcard.service`(systemd)를 재시작하고 헬스체크를 수행합니다. (인바운드 SSH 개방/SSH 키 시크릿 불필요)
- 운영 환경은 `application-prod.properties`(PostgreSQL, `spring.profiles.active=prod`)를 사용하며 카카오 관련 값은 모두 환경변수로 주입합니다.

## 🔒 보안 참고

- API 키/시크릿은 소스에 넣지 않고 환경변수 또는 `application-secret.properties`로만 관리합니다.
- H2 콘솔은 기본 비활성화이며, 카카오 지도 검색 프록시(`/api/map/**`)는 로그인 사용자만 접근할 수 있습니다.

## 📝 기타

개발 진행 상황과 이슈 내역은 [`진행사항정리.md`](./진행사항정리.md)에서 확인할 수 있습니다.
