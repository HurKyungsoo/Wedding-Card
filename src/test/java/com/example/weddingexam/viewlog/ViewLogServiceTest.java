package com.example.weddingexam.viewlog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ViewLogServiceTest {

    @Autowired
    private ViewLogRepository viewLogRepository;

    private ViewLogService viewLogService;

    @BeforeEach
    void setUp() {
        viewLogService = new ViewLogService(viewLogRepository);
    }

    @Test
    void recordView_firstVisitOfDay_createsRowWithCountOne() {
        viewLogService.recordView(1L);

        var row = viewLogRepository.findByWeddingIdAndViewDate(1L, LocalDate.now());
        assertThat(row).isPresent();
        assertThat(row.get().getCount()).isEqualTo(1L);
    }

    @Test
    void recordView_repeatedVisitsSameDay_incrementsSingleRow() {
        viewLogService.recordView(1L);
        viewLogService.recordView(1L);
        viewLogService.recordView(1L);

        var rows = viewLogRepository.findAll();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getCount()).isEqualTo(3L);
    }

    @Test
    void recordView_differentWeddings_trackedSeparately() {
        viewLogService.recordView(1L);
        viewLogService.recordView(2L);
        viewLogService.recordView(1L);

        assertThat(viewLogRepository.findByWeddingIdAndViewDate(1L, LocalDate.now()).get().getCount()).isEqualTo(2L);
        assertThat(viewLogRepository.findByWeddingIdAndViewDate(2L, LocalDate.now()).get().getCount()).isEqualTo(1L);
    }

    @Test
    void getDailyTotals_fillsDaysWithoutVisitsAsZero() {
        viewLogService.recordView(1L); // 오늘만 방문 기록

        ViewLogService.DailyChartData chart = viewLogService.getDailyTotals(7);

        assertThat(chart.labels()).hasSize(7);
        assertThat(chart.data()).hasSize(7);
        assertThat(chart.data().get(chart.data().size() - 1)).isEqualTo(1L); // 오늘(마지막 값)
        assertThat(chart.data().subList(0, 6)).allMatch(v -> v == 0L); // 이전 6일은 0
    }
}
