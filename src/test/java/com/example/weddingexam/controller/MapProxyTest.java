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
 * 지도 검색이 서버 프록시를 거치는지 검증.
 *
 * 편집기가 브라우저에서 dapi.kakao.com을 직접 호출하려면 REST 키를 JS에 박아야 하고,
 * 이 레포는 public이라 그 키가 그대로 공개된다(실제로 그런 상태였음).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:map-proxy-test;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret",
    "kakao.map.appkey=",
    "kakao.map.restkey="
})
class MapProxyTest {

    @Autowired private MockMvc mockMvc;

    /** 편집기 JS에 카카오 API 키가 하드코딩돼 있으면 안 된다 */
    @Test
    void editorJs_containsNoHardcodedKakaoKey() throws Exception {
        String js = new String(new ClassPathResource("static/js/editor.js").getInputStream().readAllBytes(),
                               StandardCharsets.UTF_8);
        assertThat(js).doesNotContain("KakaoAK");
        assertThat(js).doesNotContain("dapi.kakao.com/v2/local");
    }

    /** 한글 검색어가 두 번 인코딩되면(%EA → %25EA) 카카오가 글자 그대로를 검색해 결과가 0건이 된다 */
    @Test
    void kakaoLocalUri_encodesQueryExactlyOnce() {
        String uri = WeddingController.kakaoLocalUri("keyword", "그랜드 웨딩홀").toString();
        assertThat(uri).startsWith("https://dapi.kakao.com/v2/local/search/keyword.json?query=");
        assertThat(uri).contains("%EA%B7%B8%EB%9E%9C%EB%93%9C");
        assertThat(uri).doesNotContain("%25");
        assertThat(uri).endsWith("&size=15");
        assertThat(java.net.URLDecoder.decode(uri.substring(uri.indexOf("query=") + 6, uri.indexOf("&size")),
                java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("그랜드 웨딩홀");
    }

    /** 키가 없는 환경(로컬 개발 등)에서도 500이 아니라 빈 결과를 돌려줘야 한다 */
    @Test
    void searchProxies_returnEmptyResultWithoutKey() throws Exception {
        for (String path : new String[]{"/api/map/search", "/api/map/address"}) {
            String body = mockMvc.perform(get(path).param("query", "그랜드 웨딩홀"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(body).as(path + " 응답").contains("\"documents\":[]");
        }
    }
}
