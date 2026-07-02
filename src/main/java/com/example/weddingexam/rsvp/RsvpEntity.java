package com.example.weddingexam.rsvp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rsvp")
public class RsvpEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long weddingId;
    private String name;
    private String phone;
    private Boolean attendance;
    private Integer mealCount;

    @Column(length = 500)
    private String message;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() { this.createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getWeddingId() { return weddingId; }
    public void setWeddingId(Long v) { this.weddingId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public Boolean getAttendance() { return attendance; }
    public void setAttendance(Boolean v) { this.attendance = v; }
    public Integer getMealCount() { return mealCount; }
    public void setMealCount(Integer v) { this.mealCount = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
