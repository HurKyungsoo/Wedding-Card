package com.example.weddingexam.controller;

import com.example.weddingexam.dto.WeddingDto;
import com.example.weddingexam.service.WeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 혼주 섹션의 관계(장남/장녀)와 세례명이 편집기에서 저장되고 청첩장에 실제로 표시되는지 검증.
 *
 * 이전에는 관계는 저장만 되고 청첩장에 렌더링 코드가 아예 없었고,
 * 세례명은 입력칸에 name 속성조차 없어 전송도 저장도 되지 않는 죽은 UI였다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:hosts-relation-baptism-test;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
class HostsRelationAndBaptismTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WeddingService weddingService;

    private String invitationHtml(String slug) throws Exception {
        return mockMvc.perform(get("/w/" + slug))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
                .replaceAll("\\s+", " ");
    }

    private WeddingDto savedWithBaptismAndRelation() {
        WeddingDto dto = weddingService.getDefaultDto();
        dto.setGroomName("박지훈");
        dto.setBrideName("이수아");
        dto.setGroomRelation("장남");
        dto.setBrideRelation("차녀");
        dto.setGroomBaptism("바오로");
        dto.setBrideBaptism("마리아");
        dto.setGroomFatherBaptism("요셉");
        dto.setGroomMotherBaptism("안나");
        dto.setBrideFatherBaptism("베드로");
        dto.setBrideMotherBaptism("데레사");
        return weddingService.save(dto);
    }

    @Test
    void baptismFields_surviveSaveAndReload() {
        WeddingDto saved = savedWithBaptismAndRelation();
        WeddingDto reloaded = weddingService.findById(saved.getId());

        assertThat(reloaded.getGroomBaptism()).isEqualTo("바오로");
        assertThat(reloaded.getBrideBaptism()).isEqualTo("마리아");
        assertThat(reloaded.getGroomFatherBaptism()).isEqualTo("요셉");
        assertThat(reloaded.getGroomMotherBaptism()).isEqualTo("안나");
        assertThat(reloaded.getBrideFatherBaptism()).isEqualTo("베드로");
        assertThat(reloaded.getBrideMotherBaptism()).isEqualTo("데레사");
    }

    @Test
    void invitation_showsRelationAndBaptism() throws Exception {
        WeddingDto saved = savedWithBaptismAndRelation();
        String html = invitationHtml(saved.getSlug());

        // 관계 — 이름 앞에 표시 ("장남 박지훈")
        assertThat(html).contains("data-relation=\"groom\">장남<");
        assertThat(html).contains("data-relation=\"bride\">차녀<");

        // 세례명 — 괄호는 CSS가 붙이므로 값만 들어간다
        assertThat(html).contains("data-baptism=\"groom\">바오로<");
        assertThat(html).contains("data-baptism=\"bride\">마리아<");
        assertThat(html).contains("data-baptism=\"groomFather\">요셉<");
        assertThat(html).contains("data-baptism=\"groomMother\">안나<");
        assertThat(html).contains("data-baptism=\"brideFather\">베드로<");
        assertThat(html).contains("data-baptism=\"brideMother\">데레사<");
    }

    /**
     * 세례명 미입력이면 빈 span이라야 CSS :empty 가 괄호까지 통째로 숨긴다.
     * 괄호를 마크업이 아니라 CSS(::before/::after)로 붙이는 이유가 이것 —
     * 마크업에 넣으면 값이 비었을 때 "()"만 덩그러니 남는다.
     */
    @Test
    void invitation_leavesBaptismSpanEmptyWhenNotEntered() throws Exception {
        WeddingDto dto = weddingService.getDefaultDto();
        dto.setGroomBaptism(null);
        dto.setGroomFatherBaptism("");
        WeddingDto saved = weddingService.save(dto);

        String html = invitationHtml(saved.getSlug());

        assertThat(html).contains("data-baptism=\"groom\"></span>");
        assertThat(html).contains("data-baptism=\"groomFather\"></span>");
    }

    /** 괄호와 숨김 처리는 CSS가 담당한다 — 이 규칙이 사라지면 빈 "()"가 노출된다 */
    @Test
    void css_addsParenthesesAndHidesEmptyBaptism() throws Exception {
        String css = new String(new org.springframework.core.io.ClassPathResource("static/css/invitation.css")
                .getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                .replaceAll("\\s+", "");

        assertThat(css).contains(".host-baptism::before{content:'('");
        assertThat(css).contains(".host-baptism::after{content:')'");
        assertThat(css).contains(".host-baptism:empty{display:none;}");
        assertThat(css).contains(".host-relation:empty,");
    }

    /** 편집기 세례명 입력칸이 폼에 바인딩돼 있어야 전송·복원된다 */
    @Test
    void editor_bindsBaptismInputsToForm() throws Exception {
        String html = mockMvc.perform(get("/admin/edit"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        for (String field : new String[]{"groomBaptism", "brideBaptism",
                "groomFatherBaptism", "groomMotherBaptism",
                "brideFatherBaptism", "brideMotherBaptism"}) {
            assertThat(html).as(field + " 입력칸에 name 바인딩").contains("name=\"" + field + "\"");
        }
    }

    /** 아무 효과가 없던 인사말 정렬 탭은 제거됨 */
    @Test
    void editor_noLongerShowsGreetingAlignTabs() throws Exception {
        String html = mockMvc.perform(get("/admin/edit"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("id=\"alignTabs\"");
        assertThat(html).doesNotContain("id=\"alignInput\"");
    }
}
