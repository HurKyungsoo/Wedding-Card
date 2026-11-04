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
}
