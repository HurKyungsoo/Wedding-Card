package com.example.weddingexam.controller;

import com.example.weddingexam.dto.WeddingDto;
import com.example.weddingexam.service.WeddingService;
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
 * 개발용 도구 — 갓 만든(내용이 빈) 청첩장이 하객 화면에서 어떻게 보이는지 덤프한다.
 * 평소에는 비활성 — -DdumpBlank=true 를 줄 때만 실행된다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:blank-dump;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
@EnabledIfSystemProperty(named = "dumpBlank", matches = "true")
class BlankWeddingDumpTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WeddingService weddingService;

    @Test
    void dumpBlankInvitation() throws Exception {
        WeddingDto blank = weddingService.createForUser(7777L);

        String html = mockMvc.perform(get("/w/" + blank.getSlug()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Path out = Path.of(System.getProperty("dumpBlankPath", "target/blank-dump.html"));
        Files.createDirectories(out.getParent());
        Files.writeString(out, html, StandardCharsets.UTF_8);
        System.out.println("[DUMP] " + out.toAbsolutePath() + " (" + html.length() + " chars)");
    }
}
