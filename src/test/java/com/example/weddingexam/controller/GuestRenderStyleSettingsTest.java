package com.example.weddingexam.controller;

import com.example.weddingexam.dto.WeddingDto;
import com.example.weddingexam.service.WeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 글꼴 색상·글꼴 크기·색감 보정·연출 효과가 하객이 보는 실제 청첩장(서버 렌더)에도 반영되는지.
 *
 * 예전엔 이 넷이 편집기 postMessage 핸들러 안에서만 처리돼서,
 * 편집기 미리보기에서는 바뀌는데 게시 후 하객 화면에서는 전부 기본값으로 나왔다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:guest-render-style-test;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
class GuestRenderStyleSettingsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WeddingService weddingService;

    private String guestHtml(Long id) throws Exception {
        return mockMvc.perform(get("/wedding/" + id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private Long saveWith(String size, String effect, String color, String colorEffect) {
        WeddingDto d = new WeddingDto();
        d.setMainDesign("our_story");
        d.setMainFontSize(size);
        d.setMainEffect(effect);
        d.setMainFontColor(color);
        d.setColorEffect(colorEffect);
        return weddingService.save(d).getId();
    }

    /** 크기·연출 효과는 서버가 클래스로 바로 박아준다 — JS 가 돌기 전 첫 화면부터 맞게 */
    @Test
    void fontSizeAndEffect_areRenderedAsClasses() throws Exception {
        String html = guestHtml(saveWith("lg", "fog", null, null));
        assertThat(html).containsPattern("class=\"hero [^\"]*fs-lg[^\"]*\" id=\"heroSection\"");
        assertThat(html).containsPattern("id=\"effectLayer\"[^>]*class=\"[^\"]*eff-fog");
    }

    /** 색상·색감 보정은 편집기와 같은 함수(applyStyleSettings)에 저장값을 넣어 첫 로드에 적용한다 */
    @Test
    void colorAndColorEffect_areAppliedOnFirstLoad() throws Exception {
        String html = guestHtml(saveWith("md", "none", "#123456", "warm"));
        int init = html.indexOf("window.__WEDDING_STYLE = {");
        assertThat(init).as("첫 로드용 저장값이 있어야 한다").isGreaterThan(0);
        String block = html.substring(init, html.indexOf("};", init));
        // th:inline="javascript" 로 실제 값이 들어가야 한다 — 주석으로만 찍히면(/*#123456*/ null) 안 된다
        assertThat(block).contains("\"#123456\"").contains("\"warm\"");
        assertThat(block).doesNotContain("/*#123456*/");
        assertThat(html).contains("applyStyleSettings(initStyle);");
    }

    /** 편집기 실시간 반영도 같은 함수를 써야 한다 — 다시 둘로 갈라지면 이 버그가 돌아온다 */
    @Test
    void liveUpdate_usesSameFunction() throws Exception {
        String html = guestHtml(saveWith(null, null, null, null));
        assertThat(html).contains("function applyStyleSettings(d)");
        assertThat(html).contains("applyStyleSettings(d);");
    }

    /** 값이 없으면 클래스를 붙이지 않는다 */
    @Test
    void noSettings_noExtraClasses() throws Exception {
        String html = guestHtml(saveWith(null, null, null, null));
        assertThat(html).doesNotContainPattern("class=\"hero [^\"]*fs-[^\"]*\" id=\"heroSection\"");
        assertThat(html).doesNotContainPattern("id=\"effectLayer\"[^>]*class=\"[^\"]*eff-");
    }
}
