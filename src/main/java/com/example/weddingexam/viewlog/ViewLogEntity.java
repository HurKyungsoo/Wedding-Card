package com.example.weddingexam.viewlog;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * 청첩장 일별 방문 로그
 * (weddingId + viewDate) 조합당 1행, count만 증가시키는 방식
 */
@Entity
@Table(name = "view_log",
       uniqueConstraints = @UniqueConstraint(columnNames = {"weddingId", "viewDate"}))
public class ViewLogEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long weddingId;

    @Column(nullable = false)
    private LocalDate viewDate;

    @Column(nullable = false)
    private Long count = 0L;

    public ViewLogEntity() {}

    public ViewLogEntity(Long weddingId, LocalDate viewDate) {
        this.weddingId = weddingId;
        this.viewDate = viewDate;
        this.count = 0L;
    }

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getWeddingId() { return weddingId; }
    public void setWeddingId(Long v) { this.weddingId = v; }
    public LocalDate getViewDate() { return viewDate; }
    public void setViewDate(LocalDate v) { this.viewDate = v; }
    public Long getCount() { return count; }
    public void setCount(Long v) { this.count = v; }
}
