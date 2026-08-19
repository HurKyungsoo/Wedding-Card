package com.example.weddingexam.rsvp;

import com.example.weddingexam.security.CustomOAuth2User;
import com.example.weddingexam.service.WeddingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/rsvp")
public class RsvpController {

    private final RsvpService rsvpService;
    private final WeddingService weddingService;

    public RsvpController(RsvpService rsvpService, WeddingService weddingService) {
        this.rsvpService = rsvpService;
        this.weddingService = weddingService;
    }

    /** 하객이 청첩장 페이지에서 참석 여부를 제출 (비로그인 공개 엔드포인트) */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody RsvpDto dto) {
        if (dto.getWeddingId() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "청첩장 정보가 없습니다."));
        if (dto.getName() == null || dto.getName().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "이름을 입력해 주세요."));
        if (dto.getAttendance() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "참석 여부를 선택해 주세요."));
        try {
            return ResponseEntity.ok(rsvpService.save(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "처리 중 오류가 발생했습니다."));
        }
    }

    /** 내 청첩장에 온 응답 목록 (소유자만 조회 가능) */
    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam Long weddingId,
                                     @AuthenticationPrincipal CustomOAuth2User principal) {
        if (principal == null || !weddingService.isOwnedByUser(weddingId, principal.getUserId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(rsvpService.findByWeddingId(weddingId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                     @RequestParam Long weddingId,
                                     @AuthenticationPrincipal CustomOAuth2User principal) {
        if (principal == null || !weddingService.isOwnedByUser(weddingId, principal.getUserId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        rsvpService.deleteForWedding(id, weddingId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
