package com.example.weddingexam.guestbook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GuestbookRepository extends JpaRepository<GuestbookEntity, Long> {
    List<GuestbookEntity> findByWeddingIdOrderByCreatedAtDesc(Long weddingId);
    long countByWeddingId(Long weddingId);
}
