package com.example.weddingexam.guestbook;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "guestbook")
public class GuestbookEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long weddingId;
    private String name;

    @Column(length = 500)
    private String message;

    /** 작성자 본인 삭제용 PIN의 BCrypt 해시 — 평문은 저장하지 않는다 */
    private String passwordHash;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() { this.createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getWeddingId() { return weddingId; }
    public void setWeddingId(Long v) { this.weddingId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
