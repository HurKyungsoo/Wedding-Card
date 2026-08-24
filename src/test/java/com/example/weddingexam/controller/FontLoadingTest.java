package com.example.weddingexam.controller;

import com.example.weddingexam.dto.WeddingDto;
import com.example.weddingexam.service.WeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 글꼴은 네 군데에 흩어져 있고 넷이 어긋나면 조용히 폴백된다.
 *   ① edit.html 드롭다운 option value     — 사용자가 고르는 값
 *   ② WeddingDto.MAIN_FONT_CSS            — 서버가 첫 렌더에 쓰는 표
 *   ③ invitation.html 의 JS fontMap       — 편집기에서 실시간으로 바꿀 때 쓰는 표
 *   ④ 폰트 파일 로딩 (구글폰트 URL / 별도 CDN link)
 *
 * ②와 ③이 어긋나면 "미리보기에선 바뀌는데 저장하면 딴 글꼴"이 되고,
 * ④가 빠지면 목록엔 있는데 고르면 시스템 기본으로 떨어진다 —
 * 나눔명조가 실제로 그 상태로 한참 방치돼 있었다(2026-08-21 발견).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:font-loading-test;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
class FontLoadingTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WeddingService weddingService;

    private static String read(String p) throws Exception {
        return Files.readString(Path.of("src/main/resources", p), StandardCharsets.UTF_8);
    }

    /**
     * 사용자가 실제로 고를 수 있는 값들.
     * 목록 마크업은 서버가 내려주는 FONT_CHOICES 로 렌더되므로 그것이 곧 드롭다운이다 —
     * 템플릿에 이름을 또 적지 않기 위해 그렇게 만들었다.
     */
    private static Set<String> dropdownValues() {
        Set<String> vals = new LinkedHashSet<>();
        for (WeddingDto.FontChoice f : WeddingDto.getFontChoices()) vals.add(f.key());
        return vals;
    }

    /** invitation.html 안 JS fontMap 의 키 → CSS 스택 */
    private static java.util.Map<String, String> jsFontMap() throws Exception {
        String html = read("templates/invitation.html");
        int from = html.indexOf("var fontMap = {");
        assertThat(from).as("fontMap 을 찾지 못했다").isGreaterThan(0);
        int to = html.indexOf("};", from);
        Matcher m = Pattern.compile("([a-z_]+)\\s*:\\s*\"([^\"]+)\"").matcher(html.substring(from, to));
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        while (m.find()) map.put(m.group(1), m.group(2));
        return map;
    }

    /** ★ 서버 표와 미리보기 표가 글자 하나까지 같아야 한다 */
    @Test
    void serverFontMap_andLivePreviewFontMap_agree() throws Exception {
        WeddingDto dto = new WeddingDto();
        for (var e : jsFontMap().entrySet()) {
            dto.setMainFont(e.getKey());
            assertThat(dto.getMainFontCss())
                .as("'%s' 의 CSS 스택이 서버와 미리보기에서 다르다", e.getKey())
                .isEqualTo(e.getValue());
        }
    }

    /** 드롭다운에 있는 값은 전부 두 표에 있어야 한다 — 없으면 고르는 순간 폴백된다 */
    @Test
    void everyDropdownFont_isMapped() throws Exception {
        java.util.Map<String, String> js = jsFontMap();
        WeddingDto dto = new WeddingDto();
        String notoCss = dto.getMainFontCss();          // mainFont == null → 기본값

        for (String v : dropdownValues()) {
            dto.setMainFont(v);
            if (!"noto".equals(v)) {
                assertThat(dto.getMainFontCss())
                    .as("드롭다운의 '%s' 가 서버 표에 없어 기본값으로 떨어진다", v)
                    .isNotEqualTo(notoCss);
            }
            assertThat(js).as("드롭다운의 '%s' 가 미리보기 표에 없다", v).containsKey(v);
        }
    }

    /** 새로 넣은 세 글꼴이 실제로 목록에 있다 */
    @Test
    void newKoreanFonts_areOffered() {
        assertThat(dropdownValues())
            .contains("pretendard", "maru_buri", "gyeonggi_batang");
    }

    /**
     * ★ 목록의 각 줄이 "그 글꼴로" 렌더돼야 한다.
     * 이 선택기의 존재 이유가 그것이다 — 이름만 늘어놓을 거면 네이티브 select 로 충분했다.
     */
    @Test
    void editorRendersEveryChoice_inItsOwnTypeface() throws Exception {
        String html = mockMvc.perform(get("/admin/edit"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)
                .replaceAll("\\s+", " ")
                .replace("&#39;", "'");   // Thymeleaf 가 속성 안 작은따옴표를 이스케이프한다

        for (WeddingDto.FontChoice f : WeddingDto.getFontChoices()) {
            assertThat(html)
                .as("'%s' 가 글꼴 목록에 렌더되지 않았다", f.key())
                .contains("data-font=\"" + f.key() + "\"");
        }
        // 줄마다 style="font-family:..." 이 붙어 있어야 실제 그 서체로 보인다
        assertThat(html).contains("font-family:'Maru Buri'");
        assertThat(html).contains("font-family:'Gyeonggi Batang'");
        assertThat(html).contains("font-family:'Pretendard'");
    }

    /** 편집기는 고르기 전에 모양을 보여줘야 하므로 선택지를 전부 받아둔다 */
    @Test
    void editorPage_loadsEveryFontItOffers() throws Exception {
        String html = mockMvc.perform(get("/admin/edit"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(html).as("마루부리 CSS 가 편집기에 없다").contains(MARU);
        assertThat(html).as("경기천년바탕 CSS 가 편집기에 없다").contains(GYEONGGI);
        assertThat(html).as("프리텐다드가 편집기에 없다").contains("orioncactus/pretendard");
        for (String fam : new String[]{"Gowun+Batang", "Nanum+Myeongjo", "Song+Myung", "Gowun+Dodum"}) {
            assertThat(html).as("%s 가 편집기에 없어 목록에서 폴백된다", fam).contains(fam);
        }
    }

    /** 글자 크기는 종전 그대로 작게/보통/크게 — 글꼴 선택기를 바꾸면서 딸려 바뀌면 안 된다 */
    @Test
    void fontSizeControl_isUnchanged() throws Exception {
        String html = mockMvc.perform(get("/admin/edit"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(html).contains("id=\"fontSizeSelect\"").contains("name=\"mainFontSize\"");
        for (String v : new String[]{"sm", "md", "lg"}) {
            assertThat(html).contains("value=\"" + v + "\"");
        }
        assertThat(html).contains("작게").contains("보통").contains("크게");
    }

    /* ── 로딩: 고른 사람만 받는다 ── */

    private String guestHtml(String font) throws Exception {
        WeddingDto dto = weddingService.createForUser(4242L);
        dto.setMainFont(font);
        weddingService.update(dto.getId(), dto);
        return mockMvc.perform(get("/wedding/" + dto.getId()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static final String MARU = "MaruBuri-dynamic-subset.css";
    private static final String GYEONGGI = "GyeonggiBatang-dynamic-subset.css";

    /** 마루부리를 고른 청첩장만 마루부리 CSS 를 받는다 */
    @Test
    void guestGetsCdnFont_onlyWhenChosen() throws Exception {
        assertThat(guestHtml("maru_buri")).contains(MARU);

        String noto = guestHtml("noto");
        assertThat(noto).as("안 고른 하객은 한 바이트도 받으면 안 된다")
            .doesNotContain(MARU).doesNotContain(GYEONGGI);
    }

    /** 경기천년바탕도 같은 규칙 — 그리고 서로 섞이지 않는다 */
    @Test
    void chosenCdnFont_doesNotDragInTheOther() throws Exception {
        String html = guestHtml("gyeonggi_batang");
        assertThat(html).contains(GYEONGGI);
        assertThat(html).doesNotContain(MARU);
    }

    /** 편집기 미리보기는 바꿔가며 보므로 전부 미리 받는다 */
    @Test
    void editorPreview_loadsEveryCdnFont() throws Exception {
        WeddingDto dto = weddingService.createForUser(4243L);
        String html = mockMvc.perform(get("/wedding/" + dto.getId()).param("preview", "1"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(html).contains(MARU).contains(GYEONGGI);
    }

    /**
     * 프리텐다드는 CDN 조건부 목록에 넣으면 안 된다 —
     * invitation.css 본문이 항상 쓰므로 이미 무조건 로드된다. 넣으면 같은 걸 두 번 받는다.
     */
    @Test
    void pretendard_isAlwaysLoadedAndNotDuplicated() throws Exception {
        String html = guestHtml("pretendard");
        assertThat(html.split("orioncactus/pretendard", -1).length - 1)
            .as("프리텐다드 링크가 중복됐다").isEqualTo(1);
    }
}
