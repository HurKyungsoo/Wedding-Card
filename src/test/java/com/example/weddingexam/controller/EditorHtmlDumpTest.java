package com.example.weddingexam.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 개발용 도구 — 편집기 HTML을 파일로 덤프한다.
 * 편집기는 로그인 필수라 로컬 브라우저로 열 수 없어서, 렌더된 HTML을 뽑아
 * 같은 오리진(정적 경로)으로 서빙해 실제 editor.js를 돌려보기 위한 용도.
 * 평소에는 비활성 — -DdumpEditor=true 를 줄 때만 실행된다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
@EnabledIfSystemProperty(named = "dumpEditor", matches = "true")
class EditorHtmlDumpTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void dumpEditorHtml() throws Exception {
        String html = mockMvc.perform(get("/admin/edit"))
                .andReturn().getResponse().getContentAsString();

        Path out = Path.of(System.getProperty("dumpEditorPath", "target/editor-dump.html"));
        Files.createDirectories(out.getParent());
        Files.writeString(out, html, StandardCharsets.UTF_8);
        System.out.println("[DUMP] " + out.toAbsolutePath() + " (" + html.length() + " chars)");
    }
}
