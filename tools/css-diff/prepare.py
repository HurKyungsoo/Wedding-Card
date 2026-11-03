"""Thymeleaf 템플릿을 비교용 정적 HTML 로 만든다 (스크립트·외부 폰트 제거, CSS 링크를 자리표시자로).
사용: python3 prepare.py  →  inv.tpl.html, ed.tpl.html 생성"""
import re, pathlib
W = pathlib.Path(__file__).resolve().parents[2] / 'src/main/resources'
for name, tpl in [('inv', 'templates/invitation.html'), ('ed', 'templates/admin/edit.html')]:
    h = (W / tpl).read_text(encoding='utf-8')
    h = re.sub(r'<script\b.*?</script>', '', h, flags=re.S)
    h = re.sub(r'<link[^>]*(googleapis|gstatic|jsdelivr|cdn|unpkg)[^>]*>', '', h)
    h = re.sub(r'<link rel="stylesheet" th:href="@\{/css/([a-z-]+)\.css\}">',
               lambda m: f'<link rel="stylesheet" href="__{m.group(1)}__">', h)
    pathlib.Path(f'{name}.tpl.html').write_text(h, encoding='utf-8')
print('ok')
