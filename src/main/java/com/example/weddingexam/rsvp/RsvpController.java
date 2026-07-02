package com.example.weddingexam.rsvp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rsvp")
public class RsvpController {

    private final RsvpService rsvpService;

    public RsvpController(RsvpService rsvpService) { this.rsvpService = rsvpService; }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody RsvpDto dto) {
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

    @GetMapping
    public ResponseEntity<List<RsvpDto>> getAll() {
        return ResponseEntity.ok(rsvpService.findAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        rsvpService.delete(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
