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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 랜딩 개편 — 체험용 샘플(/demo)과 "이 테마로 만들기"(/start → /my/create?design=…).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:landing-demo-test;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
class LandingDemoTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WeddingService weddingService;

    @AfterEach
    void clearAuth() { SecurityContextHolder.clearContext(); }

    private String html(String url) throws Exception {
        return mockMvc.perform(get(url)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** 랜딩이 끝까지 렌더된다 — 스크립트 속 [[ 같은 글자 때문에 Thymeleaf 가 중간에 멈추면 </html> 이 안 나온다 */
    @Test
    void landing_rendersToTheEnd() throws Exception {
        String h = html("/");
        assertThat(h).contains("id=\"edDemo\"");
        assertThat(h).contains("const NAMES");
        assertThat(h.trim()).endsWith("</html>");
    }

    /** 사진·갤러리가 채워진 실제 청첩장 화면이고, 고른 테마로 열린다 */
    @Test
    void demo_rendersSampleWithPhotoAndRequestedTheme() throws Exception {
        String h = html("/demo?theme=forever");
        assertThat(h).contains("design-forever");
        assertThat(h).contains("data:image/jpeg;base64,");
        assertThat(h).contains("/images/demo/");
        assertThat(h).contains("name=\"robots\" content=\"noindex\"");
        // 방명록·참석 응답 제출을 막는 스크립트
        assertThat(h).contains("체험용 샘플이라 실제로 저장되지는 않아요");
    }

    /** 모르는 테마 값이면 기본(Our story)으로 */
    @Test
    void demo_unknownTheme_fallsBackToOurStory() throws Exception {
        String h = mockMvc.perform(get("/demo").param("theme", "<script>")).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(h).contains("design-our_story").doesNotContain("design-<script>");
    }

    /** 랜딩 방문마다 샘플 청첩장 조회수가 오르면 안 된다 */
    @Test
    void demo_doesNotCountViews() throws Exception {
        WeddingDto sample = weddingService.findFirst();
        Integer before = weddingService.findById(sample.getId()).getViewCount();
        html("/demo");
        html("/demo?theme=married");
        assertThat(weddingService.findById(sample.getId()).getViewCount()).isEqualTo(before);
    }

    /** 체험용 샘플은 실제 청첩장에 저장되지 않는다 — 사진이 DB에 쓰이면 안 된다 */
    @Test
    void demo_doesNotPersistPhoto() throws Exception {
        WeddingDto sample = weddingService.findFirst();
        String before = weddingService.findById(sample.getId()).getMainPhotoBase64();
        html("/demo?theme=basic");
        assertThat(weddingService.findById(sample.getId()).getMainPhotoBase64()).isEqualTo(before);
    }

    /** 로그인 전 — 고른 테마를 세션에 적어 두고 만들기로 보낸다(로그인 성공 처리에서 이어 받음) */
    @Test
    void start_beforeLogin_remembersDesign() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/start").param("design", "the_marriage").session(session))
            .andExpect(redirectedUrl("/my/create"));
        assertThat(session.getAttribute(WeddingController.PENDING_DESIGN)).isEqualTo("the_marriage");

        // 모르는 값은 적지 않는다
        MockHttpSession s2 = new MockHttpSession();
        mockMvc.perform(get("/start").param("design", "evil").session(s2));
        assertThat(s2.getAttribute(WeddingController.PENDING_DESIGN)).isNull();
    }

    /** 로그인 상태 — 바로 그 테마로 새 청첩장을 만든다 */
    @Test
    void create_withDesign_startsWithThatTheme() throws Exception {
        long userId = 77001L;
        loginAs(userId);
        mockMvc.perform(get("/start").param("design", "forever"))
            .andExpect(redirectedUrl("/my/create?design=forever"));

        String location = mockMvc.perform(get("/my/create").param("design", "forever"))
            .andReturn().getResponse().getRedirectedUrl();
        assertThat(location).startsWith("/my/edit?id=");
        Long id = Long.valueOf(location.substring("/my/edit?id=".length()));
        assertThat(weddingService.findById(id).getMainDesign()).isEqualTo("forever");

        // 이상한 값이면 기본 테마로 만든다
        String loc2 = mockMvc.perform(get("/my/create").param("design", "nope"))
            .andReturn().getResponse().getRedirectedUrl();
        Long id2 = Long.valueOf(loc2.substring("/my/edit?id=".length()));
        assertThat(weddingService.findById(id2).getMainDesign()).isNotEqualTo("nope");
    }

    private void loginAs(long userId) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setName("테스트" + userId);
        user.setRole("USER");
        CustomOAuth2User principal = new CustomOAuth2User(
            new DefaultOAuth2User(Collections.emptyList(), Map.of("id", userId), "id"), user);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
