package com.example.weddingexam.rsvp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RsvpRepository extends JpaRepository<RsvpEntity, Long> {
    List<RsvpEntity> findAllByOrderByCreatedAtDesc();
    List<RsvpEntity> findByWeddingIdOrderByCreatedAtDesc(Long weddingId);
}
