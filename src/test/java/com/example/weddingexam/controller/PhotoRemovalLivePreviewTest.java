package com.example.weddingexam.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 메인 사진을 지웠을 때 아치형(forever) 테마에도 반영되는지 검증.
 *
 * 라이브 프리뷰의 사진 처리 분기가 둘로 나뉘어 있었는데, "사진 있음"에서만 아치를
 * 갱신하고 "사진 없음"에서는 히어로만 숨겨서 아치형에는 사진이 그대로 남아 있었다.
 * 두 분기를 applyMainPhotoPresence() 하나로 합쳐서 고쳤다.
 *
 * 실제 동작(브라우저)은 자동화로 확인했고, 여기서는 그 구조가 다시 갈라지지 않도록
 * "사진 처리 경로가 하나뿐이고 아치를 함께 다룬다"는 것만 고정한다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:photo-removal-test;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
class PhotoRemovalLivePreviewTest {

    @Autowired private MockMvc mockMvc;

    private String invitationHtml() throws Exception {
        return mockMvc.perform(get("/wedding/1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** 사진 표시/숨김은 한 함수로만 처리해야 한다 — 분기가 갈라지면 한쪽을 빠뜨리게 된다 */
    @Test
    void photoPresence_isHandledBySingleSharedFunction() throws Exception {
        String html = invitationHtml();

        assertThat(html)
            .as("공통 처리 함수가 있어야 한다")
            .contains("function applyMainPhotoPresence(");

        int calls = html.split("applyMainPhotoPresence\\(d\\.mainPhotoBase64\\)", -1).length - 1;
        assertThat(calls)
            .as("두 개의 메시지 핸들러가 모두 공통 함수를 써야 한다")
            .isEqualTo(2);
    }

    /** 공통 함수가 히어로뿐 아니라 아치형 이미지·플레이스홀더까지 다뤄야 한다 */
    @Test
    void sharedFunction_coversArchThemeToo() throws Exception {
        String html = invitationHtml();
        int start = html.indexOf("function applyMainPhotoPresence(");
        assertThat(start).isGreaterThan(-1);
        // 함수 본문 범위만 잘라서 확인 (뒤쪽의 다른 코드가 섞이지 않게 넉넉히 자름)
        String body = html.substring(start, Math.min(start + 3000, html.length()));

        assertThat(body).contains("hero-fullphoto-img");
        assertThat(body).contains("hero-fullphoto-placeholder");
        assertThat(body).as("아치형 사진을 함께 처리해야 한다").contains("forever-arch-img");
        assertThat(body).as("아치형 플레이스홀더도 함께 처리해야 한다").contains("forever-arch-placeholder");
    }

    /** 사진이 없는 상태로 렌더되면 양쪽 플레이스홀더가 마크업에 있어야 한다 */
    @Test
    void invitation_withoutPhoto_rendersBothPlaceholders() throws Exception {
        String html = invitationHtml().replaceAll("\\s+", " ");

        assertThat(html).contains("hero-fullphoto-placeholder");
        assertThat(html).contains("forever-arch-placeholder");
    }

    /** 예전처럼 사진 분기가 손으로 갈라져 있으면 안 된다 */
    @Test
    void noStrandedPhotoBranchRemains() throws Exception {
        String js = new String(new ClassPathResource("templates/invitation.html").getInputStream().readAllBytes(),
                               StandardCharsets.UTF_8);

        // 예전 코드의 흔적: 핸들러 안에서 직접 hero 이미지를 숨기던 구문
        assertThat(js)
            .as("사진 숨김을 핸들러에서 직접 하지 말고 공통 함수를 쓸 것")
            .doesNotContain("if (photoImg) photoImg.style.display = 'none';");
    }
}
