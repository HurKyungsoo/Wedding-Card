package com.example.weddingexam.service;

import com.example.weddingexam.dto.WeddingDto;
import com.example.weddingexam.dto.WeddingEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WeddingService {
    private final WeddingRepository repo;
    private final ObjectMapper objectMapper;

    public WeddingService(WeddingRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

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

    /** 한 사용자가 만들 수 있는 청첩장 수 상한 */
    public static final int MAX_WEDDINGS_PER_USER = 10;

    /** 마이페이지 목록 — 최근에 만든 것부터 */
    @Transactional(readOnly = true)
    public List<WeddingDto> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(WeddingEntity::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countByUserId(Long userId) {
        return repo.countByUserId(userId);
    }

    /**
     * 새 청첩장 생성 — 마이페이지 "새로 만들기".
     * 사용자당 상한을 두지 않으면 새로고침 한 번에 계속 쌓인다.
     */
    @Transactional
    public WeddingDto createForUser(Long userId) {
        if (repo.countByUserId(userId) >= MAX_WEDDINGS_PER_USER)
            throw new IllegalStateException("청첩장은 최대 " + MAX_WEDDINGS_PER_USER + "개까지 만들 수 있습니다.");
        WeddingDto dto = blankDto();
        dto.setUserId(userId);
        return save(dto);
    }

    /**
     * 새 청첩장의 시작 상태 — 설정(테마·스타일·섹션 on/off)은 기본값을 쓰고
     * "내용"만 비운다.
     *
     * getDefaultDto()의 데모 데이터(박지훈·이수아, 그랜드 웨딩홀, 부모님 성함과 전화번호,
     * 강남 좌표…)를 그대로 두면 두 가지가 문제다.
     *  ① 마이페이지에서 새로 만든 청첩장이 전부 같은 이름으로 보여 구분이 안 된다
     *  ② 채우지 않은 채 게시하면 남의 이름·전화번호가 하객에게 그대로 나간다
     * getDefaultDto()는 데모/샘플 청첩장에도 쓰이므로 그쪽은 건드리지 않는다.
     */
    private WeddingDto blankDto() {
        WeddingDto d = getDefaultDto();

        d.setGroomName(null);        d.setBrideName(null);
        d.setWeddingDate(null);      d.setWeddingTime(null);
        d.setWeddingPlace(null);     d.setWeddingAddress(null);

        d.setGroomFatherName(null);  d.setGroomMotherName(null);
        d.setBrideFatherName(null);  d.setBrideMotherName(null);
        d.setGroomFatherPhone(null); d.setGroomMotherPhone(null);
        d.setBrideFatherPhone(null); d.setBrideMotherPhone(null);
        d.setGroomPhone(null);       d.setBridePhone(null);
        d.setGroomRelation(null);    d.setBrideRelation(null);

        /* 지도 — 데모 좌표를 남기면 엉뚱한 장소가 표시된다 */
        d.setMapPlaceName(null);     d.setMapAddressRoad(null);  d.setMapAddress(null);
        d.setMapLat(null);           d.setMapLng(null);

        /* 인사말 문구는 그대로 둔다 — 직접 쓰기 어려운 부분이라 예시가 도움이 되고,
           이름·날짜와 달리 남의 개인정보가 아니다 */
        return d;
    }

    /** 소유자일 때만 삭제 */
    @Transactional
    public boolean deleteForUser(Long weddingId, Long userId) {
        return repo.findById(weddingId)
            .filter(e -> userId != null && userId.equals(e.getUserId()))
            .map(e -> { repo.delete(e); return true; })
            .orElse(false);
    }

    /** 해당 청첩장이 로그인한 사용자 소유인지 확인 (API 스코핑용) */
    @Transactional(readOnly = true)
    public boolean isOwnedByUser(Long weddingId, Long userId) {
        if (weddingId == null || userId == null) return false;
        return repo.findById(weddingId)
            .map(WeddingEntity::getUserId)
            .map(userId::equals)
            .orElse(false);
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

    /** 편집기에 보여줄 내용 — 임시저장본이 있으면 그것을, 없으면 게시본을 반환 */
    @Transactional(readOnly = true)
    public EditableWedding getEditable(Long id) {
        WeddingEntity entity = repo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("없음 id=" + id));
        WeddingDto published = entity.toDto();
        if (entity.getDraftData() == null || entity.getDraftData().isBlank())
            return new EditableWedding(published, false, null);
        try {
            WeddingDto draft = objectMapper.readValue(entity.getDraftData(), WeddingDto.class);
            // 소유권/메타 필드는 항상 게시본 기준으로 고정 (임시저장 스냅샷이 덮어쓰지 못하게)
            draft.setId(published.getId());
            draft.setSlug(published.getSlug());
            draft.setCreatedAt(published.getCreatedAt());
            draft.setViewCount(published.getViewCount());
            draft.setUserId(published.getUserId());
            return new EditableWedding(draft, true, entity.getDraftSavedAt());
        } catch (Exception e) {
            return new EditableWedding(published, false, null);
        }
    }

    /** 편집 중인 내용을 임시저장 — 게스트 화면(게시본)에는 영향 없음 */
    @Transactional
    public void saveDraft(Long id, WeddingDto dto) {
        WeddingEntity existing = repo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("없음 id=" + id));
        dto.setId(id);
        try {
            existing.setDraftData(objectMapper.writeValueAsString(dto));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("임시저장 데이터 직렬화 실패", e);
        }
        existing.setDraftSavedAt(LocalDateTime.now());
        repo.save(existing);
    }

    /** 편집 중인 내용을 게스트 화면에 실제로 반영하고 임시저장본은 비움 */
    @Transactional
    public WeddingDto publish(Long id, WeddingDto dto) {
        WeddingDto updated = update(id, dto);
        WeddingEntity entity = repo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("없음 id=" + id));
        entity.setDraftData(null);
        entity.setDraftSavedAt(null);
        repo.save(entity);
        return updated;
    }

    /** 편집기에서 사용할 청첩장 내용과 임시저장 상태 */
    public record EditableWedding(WeddingDto dto, boolean hasDraft, LocalDateTime draftSavedAt) {}

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
            .mapVisible(true).mapDetailView(true)
            .mapNaviKakao(true).mapNaviTmap(true).mapNaviNaver(true)
            .galleryVisible(true).galleryImages("").galleryType("slide").galleryScrollGuide(true)
            .guestbookVisible(true)
            .photoFilter("none").mainPhotoBase64("")
            .endingVisible(true).endingPhotoBase64("").endingCaption("").endingEffect("none")
            .build();
    }
}
