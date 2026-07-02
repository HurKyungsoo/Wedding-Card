package com.example.weddingexam.service;

import com.example.weddingexam.dto.WeddingDto;
import com.example.weddingexam.dto.WeddingEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WeddingService {
    private final WeddingRepository repo;
    public WeddingService(WeddingRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public WeddingDto findById(Long id) {
        return repo.findById(id).map(WeddingEntity::toDto)
            .orElseThrow(() -> new IllegalArgumentException("없음 id=" + id));
    }

    @Transactional(readOnly = true)
    public Optional<WeddingDto> findBySlug(String slug) {
        return repo.findBySlug(slug).map(WeddingEntity::toDto);
    }

    @Transactional(readOnly = true)
    public WeddingDto findFirst() {
        return repo.findAll().stream().findFirst().map(WeddingEntity::toDto).orElse(getDefaultDto());
    }

    @Transactional(readOnly = true)
    public List<WeddingDto> findAll() {
        return repo.findAllByOrderByCreatedAtDesc().stream()
            .map(WeddingEntity::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WeddingDto> findByUserId(Long userId) {
        return repo.findByUserId(userId).stream()
            .map(WeddingEntity::toDto).collect(Collectors.toList());
    }

    @Transactional
    public WeddingDto save(WeddingDto dto) {
        return repo.save(WeddingEntity.fromDto(dto)).toDto();
    }

    @Transactional
    public WeddingDto update(Long id, WeddingDto dto) {
        WeddingEntity existing = repo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("없음 id=" + id));
        dto.setId(id);
        if (dto.getSlug() == null || dto.getSlug().isEmpty()) dto.setSlug(existing.getSlug());
        if (dto.getCreatedAt() == null) dto.setCreatedAt(existing.getCreatedAt());
        if (dto.getViewCount() == null) dto.setViewCount(existing.getViewCount());
        if (dto.getUserId() == null) dto.setUserId(existing.getUserId());
        return repo.save(WeddingEntity.fromDto(dto)).toDto();
    }

    @Transactional
    public void delete(Long id) { repo.deleteById(id); }

    @Transactional
    public void incrementViewCount(Long id) { repo.incrementViewCount(id); }

    public WeddingDto getDefaultDto() {
        return WeddingDto.builder()
            .groomName("박지훈").brideName("이수아")
            .weddingDate("2025-10-25").weddingTime("13:00")
            .weddingPlace("그랜드 웨딩홀 5층 로즈홀").weddingAddress("서울특별시 강남구 테헤란로 123")
            .greetingTitle("저희 두 사람이\n사랑으로 하나 됩니다")
            .greetingText("서로 다른 두 사람이 만나 하나의 가정을 이루게 되었습니다.\n\n바쁘신 중에도 귀한 발걸음 하시어\n저희의 앞날을 축복해 주시면 더없는 기쁨이겠습니다.")
            .greetingAlign("center").greetingVisible(true)
            .groomFatherName("박철수").groomMotherName("김영희")
            .groomFatherPhone("010-1234-5678").groomMotherPhone("010-2345-6789")
            .brideFatherName("이민수").brideMotherName("최정희")
            .brideFatherPhone("010-3456-7890").brideMotherPhone("010-4567-8901")
            .groomPhone("010-9999-0001").bridePhone("010-9999-0002")
            .hostsVisible(true)
            .groomFatherDeceased(false).groomMotherDeceased(false)
            .brideFatherDeceased(false).brideMotherDeceased(false)
            .deceasedDisplayType("hanja")
            .groomRelation("장남").brideRelation("장녀")
            .contactPopupEnabled(true)
            .calendarVisible(true).ddayVisible(true)
            .mapPlaceName("그랜드 웨딩홀").mapAddressRoad("서울 강남구 테헤란로 123").mapAddress("역삼동 123-45")
            .mapLat(37.5009).mapLng(127.0363).mapZoomLevel("50M")
            .mapVisible(true).mapDetailEnabled(true)
            .mapNaviKakao(true).mapNaviTmap(true).mapNaviNaver(true)
            .galleryVisible(true).galleryImages("").galleryType("slide").galleryScrollGuide(true)
            .photoFilter("none").mainPhotoBase64("")
            .build();
    }
}
