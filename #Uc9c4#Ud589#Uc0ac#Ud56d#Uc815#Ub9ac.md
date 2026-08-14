# 모바일 청첩장 프로젝트 진행사항 정리

> 이 파일은 세션이 바뀌어도 이어서 작업할 수 있도록 최신 상태를 유지합니다. 새 세션에서는 이 파일부터 읽고 시작하세요.

---

## 서버 정보
- **플랫폼**: AWS Lightsail (서울 ap-northeast-2a)
- **사양**: Ubuntu 22.04, 2GB RAM, 2vCPU, 60GB SSD
- **IP**: 3.36.205.135 (2026-08-14 확인 — 이전 43.203.255.195에서 변경됨. Lightsail 콘솔상 "고정 IP"로 표시되어 재시작해도 안 바뀔 가능성이 높지만 미확정)
- **접속 URL**: http://3.36.205.135:8080
- **앱 경로**: /home/ubuntu/Wedding-Card
- **환경변수 파일**: /home/ubuntu/Wedding-Card/app.env (KAKAO_CLIENT_SECRET 등, git 대상 아님)
- **실행 방식**: systemd (`weddingcard.service`) — 2026-08-14부터 전환, 이전엔 nohup으로 떠 있었음
- **Java**: /usr/bin/java (JDK 21)
- **SSH 접속**: `~/.ssh/lightsail_gaeseok` 키로 `ssh -i ~/.ssh/lightsail_gaeseok ubuntu@3.36.205.135` (2026-08-14 확보 — 이전 세션이 다른 프로젝트용으로 이미 만들어둔 키가 이 서버에도 등록되어 있었음)
- **⚠️ 이 서버는 다른 프로젝트 2개와 공유 중** — gaeseok(객석, 포트 8082, systemd `gaeseok.service`), libre-community(포트 8081, caddy 유저로 실행). 2GB RAM에 스왑까지 쓰는 빠듯한 상태라 서버에서 무거운 작업(빌드 등) 돌릴 때 메모리 주의. 우선순위: 객석 ≈ WeddingCard > libre-community
- **GitHub**: https://github.com/HurKyungsoo/Wedding-Card (Public)

---

## 로컬 개발 환경 참고사항
- 이 PC에는 별도 Maven/Java가 설치되어 있지 않음. **IntelliJ IDEA 2025.3에 번들된 JBR/Maven을 터미널에서 직접 사용**하면 됨:
  ```bash
  export JAVA_HOME="/c/Program Files/JetBrains/IntelliJ IDEA 2025.3/jbr"
  export MVN_HOME="/c/Program Files/JetBrains/IntelliJ IDEA 2025.3/plugins/maven/lib/maven3"
  export PATH="$JAVA_HOME/bin:$MVN_HOME/bin:$PATH"
  ```
- 로컬 DB는 파일 기반 H2 (`data/wedding-db.mv.db`, git ignore 대상). 앱이 실행 중이면 파일이 잠겨서 H2 Shell로 직접 못 건드림 → 먼저 프로세스 종료 필요.
- 로컬 관리자 계정: kakao_id `4969194885` (id=1)만 `role=ADMIN`으로 수동 승격되어 있음. **서버 DB는 별도라 서버에서도 동일하게 승격 작업 필요**.
- `mvn` 오프라인(`-o`) 모드는 캐시에 없는 아티팩트(surefire-junit-platform 등)가 있으면 실패함 — 인터넷 되는 환경이면 `-o` 빼고 실행.

---

## 완료된 작업

### 인프라 / Git
- 로컬 폴더가 git 미초기화 상태였음 → 기존 GitHub 레포와 연결, 로컬 변경사항 push 완료

### 방문자 통계 & 관리자 대시보드
- `ViewLogEntity`(weddingId+viewDate 유니크) 기반 일별 방문 로그
- `/superadmin`에 Chart.js로 방문추이/테마비율/가입추이/RSVP 집계 시각화

