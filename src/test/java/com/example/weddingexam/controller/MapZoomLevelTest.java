package com.example.weddingexam.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 지도 줌 값은 카카오맵 축척 막대에 찍히는 거리다 — level 3 = 50m, 4 = 100m, 5 = 250m.
 * 예전엔 편집기·청첩장 모두 두 단계씩 밀려(50M → level 5) 고른 것보다 넓게 보였다.
 * 버튼은 가깝게(50M)·보통(100M)·넓게(250M) 셋, 예전 값 20M·30M·500M 은 가까운 단계로 읽는다.
 */
class MapZoomLevelTest {

    private static final String EXPECTED = "{'20M':3,'30M':3,'50M':3,'100M':4,'250M':5,'500M':5}";

    private String read(String path) throws Exception {
        return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void editorAndInvitation_useSameScaleLevels() throws Exception {
        assertThat(read("static/js/editor.js")).contains("var ZOOM_LEVEL = " + EXPECTED);
        assertThat(read("static/js/invitation.js")).contains("var MAP_ZOOM_LEVELS = " + EXPECTED);
    }
}
