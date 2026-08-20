package com.example.weddingexam.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.MultiValueMap;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * H2 콘솔 사용 여부. 기본값 false —
     * 프로덕션에서 /h2-console/ 이 로그인 없이 열려 있던 것을 막기 위해,
     * 콘솔 자체를 끄고 그 경로의 permitAll도 콘솔이 켜져 있을 때만 등록한다.
     * 로컬에서 쓰려면 H2_CONSOLE_ENABLED=true 로 띄울 것.
     */
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           KakaoOAuth2UserService kakaoUserService) throws Exception {
        http
            .csrf(csrf -> csrf
                // AJAX 엔드포인트는 CSRF 비활성화
                .ignoringRequestMatchers("/api/**", "/admin/**")
            )
            .headers(headers -> headers
                // H2 콘솔 iframe 허용
                .frameOptions(frame -> frame.sameOrigin())
            )
            .authorizeHttpRequests(auth -> auth
                // 공개 경로
                .requestMatchers(
                    "/", "/login", "/login/**",
                    "/w/**", "/wedding/**",
                    "/css/**", "/js/**", "/images/**", "/fonts/**",
                    // 파비콘류는 루트 경로라 위 정적 패턴에 안 걸린다 — 따로 열어줘야
                    // 브라우저가 탭 아이콘을 못 받고 로그인 페이지로 튕긴다
                    "/favicon.svg", "/favicon-32.png", "/favicon.ico", "/apple-touch-icon.png",
                    // /api/map/** 은 공개하지 않는다 — 편집기(로그인 필수)만 쓰는 카카오 API
                    // 프록시라, 열어두면 누구나 우리 REST 키 할당량을 소진시킬 수 있다.
                    "/api/wedding/*/calendar.ics",
                    "/error"
                ).permitAll()
                // 하객 참석 응답 제출 — 비로그인 공개
                .requestMatchers(HttpMethod.POST, "/api/rsvp").permitAll()
                // 방명록 — 하객이 읽고(GET) 쓰고(POST) 본인 글을 지운다(POST /{id}/delete).
                // 삭제는 PIN 검증을 서비스가 하므로 인증 없이 열어둔다.
                .requestMatchers(HttpMethod.GET,  "/api/guestbook").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/guestbook").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/guestbook/*/delete").permitAll()
                // 슈퍼어드민
                .requestMatchers("/superadmin/**").hasRole("ADMIN")
                // H2 콘솔 — 명시적으로 켠 환경(로컬)에서만 공개
                .requestMatchers("/h2-console/**").access((authentication, ctx) ->
                        new org.springframework.security.authorization.AuthorizationDecision(h2ConsoleEnabled))
                // 그 외는 로그인 필요
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(info -> info.userService(kakaoUserService))
                .tokenEndpoint(token -> token.accessTokenResponseClient(kakaoTokenClient()))
                .defaultSuccessUrl("/my/edit", true)
                .failureHandler((req, res, ex) -> {
                    System.err.println("[OAuth2 ERROR] " + ex.getMessage());
                    Throwable c = ex.getCause();
                    while (c != null) { System.err.println("[OAuth2 CAUSE] " + c.getMessage()); c = c.getCause(); }
                    res.sendRedirect("/login?error=true");
                })
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> kakaoTokenClient() {
        var converter = new OAuth2AuthorizationCodeGrantRequestEntityConverter();
        var client = new DefaultAuthorizationCodeTokenResponseClient();
        client.setRequestEntityConverter(request -> {
            RequestEntity<?> entity = converter.convert(request);
            if (entity == null) return null;
            @SuppressWarnings("unchecked")
            MultiValueMap<String, String> body = (MultiValueMap<String, String>) entity.getBody();
            if (body != null) {
                String secret = body.getFirst("client_secret");
                if (secret == null || secret.isBlank()) {
                    body.remove("client_secret");
                }
            }
            return entity;
        });
        return client;
    }
}
