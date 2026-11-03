# 💌 Wedding Card — 모바일 청첩장 서비스

카카오 로그인 후 직접 청첩장을 만들고, 링크·QR·카카오톡으로 하객에게 공유할 수 있는 **모바일 청첩장 제작/게시 웹 서비스**입니다.
편집기에서 수정하는 내용이 실시간 미리보기로 반영되며, 게시하면 `/w/{slug}` 주소의 공개 청첩장이 만들어집니다.

🔗 **데모**: http://3.36.205.135:8080 (커스텀 도메인 연결 전이라 IP 주소가 변경되면 링크가 달라질 수 있습니다)

## ✨ 주요 기능

**랜딩 페이지 (`/`)**
- 히어로의 폰 안에 **실제 샘플 청첩장**(`/demo`)이 들어 있고 천천히 저절로 스크롤 (마우스를 올리면 멈추고 직접 스크롤)
- 테마 캐러셀 + 폰 모양 **미리보기 창**, **"이 테마로 만들기"** → 로그인 후 그 테마가 골라진 상태로 편집기 시작 (`/start?design=`)
- **편집기 체험** 섹션: 이름·글꼴·글꼴 색·사진 필터·연출 효과를 바꾸면 옆 폰 속 샘플이 실시간으로 바뀜 (화면에 보이면 자동 시연)
- 기능 소개 벤토: 하객 화면을 옮겨 그린 예시 + 오시는 길 카드는 **실제 카카오 지도**
- 링크 미리보기(og:image 1200×630)
- 체험용 샘플 `/demo?theme=` — 사진·갤러리를 채운 데모, 조회수 미집계·검색 제외, 방명록/RSVP 제출 차단

**청첩장 제작 (편집기)**
- 카카오 OAuth2 로그인, 내 청첩장 목록/생성/삭제 (`/my`)
- 폼 + **폰 모양 실시간 미리보기**(실제 폰 비율 390×844를 화면 높이에 맞춰 축소), 편집 중인 섹션으로 미리보기 자동 이동
- **임시저장과 게시의 분리**: 입력하면 자동 임시저장(하객에게는 안 보임), "게시하기"를 눌러야 하객 화면에 반영 · 헤더의 저장 상태를 누르면 즉시 저장
- 테마 6종 (기본 / Our story / Our story 핑크 / Getting Married / 아치형 / THE MARRIAGE) — 썸네일은 실제 첫 화면과 같은 배치, 날짜는 예식 날짜(없으면 오늘)
- 한글·영문 글꼴 13종(목록을 그 글꼴로 표시), 글꼴 색·크기, 사진 필터·색감 보정, 연출 효과(안개·물결·페이퍼)
- 예식 정보: 연·월·일 / 오전·오후·시·분 선택, **장소 검색 한 번으로 예식장 이름·주소·지도 위치를 함께 입력**
- 혼주·연락처 한 줄 입력(고인·성함·세례명·전화번호), 전화번호 자동 정리(`010-1234-5678`)
- 섹션별 표시 on/off·드래그 순서 변경·섹션별 되돌리기 (인사말, 달력, D-Day, 혼주, 갤러리, 지도, 계좌, RSVP, 방명록, 엔딩)
- 사진: 메인 사진 크롭/위치, 갤러리 끌어다 놓기 업로드(최대 60장, 슬라이드·격자·모자이크·썸네일 스와이프)
- 게시 전 필수 항목 확인, 첫 방문 가이드

**하객용 청첩장 (`/w/{slug}`)**
- 인트로, D-Day 카운트다운, 캘린더 4종 및 `.ics` 일정 저장
- 카카오맵 위치 안내 + 카카오내비·티맵·네이버지도 길안내 (검색 API는 서버 프록시 경유)
- 신랑·신부측 계좌 복사 / 카카오페이 송금 링크
- **RSVP(참석 여부 응답)**, **방명록**, 연락하기
- 공유하기: 카카오톡 / 링크 복사 / 문자 / QR 코드

**운영**
- 일별 방문 로그 수집 (편집기 미리보기·데모는 집계 제외)
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
├── controller/   # 인증, 청첩장(공개/편집/데모), 슈퍼관리자
├── demo/         # 랜딩 체험용 샘플 사진·갤러리 (DemoPhotos)
├── dto/          # Wedding DTO / Entity
├── guestbook/    # 방명록
├── rsvp/         # 참석 여부 응답
├── security/     # SecurityConfig, 카카오 OAuth2 사용자 서비스
├── service/      # Wedding 서비스/리포지토리
├── user/         # 사용자
└── viewlog/      # 방문 로그
src/main/resources
├── templates/    # index(랜딩), invitation, admin/edit, my/list, superadmin, login
└── static/       # css, js, images (demo/ 샘플 사진, navi/ 길안내 앱 아이콘)
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

## 🧯 트러블슈팅 기록 (일부)

| 문제 | 원인 | 해결 |
|---|---|---|
| 배포 후 랜딩 하단 스크립트가 통째로 동작 안 함 | 스크립트 속 `[['a','b'], …]` 를 Thymeleaf 가 `[[...]]` 인라인 식으로 해석해 렌더가 중간에 끊김 | 대괄호를 띄워 쓰고, 랜딩이 `</html>` 까지 렌더되는지 테스트 추가 |
| 지도 장소 검색 결과가 항상 0건 | URL 인코딩한 문자열을 `RestTemplate.exchange(String)` 에 넘겨 한글이 두 번 인코딩(`%` → `%25`) | `UriComponentsBuilder` 로 한 번만 인코딩한 `URI` 전달 + 인코딩 테스트 |
| 이름을 안 넣으면 인트로에 `null · null` | 템플릿 문자열 결합에서 null 이 그대로 출력 | 비어 있으면 '신랑 · 신부' 로 대체 |
| 랜딩 히어로 폰의 자동 스크롤이 멈춤 | 청첩장 `html { scroll-behavior:smooth }` 때문에 프로그램 스크롤이 매 프레임 보간 | `scrollTo({ behavior:'instant' })` |

## 📝 기타

개발 진행 상황과 이슈 내역은 [`진행사항정리.md`](./진행사항정리.md)에서 확인할 수 있습니다.
