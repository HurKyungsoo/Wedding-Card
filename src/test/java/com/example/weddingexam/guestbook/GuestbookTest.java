package com.example.weddingexam.guestbook;

import com.example.weddingexam.dto.WeddingDto;
import com.example.weddingexam.service.WeddingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 방명록 — 하객이 축하글을 남기고 본인이 정한 PIN으로 지운다.
 * 필터를 켠 채로 돌려서 비로그인 하객이 실제로 읽고·쓰고·지울 수 있는지까지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:guestbook-test;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
class GuestbookTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WeddingService weddingService;
    @Autowired private GuestbookService guestbookService;
    @Autowired private ObjectMapper om;

    private Long newWeddingId() {
        return weddingService.save(weddingService.getDefaultDto()).getId();
    }

    private MvcResult postJson(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body))).andReturn();
    }

    private Long write(Long weddingId, String name, String msg, String pin) throws Exception {
        MvcResult r = postJson("/api/guestbook",
                Map.of("weddingId", weddingId, "name", name, "message", msg, "password", pin));
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return om.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void guest_canWriteAndRead() throws Exception {
        Long w = newWeddingId();
        write(w, "홍길동", "결혼 축하합니다!", "1234");

        String body = mockMvc.perform(get("/api/guestbook").param("weddingId", String.valueOf(w)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("홍길동").contains("결혼 축하합니다!");
    }

    /**
     * 목록은 하객 누구나 읽으므로 PIN/해시가 절대 실리면 안 된다.
     *
     * 본문을 문자열로 부분검색하지 않는다 — PIN 숫자 4자리는 createdAt의 마이크로초
     * (예: 12:22:52.432164 안의 "4321")에 우연히 들어가서 시각에 따라 실패한다.
     * 대신 응답 객체의 필드 집합 자체를 고정해, 새 필드가 새어 나오면 바로 걸리게 한다.
     */
    @Test
    void list_neverLeaksPassword() throws Exception {
        Long w = newWeddingId();
        write(w, "홍길동", "축하해요", "4321");

        String body = mockMvc.perform(get("/api/guestbook").param("weddingId", String.valueOf(w)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode arr = om.readTree(body);
        assertThat(arr).hasSize(1);

        List<String> fields = new ArrayList<>();
        arr.get(0).fieldNames().forEachRemaining(fields::add);
        assertThat(fields)
            .as("방명록 응답에 노출해도 되는 필드만 있어야 한다")
            .containsExactlyInAnyOrder("id", "weddingId", "name", "message", "createdAt");
    }

    @Test
    void delete_succeedsWithCorrectPin() throws Exception {
        Long w = newWeddingId();
        Long id = write(w, "홍길동", "축하해요", "1111");

        MvcResult r = postJson("/api/guestbook/" + id + "/delete",
                Map.of("weddingId", w, "password", "1111"));

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(guestbookService.findByWeddingId(w)).isEmpty();
    }

    @Test
    void delete_isRejectedWithWrongPin() throws Exception {
        Long w = newWeddingId();
        Long id = write(w, "홍길동", "축하해요", "1111");

        MvcResult r = postJson("/api/guestbook/" + id + "/delete",
                Map.of("weddingId", w, "password", "9999"));

        assertThat(r.getResponse().getStatus()).isEqualTo(403);
        assertThat(guestbookService.findByWeddingId(w)).hasSize(1);
    }

    /** 다른 청첩장의 글을 그 청첩장 id로 지울 수 없어야 한다 */
    @Test
    void delete_isScopedToItsWedding() throws Exception {
        Long a = newWeddingId();
        Long b = newWeddingId();
        Long id = write(a, "홍길동", "축하해요", "1111");

        MvcResult r = postJson("/api/guestbook/" + id + "/delete",
                Map.of("weddingId", b, "password", "1111"));

        assertThat(r.getResponse().getStatus()).isEqualTo(403);
        assertThat(guestbookService.findByWeddingId(a)).hasSize(1);
    }

    @Test
    void write_rejectsInvalidInput() throws Exception {
        Long w = newWeddingId();

        assertThat(postJson("/api/guestbook", Map.of("weddingId", w, "name", "", "message", "축하", "password", "1234"))
                .getResponse().getStatus()).isEqualTo(400);
        assertThat(postJson("/api/guestbook", Map.of("weddingId", w, "name", "홍길동", "message", "", "password", "1234"))
                .getResponse().getStatus()).isEqualTo(400);
        // PIN이 숫자 4자리가 아니면 거부
        assertThat(postJson("/api/guestbook", Map.of("weddingId", w, "name", "홍길동", "message", "축하", "password", "12"))
                .getResponse().getStatus()).isEqualTo(400);
        assertThat(postJson("/api/guestbook", Map.of("weddingId", w, "name", "홍길동", "message", "축하", "password", "abcd"))
                .getResponse().getStatus()).isEqualTo(400);

        assertThat(guestbookService.findByWeddingId(w)).isEmpty();
    }

    /** 청첩장에 방명록 섹션 DOM이 있어야 스크립트가 붙는다 */
    @Test
    void invitation_rendersGuestbookSection() throws Exception {
        WeddingDto w = weddingService.save(weddingService.getDefaultDto());

        String html = mockMvc.perform(get("/w/" + w.getSlug()))
                .andReturn().getResponse().getContentAsString().replaceAll("\\s+", " ");

        assertThat(html).contains("id=\"guestbookCard\"");
        assertThat(html).contains("data-vis=\"guestbook\"");
        assertThat(html).contains("id=\"gbSubmitBtn\"");
    }
}
