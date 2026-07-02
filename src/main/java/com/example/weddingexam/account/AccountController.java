package com.example.weddingexam.account;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** 전체 계좌 목록 */
    @GetMapping
    public ResponseEntity<List<AccountDto>> getAll() {
        return ResponseEntity.ok(accountService.findAll());
    }

    /** 신랑/신부 구분 조회 */
    @GetMapping("/side/{side}")
    public ResponseEntity<List<AccountDto>> getBySide(@PathVariable String side) {
        return ResponseEntity.ok(accountService.findBySide(side));
    }

    /** 단건 추가 */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody AccountDto dto) {
        try {
            return ResponseEntity.ok(accountService.save(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 전체 목록 일괄 저장 (편집 페이지) */
    @PostMapping("/bulk")
    public ResponseEntity<?> saveAll(@RequestBody List<AccountDto> dtos) {
        try {
            accountService.saveAll(dtos);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
