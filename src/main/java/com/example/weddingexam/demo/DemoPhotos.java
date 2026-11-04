package com.example.weddingexam.demo;

import java.util.Map;
import java.util.Set;

/**
 * 랜딩 체험 폰·테마 미리보기(/demo)와 편집기 샘플 사진이 쓰는 예시 사진.
 *
 * 메인 사진도 갤러리처럼 정적 파일 URL 을 그대로 쓴다(예전엔 base64 로 바꿔 HTML 에 박았다 — 데모 한 장에 350KB).
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

    private DemoPhotos() {}

    public static boolean isDesign(String key) {
        return key != null && DESIGNS.contains(key);
    }

    public static String photoUrl(String design) {
        return PHOTO_BY_DESIGN.getOrDefault(design, PHOTO_BY_DESIGN.get("our_story"));
    }
}
