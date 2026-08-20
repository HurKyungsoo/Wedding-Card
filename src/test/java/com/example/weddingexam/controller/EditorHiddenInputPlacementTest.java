package com.example.weddingexam.controller;

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
 * 편집기의 히든 입력이 자기 섹션(.ed-section) 안에 렌더되는지 검증.
 *
 * revertSection()은 #sec-xxx 안쪽 폼 요소만 훑어서 되돌린다. 히든 입력을 폼 최상단에
 * 모아두면 그 섹션의 "되돌리기"가 해당 값을 되돌리지 못한다 — 실제로 캘린더·D-Day는
 * 되돌리기가 완전히 무동작이었고, 메인테마는 사진·위치·확대가 되돌아가지 않았다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:editor-hidden-placement-test;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
class EditorHiddenInputPlacementTest {

    @Autowired private MockMvc mockMvc;

    private String editorHtml() throws Exception {
        return mockMvc.perform(get("/admin/edit"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** html에서 inputId가 sectionId 섹션과 그 다음 섹션 사이에 있는지 확인 */
    private void assertInsideSection(String html, String inputId, String sectionId, String nextSectionId) {
        int sectionStart = html.indexOf("id=\"" + sectionId + "\"");
        int sectionEnd   = html.indexOf("id=\"" + nextSectionId + "\"");
        int input        = html.indexOf("id=\"" + inputId + "\"");

        assertThat(sectionStart).as(sectionId + " 섹션이 존재해야 함").isGreaterThan(-1);
        assertThat(sectionEnd).as(nextSectionId + " 섹션이 존재해야 함").isGreaterThan(sectionStart);
        assertThat(input).as(inputId + " 히든 입력이 존재해야 함").isGreaterThan(-1);
        assertThat(input)
            .as(inputId + " 는 " + sectionId + " 안에 있어야 되돌리기가 동작한다")
            .isBetween(sectionStart, sectionEnd);
    }

    @Test
    void mainPhotoInputs_areInsideMainSection() throws Exception {
        String html = editorHtml();
        assertInsideSection(html, "mainPhotoBase64",     "sec-main", "sec-basic");
        assertInsideSection(html, "mainPhotoPosXInput",  "sec-main", "sec-basic");
        assertInsideSection(html, "mainPhotoPosYInput",  "sec-main", "sec-basic");
        assertInsideSection(html, "mainPhotoScaleInput", "sec-main", "sec-basic");
        assertInsideSection(html, "photoFilterInput",    "sec-main", "sec-basic");
    }

    @Test
    void calendarAndDdayStyleInputs_areInsideTheirSections() throws Exception {
        String html = editorHtml();
        assertInsideSection(html, "calStyleInput",  "sec-cal",  "sec-dday");
        assertInsideSection(html, "ddayStyleInput", "sec-dday", "sec-gal");
    }

    @Test
    void galleryImagesInput_isInsideGallerySection() throws Exception {
        String html = editorHtml();
        assertInsideSection(html, "galleryImagesInput", "sec-gal", "sec-map");
    }

    /** deceasedDisplayType 히든 입력이 둘로 중복되면 FormData에서 name이 충돌한다 */
    @Test
    void deceasedDisplayType_hasExactlyOneHiddenInput() throws Exception {
        String html = editorHtml();
        int count = html.split("name=\"deceasedDisplayType\"", -1).length - 1;
        assertThat(count).isEqualTo(1);
    }
}
