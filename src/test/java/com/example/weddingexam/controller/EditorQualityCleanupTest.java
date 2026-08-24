package com.example.weddingexam.controller;

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
 * 갤러리·계좌·참석응답 섹션에 없던 되돌리기 버튼을 추가한 작업 고정.
 *
 * 갤러리/계좌는 다른 섹션과 달리 히든 입력 하나가 아니라 배열 전체(galImages/acctData)가
 * 상태라, 되돌리기 버튼만 붙이면 "스냅샷 이후 새로 추가한 항목"이 안 지워지는 반쪽짜리
 * 동작이 된다(revertSection()의 기존 주석에 명시된 한계). 두 섹션 다 전용 되돌리기
 * 훅(SECTION_REVERT_HOOKS.gal/.acct)으로 배열 자체를 스냅샷으로 되돌리고 다시 그린다.
 * 실제 동작(추가 → 되돌리기 → 빈 상태로 복귀)은 브라우저로 확인했고, 여기서는 그 배선
 * 자체가 없어지지 않도록 고정한다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:editor-quality-cleanup-test;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
class EditorQualityCleanupTest {

    @Autowired private MockMvc mockMvc;

    private String editorHtml() throws Exception {
        return mockMvc.perform(get("/admin/edit"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String editorJs() throws Exception {
        return new String(
            getClass().getClassLoader().getResourceAsStream("static/js/editor.js").readAllBytes(),
            StandardCharsets.UTF_8);
    }

    @Test
    void galleryAccountRsvp_haveRevertButtons() throws Exception {
        String html = editorHtml();
        for (String key : new String[]{"gal", "acct", "rsvp"}) {
            assertThat(html)
                .as("'%s' 섹션에 되돌리기 버튼이 없다", key)
                .contains("revertSection(event,'" + key + "')");
        }
    }

    /** 배열 전체가 상태인 두 섹션은 전용 훅으로 되돌려야 새로 추가한 항목까지 지워진다 */
    @Test
    void galleryAndAccount_haveDedicatedRevertHooks() throws Exception {
        String js = editorJs();
        assertThat(js).as("gal 되돌리기 훅이 없다").contains("gal: function()");
        assertThat(js).as("acct 되돌리기 훅이 없다").contains("acct: function()");
        assertThat(js)
            .as("계좌 스냅샷이 captureSectionSnapshot()과 같은 시점에 떠져야 한다")
            .contains("savedAcctSnapshot = JSON.parse(JSON.stringify(acctData))");
    }

    /** 되돌린 뒤에도 photoFilter가 서버 기존값을 유지하도록 하는 배관은 그대로 둬야 한다 —
        지우면 다음 임시저장 때 payload에서 빠져 기존 값이 null로 덮어써진다 */
    @Test
    void photoFilterPlumbing_isIntentionallyKept() throws Exception {
        String html = editorHtml();
        assertThat(html).contains("id=\"photoFilterInput\"");
    }

    /** 순수 죽은 함수 정의 3종은 실제로 지워졌는지(재발 방지) */
    @Test
    void deadFunctionDefinitions_areGone() throws Exception {
        String js = editorJs();
        assertThat(js).doesNotContain("function searchWithDaumPostcode");
        assertThat(js).doesNotContain("function initKakaoMap");
        assertThat(js).doesNotContain("function searchAddress()");

        String invitationHtml = new String(
            getClass().getClassLoader().getResourceAsStream("templates/invitation.html").readAllBytes(),
            StandardCharsets.UTF_8);
        assertThat(invitationHtml).doesNotContain("function showHideSection");
        assertThat(invitationHtml).doesNotContain("function showHidePair");
        assertThat(invitationHtml).doesNotContain("var phoneMap");
    }

    /** 실사용 지도 검색 함수는 죽은 코드 정리 과정에서 다치지 않아야 한다 */
    @Test
    void realMapSearchFunction_stillExists() throws Exception {
        assertThat(editorJs()).contains("function doMapSearch");
    }
}
