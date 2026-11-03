package com.example.weddingexam.controller;

import com.example.weddingexam.dto.WeddingDto;
import com.example.weddingexam.service.WeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 청첩장(하객이 여는 링크)에서는 빈 칸·편집용 안내를 숨기고,
 * 편집기 미리보기(?preview=1)에서는 그대로 보여준다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:live-empty-state-test;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
class LiveInvitationEmptyStateTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WeddingService weddingService;

    private String html(String url) throws Exception {
        return mockMvc.perform(get(url)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private Long emptyWedding() {
        WeddingDto d = new WeddingDto();
        d.setMainDesign("basic");
        return weddingService.save(d).getId();
    }

    @Test
    void liveInvitation_marksEmptyDateAndPlace_andHidesEmptySections() throws Exception {
        String h = html("/wedding/" + emptyWedding());
        assertThat(h).containsPattern("class=\"wc is-live no-date no-place\"");
        // 날짜가 없으면 달력·D-Day, 사진이 없으면 갤러리, 지도·장소가 없으면 오시는 길이 숨겨진다
        assertThat(h).containsPattern("class=\"sec live-empty\"[^>]*data-vis=\"calendar\"|data-vis=\"calendar\"[^>]*class=\"sec live-empty\"|class=\"sec live-empty\" data-vis=\"calendar\"");
        assertThat(h).contains("live-empty");
    }

    @Test
    void editorPreview_keepsEverythingVisible() throws Exception {
        String h = html("/wedding/" + emptyWedding() + "?preview=1");
        assertThat(h).doesNotContain("is-live");
        assertThat(h).doesNotContain("live-empty");
    }

    @Test
    void filledWedding_hasNoEmptyMarkers() throws Exception {
        WeddingDto d = new WeddingDto();
        d.setMainDesign("basic");
        d.setWeddingDate("2026-12-12");
        d.setWeddingTime("13:00");
        d.setWeddingPlace("더채플앳청담");
        Long id = weddingService.save(d).getId();
        String h = html("/wedding/" + id);
        assertThat(h).contains("class=\"wc is-live\"");
        assertThat(h).doesNotContain("no-date").doesNotContain("no-place");
    }

    /** 게시 전 필수 항목 확인 — 이름 둘, 날짜, 예식장 */
    @Test
    void publishCheck_coversRequiredFields() throws Exception {
        String js = new String(new ClassPathResource("static/js/editor.js").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        assertThat(js).contains("var PUBLISH_REQUIRED");
        for (String f : new String[]{"groomName", "brideName", "weddingDate", "weddingPlace"}) {
            assertThat(js).contains("[name=\"" + f + "\"]");
        }
        // 두 게시 버튼 모두 확인을 거친다
        assertThat(js.split("checkBeforePublish\\(function", -1).length - 1).isEqualTo(2);
    }
}
