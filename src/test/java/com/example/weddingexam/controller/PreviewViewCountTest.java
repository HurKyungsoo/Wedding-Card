package com.example.weddingexam.controller;

import com.example.weddingexam.dto.WeddingDto;
import com.example.weddingexam.security.CustomOAuth2User;
import com.example.weddingexam.service.WeddingService;
import com.example.weddingexam.user.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 편집기 미리보기(iframe·전체 미리보기)가 조회수/방문로그를 올리지 않는지 검증.
 * 미리보기는 청첩장을 그대로 로드하므로, 제외하지 않으면 편집기를 열거나
 * 미리보기를 새로고침할 때마다 본인 트래픽이 통계에 그대로 쌓인다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:preview-viewcount-test;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
class PreviewViewCountTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WeddingService weddingService;

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    /** 편집기에서 쓰는 로그인 사용자 principal을 SecurityContext에 심는다 */
    private void loginAs(long userId) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setName("테스트 사용자");
        user.setRole("USER");
        CustomOAuth2User principal = new CustomOAuth2User(
            new DefaultOAuth2User(Collections.emptyList(), Map.of("id", userId), "id"), user);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private WeddingDto newWedding() {
        return weddingService.save(weddingService.getDefaultDto());
    }

    private int viewCountOf(Long id) {
        return weddingService.findById(id).getViewCount();
    }

    @Test
    void editorPreview_doesNotIncrementViewCount() throws Exception {
        WeddingDto w = newWedding();
        int before = viewCountOf(w.getId());
        loginAs(1001L);

        mockMvc.perform(get("/w/" + w.getSlug()).param("preview", "1"))
               .andExpect(status().isOk());

        assertThat(viewCountOf(w.getId())).isEqualTo(before);
    }

    @Test
    void normalVisit_incrementsViewCount() throws Exception {
        WeddingDto w = newWedding();
        int before = viewCountOf(w.getId());

        mockMvc.perform(get("/w/" + w.getSlug()))
               .andExpect(status().isOk());

        assertThat(viewCountOf(w.getId())).isEqualTo(before + 1);
    }

    /** 비로그인 하객이 URL에 ?preview=1 을 붙여 집계를 피하지 못해야 한다 */
    @Test
    void anonymousVisitor_cannotSuppressCountWithPreviewParam() throws Exception {
        WeddingDto w = newWedding();
        int before = viewCountOf(w.getId());

        mockMvc.perform(get("/w/" + w.getSlug()).param("preview", "1"))
               .andExpect(status().isOk());

        assertThat(viewCountOf(w.getId())).isEqualTo(before + 1);
    }
}
