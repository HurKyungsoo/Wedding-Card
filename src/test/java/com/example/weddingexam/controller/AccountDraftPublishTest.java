package com.example.weddingexam.controller;

import com.example.weddingexam.account.AccountDto;
import com.example.weddingexam.account.AccountService;
import com.example.weddingexam.dto.WeddingDto;
import com.example.weddingexam.service.WeddingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 계좌가 다른 섹션과 동일한 초안/게시 모델을 따르는지 검증.
 *
 * 이전에는 "계좌 저장"이 /api/account/bulk 로 실데이터를 즉시 덮어써서,
 * 계좌만 게시 전인데 하객 화면에 이미 반영되고 미리보기에는 안 보이는
 * (다른 모든 섹션과 정반대인) 동작이었다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:account-draft-publish-test;NON_KEYWORDS=USER",
    "kakao.oauth.client-id=test-client-id",
    "kakao.oauth.client-secret=test-secret"
})
class AccountDraftPublishTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WeddingService weddingService;
    @Autowired private AccountService accountService;
    @Autowired private ObjectMapper objectMapper;

    private AccountDto account(String side, String owner, String number) {
        AccountDto a = new AccountDto();
        a.setSide(side);
        a.setOwner(owner);
        a.setBank("국민은행");
        a.setAccountNumber(number);
        return a;
    }

    private WeddingDto weddingWithAccounts(Long id, List<AccountDto> accounts) {
        WeddingDto dto = weddingService.findById(id);
        dto.setAccounts(accounts);
        return dto;
    }

    private void postJson(String url, Object body) throws Exception {
        mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isOk());
    }

    @Test
    void autosave_keepsAccountsOutOfGuestView() throws Exception {
        WeddingDto w = weddingService.save(weddingService.getDefaultDto());
        int before = accountService.findByWeddingId(w.getId()).size();

        postJson("/api/admin/autosave",
                 weddingWithAccounts(w.getId(), List.of(account("groom", "박철수", "111-222"))));

        // 임시저장이므로 실제 계좌 테이블은 그대로여야 한다
        assertThat(accountService.findByWeddingId(w.getId())).hasSize(before);
    }

    @Test
    void publish_writesAccountsToGuestView() throws Exception {
        WeddingDto w = weddingService.save(weddingService.getDefaultDto());

        postJson("/api/admin/publish",
                 weddingWithAccounts(w.getId(), List.of(
                     account("groom", "박철수", "111-222"),
                     account("bride", "이민수", "333-444"))));

        List<AccountDto> saved = accountService.findByWeddingId(w.getId());
        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(AccountDto::getOwner).containsExactlyInAnyOrder("박철수", "이민수");
    }

    /** 임시저장된 계좌는 편집기를 다시 열었을 때 게시본이 아니라 초안이 보여야 한다 */
    @Test
    void editable_prefersDraftAccountsOverPublished() throws Exception {
        WeddingDto w = weddingService.save(weddingService.getDefaultDto());

        postJson("/api/admin/publish",
                 weddingWithAccounts(w.getId(), List.of(account("groom", "게시된예금주", "111-222"))));
        postJson("/api/admin/autosave",
                 weddingWithAccounts(w.getId(), List.of(account("groom", "편집중예금주", "999-888"))));

        WeddingService.EditableWedding editable = weddingService.getEditable(w.getId());
        assertThat(editable.hasDraft()).isTrue();
        assertThat(editable.dto().getAccounts()).isNotNull();
        assertThat(editable.dto().getAccounts()).extracting(AccountDto::getOwner)
                .containsExactly("편집중예금주");

        // 게시본은 아직 예전 값 그대로
        assertThat(accountService.findByWeddingId(w.getId()))
                .extracting(AccountDto::getOwner).containsExactly("게시된예금주");
    }

    /** 계좌가 없어도 청첩장에 섹션 DOM은 있어야 미리보기에서 추가한 계좌가 나타난다 */
    @Test
    void invitation_keepsAccountSectionInDomButHiddenWhenEmpty() throws Exception {
        WeddingDto w = weddingService.save(weddingService.getDefaultDto());

        String html = mockMvc.perform(get("/w/" + w.getSlug()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
                .replaceAll("\\s+", " ");

        assertThat(html).contains("id=\"acctPanelGroom\"");
        assertThat(html).contains("id=\"acctPanelBride\"");
        // 계좌가 없으므로 감춰져 있어야 한다 (하객에게 빈 카드가 보이면 안 됨)
        assertThat(html).contains("class=\"sec\" data-vis=\"acct\" style=\"display:none\"");
    }

    /** 편집기가 계좌 초기값을 별도 fetch 대신 인라인으로 받는다 */
    @Test
    void editor_inlinesAccountsIntoWeddingObject() throws Exception {
        String html = mockMvc.perform(get("/admin/edit"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("accounts:");
        assertThat(html).doesNotContain("/api/account?weddingId=");
    }
}
