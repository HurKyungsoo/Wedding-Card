package com.example.weddingexam.account;

import com.example.weddingexam.security.CustomOAuth2User;
import com.example.weddingexam.service.WeddingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;
    private final WeddingService weddingService;

    public AccountController(AccountService accountService, WeddingService weddingService) {
        this.accountService = accountService;
        this.weddingService = weddingService;
    }

    /** 내 청첩장 계좌 목록 (소유자만 조회 가능) */
    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam Long weddingId,
                                     @AuthenticationPrincipal CustomOAuth2User principal) {
        if (principal == null || !weddingService.isOwnedByUser(weddingId, principal.getUserId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(accountService.findByWeddingId(weddingId));
    }

    /** 단건 추가 */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody AccountDto dto,
                                     @AuthenticationPrincipal CustomOAuth2User principal) {
        if (principal == null || !weddingService.isOwnedByUser(dto.getWeddingId(), principal.getUserId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            return ResponseEntity.ok(accountService.save(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 전체 목록 일괄 저장 (편집 페이지) */
    @PostMapping("/bulk")
    public ResponseEntity<?> saveAll(@RequestParam Long weddingId,
                                      @RequestBody List<AccountDto> dtos,
                                      @AuthenticationPrincipal CustomOAuth2User principal) {
        if (principal == null || !weddingService.isOwnedByUser(weddingId, principal.getUserId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            accountService.saveAllForWedding(weddingId, dtos);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                     @RequestParam Long weddingId,
                                     @AuthenticationPrincipal CustomOAuth2User principal) {
        if (principal == null || !weddingService.isOwnedByUser(weddingId, principal.getUserId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        accountService.deleteForWedding(id, weddingId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
