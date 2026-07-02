package com.example.weddingexam.viewlog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ViewLogRepository extends JpaRepository<ViewLogEntity, Long> {

    Optional<ViewLogEntity> findByWeddingIdAndViewDate(Long weddingId, LocalDate viewDate);

    // 오늘 행이 이미 있으면 count +1 (동시성 안전한 벌크 UPDATE)
    // clearAutomatically: 벌크 업데이트 후 영속성 컨텍스트를 비워 같은 트랜잭션 내 재조회 시 stale 값을 방지
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ViewLogEntity v SET v.count = v.count + 1 " +
           "WHERE v.weddingId = :weddingId AND v.viewDate = :viewDate")
    int incrementCount(@Param("weddingId") Long weddingId, @Param("viewDate") LocalDate viewDate);

    // ── 전체 서비스 기준: 일별 총 방문 수 (어드민 차트용) ──
    @Query("SELECT v.viewDate AS viewDate, SUM(v.count) AS total " +
           "FROM ViewLogEntity v WHERE v.viewDate >= :from " +
           "GROUP BY v.viewDate ORDER BY v.viewDate ASC")
    List<DailyViewCount> findDailyTotals(@Param("from") LocalDate from);

    // ── 특정 청첩장 기준: 일별 방문 수 ──
    @Query("SELECT v.viewDate AS viewDate, SUM(v.count) AS total " +
           "FROM ViewLogEntity v WHERE v.weddingId = :weddingId AND v.viewDate >= :from " +
           "GROUP BY v.viewDate ORDER BY v.viewDate ASC")
    List<DailyViewCount> findDailyByWedding(@Param("weddingId") Long weddingId,
                                            @Param("from") LocalDate from);

    /** 집계 결과 프로젝션 */
    interface DailyViewCount {
        LocalDate getViewDate();
        Long getTotal();
    }
}
