package com.example.weddingexam.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * H2 콘솔이 프로덕션에 노출되지 않는지 검증.
 *
 * 실제로 http://<서버>:8080/h2-console/ 이 로그인 없이 200으로 열려 있었다.
 * 원인은 두 가지가 겹친 것:
 *  ① application.properties 의 spring.h2.console.enabled=true
 *  ② SecurityConfig 의 permitAll 목록에 /h2-console/**
 * 서버 실행 커맨드에 --spring.profiles.active=prod 가 없어서
 * application-prod.properties 의 enabled=false 는 적용된 적이 없었다.
 */
@SpringBootTest
@AutoConfigureMockMvc            // 필터를 켠 채로 — 보안 규칙 자체를 검증해야 하므로
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:h2-console-test;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
class H2ConsoleExposureTest {

    @Autowired private MockMvc mockMvc;

    /** 기본 설정에서는 콘솔이 꺼져 있어야 한다 */
    @Test
    void h2Console_isDisabledByDefault(
            @Autowired org.springframework.core.env.Environment env) {
        assertThat(env.getProperty("spring.h2.console.enabled", Boolean.class, false)).isFalse();
    }

    /** 비로그인 접근이 200으로 뚫리면 안 된다 */
    @Test
    void h2Console_isNotPubliclyAccessible() throws Exception {
        int status = mockMvc.perform(get("/h2-console/"))
                .andReturn().getResponse().getStatus();

        assertThat(status)
            .as("/h2-console/ 는 비로그인으로 열리면 안 된다 (실제 status=%s)", status)
            .isNotEqualTo(200);
    }

    /** 공개 청첩장은 그대로 열려야 한다 — 보안 규칙을 과하게 조인 게 아닌지 확인 */
    @Test
    void publicInvitationPath_staysOpen() throws Exception {
        int status = mockMvc.perform(get("/login")).andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(200);
    }
}
