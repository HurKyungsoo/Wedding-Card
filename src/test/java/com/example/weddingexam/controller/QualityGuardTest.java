package com.example.weddingexam.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 전체 품질 점검에서 나온 항목들이 다시 깨지지 않게 막아 둔다.
 * - D-day 문구는 신랑·신부 이름을 innerHTML 로 넣으므로 이름을 반드시 이스케이프한다 (하객 화면 XSS)
 * - CDN 은 @latest 를 쓰지 않는다 (메이저 업데이트 때 아이콘이 갑자기 깨지는 것 방지)
 * - 응답 압축과 정적 리소스 캐시를 켜 둔다
 */
class QualityGuardTest {

    private String read(String path) throws Exception {
        return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void ddayNames_areEscapedBeforeInnerHtml() throws Exception {
        String js = read("static/js/invitation.js");
        assertThat(js).contains("function _escHtml(");
        assertThat(js).contains("var groomRaw = _escHtml(data.groomName");
        assertThat(js).contains("var brideRaw = _escHtml(data.brideName");
    }

    @Test
    void templates_doNotUseLatestCdnVersions() throws Exception {
        for (String t : List.of("templates/index.html", "templates/invitation.html", "templates/admin/edit.html",
                "templates/my/list.html", "templates/superadmin/dashboard.html")) {
            assertThat(read(t)).as(t).doesNotContain("@latest");
        }
    }

    @Test
    void compressionAndStaticCache_areEnabled() throws Exception {
        String props = read("application.properties");
        assertThat(props).contains("server.compression.enabled=true");
        assertThat(props).contains("spring.web.resources.cache.cachecontrol.max-age=");
    }

    @Test
    void invitationMessageListeners_checkOrigin() throws Exception {
        String html = read("templates/invitation.html");
        int listeners = html.split("addEventListener\\('message'", -1).length - 1;
        int checks = html.split("e.origin !== window.location.origin", -1).length - 1;
        assertThat(listeners).isPositive();
        assertThat(checks).isEqualTo(listeners);
    }

    /**
     * ★ 아이콘은 쓰는 것만 남겨 자체 호스팅한다(static/css/icons.css + fonts/tabler-icons-subset.woff2).
     * 예전엔 CDN 의 전체 CSS(200KB+)를 받았고, 버전 고정 때 경로를 잘못 적어 아이콘이 전부 사라진 적이 있다.
     * 새 아이콘을 쓰고 tools/icons/build_icons.py 를 안 돌리면 그 아이콘만 빈칸이 된다 — 여기서 잡는다.
     */
    @Test
    void everyUsedIcon_isInTheSelfHostedSubset() throws Exception {
        String iconsCss = read("static/css/icons.css");
        assertThat(new ClassPathResource("static/fonts/tabler-icons-subset.woff2").exists()).isTrue();
        java.util.Set<String> used = new java.util.TreeSet<>();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\bti-([a-z0-9]+(?:-[a-z0-9]+)*)");
        org.springframework.core.io.support.PathMatchingResourcePatternResolver r =
                new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
        for (String pattern : List.of("classpath:templates/**/*.html", "classpath:static/js/*.js", "classpath:static/css/*.css")) {
            for (org.springframework.core.io.Resource res : r.getResources(pattern)) {
                if (res.getFilename() != null && res.getFilename().startsWith("icons")) continue;
                String text = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                java.util.regex.Matcher m = p.matcher(text);
                while (m.find()) used.add(m.group(1));
            }
        }
        assertThat(used).isNotEmpty();
        for (String name : used) {
            assertThat(iconsCss).as("tools/icons/build_icons.py 를 다시 돌리세요 — 빠진 아이콘: ti-" + name)
                    .contains(".ti-" + name + ":before");
        }
        for (String t : List.of("templates/index.html", "templates/invitation.html", "templates/admin/edit.html",
                "templates/my/list.html", "templates/superadmin/dashboard.html")) {
            assertThat(read(t)).as(t).doesNotContain("icons-webfont@").contains("/css/icons.css");
        }
    }

    /** CSS 안의 구글폰트 @import 는 HTML <link> 와 중복 + 렌더링 지연 — 글꼴은 HTML 에서만 받는다 */
    @Test
    void css_doesNotImportGoogleFonts() throws Exception {
        for (String c : List.of("static/css/invitation.css", "static/css/editor.css", "static/css/tokens.css")) {
            assertThat(read(c)).as(c).doesNotContain("@import url('https://fonts.googleapis.com");
        }
    }

    /** 편집기 접근성 — 키보드로 섹션 열기·토글 켜기, 아이콘만 있는 버튼의 이름 */
    @Test
    void editor_isUsableWithKeyboardAndScreenReader() throws Exception {
        String css = read("static/css/editor.css");
        assertThat(css).as("토글 체크박스를 display:none 으로 숨기면 키보드로 못 켠다")
                .doesNotContain(".ed-toggle-wrap input{display:none;}");
        String js = read("static/js/editor.js");
        assertThat(js).contains("(function editorA11y()").contains("setAttribute('role', 'button')")
                .contains("aria-expanded");
        String html = read("templates/admin/edit.html");
        assertThat(html).contains("id=\"navPanelClose\" aria-label=\"메뉴 닫기\"");
        assertThat(html).contains("id=\"fontColorReset\" aria-label=");
    }

    /** 모바일 편집 — 폼 전체 화면 + 「미리보기」는 마지막으로 만진 섹션부터 바로(표지부터 X) */
    @Test
    void mobilePreview_opensAtTheEditedSection() throws Exception {
        assertThat(read("templates/admin/edit.html")).contains("id=\"mobilePreviewBtn\"");
        String js = read("static/js/editor.js");
        assertThat(js).contains("lastEditedSection").contains("instant: true");
        assertThat(js).as("닫아도 미리보기를 남겨 둬야 다음엔 바로 그 섹션으로 열린다")
                .doesNotContain("getElementById('fullPrevFrame').src = ''");
        assertThat(read("templates/invitation.html")).contains("e.data.instant ? 'instant' : 'smooth'");
    }

    /** 인트로는 하객 화면에선 늘, 편집기 미리보기에선 처음 한 번만 */
    @Test
    void intro_isSkippedOnlyInEditorPreviewAfterFirstTime() throws Exception {
        String html = read("templates/invitation.html");
        assertThat(html).contains("<script th:if=\"${editorPreview}\">").contains("wc.introSeen")
                .contains("html.no-intro .intro-screen{display:none !important;}");
    }
}
