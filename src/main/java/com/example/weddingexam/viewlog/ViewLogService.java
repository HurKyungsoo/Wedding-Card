package com.example.weddingexam.viewlog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ViewLogService {

    private final ViewLogRepository repo;

    public ViewLogService(ViewLogRepository repo) {
        this.repo = repo;
    }

    /** 청첩장 방문 기록 — 오늘 날짜 행이 없으면 생성 후 +1 */
    @Transactional
    public void recordView(Long weddingId) {
        LocalDate today = LocalDate.now();
        int updated = repo.incrementCount(weddingId, today);
        if (updated == 0) {
            // 오늘 첫 방문 — 행 생성 (동시 삽입 충돌 시 재시도 1회)
            try {
                ViewLogEntity log = new ViewLogEntity(weddingId, today);
                log.setCount(1L);
                repo.save(log);
            } catch (Exception e) {
                repo.incrementCount(weddingId, today);
            }
        }
    }

    /** 최근 N일 전체 방문 통계 — 차트용 (빈 날짜는 0으로 채움) */
    @Transactional(readOnly = true)
    public DailyChartData getDailyTotals(int days) {
        LocalDate from = LocalDate.now().minusDays(days - 1);
        return buildChartData(repo.findDailyTotals(from), from, days);
    }

    /** 특정 청첩장 최근 N일 방문 통계 */
    @Transactional(readOnly = true)
    public DailyChartData getDailyByWedding(Long weddingId, int days) {
        LocalDate from = LocalDate.now().minusDays(days - 1);
        return buildChartData(repo.findDailyByWedding(weddingId, from), from, days);
    }

    private DailyChartData buildChartData(List<ViewLogRepository.DailyViewCount> rows,
                                          LocalDate from, int days) {
        // 날짜 → 카운트 맵
        Map<LocalDate, Long> map = new LinkedHashMap<>();
        for (ViewLogRepository.DailyViewCount row : rows) {
            map.put(row.getViewDate(), row.getTotal());
        }

        // 빈 날짜 0으로 채워서 연속된 라벨/데이터 생성
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d");
        for (int i = 0; i < days; i++) {
            LocalDate d = from.plusDays(i);
            labels.add(d.format(fmt));
            data.add(map.getOrDefault(d, 0L));
        }
        return new DailyChartData(labels, data);
    }

    /** Chart.js에 바로 넣을 수 있는 형태 */
    public record DailyChartData(List<String> labels, List<Long> data) {}
}
