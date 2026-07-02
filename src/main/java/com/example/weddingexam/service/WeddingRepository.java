package com.example.weddingexam.service;

import com.example.weddingexam.dto.WeddingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeddingRepository extends JpaRepository<WeddingEntity, Long> {
    Optional<WeddingEntity> findBySlug(String slug);
    List<WeddingEntity> findByUserId(Long userId);
    List<WeddingEntity> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE WeddingEntity w SET w.viewCount = COALESCE(w.viewCount, 0) + 1 WHERE w.id = :id")
    void incrementViewCount(Long id);
}
