# 모바일 청첩장 프로젝트 진행사항 정리

> 이 파일은 세션이 바뀌어도 이어서 작업할 수 있도록 최신 상태를 유지합니다. 새 세션에서는 이 파일부터 읽고 시작하세요.

---

## 서버 정보
- **플랫폼**: AWS Lightsail (서울 ap-northeast-2a)
- **사양**: Ubuntu 22.04, 2GB RAM, 2vCPU, 60GB SSD
- **IP**: 43.203.255.195
- **접속 URL**: http://43.203.255.195:8080
- **앱 경로**: /home/ubuntu/Wedding-Card
- **로그**: /home/ubuntu/Wedding-Card/app.log
- **Java**: /usr/lib/jvm/java-21-openjdk-amd64
- **⚠️ SSH 접속 수단 미확보** — 서버 재배포가 계속 막혀있는 원인. 키/비밀번호 찾으면 최우선으로 재배포 진행할 것.
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
- 재시작마다 샘플 청첩장 중복 생성되던 버그 → `CommandLineRunner`에 기존 데이터 존재 여부 체크 추가 (누적됐던 중복 12개는 로컬 DB에서 정리 완료)
- `@Modifying` 벌크 UPDATE 쿼리가 영속성 컨텍스트를 안 비워서 같은 트랜잭션 내 재조회 시 stale 값 반환 가능 → `clearAutomatically = true` 추가

### 테스트 코드
- 이전까지 0개 → `@DataJpaTest` 기반 14개 추가 (RsvpServiceTest, ViewLogServiceTest, WeddingServiceTest)
- `CommandLineRunner`를 메인 앱 클래스에서 `config/DemoDataInitializer`로 분리 (테스트 슬라이스 격리 때문에 필요했음)

### CI/CD
- `.github/workflows/ci.yml` — main push/PR마다 Java 21 + `mvn clean verify` 자동 실행, jar 아티팩트 업로드
- 실제 GitHub Actions 실행 성공 확인 (약 36초)
- **CD(서버 자동 배포)는 아직 없음** — SSH 키 확보되면 GitHub Secrets 등록 후 진행

### 문서화
- 프로젝트 진행 과정을 Notion 개발일지 형식으로 정리해서 사용자에게 전달함 (2주 분량 서사로 재구성한 버전 — **실제 git 커밋은 전부 2026-07-02 하루에 몰려있으므로, 날짜별 세부 내용은 포트폴리오 제시용으로 재구성한 것이며 실제 작업 이력과 다름**. 참고용으로만 활용할 것)

---

## 남은 작업

### 최우선
- [ ] SSH 키/접속 수단 확보 → 확보되는 즉시 서버 재배포 (아래 명령어 참고)
- [ ] 서버 환경변수에 새 `KAKAO_CLIENT_SECRET`, `KAKAO_MAP_APPKEY`, `KAKAO_REST_API_KEY` 반영
- [ ] 서버 DB에서 관리자 계정 별도 ADMIN 승격 (로컬과 별개)

### 선택사항
- [ ] CD 파이프라인 (SSH 키 확보 후 GitHub Actions에 배포 스텝 추가)
- [ ] systemd 서비스 등록 (서버 재부팅 시 자동 실행)
- [ ] 카카오 개발자 콘솔에 서버 도메인 등록 (Redirect URI: `http://43.203.255.195:8080/login/oauth2/code/kakao`)
- [ ] HTTPS 적용 및 커스텀 도메인 연결
- [ ] 모바일 편집기 UX 개선
- [ ] 메인 사진 base64 저장 → 오브젝트 스토리지(S3 호환)로 이전 (포트폴리오 어필 포인트)
- [ ] RSVP 응답 시 카카오톡/이메일 알림
- [ ] "Pro" 플랜 결제 연동

---

## 서버 재배포 명령어 (SSH 접속 확보 후)

```bash
# 1. 기존 앱 종료
pkill -f "java -jar"

# 2. 최신 코드 받기
cd Wedding-Card
git pull

# 3. 환경변수 설정 (최초 1회, ~/.bashrc에 등록 권장)
export KAKAO_MAP_APPKEY="502b99c57514360a34fdf5b9181ed284"
export KAKAO_REST_API_KEY="03a041000c72178b476cbb6e29431e81"
export KAKAO_CLIENT_SECRET="<카카오 콘솔에서 재발급한 새 값으로 교체>"

# 4. 빌드
mvn clean package -DskipTests

# 5. 앱 실행
nohup java -jar target/*.jar > app.log 2>&1 &

# 6. 로그 확인
tail -f app.log
```

> **주의**: 터미널 재접속 시 JAVA_HOME이 초기화될 수 있음. ~/.bashrc에 이미 추가되어 있어 자동 적용됨. 위 카카오 환경변수도 ~/.bashrc에 함께 등록해두면 재부팅 후에도 유지됨.

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
| `.github/workflows/ci.yml` | CI 파이프라인 |
