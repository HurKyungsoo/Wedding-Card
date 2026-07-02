package com.example.weddingexam.rsvp;

import java.time.LocalDateTime;

public class RsvpDto {

    private Long id;
    private String name;
    private String phone;
    private Boolean attendance;
    private Integer mealCount;
    private String message;
    private LocalDateTime createdAt;

    public RsvpDto() {}

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
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

    public static RsvpDto from(RsvpEntity e) {
        RsvpDto dto = new RsvpDto();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setPhone(e.getPhone());
        dto.setAttendance(e.getAttendance());
        dto.setMealCount(e.getMealCount());
        dto.setMessage(e.getMessage());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }
}
