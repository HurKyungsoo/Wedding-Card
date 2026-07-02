package com.example.weddingexam.rsvp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RsvpServiceTest {

    @Autowired
    private RsvpRepository rsvpRepository;

    private RsvpService rsvpService;

    @BeforeEach
    void setUp() {
        rsvpService = new RsvpService(rsvpRepository);
    }

    @Test
    void summary_withNoResponses_returnsAllZeros() {
        RsvpService.RsvpSummary summary = rsvpService.summary();

        assertThat(summary.total()).isZero();
        assertThat(summary.attend()).isZero();
        assertThat(summary.decline()).isZero();
        assertThat(summary.totalMeals()).isZero();
    }

    @Test
    void summary_countsAttendDeclineAndMeals() {
        save("김철수", true, 2);
        save("이영희", true, null); // mealCount 미입력 시 1명으로 집계
        save("박민수", false, 3);   // 불참자의 식사 인원은 집계에서 제외

        RsvpService.RsvpSummary summary = rsvpService.summary();

        assertThat(summary.total()).isEqualTo(3);
        assertThat(summary.attend()).isEqualTo(2);
        assertThat(summary.decline()).isEqualTo(1);
        assertThat(summary.totalMeals()).isEqualTo(3); // 2 + 1(기본값)
    }

    @Test
    void save_persistsRsvpAndReturnsGeneratedId() {
        RsvpDto dto = new RsvpDto();
        dto.setName("정다은");
        dto.setPhone("010-0000-0000");
        dto.setAttendance(true);
        dto.setMealCount(1);

        RsvpDto saved = rsvpService.save(dto);

        assertThat(saved.getId()).isNotNull();
        assertThat(rsvpService.findAll()).hasSize(1);
    }

    private void save(String name, boolean attendance, Integer mealCount) {
        RsvpDto dto = new RsvpDto();
        dto.setName(name);
        dto.setAttendance(attendance);
        dto.setMealCount(mealCount);
        rsvpService.save(dto);
    }
}
