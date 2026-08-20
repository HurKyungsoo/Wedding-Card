package com.example.weddingexam.guestbook;

import com.example.weddingexam.security.CustomOAuth2User;
import com.example.weddingexam.service.WeddingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/guestbook")
public class GuestbookController {

    private final GuestbookService guestbookService;
    private final WeddingService weddingService;

    public GuestbookController(GuestbookService guestbookService, WeddingService weddingService) {
        this.guestbookService = guestbookService;
        this.weddingService = weddingService;
    }

    /** 방명록 목록 — 하객 누구나 읽는다(공개). 응답에 PIN 해시는 포함되지 않는다 */
    @GetMapping
    public ResponseEntity<?> list(@RequestParam Long weddingId) {
        return ResponseEntity.ok(guestbookService.findByWeddingId(weddingId));
    }

    /** 축하글 남기기 — 비로그인 공개 */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody GuestbookDto dto) {
        if (dto.getWeddingId() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "청첩장 정보가 없습니다."));
        try {
            return ResponseEntity.ok(guestbookService.save(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 삭제 — 작성자는 PIN으로, 청첩장 주인은 PIN 없이.
     * PIN을 URL 쿼리에 싣지 않으려고 DELETE 대신 POST + 본문을 쓴다
     * (쿼리스트링은 접근로그·리퍼러에 그대로 남는다).
     */
    @PostMapping("/{id}/delete")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @RequestBody GuestbookDto body,
                                    @AuthenticationPrincipal CustomOAuth2User principal) {
        Long weddingId = body.getWeddingId();
        if (weddingId == null)
            return ResponseEntity.badRequest().body(Map.of("error", "청첩장 정보가 없습니다."));

        boolean isOwner = principal != null
                && weddingService.isOwnedByUser(weddingId, principal.getUserId());
        if (isOwner) {
            guestbookService.deleteForWedding(id, weddingId);
            return ResponseEntity.ok(Map.of("success", true));
        }

        if (guestbookService.deleteWithPassword(id, weddingId, body.getPassword()))
            return ResponseEntity.ok(Map.of("success", true));

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "비밀번호가 일치하지 않습니다."));
    }
}
