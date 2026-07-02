package com.example.weddingexam.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.RequestEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.MultiValueMap;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
                    "/api/map/**",
                    "/h2-console/**",
                    "/error"
                ).permitAll()
                // 슈퍼어드민
                .requestMatchers("/superadmin/**").hasRole("ADMIN")
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
