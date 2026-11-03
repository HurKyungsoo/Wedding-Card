# css-diff — CSS 리팩터링 회귀 검사

CSS를 정리(중복 제거 등)한 뒤 **화면이 하나도 달라지지 않았는지** 기계적으로 확인하는 도구.

두 CSS로 같은 템플릿을 Chromium(Playwright)에 렌더하고, 모든 요소(::before/::after 포함)의
계산된 스타일(getComputedStyle)을 전부 비교한다. 청첩장은 테마 6개 × 글꼴 크기 3개 ×
실제/미리보기 2가지 × 화면 폭 360/430px 조합을 모두 돈다.

```bash
cd tools/css-diff
cp ../../src/main/resources/static/css/tokens.css .
python3 prepare.py                                   # 템플릿 → 정적 HTML
cp ../../src/main/resources/static/css/invitation.css old.css
python3 dedupe.py old.css new.css                    # 같은 선택자에서 뒤에서 덮어쓰는 선언 제거
node compare.js inv.tpl.html old.css new.css inv     # DIFFS 0 이면 화면 동일
```

`dedupe.py` 는 같은 컨텍스트(@media 등)·같은 선택자의 규칙 중, 뒤 규칙이 같은 속성을
(같거나 더 강한 !important 로) 다시 선언해서 **절대 적용되지 않는 앞쪽 선언만** 지운다.
서로 다른 속성을 가진 중복 규칙은 합치지 않는다 — 중간에 낀 다른 규칙과의 우선순위가 바뀔 수 있어서다.
