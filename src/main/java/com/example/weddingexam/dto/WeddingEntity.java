package com.example.weddingexam.dto;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wedding")
public class WeddingEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String slug;
    private Long userId;
    private LocalDateTime createdAt;
    private Integer viewCount = 0;

    private String groomName, brideName;
    private String weddingDate, weddingTime, weddingPlace, weddingAddress;
    private String greetingTitle;
    @Column(length = 2000) private String greetingText;
    private String greetingAlign;
    private Boolean greetingVisible;

    private String groomFatherName, groomMotherName, groomFatherPhone, groomMotherPhone;
    private String brideFatherName, brideMotherName, brideFatherPhone, brideMotherPhone;
    private String groomPhone, bridePhone;
    private Boolean hostsVisible;

    private Boolean groomFatherDeceased, groomMotherDeceased;
    private Boolean brideFatherDeceased, brideMotherDeceased;
    private String deceasedDisplayType;
    private String groomRelation, brideRelation;
    /** 세례명 — 성당 예식에서 이름 옆에 괄호로 표기. 미입력이면 표시하지 않음 */
    private String groomBaptism, brideBaptism;
    private String groomFatherBaptism, groomMotherBaptism;
    private String brideFatherBaptism, brideMotherBaptism;

    @Column(name="display_order")
    private String displayOrder = "groom";
    private Boolean contactPopupEnabled;

    private Boolean calendarVisible, ddayVisible;
    private String calendarStyle;
    private String ddayStyle;

    private String mapPlaceName, mapAddressRoad, mapAddress;
    private Double mapLat, mapLng;
    private String mapZoomLevel;
    private Boolean mapDetailView;
    private Boolean mapVisible, mapLocked;
    private Boolean mapNaviKakao = false, mapNaviTmap = false, mapNaviNaver = false;

    private Boolean galleryVisible;
    private Boolean accountVisible;
    @Column(columnDefinition = "TEXT") private String galleryImages;
    private String galleryType;
    private Boolean galleryScrollGuide;

    private String photoFilter;
    @Column(columnDefinition = "TEXT") private String mainPhotoBase64;
    /** 메인 사진 초점 위치 — CSS object-position 값(%), 미설정 시 테마 기본값 사용 */
    private Double mainPhotoPosX, mainPhotoPosY;
    /** 메인 사진 확대 배율 — CSS transform:scale() 값(1.0 = 원본), 미설정/1.0이면 확대 없음 */
    private Double mainPhotoScale;
    private String mainDesign, mainFont, mainFontSize, mainFontColor, colorEffect, mainEffect, bgm;
    private Boolean rsvpEnabled;
    /** 방명록 섹션 표시 여부 */
    private Boolean guestbookVisible;

    /** 엔딩(마지막 인사) 섹션 — 방명록·공유하기 직전, 사진 한 장 + 문구로 마무리하는 클로징 섹션.
     * 사진이 없으면 endingVisible이 true여도 화면에 표시하지 않는다(빈 섹션 방지) */
    private Boolean endingVisible;
    @Column(columnDefinition = "TEXT") private String endingPhotoBase64;
    @Column(length = 2000) private String endingCaption;
    /** 메인 사진과 같은 어휘: none/fog/wave/paper */
    private String endingEffect;

    /** 섹션 표시 순서 — 콤마 구분(예: "greet,cal,dday,hosts,gal,map,acct,rsvp"). 비어 있으면 템플릿 기본 순서 */
    private String sectionOrder;

    /** 게시되지 않은 편집 중 내용 — 게스트 화면에는 반영되지 않음 */
    @Column(columnDefinition = "TEXT") private String draftData;
    private LocalDateTime draftSavedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (viewCount == null) viewCount = 0;
        if (slug == null || slug.isEmpty()) slug = generateSlug();
    }

    private String generateSlug() {
        return Long.toHexString(System.currentTimeMillis()).substring(4)
             + Integer.toHexString((int)(Math.random() * 0xFFFF));
    }

    public WeddingEntity() {}

    // Slug / meta getters-setters
    public String getSlug() { return slug; } public void setSlug(String v) { this.slug = v; }
    public Long getUserId() { return userId; } public void setUserId(Long v) { this.userId = v; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public Integer getViewCount() { return viewCount; } public void setViewCount(Integer v) { this.viewCount = v; }

    // Core getters/setters
    public Long getId() { return id; } public void setId(Long v) { this.id=v; }
    public String getGroomName() { return groomName; } public void setGroomName(String v) { this.groomName=v; }
    public String getBrideName() { return brideName; } public void setBrideName(String v) { this.brideName=v; }
    public String getWeddingDate() { return weddingDate; } public void setWeddingDate(String v) { this.weddingDate=v; }
    public String getWeddingTime() { return weddingTime; } public void setWeddingTime(String v) { this.weddingTime=v; }
    public String getWeddingPlace() { return weddingPlace; } public void setWeddingPlace(String v) { this.weddingPlace=v; }
    public String getWeddingAddress() { return weddingAddress; } public void setWeddingAddress(String v) { this.weddingAddress=v; }
    public String getGreetingTitle() { return greetingTitle; } public void setGreetingTitle(String v) { this.greetingTitle=v; }
    public String getGreetingText() { return greetingText; } public void setGreetingText(String v) { this.greetingText=v; }
    public String getGreetingAlign() { return greetingAlign; } public void setGreetingAlign(String v) { this.greetingAlign=v; }
    public Boolean getGreetingVisible() { return greetingVisible; } public void setGreetingVisible(Boolean v) { this.greetingVisible=v; }
    public String getGroomFatherName() { return groomFatherName; } public void setGroomFatherName(String v) { this.groomFatherName=v; }
    public String getGroomMotherName() { return groomMotherName; } public void setGroomMotherName(String v) { this.groomMotherName=v; }
    public String getGroomFatherPhone() { return groomFatherPhone; } public void setGroomFatherPhone(String v) { this.groomFatherPhone=v; }
    public String getGroomMotherPhone() { return groomMotherPhone; } public void setGroomMotherPhone(String v) { this.groomMotherPhone=v; }
    public String getBrideFatherName() { return brideFatherName; } public void setBrideFatherName(String v) { this.brideFatherName=v; }
    public String getBrideMotherName() { return brideMotherName; } public void setBrideMotherName(String v) { this.brideMotherName=v; }
    public String getBrideFatherPhone() { return brideFatherPhone; } public void setBrideFatherPhone(String v) { this.brideFatherPhone=v; }
    public String getBrideMotherPhone() { return brideMotherPhone; } public void setBrideMotherPhone(String v) { this.brideMotherPhone=v; }
    public String getGroomPhone() { return groomPhone; } public void setGroomPhone(String v) { this.groomPhone=v; }
    public String getBridePhone() { return bridePhone; } public void setBridePhone(String v) { this.bridePhone=v; }
    public Boolean getHostsVisible() { return hostsVisible; } public void setHostsVisible(Boolean v) { this.hostsVisible=v; }
    public Boolean getGroomFatherDeceased() { return groomFatherDeceased; } public void setGroomFatherDeceased(Boolean v) { this.groomFatherDeceased=v; }
    public Boolean getGroomMotherDeceased() { return groomMotherDeceased; } public void setGroomMotherDeceased(Boolean v) { this.groomMotherDeceased=v; }
    public Boolean getBrideFatherDeceased() { return brideFatherDeceased; } public void setBrideFatherDeceased(Boolean v) { this.brideFatherDeceased=v; }
    public Boolean getBrideMotherDeceased() { return brideMotherDeceased; } public void setBrideMotherDeceased(Boolean v) { this.brideMotherDeceased=v; }
    public String getDeceasedDisplayType() { return deceasedDisplayType; } public void setDeceasedDisplayType(String v) { this.deceasedDisplayType=v; }
    public String getGroomRelation() { return groomRelation; } public void setGroomRelation(String v) { this.groomRelation=v; }
    public String getBrideRelation() { return brideRelation; } public void setBrideRelation(String v) { this.brideRelation=v; }
    public String getGroomBaptism() { return groomBaptism; } public void setGroomBaptism(String v) { this.groomBaptism=v; }
    public String getBrideBaptism() { return brideBaptism; } public void setBrideBaptism(String v) { this.brideBaptism=v; }
    public String getGroomFatherBaptism() { return groomFatherBaptism; } public void setGroomFatherBaptism(String v) { this.groomFatherBaptism=v; }
    public String getGroomMotherBaptism() { return groomMotherBaptism; } public void setGroomMotherBaptism(String v) { this.groomMotherBaptism=v; }
    public String getBrideFatherBaptism() { return brideFatherBaptism; } public void setBrideFatherBaptism(String v) { this.brideFatherBaptism=v; }
    public String getBrideMotherBaptism() { return brideMotherBaptism; } public void setBrideMotherBaptism(String v) { this.brideMotherBaptism=v; }
    public Boolean getContactPopupEnabled() { return contactPopupEnabled; } public void setContactPopupEnabled(Boolean v) { this.contactPopupEnabled=v; }
    public Boolean getCalendarVisible() { return calendarVisible; } public void setCalendarVisible(Boolean v) { this.calendarVisible=v; }
    public Boolean getDdayVisible() { return ddayVisible; } public void setDdayVisible(Boolean v) { this.ddayVisible=v; }
    public String getCalendarStyle() { return calendarStyle; } public void setCalendarStyle(String v) { this.calendarStyle=v; }
    public String getDdayStyle() { return ddayStyle; } public void setDdayStyle(String v) { this.ddayStyle=v; }
    public String getMapPlaceName() { return mapPlaceName; } public void setMapPlaceName(String v) { this.mapPlaceName=v; }
    public String getMapAddressRoad() { return mapAddressRoad; } public void setMapAddressRoad(String v) { this.mapAddressRoad=v; }
    public String getMapAddress() { return mapAddress; } public void setMapAddress(String v) { this.mapAddress=v; }
    public Double getMapLat() { return mapLat; } public void setMapLat(Double v) { this.mapLat=v; }
    public Double getMapLng() { return mapLng; } public void setMapLng(Double v) { this.mapLng=v; }
    public String getMapZoomLevel() { return mapZoomLevel; } public void setMapZoomLevel(String v) { this.mapZoomLevel=v; }
    public Boolean getMapDetailView() { return mapDetailView; } public void setMapDetailView(Boolean v) { this.mapDetailView=v; }
    public Boolean getMapVisible() { return mapVisible; } public void setMapVisible(Boolean v) { this.mapVisible=v; }
    public Boolean getMapLocked() { return mapLocked; } public void setMapLocked(Boolean v) { this.mapLocked=v; }
    public Boolean getMapNaviKakao() { return mapNaviKakao; } public void setMapNaviKakao(Boolean v) { this.mapNaviKakao=v; }
    public Boolean getMapNaviTmap() { return mapNaviTmap; } public void setMapNaviTmap(Boolean v) { this.mapNaviTmap=v; }
    public Boolean getMapNaviNaver() { return mapNaviNaver; } public void setMapNaviNaver(Boolean v) { this.mapNaviNaver=v; }
    public Boolean getGalleryVisible() { return galleryVisible; } public void setGalleryVisible(Boolean v) { this.galleryVisible=v; }
    public Boolean getAccountVisible() { return accountVisible; } public void setAccountVisible(Boolean v) { this.accountVisible=v; }
    public String getGalleryImages() { return galleryImages; } public void setGalleryImages(String v) { this.galleryImages=v; }
    public String getGalleryType() { return galleryType; } public void setGalleryType(String v) { this.galleryType=v; }
    public Boolean getGalleryScrollGuide() { return galleryScrollGuide; } public void setGalleryScrollGuide(Boolean v) { this.galleryScrollGuide=v; }
    public String getPhotoFilter() { return photoFilter; } public void setPhotoFilter(String v) { this.photoFilter=v; }
    public String getMainPhotoBase64() { return mainPhotoBase64; } public void setMainPhotoBase64(String v) { this.mainPhotoBase64=v; }
    public Double getMainPhotoPosX() { return mainPhotoPosX; } public void setMainPhotoPosX(Double v) { this.mainPhotoPosX=v; }
    public Double getMainPhotoPosY() { return mainPhotoPosY; } public void setMainPhotoPosY(Double v) { this.mainPhotoPosY=v; }
    public Double getMainPhotoScale() { return mainPhotoScale; } public void setMainPhotoScale(Double v) { this.mainPhotoScale=v; }
    public String getMainDesign() { return mainDesign; } public void setMainDesign(String v) { this.mainDesign=v; }
    public String getMainFont() { return mainFont; } public void setMainFont(String v) { this.mainFont=v; }
    public String getMainFontSize() { return mainFontSize; } public void setMainFontSize(String v) { this.mainFontSize=v; }
    public String getMainFontColor() { return mainFontColor; } public void setMainFontColor(String v) { this.mainFontColor=v; }
    public String getColorEffect() { return colorEffect; } public void setColorEffect(String v) { this.colorEffect=v; }
    public String getMainEffect() { return mainEffect; } public void setMainEffect(String v) { this.mainEffect=v; }
    public String getBgm() { return bgm; } public void setBgm(String v) { this.bgm=v; }
    public String getDisplayOrder() { return displayOrder; } public void setDisplayOrder(String v) { this.displayOrder=v; }
    public Boolean getRsvpEnabled() { return rsvpEnabled; } public void setRsvpEnabled(Boolean v) { this.rsvpEnabled=v; }
    public Boolean getGuestbookVisible() { return guestbookVisible; } public void setGuestbookVisible(Boolean v) { this.guestbookVisible=v; }
    public Boolean getEndingVisible() { return endingVisible; } public void setEndingVisible(Boolean v) { this.endingVisible=v; }
    public String getEndingPhotoBase64() { return endingPhotoBase64; } public void setEndingPhotoBase64(String v) { this.endingPhotoBase64=v; }
    public String getEndingCaption() { return endingCaption; } public void setEndingCaption(String v) { this.endingCaption=v; }
    public String getEndingEffect() { return endingEffect; } public void setEndingEffect(String v) { this.endingEffect=v; }
    public String getSectionOrder() { return sectionOrder; } public void setSectionOrder(String v) { this.sectionOrder=v; }
    public String getDraftData() { return draftData; } public void setDraftData(String v) { this.draftData=v; }
    public LocalDateTime getDraftSavedAt() { return draftSavedAt; } public void setDraftSavedAt(LocalDateTime v) { this.draftSavedAt=v; }

    public WeddingDto toDto() {
        return WeddingDto.builder()
            .id(id).slug(slug).userId(userId).createdAt(createdAt).viewCount(viewCount)
            .groomName(groomName).brideName(brideName)
            .weddingDate(weddingDate).weddingTime(weddingTime).weddingPlace(weddingPlace).weddingAddress(weddingAddress)
            .greetingTitle(greetingTitle).greetingText(greetingText).greetingAlign(greetingAlign).greetingVisible(greetingVisible)
            .groomFatherName(groomFatherName).groomMotherName(groomMotherName).groomFatherPhone(groomFatherPhone).groomMotherPhone(groomMotherPhone)
            .brideFatherName(brideFatherName).brideMotherName(brideMotherName).brideFatherPhone(brideFatherPhone).brideMotherPhone(brideMotherPhone)
            .groomPhone(groomPhone).bridePhone(bridePhone).hostsVisible(hostsVisible)
            .groomFatherDeceased(groomFatherDeceased).groomMotherDeceased(groomMotherDeceased)
            .brideFatherDeceased(brideFatherDeceased).brideMotherDeceased(brideMotherDeceased)
            .deceasedDisplayType(deceasedDisplayType).groomRelation(groomRelation).brideRelation(brideRelation)
            .groomBaptism(groomBaptism).brideBaptism(brideBaptism)
            .groomFatherBaptism(groomFatherBaptism).groomMotherBaptism(groomMotherBaptism)
            .brideFatherBaptism(brideFatherBaptism).brideMotherBaptism(brideMotherBaptism)
            .contactPopupEnabled(contactPopupEnabled)
            .calendarVisible(calendarVisible).ddayVisible(ddayVisible).calendarStyle(calendarStyle).ddayStyle(ddayStyle)
            .mapPlaceName(mapPlaceName).mapAddressRoad(mapAddressRoad).mapAddress(mapAddress)
            .mapLat(mapLat).mapLng(mapLng).mapZoomLevel(mapZoomLevel).mapDetailView(mapDetailView)
            .mapVisible(mapVisible).mapLocked(mapLocked)
            .mapNaviKakao(mapNaviKakao).mapNaviTmap(mapNaviTmap).mapNaviNaver(mapNaviNaver)
            .galleryVisible(galleryVisible).accountVisible(accountVisible).galleryImages(galleryImages).galleryType(galleryType).galleryScrollGuide(galleryScrollGuide)
            .photoFilter(photoFilter).mainPhotoBase64(mainPhotoBase64)
            .mainPhotoPosX(mainPhotoPosX).mainPhotoPosY(mainPhotoPosY).mainPhotoScale(mainPhotoScale)
            .mainDesign(mainDesign).mainFont(mainFont).mainFontSize(mainFontSize).mainFontColor(mainFontColor)
            .colorEffect(colorEffect).mainEffect(mainEffect).bgm(bgm)
            .displayOrder(displayOrder)
            .rsvpEnabled(rsvpEnabled).guestbookVisible(guestbookVisible)
            .endingVisible(endingVisible).endingPhotoBase64(endingPhotoBase64)
            .endingCaption(endingCaption).endingEffect(endingEffect)
            .sectionOrder(sectionOrder)
            .build();
    }

    public static WeddingEntity fromDto(WeddingDto d) {
        WeddingEntity e = new WeddingEntity();
        e.setId(d.getId());
        e.setSlug(d.getSlug());
        e.setUserId(d.getUserId());
        if (d.getCreatedAt() != null) e.setCreatedAt(d.getCreatedAt());
        if (d.getViewCount() != null) e.setViewCount(d.getViewCount());
        e.setGroomName(d.getGroomName()); e.setBrideName(d.getBrideName());
        e.setWeddingDate(d.getWeddingDate()); e.setWeddingTime(d.getWeddingTime());
        e.setWeddingPlace(d.getWeddingPlace()); e.setWeddingAddress(d.getWeddingAddress());
        e.setGreetingTitle(d.getGreetingTitle()); e.setGreetingText(d.getGreetingText());
        e.setGreetingAlign(d.getGreetingAlign()); e.setGreetingVisible(d.getGreetingVisible());
        e.setGroomFatherName(d.getGroomFatherName()); e.setGroomMotherName(d.getGroomMotherName());
        e.setGroomFatherPhone(d.getGroomFatherPhone()); e.setGroomMotherPhone(d.getGroomMotherPhone());
        e.setBrideFatherName(d.getBrideFatherName()); e.setBrideMotherName(d.getBrideMotherName());
        e.setBrideFatherPhone(d.getBrideFatherPhone()); e.setBrideMotherPhone(d.getBrideMotherPhone());
        e.setGroomPhone(d.getGroomPhone()); e.setBridePhone(d.getBridePhone()); e.setHostsVisible(d.getHostsVisible());
        e.setGroomFatherDeceased(d.getGroomFatherDeceased()); e.setGroomMotherDeceased(d.getGroomMotherDeceased());
        e.setBrideFatherDeceased(d.getBrideFatherDeceased()); e.setBrideMotherDeceased(d.getBrideMotherDeceased());
        e.setDeceasedDisplayType(d.getDeceasedDisplayType()); e.setGroomRelation(d.getGroomRelation()); e.setBrideRelation(d.getBrideRelation());
        e.setGroomBaptism(d.getGroomBaptism()); e.setBrideBaptism(d.getBrideBaptism());
        e.setGroomFatherBaptism(d.getGroomFatherBaptism()); e.setGroomMotherBaptism(d.getGroomMotherBaptism());
        e.setBrideFatherBaptism(d.getBrideFatherBaptism()); e.setBrideMotherBaptism(d.getBrideMotherBaptism());
        e.setContactPopupEnabled(d.getContactPopupEnabled());
        e.setCalendarVisible(d.getCalendarVisible()); e.setDdayVisible(d.getDdayVisible());
        e.setCalendarStyle(d.getCalendarStyle()); e.setDdayStyle(d.getDdayStyle());
        e.setMapPlaceName(d.getMapPlaceName()); e.setMapAddressRoad(d.getMapAddressRoad()); e.setMapAddress(d.getMapAddress());
        e.setMapLat(d.getMapLat()); e.setMapLng(d.getMapLng()); e.setMapZoomLevel(d.getMapZoomLevel()); e.setMapDetailView(d.getMapDetailView());
        e.setMapVisible(d.getMapVisible()); e.setMapLocked(d.getMapLocked());
        e.setMapNaviKakao(d.getMapNaviKakao()); e.setMapNaviTmap(d.getMapNaviTmap()); e.setMapNaviNaver(d.getMapNaviNaver());
        e.setGalleryVisible(d.getGalleryVisible()); e.setAccountVisible(d.getAccountVisible()); e.setGalleryImages(d.getGalleryImages());
        e.setGalleryType(d.getGalleryType()); e.setGalleryScrollGuide(d.getGalleryScrollGuide());
        e.setPhotoFilter(d.getPhotoFilter()); e.setMainPhotoBase64(d.getMainPhotoBase64());
        e.setMainPhotoPosX(d.getMainPhotoPosX()); e.setMainPhotoPosY(d.getMainPhotoPosY());
        e.setMainPhotoScale(d.getMainPhotoScale());
        e.setMainDesign(d.getMainDesign()); e.setMainFont(d.getMainFont()); e.setMainFontSize(d.getMainFontSize());
        e.setMainFontColor(d.getMainFontColor()); e.setColorEffect(d.getColorEffect());
        e.setMainEffect(d.getMainEffect()); e.setBgm(d.getBgm());
        if (d.getDisplayOrder() != null) e.setDisplayOrder(d.getDisplayOrder());
        e.setRsvpEnabled(d.getRsvpEnabled());
        e.setGuestbookVisible(d.getGuestbookVisible());
        e.setEndingVisible(d.getEndingVisible());
        e.setEndingPhotoBase64(d.getEndingPhotoBase64());
        e.setEndingCaption(d.getEndingCaption());
        e.setEndingEffect(d.getEndingEffect());
        e.setSectionOrder(d.getSectionOrder());
        return e;
    }
}
