package com.example.weddingexam.security;

import com.example.weddingexam.user.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User delegate;
    private final UserEntity userEntity;

    public CustomOAuth2User(OAuth2User delegate, UserEntity userEntity) {
        this.delegate = delegate;
        this.userEntity = userEntity;
    }

    public Long getUserId()      { return userEntity.getId(); }
    public String getUserName2() { return userEntity.getName(); }
    public String getUserRole()  { return userEntity.getRole(); }
    public UserEntity getEntity(){ return userEntity; }

    @Override
    public Map<String, Object> getAttributes() { return delegate.getAttributes(); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + userEntity.getRole()));
    }

    @Override
    public String getName() { return String.valueOf(userEntity.getId()); }
}
