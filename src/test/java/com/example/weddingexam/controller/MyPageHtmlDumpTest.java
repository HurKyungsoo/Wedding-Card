package com.example.weddingexam.controller;

import com.example.weddingexam.dto.WeddingDto;
import com.example.weddingexam.security.CustomOAuth2User;
import com.example.weddingexam.service.WeddingService;
import com.example.weddingexam.user.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 개발용 도구 — 마이페이지 HTML을 파일로 덤프한다.
 * 마이페이지는 로그인 필수라 로컬 브라우저로 열 수 없어서, 렌더된 HTML을 뽑아
 * 같은 오리진(정적 경로)으로 서빙해 화면을 확인하기 위한 용도.
 * 인메모리 DB를 써서 로컬 개발 DB를 건드리지 않는다.
 * 평소에는 비활성 — -DdumpMyPage=true 를 줄 때만 실행된다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:mypage-dump;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
@EnabledIfSystemProperty(named = "dumpMyPage", matches = "true")
class MyPageHtmlDumpTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WeddingService weddingService;

    @Test
    void dumpMyPageHtml() throws Exception {
        long userId = 4242L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setName("허경수");
        user.setRole("USER");
        CustomOAuth2User principal = new CustomOAuth2User(
            new DefaultOAuth2User(Collections.emptyList(), Map.of("id", userId), "id"), user);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        // 게시된 청첩장
        WeddingDto published = weddingService.createForUser(userId);
        published.setGroomName("박지훈");
        published.setBrideName("이수아");
        published.setWeddingDate("2026-10-24");
        published.setWeddingPlace("더채플앳청담 커티지홀");
        weddingService.update(published.getId(), published);

        // 작성 중(임시저장) 청첩장
        WeddingDto draft = weddingService.createForUser(userId);
        draft.setGroomName("김진호");
        draft.setBrideName("이나은");
        draft.setWeddingDate("2027-03-14");
        draft.setWeddingPlace("그랜드 웨딩홀");
        weddingService.saveDraft(draft.getId(), draft);

        // 아직 아무것도 안 채운 청첩장
        WeddingDto blank = weddingService.createForUser(userId);
        weddingService.saveDraft(blank.getId(), blank);

        String html = mockMvc.perform(get("/my"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Path out = Path.of(System.getProperty("dumpMyPagePath", "target/mypage-dump.html"));
        Files.createDirectories(out.getParent());
        Files.writeString(out, html, StandardCharsets.UTF_8);
        System.out.println("[DUMP] " + out.toAbsolutePath() + " (" + html.length() + " chars)");

        SecurityContextHolder.clearContext();
    }
}
