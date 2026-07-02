# 모바일 청첩장 프로젝트 진행사항 정리

---

## 서버 정보
- **플랫폼**: AWS Lightsail (서울 ap-northeast-2a)
- **사양**: Ubuntu 22.04, 2GB RAM, 2vCPU, 60GB SSD
- **IP**: 43.203.255.195
- **접속 URL**: http://43.203.255.195:8080
- **앱 경로**: /home/ubuntu/Wedding-Card
- **로그**: /home/ubuntu/Wedding-Card/app.log
- **Java**: /usr/lib/jvm/java-21-openjdk-amd64

---

## 완료된 작업

### 기능 버그 수정
- 2번 테마(Our story) 영어 타이틀 크기 수정
  - CSS: `font-size: min(19cqw, 90px)`
  - JS postMessage 핸들러에 인라인 스타일 동기화 추가 (live preview 반영)
- 3번→2번 테마 전환 시 핑크 색상 잔류 버그 수정
  - 디자인 전환 핸들러에서 색상 명시적 초기화

### 테마 이름 변경
- 5번 테마: "Together forever" → **아치형**
  - `src/main/resources/templates/admin/edit.html`
  - `src/main/resources/templates/index.html`

### 모바일 반응형 수정
- **랜딩페이지** (`index.html`):
  - NAV 패딩: 모바일에서 56px → 20px
  - STATS 섹션 (5가지 테마 / 5분 / 무료): 가로 → 세로 스택
  - 테마 카드: 가로 스크롤로 변경 (잘리지 않음)
- **편집창** (`editor.css`):
  - 전체 미리보기 모달: 430px 이하에서 iframe scale 0.9→0.79, 360px 이하 0.74
  - 사이드 미리보기 높이: 40vh → 50vh

### 배포
- GitHub 레포: https://github.com/HurKyungsoo/Wedding-Card.git (Public)
- AWS Lightsail 서버에 Java 21 + Maven 설치
- 빌드 및 앱 실행 완료 (`nohup java -jar ...`)
- Lightsail 방화벽 포트 8080 오픈
- **현재 http://43.203.255.195:8080 접속 가능**

---

## 남은 작업

### 즉시 필요
- [ ] 로컬 변경사항 git push (index.html, editor.css 수정됨)
- [ ] 서버 재배포 (git pull + mvn build + 앱 재시작)

### 선택사항
- [ ] systemd 서비스 등록 (서버 재부팅 시 자동 실행)
- [ ] 카카오 개발자 콘솔에 서버 도메인 등록 (카카오 로그인/지도 사용 시 필요)
  - 사이트 도메인: http://43.203.255.195:8080
  - Redirect URI: http://43.203.255.195:8080/login/oauth2/code/kakao
- [ ] HTTPS 적용 및 커스텀 도메인 연결
- [ ] 모바일 편집기 UX 개선

---

## 서버 재배포 명령어

```bash
# 1. 기존 앱 종료
pkill -f "java -jar"

# 2. 최신 코드 받기
cd Wedding-Card
git pull

# 3. 빌드
mvn clean package -DskipTests

# 4. 앱 실행
nohup java -jar target/*.jar > app.log 2>&1 &

# 5. 로그 확인
tail -f app.log
```

> **주의**: 터미널 재접속 시 JAVA_HOME이 초기화될 수 있음.  
> ~/.bashrc에 이미 추가되어 있어 자동 적용됨.

---

## 주요 파일 경로

| 파일 | 용도 |
|------|------|
| `src/main/resources/templates/index.html` | 랜딩페이지 |
| `src/main/resources/templates/admin/edit.html` | 편집창 |
| `src/main/resources/templates/invitation.html` | 청첩장 본문 |
| `src/main/resources/static/css/invitation.css` | 청첩장 스타일 |
| `src/main/resources/static/css/editor.css` | 편집창 스타일 |
| `src/main/resources/application.properties` | 기본 설정 (카카오 API 키 포함) |
