package com.example.weddingexam.guestbook;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class GuestbookDto {

    private Long id;
    private Long weddingId;
    private String name;
    private String message;
    private LocalDateTime createdAt;

    /**
     * 작성/삭제 요청에서만 쓰는 PIN. WRITE_ONLY라 응답 JSON에는 절대 실리지 않는다 —
     * 방명록 목록은 하객 누구나 읽으므로 여기로 PIN이 새어 나가면 안 된다.
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    public GuestbookDto() {}

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getWeddingId() { return weddingId; }
    public void setWeddingId(Long v) { this.weddingId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }

    /** 엔티티 → DTO. passwordHash는 옮기지 않는다 */
    public static GuestbookDto from(GuestbookEntity e) {
        GuestbookDto dto = new GuestbookDto();
        dto.setId(e.getId());
        dto.setWeddingId(e.getWeddingId());
        dto.setName(e.getName());
        dto.setMessage(e.getMessage());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }
}
