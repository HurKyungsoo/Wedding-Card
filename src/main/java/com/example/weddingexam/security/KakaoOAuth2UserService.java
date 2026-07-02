package com.example.weddingexam.security;

import com.example.weddingexam.user.UserEntity;
import com.example.weddingexam.user.UserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class KakaoOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserService userService;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    public KakaoOAuth2UserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        Map<String, Object> attrs = oAuth2User.getAttributes();

        String kakaoId = String.valueOf(attrs.get("id"));

        Map<String, Object> kakaoAccount = (Map<String, Object>) attrs.getOrDefault("kakao_account", Map.of());
        Map<String, Object> profile      = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Map.of());

        String nickname = (String) profile.getOrDefault("nickname", "사용자");
        String email    = (String) kakaoAccount.get("email");

        UserEntity user = userService.findByKakaoId(kakaoId).orElseGet(() -> {
            UserEntity u = new UserEntity();
            u.setKakaoId(kakaoId);
            u.setName(nickname);
            u.setEmail(email);
            return userService.save(u);
        });

        // 이름이 바뀐 경우 갱신
        if (!nickname.equals(user.getName())) {
            user.setName(nickname);
            userService.save(user);
        }

        return new CustomOAuth2User(oAuth2User, user);
    }
}
