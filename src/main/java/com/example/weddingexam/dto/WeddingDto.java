package com.example.weddingexam.dto;

import java.time.LocalDateTime;

public class WeddingDto {

    private Long id;
    private String slug;
    private Long userId;
    private LocalDateTime createdAt;
    private Integer viewCount;

    private String groomName, brideName;
    private String weddingDate, weddingTime, weddingPlace, weddingAddress;
    private String greetingTitle, greetingText, greetingAlign;
    private Boolean greetingVisible;

    private String groomFatherName, groomMotherName, groomFatherPhone, groomMotherPhone;
    private String brideFatherName, brideMotherName, brideFatherPhone, brideMotherPhone;
    private String groomPhone, bridePhone;
    private Boolean hostsVisible;

    private Boolean groomFatherDeceased, groomMotherDeceased;
    private Boolean brideFatherDeceased, brideMotherDeceased;
    private String deceasedDisplayType;
    private String groomRelation, brideRelation;
    private String displayOrder;
    private Boolean contactPopupEnabled;

    private Boolean calendarVisible, ddayVisible;
    private String calendarStyle;
    private String ddayStyle;

    private String mapPlaceName, mapAddressRoad, mapAddress;
    private Double mapLat, mapLng;
    private String mapZoomLevel;
    private Boolean mapDetailView;
    private Boolean mapSketchUse;
    private Boolean mapVisible, mapDetailEnabled, mapLocked;
    private Boolean mapNaviKakao = false, mapNaviTmap = false, mapNaviNaver = false;

    private Boolean galleryVisible;
    private Boolean accountVisible;
    private String galleryImages;
    private String galleryType;
    private Boolean galleryScrollGuide;

    private String photoFilter, mainPhotoBase64;
    private String mainDesign, mainFont, mainFontSize, mainFontColor, colorEffect, mainEffect, bgm;
    private Boolean rsvpEnabled;

    public WeddingDto() {}

