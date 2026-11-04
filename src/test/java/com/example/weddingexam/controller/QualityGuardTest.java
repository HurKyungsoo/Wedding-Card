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
     * ★ 아이콘 CSS 경로 — 2.x 는 패키지 루트에, 3.x 는 dist/ 아래에 있다.
     * @latest 를 버전으로 고정하면서 루트 경로에 3.x 를 적었다가 404 → 청첩장·편집기 아이콘이 전부 사라졌다.
     * 루트 경로는 예전 @latest 가 실제로 주던 2.47.0, dist 경로는 3.x 로만 쓴다.
     */
    @Test
    void tablerIconCss_versionMatchesPath() throws Exception {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("icons-webfont@([0-9.]+)/(dist/)?tabler-icons\\.min\\.css");
        for (String t : List.of("templates/index.html", "templates/invitation.html", "templates/admin/edit.html",
                "templates/my/list.html", "templates/superadmin/dashboard.html")) {
            java.util.regex.Matcher m = p.matcher(read(t));
            while (m.find()) {
                boolean v3 = m.group(1).startsWith("3.");
                assertThat(m.group(2) != null).as(t + " — " + m.group()).isEqualTo(v3);
            }
        }
    }
}
