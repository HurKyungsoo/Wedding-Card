package com.example.weddingexam.demo;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 랜딩 체험 폰·테마 미리보기(/demo)와 편집기 샘플 사진이 쓰는 예시 사진.
 *
 * 청첩장 템플릿은 메인 사진을 base64 로만 받으므로(data:image/jpeg) 정적 파일을 한 번 읽어
 * base64 로 바꿔 캐시한다. webp 를 jpeg 라벨로 넣으면 일부 브라우저가 못 그리므로 jpg 로 둔다.
 * 테마마다 레이아웃에 어울리는 사진이 달라서(전면형·아치·액자) 테마별로 고른다.
 */
public final class DemoPhotos {

    /** 청첩장 디자인 키 — 편집기 썸네일·랜딩·하객 화면이 같은 키를 쓴다 */
    public static final Set<String> DESIGNS =
        Set.of("basic", "our_story", "our_story_pink", "married", "forever", "the_marriage");

    /** 테마 → 예시 사진(정적 경로) */
    private static final Map<String, String> PHOTO_BY_DESIGN = Map.of(
        "basic",          "/images/demo/basic.jpg",
        "our_story",      "/images/demo/story.jpg",
        "our_story_pink", "/images/demo/story.jpg",
        "married",        "/images/demo/married.jpg",
        "forever",        "/images/demo/forever.jpg",
        "the_marriage",   "/images/demo/basic.jpg"
    );

    /** 갤러리 예시 — 템플릿이 '|||' 로 나눠 읽는다 */
    public static final String GALLERY = String.join("|||",
        "/images/demo/story.jpg", "/images/demo/basic.jpg", "/images/demo/married.jpg",
        "/images/demo/forever.jpg", "/images/demo/basic.jpg", "/images/demo/story.jpg");

    private static final Map<String, String> BASE64 = new ConcurrentHashMap<>();

    private DemoPhotos() {}

    public static boolean isDesign(String key) {
        return key != null && DESIGNS.contains(key);
    }

    public static String photoUrl(String design) {
        return PHOTO_BY_DESIGN.getOrDefault(design, PHOTO_BY_DESIGN.get("our_story"));
    }

    /** 테마 예시 사진의 base64 (data: 접두어 없이) */
    public static String photoBase64(String design) {
        String url = photoUrl(design);
        return BASE64.computeIfAbsent(url, u -> {
            try (InputStream in = new ClassPathResource("static" + u).getInputStream()) {
                return Base64.getEncoder().encodeToString(in.readAllBytes());
            } catch (IOException e) {
                throw new UncheckedIOException("예시 사진을 읽지 못했습니다: " + u, e);
            }
        });
    }
}
