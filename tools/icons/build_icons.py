#!/usr/bin/env python3
"""
Tabler 아이콘 폰트를 '실제로 쓰는 아이콘만' 남겨 자체 호스팅용으로 만든다.

전체 tabler-icons CSS 는 200KB 넘고(아이콘 5,000개+), 폰트도 수백 KB 다. 우리가 쓰는 건 70개 남짓.
템플릿·JS·CSS 에서 ti-xxx 클래스를 모아 그 글리프만 남긴 woff2 와 CSS 를 만든다.

사용법 (저장소 루트에서):
  npm pack @tabler/icons-webfont@2.47.0 && tar xzf tabler-icons-webfont-2.47.0.tgz -C /tmp
  pip install fonttools brotli
  python3 tools/icons/build_icons.py /tmp/package

아이콘을 새로 쓰면 이 스크립트를 다시 돌려야 한다 — QualityGuardTest 가 빠진 아이콘을 잡아낸다.
"""
import hashlib
import pathlib
import re
import sys

from fontTools import subset

ROOT = pathlib.Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources"
OUT_FONT = RES / "static/fonts/tabler-icons-subset.woff2"
OUT_CSS = RES / "static/css/icons.css"
VERSION = "2.47.0"


def used_icons():
    names = set()
    for d in ("templates", "static/js", "static/css"):
        for p in (RES / d).rglob("*"):
            if p.is_file() and p.suffix in (".html", ".js", ".css") and p != OUT_CSS:
                names.update(re.findall(r"\bti-([a-z0-9]+(?:-[a-z0-9]+)*)", p.read_text(encoding="utf-8")))
    return sorted(names)


def main(pkg):
    pkg = pathlib.Path(pkg)
    css = (pkg / "tabler-icons.css").read_text(encoding="utf-8")
    codes = dict(re.findall(r'\.ti-([a-z0-9-]+):before\s*\{\s*content:\s*"\\([0-9a-f]+)"', css))
    names = used_icons()
    missing = [n for n in names if n not in codes]
    if missing:
        sys.exit("tabler " + VERSION + " 에 없는 아이콘: " + ", ".join(missing))

    OUT_FONT.parent.mkdir(parents=True, exist_ok=True)
    opts = subset.Options()
    opts.flavor = "woff2"
    opts.layout_features = []
    opts.name_IDs = ["*"]
    opts.notdef_outline = True
    font = subset.load_font(str(pkg / "fonts/tabler-icons.ttf"), opts)
    s = subset.Subsetter(opts)
    s.populate(unicodes=[int(codes[n], 16) for n in names])
    s.subset(font)
    subset.save_font(font, str(OUT_FONT), opts)
    ver = hashlib.sha256(OUT_FONT.read_bytes()).hexdigest()[:10]

    lines = [
        "/* 자동 생성 — tools/icons/build_icons.py. 직접 고치지 말 것.",
        " * Tabler Icons " + VERSION + " (MIT) 중 이 사이트가 쓰는 " + str(len(names)) + "개만 남긴 것 */",
        "@font-face{font-family:\"tabler-icons\";font-style:normal;font-weight:400;font-display:block;"
        "src:url(\"/fonts/tabler-icons-subset.woff2?v=" + ver + "\") format(\"woff2\");}",
        ".ti{font-family:\"tabler-icons\"!important;speak:none;font-style:normal;font-weight:normal;"
        "font-variant:normal;text-transform:none;line-height:1;-webkit-font-smoothing:antialiased;"
        "-moz-osx-font-smoothing:grayscale;}",
    ]
    lines += ['.ti-%s:before{content:"\\%s";}' % (n, codes[n]) for n in names]
    OUT_CSS.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(len(names), "icons ->", OUT_FONT.stat().st_size, "bytes font,", OUT_CSS.stat().st_size, "bytes css")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "/tmp/package")
