package com.example.weddingexam.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    private String name;
    private String passwordHash;

    @Column(unique = true)
    private String kakaoId;

    private String role = "USER";
    private String plan = "FREE";

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public UserEntity() {}

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public String getKakaoId() { return kakaoId; }
    public void setKakaoId(String v) { this.kakaoId = v; }
    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }
    public String getPlan() { return plan; }
    public void setPlan(String v) { this.plan = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
