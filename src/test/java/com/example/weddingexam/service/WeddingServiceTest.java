package com.example.weddingexam.service;

import com.example.weddingexam.dto.WeddingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class WeddingServiceTest {

    @Autowired
    private WeddingRepository weddingRepository;

    private WeddingService weddingService;

    @BeforeEach
    void setUp() {
        weddingService = new WeddingService(weddingRepository);
    }

    @Test
    void save_withoutSlug_autoGeneratesUniqueSlug() {
        WeddingDto saved = weddingService.save(weddingService.getDefaultDto());

        assertThat(saved.getSlug()).isNotNull().isNotEmpty();
    }

    @Test
    void save_thenFindBySlug_returnsSameWedding() {
        WeddingDto dto = weddingService.getDefaultDto();
        dto.setGroomName("김민준");
        dto.setBrideName("이서연");
        WeddingDto saved = weddingService.save(dto);

        var found = weddingService.findBySlug(saved.getSlug());

        assertThat(found).isPresent();
        assertThat(found.get().getGroomName()).isEqualTo("김민준");
        assertThat(found.get().getBrideName()).isEqualTo("이서연");
    }

    @Test
    void findBySlug_unknownSlug_returnsEmpty() {
        assertThat(weddingService.findBySlug("존재하지-않는-슬러그")).isEmpty();
    }

    @Test
    void incrementViewCount_incrementsFromNull() {
        WeddingDto saved = weddingService.save(weddingService.getDefaultDto());

        weddingService.incrementViewCount(saved.getId());
        weddingService.incrementViewCount(saved.getId());

        WeddingDto updated = weddingService.findById(saved.getId());
        assertThat(updated.getViewCount()).isEqualTo(2);
    }

    @Test
    void update_missingFields_fallBackToExistingValues() {
        WeddingDto original = weddingService.save(weddingService.getDefaultDto());
        weddingService.incrementViewCount(original.getId());

        WeddingDto patch = weddingService.getDefaultDto();
        patch.setGroomName("변경된이름");
        patch.setSlug(null);       // 슬러그 미지정 시 기존 값 유지되어야 함
        patch.setViewCount(null);  // 조회수 미지정 시 기존 값 유지되어야 함

        WeddingDto updated = weddingService.update(original.getId(), patch);

        assertThat(updated.getGroomName()).isEqualTo("변경된이름");
        assertThat(updated.getSlug()).isEqualTo(original.getSlug());
        assertThat(updated.getViewCount()).isEqualTo(1);
    }

    @Test
    void update_unknownId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> weddingService.update(999L, weddingService.getDefaultDto()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_removesWedding() {
        WeddingDto saved = weddingService.save(weddingService.getDefaultDto());

        weddingService.delete(saved.getId());

        assertThat(weddingService.findBySlug(saved.getSlug())).isEmpty();
    }
}