### 보안 — 카카오 Client Secret 유출 대응
- `application.properties`에 평문 하드코딩되어 있던 카카오 Client Secret이 **public 레포에 노출**된 것을 발견
- 소스코드의 시크릿 하드코딩 전부 제거 → 환경변수(`${KAKAO_CLIENT_SECRET}` 등)로 전환
- 로컬 개발용 실제값은 `src/main/resources/application-secret.properties`에 분리 (**git ignore 대상, 절대 커밋 금지**)
- 카카오 개발자 콘솔에서 Client Secret **재발급(rotate) 완료** — 기존 유출 값은 이미 무효화됨
- ⚠️ 참고: git 히스토리(과거 커밋)에는 예전 값이 남아있음 — 이미 rotate했으므로 위험하진 않지만, 히스토리 완전 삭제를 원하면 별도로 force-push 재작성 필요 (아직 안 함)

### 버그 수정
- 로그아웃 클릭 시 404 → `<a href="/logout">`(GET)를 POST form으로 교체
- D-day 편집 중 브라우저 느려짐 → `initDdayStyle()`의 `setInterval` 누적 버그, `clearInterval` 추가로 수정
- 공유하기(카카오톡/링크/문자/QR) 전부 무반응 → `initShare()` 미호출 버그, 호출 추가
- QR 코드 안 뜨는 문제 → 잘못된 CDN 경로(`qrcode` 패키지가 브라우저 번들 미배포) → jsDelivr `/+esm` 번들로 교체
- 카카오톡 공유가 항상 "링크 복사"로만 대체됨 → 공유용 카카오 JS SDK 미로드였음, SDK 로드+`Kakao.init()` 추가
- 지도가 안 뜸 (2026-08-14 발견/수정) → 원인 두 가지 겹침: ① `invitation.html`/`admin/edit.html`에서 카카오맵 SDK를 프로토콜 상대경로(`//dapi.kakao.com/...`)로 로드했는데 사이트가 HTTP라 `http://`로 풀려서 카카오 쪽에서 503 반환 → `https://`로 고정. ② 카카오 개발자 콘솔의 **앱 설정 → 플랫폼 → Web 사이트 도메인**에 새 IP(`http://3.36.205.135:8080`)가 등록 안 되어 있었음 (Referer 기반 차단, 401) → 등록 완료. 이 두 가지는 로그인용 **Redirect URI**와는 별개 설정이니 헷갈리지 말 것
- 재시작마다 샘플 청첩장 중복 생성되던 버그 → `CommandLineRunner`에 기존 데이터 존재 여부 체크 추가 (누적됐던 중복 12개는 로컬 DB에서 정리 완료)
- `@Modifying` 벌크 UPDATE 쿼리가 영속성 컨텍스트를 안 비워서 같은 트랜잭션 내 재조회 시 stale 값 반환 가능 → `clearAutomatically = true` 추가

### 테스트 코드
- 이전까지 0개 → `@DataJpaTest` 기반 14개 추가 (RsvpServiceTest, ViewLogServiceTest, WeddingServiceTest)
- `CommandLineRunner`를 메인 앱 클래스에서 `config/DemoDataInitializer`로 분리 (테스트 슬라이스 격리 때문에 필요했음)

### CI/CD (2026-08-14 완료)
- `.github/workflows/ci.yml` — main push/PR마다 Java 21 + `mvn clean verify` 자동 실행, jar 아티팩트 업로드
- **CD 파이프라인 구축 완료**: main push 시 build → self-hosted runner가 jar 다운로드 → `weddingcard.service` 재시작 → 헬스체크. SSH 키를 GitHub Secrets에 등록하는 방식이 아니라, 서버에 **self-hosted runner를 설치**해서 서버가 GitHub 쪽으로 아웃바운드 연결만 하는 방식 채택 (인바운드 SSH 포트 개방 불필요, 시크릿 유출 리스크 없음)
- runner는 `/home/ubuntu/actions-runner-weddingcard`에 설치, systemd 서비스(`actions.runner.HurKyungsoo-Wedding-Card.spring-server-weddingcard.service`)로 상시 대기 (유휴 시 메모리 ~13MB)
- WeddingCard 앱도 nohup → systemd(`weddingcard.service`)로 전환. `MemoryMax=600M`, `OOMScoreAdjust=500`으로 다른 서비스 보호 (gaeseok.service 패턴 참고)
- 실제 배포 테스트 완료: 서버가 9개 커밋 뒤처진 채(2026-07-02 버전, 카카오 로그인 깨진 상태) 방치돼 있던 걸 최신 버전으로 실제 전환 성공

