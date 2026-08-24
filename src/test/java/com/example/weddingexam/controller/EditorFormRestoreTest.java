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
 * 편집기 메인테마 섹션이 저장된 값을 다시 보여주는지 검증.
 * 이 값들이 복원되지 않으면 편집기를 다시 열었을 때 기본값이 표시되고,
 * 이후 자동저장이 그 기본값으로 사용자의 선택을 덮어쓴다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:editor-restore-test;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
class EditorFormRestoreTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WeddingService weddingService;

    @Test
    void editForm_restoresSavedThemeFontEffectAndColor() throws Exception {
        // /admin/edit 는 findFirst() 기준이므로 첫 번째 청첩장을 직접 수정한다
        WeddingDto target = weddingService.findFirst();
        Long id = (target.getId() != null) ? target.getId() : weddingService.save(target).getId();

        target.setMainFont("playfair");
        target.setMainFontSize("lg");
        target.setColorEffect("warm");
        target.setMainEffect("fog");
        target.setMainFontColor("#123456");
        weddingService.update(id, target);

        String html = mockMvc.perform(get("/admin/edit"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String norm = html.replaceAll("\\s+", " ");

        // 글꼴은 select 가 아니라 히든 입력이 값을 든다 — 목록의 각 줄을 실제 그 글꼴로
        // 렌더해야 해서 커스텀 드롭다운으로 바꿨다. 복원돼야 한다는 것 자체는 그대로다.
        assertThat(norm).containsPattern("id=\"fontVal\"[^>]*value=\"playfair\"");
        assertThat(norm).contains("value=\"lg\" selected");
        assertThat(norm).contains("value=\"warm\" selected");
        assertThat(norm).contains("value=\"fog\" selected");
        assertThat(norm).contains("#123456");

        // 저장하지 않은 선택지가 잘못 선택돼 있으면 안 된다
        assertThat(norm).doesNotContainPattern("id=\"fontVal\"[^>]*value=\"noto\"");
        assertThat(norm).doesNotContain("value=\"vintage\" selected");
    }

    @Test
    void editForm_withNoSavedColor_leavesColorInputEmptyForJsDefault() throws Exception {
        WeddingDto target = weddingService.findFirst();
        Long id = (target.getId() != null) ? target.getId() : weddingService.save(target).getId();
        target.setMainFontColor(null);
        weddingService.update(id, target);

        String html = mockMvc.perform(get("/admin/edit"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String norm = html.replaceAll("\\s+", " ");

        // 색상 미지정 시 hex 입력은 비어 있어야 JS가 테마 기본색을 채운다
        assertThat(norm).contains("id=\"fontColorHex\" name=\"mainFontColor\" value=\"\"");
    }
}
