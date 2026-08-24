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
 * 엔딩(마지막 인사) 섹션이 되살아난 뒤에도 이전과 같은 이유로 다시 죽지 않도록 고정한다.
 *
 * 이 기능은 한 번 추가됐다가(d800a4d) 30분 만에 되돌려졌다(3cbccf1) — 커밋 메시지엔 이유가
 * 안 적혀 있었지만, 코드를 보니 원인이 뚜렷했다: 하객 화면의 #endingPhotoImg 와 편집기의
 * #endingThumbImg 둘 다 th:if 로 렌더돼 있어서, 사진이 없던 청첩장에서 처음 사진을 올려도
 * JS(getElementById)가 그 엘리먼트를 못 찾아 라이브 미리보기·편집기 썸네일 어느 쪽도 갱신되지
 * 않았다(업로드했는데 화면엔 아무것도 안 보이는 상태). 이 프로젝트에서 반복적으로 나온
 * "th:if vs th:style" 버그 유형과 같다 — img 는 항상 DOM에 두고 src만 조건부로 채워야 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:ending-section-test;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
class EndingSectionLivePreviewTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WeddingService weddingService;

    private WeddingDto weddingWithout(String... unused) throws Exception {
        WeddingDto dto = weddingService.findFirst();
        Long id = (dto.getId() != null) ? dto.getId() : weddingService.save(dto).getId();
        dto.setId(id);
        dto.setEndingVisible(true);
        dto.setEndingPhotoBase64(null);
        dto.setEndingCaption(null);
        weddingService.update(id, dto);
        return dto;
    }

    private String guestHtml() throws Exception {
        return mockMvc.perform(get("/wedding/1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String editorHtml() throws Exception {
        return mockMvc.perform(get("/admin/edit"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** ★ 핵심 회귀 방지 — 사진이 없어도 하객 화면의 img 태그는 DOM에 있어야 라이브 미리보기가 된다 */
    @Test
    void guestPhotoTag_existsEvenWithoutAPhotoYet() throws Exception {
        weddingWithout();
        String html = guestHtml();

        assertThat(html)
            .as("th:if로 img 자체를 빼면 사진을 처음 올렸을 때 라이브 미리보기가 못 찾는다")
            .contains("id=\"endingPhotoImg\"");
    }

    /** ★ 편집기 썸네일도 마찬가지 — 여기가 실제로 화면이 '깨져 보이는' 지점이었다 */
    @Test
    void editorThumbTag_existsEvenWithoutAPhotoYet() throws Exception {
        weddingWithout();
        String html = editorHtml();

        assertThat(html)
            .as("th:if로 썸네일 img를 빼면 업로드해도 편집기 자체 미리보기에 사진이 안 보인다")
            .contains("id=\"endingThumbImg\"");
    }

    /** 사진 + 노출 둘 다 켜졌을 때만 섹션이 보인다 */
    @Test
    void section_visibleOnlyWhenPhotoExistsAndVisibleIsOn() throws Exception {
        WeddingDto dto = weddingWithout();
        dto.setEndingPhotoBase64("Zm9v");   // "foo"의 base64 — 실제 이미지일 필요 없음, 존재 여부만 검증
        dto.setEndingCaption("고맙습니다");
        weddingService.update(dto.getId(), dto);

        String html = guestHtml().replaceAll("\\s+", " ");
        assertThat(html).containsPattern("ending-sec[^>]*(?<!display:none)\"");
        assertThat(html).contains("Zm9v");
        assertThat(html).contains("고맙습니다");
    }

    /** 사진이 없으면 endingVisible=true여도 빈 섹션이 노출되면 안 된다 */
    @Test
    void section_staysHidden_whenVisibleButNoPhoto() throws Exception {
        weddingWithout();
        String html = guestHtml();

        assertThat(html).containsPattern("class=\"section-divider\"[^>]*data-vis=\"ending\"[^>]*style=\"display:\\s*none");
        assertThat(html).containsPattern("class=\"sec ending-sec\"[^>]*style=\"display:\\s*none");
    }

    /** 편집기 되돌리기 훅(SECTION_REVERT_HOOKS.ending)이 존재해야 한다 —
        없으면 섹션을 되돌려도 업로드 영역/썸네일 표시가 이전 상태로 안 맞는다 */
    @Test
    void editorRevertHook_isWired() throws Exception {
        String js = new String(
            getClass().getClassLoader()
                .getResourceAsStream("static/js/editor.js")
                .readAllBytes(),
            StandardCharsets.UTF_8);

        assertThat(js).contains("ending: function()");
    }
}