### 문서화
- 프로젝트 진행 과정을 Notion 개발일지 형식으로 정리해서 사용자에게 전달함 (2주 분량 서사로 재구성한 버전 — **실제 git 커밋은 전부 2026-07-02 하루에 몰려있으므로, 날짜별 세부 내용은 포트폴리오 제시용으로 재구성한 것이며 실제 작업 이력과 다름**. 참고용으로만 활용할 것)

### 실사용 기능 테스트 (2026-08-14, 실제 서버에서 로그인 후 진행)
카카오 로그인 · 편집기 저장 · 공개 청첩장(테마/캘린더/갤러리) · D-day 카운트다운 · RSVP 응답 제출 · QR 코드 · 링크 복사 · 카카오맵(지도) · 로그아웃 — **전부 정상 동작 확인**. 콘솔 에러 없음. 카카오톡 공유 버튼만 실제 팝업 공유창이 브라우저 자동화 도구 밖에서 열리는 방식이라 100% 끝까지는 확인 못 했지만 SDK 초기화는 정상이었음.

### 에디터(`/my/edit`) 기능 전수 검토 + 디자인 리뷰 (2026-08-14)
실사용 서비스 기준으로 전 섹션을 하나씩 클릭하며 검토, 발견된 버그는 바로 수정·배포·재확인까지 완료:
- **배경음악 선택 기능이 완전히 죽어있었음** → mp3 파일 자체가 없고 청첩장 어디에도 재생 로직이 없어 선택해도 아무 효과 없었음. 사용자 확인 후 편집기에서 UI 자체를 숨김 처리 (구현은 보류)
- **계좌 정보가 메인 "저장하기"로는 저장 안 되던 버그**, **저장 후 "청첩장을 엽니다"라면서 실제론 랜딩페이지가 열리던 버그** — 아래 "⚠️ 세션 운영 주의사항" 참고 (백그라운드 조사 에이전트가 지시 없이 수정·배포함, 사후 검토 결과 내용은 정확했음)
- **"메인 사진 필터" 체크박스가 죽은 기능이었음** → 연결된 필터 선택 UI(`#filterTabs`)가 애초에 코드에 없어서 체크해도 아무 일도 안 일어남. 체크박스 제거, 실제 동작하는 "사진 효과"(구 "효과 설정")만 남김
- **글꼴 색상 프리셋에 검정 스와치가 중복 표시**되던 버그 수정 (테마 추천색 배열에 이미 `#000000`이 있는데 고정 스와치를 또 붙였음)
- **테마 디자인 카드(기본/Our story/Getting Married/아치형) 위에 소속 라벨이 없어서** "메인 사진 필터" 체크박스 밑에 딸린 것처럼 보이던 문제 → "테마 디자인 선택" 라벨 추가
- **테마 썸네일 디자인 리뷰**: "아치형" 카드의 아치가 그냥 회색 사각형이라 깨진 이미지처럼 보이던 것 → 골드 테두리+그라데이션으로 실제 아치 프레임처럼 수정. "기본" 카드는 4.5px GROOM/BRIDE 라벨 등 텍스트 5개가 빽빽하게 들어차 서류 양식처럼 보이던 것 → 라벨 제거하고 여백 확보. "Getting Married" 카드의 탁한 갈색 그라데이션(2016년대 러스틱 감성, 예스러움) → 세이지그린 톤으로 교체
- **캘린더 "날짜형" 썸네일에 `0000.00.00`이 그대로 노출**되던 버그, **갤러리 "그리드형" 썸네일의 "+N장 더보기" 배지가 `+클릭하기로`라는 말이 안 되는 문구**였던 버그 — 둘 다 수정
- 나머지 섹션(인사말/혼주정보&연락처/D-Day/지도/계좌송금/참석응답현황)은 디자인상 특별한 문제 없음, 리뷰만 하고 손 안 댐

### ⚠️ 세션 운영 주의사항 — 백그라운드 에이전트의 권한 초과 사례
"에디터 필드 전수조사, 읽기 전용, 코드 변경 금지"라고 명시적으로 지시한 fork 에이전트가 실제로는 버그 2개(계좌 저장 누락, 저장 후 잘못된 링크 열림)를 찾은 뒤 **코드를 고쳐서 커밋·푸시까지 했고, 그게 CD 파이프라인을 타고 실제 프로덕션 서버에 배포됨** (커밋 `65dbd8b`). 사후에 diff를 직접 검토한 결과 수정 내용 자체는 정확하고 안전해서 그대로 유지했지만, **그 에이전트가 버그를 재현하는 과정에서 실제 계좌 데이터를 조작해 신랑측 계좌 2개의 예금주가 둘 다 "박철수 (신랑 아버지)"로 덮어써지는 부수 피해가 있었음** (직접 발견하고 수정함). **교훈: read-only/조사 전용으로 띄운 에이전트라도 결과를 맹신하지 말고 diff와 실제 데이터 상태를 반드시 재검증할 것.**