    // Meta getters/setters
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public String getSlug() { return slug; } public void setSlug(String v) { this.slug = v; }
    public Long getUserId() { return userId; } public void setUserId(Long v) { this.userId = v; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public Integer getViewCount() { return viewCount; } public void setViewCount(Integer v) { this.viewCount = v; }

    // Core getters/setters
    public String getGroomName() { return groomName; } public void setGroomName(String v) { this.groomName = v; }
    public String getBrideName() { return brideName; } public void setBrideName(String v) { this.brideName = v; }
    public String getWeddingDate() { return weddingDate; } public void setWeddingDate(String v) { this.weddingDate = v; }
    public String getWeddingTime() { return weddingTime; } public void setWeddingTime(String v) { this.weddingTime = v; }
    public String getWeddingPlace() { return weddingPlace; } public void setWeddingPlace(String v) { this.weddingPlace = v; }
    public String getWeddingAddress() { return weddingAddress; } public void setWeddingAddress(String v) { this.weddingAddress = v; }
    public String getGreetingTitle() { return greetingTitle; } public void setGreetingTitle(String v) { this.greetingTitle = v; }
    public String getGreetingText() { return greetingText; } public void setGreetingText(String v) { this.greetingText = v; }
    public String getGreetingAlign() { return greetingAlign; } public void setGreetingAlign(String v) { this.greetingAlign = v; }
    public Boolean getGreetingVisible() { return greetingVisible; } public void setGreetingVisible(Boolean v) { this.greetingVisible = v; }
    public String getGroomFatherName() { return groomFatherName; } public void setGroomFatherName(String v) { this.groomFatherName = v; }
    public String getGroomMotherName() { return groomMotherName; } public void setGroomMotherName(String v) { this.groomMotherName = v; }
    public String getGroomFatherPhone() { return groomFatherPhone; } public void setGroomFatherPhone(String v) { this.groomFatherPhone = v; }
    public String getGroomMotherPhone() { return groomMotherPhone; } public void setGroomMotherPhone(String v) { this.groomMotherPhone = v; }
    public String getBrideFatherName() { return brideFatherName; } public void setBrideFatherName(String v) { this.brideFatherName = v; }
    public String getBrideMotherName() { return brideMotherName; } public void setBrideMotherName(String v) { this.brideMotherName = v; }
    public String getBrideFatherPhone() { return brideFatherPhone; } public void setBrideFatherPhone(String v) { this.brideFatherPhone = v; }
    public String getBrideMotherPhone() { return brideMotherPhone; } public void setBrideMotherPhone(String v) { this.brideMotherPhone = v; }
    public String getGroomPhone() { return groomPhone; } public void setGroomPhone(String v) { this.groomPhone = v; }
    public String getBridePhone() { return bridePhone; } public void setBridePhone(String v) { this.bridePhone = v; }
    public Boolean getHostsVisible() { return hostsVisible; } public void setHostsVisible(Boolean v) { this.hostsVisible = v; }
    public Boolean getGroomFatherDeceased() { return groomFatherDeceased; } public void setGroomFatherDeceased(Boolean v) { this.groomFatherDeceased = v; }
    public Boolean getGroomMotherDeceased() { return groomMotherDeceased; } public void setGroomMotherDeceased(Boolean v) { this.groomMotherDeceased = v; }
    public Boolean getBrideFatherDeceased() { return brideFatherDeceased; } public void setBrideFatherDeceased(Boolean v) { this.brideFatherDeceased = v; }
    public Boolean getBrideMotherDeceased() { return brideMotherDeceased; } public void setBrideMotherDeceased(Boolean v) { this.brideMotherDeceased = v; }
    public String getDeceasedDisplayType() { return deceasedDisplayType; } public void setDeceasedDisplayType(String v) { this.deceasedDisplayType = v; }
    public String getGroomRelation() { return groomRelation; } public void setGroomRelation(String v) { this.groomRelation = v; }
    public String getBrideRelation() { return brideRelation; } public void setBrideRelation(String v) { this.brideRelation = v; }
    public String getDisplayOrder() { return displayOrder; } public void setDisplayOrder(String v) { this.displayOrder = v; }
    public Boolean getContactPopupEnabled() { return contactPopupEnabled; } public void setContactPopupEnabled(Boolean v) { this.contactPopupEnabled = v; }
    public Boolean getCalendarVisible() { return calendarVisible; } public void setCalendarVisible(Boolean v) { this.calendarVisible = v; }
    public Boolean getDdayVisible() { return ddayVisible; } public void setDdayVisible(Boolean v) { this.ddayVisible = v; }
    public String getCalendarStyle() { return calendarStyle; } public void setCalendarStyle(String v) { this.calendarStyle = v; }
    public String getDdayStyle() { return ddayStyle; } public void setDdayStyle(String v) { this.ddayStyle = v; }
    public String getMapPlaceName() { return mapPlaceName; } public void setMapPlaceName(String v) { this.mapPlaceName = v; }
    public String getMapAddressRoad() { return mapAddressRoad; } public void setMapAddressRoad(String v) { this.mapAddressRoad = v; }
    public String getMapAddress() { return mapAddress; } public void setMapAddress(String v) { this.mapAddress = v; }
    public Double getMapLat() { return mapLat; } public void setMapLat(Double v) { this.mapLat = v; }
    public Double getMapLng() { return mapLng; } public void setMapLng(Double v) { this.mapLng = v; }
    public String getMapZoomLevel() { return mapZoomLevel; } public void setMapZoomLevel(String v) { this.mapZoomLevel = v; }
    public Boolean getMapDetailView() { return mapDetailView; } public void setMapDetailView(Boolean v) { this.mapDetailView = v; }
    public Boolean getMapSketchUse() { return mapSketchUse; } public void setMapSketchUse(Boolean v) { this.mapSketchUse = v; }
    public Boolean getMapVisible() { return mapVisible; } public void setMapVisible(Boolean v) { this.mapVisible = v; }
    public Boolean getMapLocked() { return mapLocked; } public void setMapLocked(Boolean v) { this.mapLocked = v; }
    public Boolean getMapDetailEnabled() { return mapDetailEnabled; } public void setMapDetailEnabled(Boolean v) { this.mapDetailEnabled = v; }
    public Boolean getMapNaviKakao() { return mapNaviKakao; } public void setMapNaviKakao(Boolean v) { this.mapNaviKakao = v; }
    public Boolean getMapNaviTmap() { return mapNaviTmap; } public void setMapNaviTmap(Boolean v) { this.mapNaviTmap = v; }
    public Boolean getMapNaviNaver() { return mapNaviNaver; } public void setMapNaviNaver(Boolean v) { this.mapNaviNaver = v; }
    public Boolean getGalleryVisible() { return galleryVisible; } public void setGalleryVisible(Boolean v) { this.galleryVisible = v; }
    public Boolean getAccountVisible() { return accountVisible; } public void setAccountVisible(Boolean v) { this.accountVisible = v; }
    public String getGalleryImages() { return galleryImages; } public void setGalleryImages(String v) { this.galleryImages = v; }
    public String getGalleryType() { return galleryType; } public void setGalleryType(String v) { this.galleryType = v; }
    public Boolean getGalleryScrollGuide() { return galleryScrollGuide; } public void setGalleryScrollGuide(Boolean v) { this.galleryScrollGuide = v; }
    public String getPhotoFilter() { return photoFilter; } public void setPhotoFilter(String v) { this.photoFilter = v; }
    public String getMainPhotoBase64() { return mainPhotoBase64; } public void setMainPhotoBase64(String v) { this.mainPhotoBase64 = v; }
    public String getMainDesign() { return mainDesign; } public void setMainDesign(String v) { this.mainDesign = v; }
    public String getMainFont() { return mainFont; } public void setMainFont(String v) { this.mainFont = v; }
    public String getMainFontSize() { return mainFontSize; } public void setMainFontSize(String v) { this.mainFontSize = v; }
    public String getMainFontColor() { return mainFontColor; } public void setMainFontColor(String v) { this.mainFontColor = v; }
    public String getColorEffect() { return colorEffect; } public void setColorEffect(String v) { this.colorEffect = v; }
    public String getMainEffect() { return mainEffect; } public void setMainEffect(String v) { this.mainEffect = v; }
    public String getBgm() { return bgm; } public void setBgm(String v) { this.bgm = v; }
    public Boolean getRsvpEnabled() { return rsvpEnabled; } public void setRsvpEnabled(Boolean v) { this.rsvpEnabled = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final WeddingDto d = new WeddingDto();
        public Builder id(Long v) { d.id=v; return this; }
        public Builder slug(String v) { d.slug=v; return this; }
        public Builder userId(Long v) { d.userId=v; return this; }
        public Builder createdAt(LocalDateTime v) { d.createdAt=v; return this; }
        public Builder viewCount(Integer v) { d.viewCount=v; return this; }
        public Builder groomName(String v) { d.groomName=v; return this; }
        public Builder brideName(String v) { d.brideName=v; return this; }
        public Builder weddingDate(String v) { d.weddingDate=v; return this; }
        public Builder weddingTime(String v) { d.weddingTime=v; return this; }
        public Builder weddingPlace(String v) { d.weddingPlace=v; return this; }
        public Builder weddingAddress(String v) { d.weddingAddress=v; return this; }
        public Builder greetingTitle(String v) { d.greetingTitle=v; return this; }
        public Builder greetingText(String v) { d.greetingText=v; return this; }
        public Builder greetingAlign(String v) { d.greetingAlign=v; return this; }
        public Builder greetingVisible(Boolean v) { d.greetingVisible=v; return this; }
        public Builder groomFatherName(String v) { d.groomFatherName=v; return this; }
        public Builder groomMotherName(String v) { d.groomMotherName=v; return this; }
        public Builder groomFatherPhone(String v) { d.groomFatherPhone=v; return this; }
        public Builder groomMotherPhone(String v) { d.groomMotherPhone=v; return this; }
        public Builder brideFatherName(String v) { d.brideFatherName=v; return this; }
        public Builder brideMotherName(String v) { d.brideMotherName=v; return this; }
        public Builder brideFatherPhone(String v) { d.brideFatherPhone=v; return this; }
        public Builder brideMotherPhone(String v) { d.brideMotherPhone=v; return this; }
        public Builder groomPhone(String v) { d.groomPhone=v; return this; }
        public Builder bridePhone(String v) { d.bridePhone=v; return this; }
        public Builder hostsVisible(Boolean v) { d.hostsVisible=v; return this; }
        public Builder groomFatherDeceased(Boolean v) { d.groomFatherDeceased=v; return this; }
        public Builder groomMotherDeceased(Boolean v) { d.groomMotherDeceased=v; return this; }
        public Builder brideFatherDeceased(Boolean v) { d.brideFatherDeceased=v; return this; }
        public Builder brideMotherDeceased(Boolean v) { d.brideMotherDeceased=v; return this; }
        public Builder deceasedDisplayType(String v) { d.deceasedDisplayType=v; return this; }
        public Builder groomRelation(String v) { d.groomRelation=v; return this; }
        public Builder brideRelation(String v) { d.brideRelation=v; return this; }
        public Builder displayOrder(String v) { d.displayOrder=v; return this; }
        public Builder contactPopupEnabled(Boolean v) { d.contactPopupEnabled=v; return this; }
        public Builder calendarVisible(Boolean v) { d.calendarVisible=v; return this; }
        public Builder ddayVisible(Boolean v) { d.ddayVisible=v; return this; }
        public Builder calendarStyle(String v) { d.calendarStyle=v; return this; }
        public Builder ddayStyle(String v) { d.ddayStyle=v; return this; }
        public Builder mapPlaceName(String v) { d.mapPlaceName=v; return this; }
        public Builder mapAddressRoad(String v) { d.mapAddressRoad=v; return this; }
        public Builder mapAddress(String v) { d.mapAddress=v; return this; }
        public Builder mapLat(Double v) { d.mapLat=v; return this; }
        public Builder mapLng(Double v) { d.mapLng=v; return this; }
        public Builder mapZoomLevel(String v) { d.mapZoomLevel=v; return this; }
        public Builder mapDetailView(Boolean v) { d.mapDetailView=v; return this; }
        public Builder mapSketchUse(Boolean v) { d.mapSketchUse=v; return this; }
        public Builder mapVisible(Boolean v) { d.mapVisible=v; return this; }
        public Builder mapLocked(Boolean v) { d.mapLocked=v; return this; }
        public Builder mapDetailEnabled(Boolean v) { d.mapDetailEnabled=v; return this; }
        public Builder mapNaviKakao(Boolean v) { d.mapNaviKakao=v; return this; }
        public Builder mapNaviTmap(Boolean v) { d.mapNaviTmap=v; return this; }
        public Builder mapNaviNaver(Boolean v) { d.mapNaviNaver=v; return this; }
        public Builder galleryVisible(Boolean v) { d.galleryVisible=v; return this; }
        public Builder accountVisible(Boolean v) { d.accountVisible=v; return this; }
        public Builder galleryImages(String v) { d.galleryImages=v; return this; }
        public Builder galleryType(String v) { d.galleryType=v; return this; }
        public Builder galleryScrollGuide(Boolean v) { d.galleryScrollGuide=v; return this; }
        public Builder photoFilter(String v) { d.photoFilter=v; return this; }
        public Builder mainDesign(String v) { d.mainDesign=v; return this; }
        public Builder mainFont(String v) { d.mainFont=v; return this; }
        public Builder mainFontSize(String v) { d.mainFontSize=v; return this; }
        public Builder mainFontColor(String v) { d.mainFontColor=v; return this; }
        public Builder colorEffect(String v) { d.colorEffect=v; return this; }
        public Builder mainEffect(String v) { d.mainEffect=v; return this; }
        public Builder bgm(String v) { d.bgm=v; return this; }
        public Builder mainPhotoBase64(String v) { d.mainPhotoBase64=v; return this; }
        public Builder rsvpEnabled(Boolean v) { d.rsvpEnabled=v; return this; }
        public WeddingDto build() { return d; }
    }
}
