package com.example.weddingexam.controller;

import com.example.weddingexam.dto.WeddingDto;
import com.example.weddingexam.security.CustomOAuth2User;
import com.example.weddingexam.service.WeddingService;
import com.example.weddingexam.user.UserEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 한 사용자가 청첩장을 여러 개 갖는 경우.
 *
 * 예전 resolveWeddingId()는 로그인 사용자면 요청 본문의 id를 무시하고 무조건
 * "첫 번째 청첩장"을 돌려줬다. 청첩장이 하나뿐일 땐 드러나지 않았지만, 마이페이지로
 * 여러 개를 만들 수 있게 되면 2번째를 편집해도 1번째에 저장되는 데이터 훼손이 된다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:multi-wedding-test;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
class MultipleWeddingsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WeddingService weddingService;
    @Autowired private ObjectMapper om;

    /* 테스트마다 새 사용자 id — 같은 인메모리 DB를 공유하므로 사용자를 나눠야
       한 테스트(상한 테스트 등)가 다른 테스트를 깨뜨리지 않는다 */
    private static final java.util.concurrent.atomic.AtomicLong SEQ =
            new java.util.concurrent.atomic.AtomicLong(9000);
    private long userA;
    private long userB;

    @org.junit.jupiter.api.BeforeEach
    void freshUsers() {
        userA = SEQ.incrementAndGet();
        userB = SEQ.incrementAndGet();
    }

    @AfterEach
    void clearAuth() { SecurityContextHolder.clearContext(); }

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

    private void publish(Long weddingId, String groomName) throws Exception {
        WeddingDto dto = weddingService.findById(weddingId);
        dto.setGroomName(groomName);
        mockMvc.perform(post("/api/admin/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(dto)));
    }

    /** ★ 핵심 — 2번째 청첩장을 편집하면 2번째에 저장돼야 한다 */
    @Test
    void publishing_writesToTheRequestedWedding_notTheFirst() throws Exception {
        loginAs(userA);
        Long first  = weddingService.createForUser(userA).getId();
        Long second = weddingService.createForUser(userA).getId();

        publish(second, "두번째신랑");

        assertThat(weddingService.findById(second).getGroomName()).isEqualTo("두번째신랑");
        assertThat(weddingService.findById(first).getGroomName())
            .as("다른 청첩장이 덮어써지면 안 된다")
            .isNotEqualTo("두번째신랑");
    }

    /** 남의 청첩장 id를 보내도 그쪽에 쓰이면 안 된다 */
    @Test
    void publishing_cannotTargetAnotherUsersWedding() throws Exception {
        Long victim = weddingService.createForUser(userB).getId();
        String before = weddingService.findById(victim).getGroomName();

        loginAs(userA);
        weddingService.createForUser(userA);
        publish(victim, "침입자");

        assertThat(weddingService.findById(victim).getGroomName())
            .as("남의 청첩장은 그대로여야 한다")
            .isEqualTo(before);
    }

    /** 마이페이지는 내 청첩장만 보여준다 */
    @Test
    void myPage_listsOnlyMyWeddings() throws Exception {
        weddingService.createForUser(userB);
        loginAs(userA);
        Long mine = weddingService.createForUser(userA).getId();
        WeddingDto m = weddingService.findById(mine);
        m.setGroomName("내신랑");
        weddingService.update(mine, m);

        String html = mockMvc.perform(get("/my"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(html).contains("내신랑");
        assertThat(weddingService.findByUserIdOrderByCreatedAtDesc(userA)).hasSize(1);
    }

    /** 남의 청첩장을 id로 열 수 없다 */
    @Test
    void editor_cannotOpenAnotherUsersWedding() throws Exception {
        Long victim = weddingService.createForUser(userB).getId();
        loginAs(userA);

        mockMvc.perform(get("/my/edit").param("id", String.valueOf(victim)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/my"));
    }

    /** 여러 개를 갖고 있으면 id 없이 편집기에 못 들어가고 목록으로 보낸다 */
    @Test
    void editor_withoutId_redirectsToMyPageWhenMultiple() throws Exception {
        loginAs(userA);
        weddingService.createForUser(userA);
        weddingService.createForUser(userA);

        mockMvc.perform(get("/my/edit"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/my"));
    }

    /** 삭제는 소유자만 */
    @Test
    void delete_onlyRemovesOwnWedding() throws Exception {
        Long victim = weddingService.createForUser(userB).getId();
        loginAs(userA);
        Long mine = weddingService.createForUser(userA).getId();

        mockMvc.perform(post("/my/" + victim + "/delete"));
        assertThat(weddingService.findById(victim)).isNotNull();   // 남의 것은 남아있다

        mockMvc.perform(post("/my/" + mine + "/delete"));
        assertThat(weddingService.findByUserIdOrderByCreatedAtDesc(userA))
            .extracting(WeddingDto::getId).doesNotContain(mine);
    }

    /** 사용자당 상한을 넘겨 만들 수 없다 */
    @Test
    void create_isCappedPerUser() {
        for (int i = 0; i < WeddingService.MAX_WEDDINGS_PER_USER; i++) {
            weddingService.createForUser(userA);
        }
        assertThat(weddingService.countByUserId(userA))
            .isEqualTo(WeddingService.MAX_WEDDINGS_PER_USER);

        org.assertj.core.api.Assertions
            .assertThatThrownBy(() -> weddingService.createForUser(userA))
            .isInstanceOf(IllegalStateException.class);
    }
}