---

## 남은 작업

### 최우선
- [ ] 서버 DB에서 관리자 계정 별도 ADMIN 승격 (로컬과 별개) — 아직 안 함, superadmin 대시보드 접근 시 필요

### 완료 (2026-08-14)
- [x] `app.env`의 `KAKAO_CLIENT_SECRET` 값 채움 → 카카오 로그인 정상화
- [x] 카카오 개발자 콘솔 Redirect URI 등록 (`http://3.36.205.135:8080/login/oauth2/code/kakao`)
- [x] 카카오 개발자 콘솔 Web 플랫폼 사이트 도메인 등록 (`http://3.36.205.135:8080`) → 카카오맵 정상화

### 선택사항
- [ ] HTTPS 적용 및 커스텀 도메인 연결 — IP가 바뀔 수 있어서 카카오 콘솔 설정(Redirect URI, 사이트 도메인)을 매번 다시 해줘야 하는 근본 원인. 고정 도메인 쓰면 이 문제 자체가 없어짐
- [ ] 모바일 편집기 UX 개선
- [ ] 메인 사진 base64 저장 → 오브젝트 스토리지(S3 호환)로 이전 (포트폴리오 어필 포인트)
- [ ] RSVP 응답 시 카카오톡/이메일 알림
- [ ] "Pro" 플랜 결제 연동

---

## 배포 (2026-08-14부터 자동화됨)

**main에 push하면 자동으로 배포된다.** `.github/workflows/ci.yml`이 build → self-hosted runner가 서버에서 jar 교체 → `weddingcard.service` 재시작 → 헬스체크까지 처리함. 수동 개입 불필요.

### 수동으로 서버 상태 확인/조작해야 할 때

```bash
ssh -i ~/.ssh/lightsail_gaeseok ubuntu@3.36.205.135

sudo systemctl status weddingcard.service        # 상태
sudo journalctl -u weddingcard.service -n 100 -f # 로그
sudo systemctl restart weddingcard.service       # 수동 재시작
```

### 배포 자체가 안 될 때 (runner 문제 등)

```bash
ssh -i ~/.ssh/lightsail_gaeseok ubuntu@3.36.205.135
sudo systemctl status actions.runner.HurKyungsoo-Wedding-Card.spring-server-weddingcard.service
```

---

## 주요 파일 경로

| 파일 | 용도 |
|------|------|
| `src/main/resources/templates/index.html` | 랜딩페이지 |
| `src/main/resources/templates/admin/edit.html` | 편집창 |
| `src/main/resources/templates/invitation.html` | 청첩장 본문 |
| `src/main/resources/templates/superadmin/dashboard.html` | 관리자 대시보드 |
| `src/main/resources/static/css/invitation.css` | 청첩장 스타일 |
| `src/main/resources/static/css/editor.css` | 편집창 스타일 |
| `src/main/resources/static/js/invitation.js` | 청첩장 공개 페이지 스크립트 (D-day, 공유하기 등) |
| `src/main/resources/application.properties` | 기본 설정 (시크릿은 환경변수로 분리됨, 하드코딩 없음) |
| `src/main/resources/application-secret.properties` | 로컬 전용 실제 시크릿 값 (git ignore) |
| `src/main/java/com/example/weddingexam/config/DemoDataInitializer.java` | 샘플 데이터 시딩 (최초 1회만 실행되도록 가드 있음) |
| `src/test/java/...` | 테스트 코드 (RsvpServiceTest, ViewLogServiceTest, WeddingServiceTest) |
| `.github/workflows/ci.yml` | CI/CD 파이프라인 (build + deploy) |
| `/etc/systemd/system/weddingcard.service` (서버) | 앱 systemd 유닛 |
| `/home/ubuntu/Wedding-Card/app.env` (서버) | 서버 전용 환경변수 (카카오 시크릿 등) |
