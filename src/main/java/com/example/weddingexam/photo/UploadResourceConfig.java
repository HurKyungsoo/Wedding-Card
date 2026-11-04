package com.example.weddingexam.photo;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

/**
 * /uploads/** → 사진 저장 폴더.
 * 파일 이름이 내용 해시라 내용이 바뀌면 URL 도 바뀐다 — 1년 + immutable 로 캐시해도 안전하다.
 */
@Configuration
@EnableScheduling
public class UploadResourceConfig implements WebMvcConfigurer {

    private final PhotoStorage storage;

    public UploadResourceConfig(PhotoStorage storage) {
        this.storage = storage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 폴더 위치는 끝에 '/' 가 있어야 하위 파일로 풀린다 — Path.toUri() 는 폴더가 아직 없으면 '/' 를 안 붙인다
        String location = storage.dir().toUri().toString();
        if (!location.endsWith("/")) location += "/";
        registry.addResourceHandler(PhotoStorage.URL_PREFIX + "**")
                .addResourceLocations(location)
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
    }
}
