package com.example.weddingexam.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** 임시저장 스냅샷을 JSON으로 저장/복원하므로, 파생 getter(mainFontCss 등)는 역직렬화 시 무시 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
    /** 세례명 — 성당 예식에서 이름 옆에 괄호로 표기. 미입력이면 표시하지 않음 */
    private String groomBaptism, brideBaptism;
    private String groomFatherBaptism, groomMotherBaptism;
    private String brideFatherBaptism, brideMotherBaptism;
    private String displayOrder;
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
    private String galleryImages;
    private String galleryType;
    private Boolean galleryScrollGuide;

    private String photoFilter, mainPhotoBase64;
    private Double mainPhotoPosX, mainPhotoPosY;
    private Double mainPhotoScale;
    private String mainDesign, mainFont, mainFontSize, mainFontColor, colorEffect, mainEffect, bgm;
    private Boolean rsvpEnabled;
    private Boolean guestbookVisible;

    /** 섹션 표시 순서 — 콤마 구분(예: "greet,cal,dday,hosts,gal,map,acct,rsvp"). 비어 있으면 템플릿 기본 순서 */
    private String sectionOrder;

    /**
     * 계좌 목록 — wedding 테이블이 아니라 account 테이블에 저장되므로
     * WeddingEntity와는 주고받지 않는다. 임시저장 스냅샷(draftData)과 게시 페이로드에만 실려서,
     * 다른 섹션과 똑같이 "게시해야 하객 화면에 반영"되도록 만드는 용도.
     */
    private java.util.List<com.example.weddingexam.account.AccountDto> accounts;

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
    public String getGroomBaptism() { return groomBaptism; } public void setGroomBaptism(String v) { this.groomBaptism = v; }
    public String getBrideBaptism() { return brideBaptism; } public void setBrideBaptism(String v) { this.brideBaptism = v; }
    public String getGroomFatherBaptism() { return groomFatherBaptism; } public void setGroomFatherBaptism(String v) { this.groomFatherBaptism = v; }
    public String getGroomMotherBaptism() { return groomMotherBaptism; } public void setGroomMotherBaptism(String v) { this.groomMotherBaptism = v; }
    public String getBrideFatherBaptism() { return brideFatherBaptism; } public void setBrideFatherBaptism(String v) { this.brideFatherBaptism = v; }
    public String getBrideMotherBaptism() { return brideMotherBaptism; } public void setBrideMotherBaptism(String v) { this.brideMotherBaptism = v; }
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
    public Boolean getMapVisible() { return mapVisible; } public void setMapVisible(Boolean v) { this.mapVisible = v; }
    public Boolean getMapLocked() { return mapLocked; } public void setMapLocked(Boolean v) { this.mapLocked = v; }
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
    public Double getMainPhotoPosX() { return mainPhotoPosX; } public void setMainPhotoPosX(Double v) { this.mainPhotoPosX = v; }
    public Double getMainPhotoPosY() { return mainPhotoPosY; } public void setMainPhotoPosY(Double v) { this.mainPhotoPosY = v; }
    public Double getMainPhotoScale() { return mainPhotoScale; } public void setMainPhotoScale(Double v) { this.mainPhotoScale = v; }

    /** 메인 사진 img 태그용 style 값 — 필터 + (드래그로 지정한) 초점 위치 + 확대 배율. 미지정 시 테마 기본 CSS를 따름 */
    public String getMainPhotoImgStyle() {
        StringBuilder sb = new StringBuilder();
        if (photoFilter != null && !photoFilter.equals("none")) sb.append("filter:").append(photoFilter).append(";");
        if (mainPhotoPosX != null && mainPhotoPosY != null)
            sb.append("object-position:").append(mainPhotoPosX).append("% ").append(mainPhotoPosY).append("% !important;");
        if (mainPhotoScale != null && mainPhotoScale != 1.0)
            sb.append("transform:scale(").append(mainPhotoScale).append(") !important;");
        return sb.toString();
    }

    /** "Google 캘린더에 추가" 링크 — 날짜/시간이 없으면 빈 문자열 */
    public String getGoogleCalendarUrl() {
        if (weddingDate == null || weddingDate.isBlank() || weddingTime == null || weddingTime.isBlank()) return "";
        try {
            LocalDateTime startKst = LocalDateTime.parse(weddingDate + "T" + weddingTime);
            ZonedDateTime startUtc = startKst.atZone(ZoneId.of("Asia/Seoul")).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime endUtc = startUtc.plusHours(1);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
            String text = (groomName != null ? groomName : "신랑") + " ♥ " + (brideName != null ? brideName : "신부") + " 결혼식";
            String location = (weddingAddress != null && !weddingAddress.isBlank()) ? weddingAddress : (weddingPlace != null ? weddingPlace : "");
            return "https://calendar.google.com/calendar/render?action=TEMPLATE"
                + "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                + "&dates=" + startUtc.format(fmt) + "/" + endUtc.format(fmt)
                + "&details=" + URLEncoder.encode(weddingPlace != null ? weddingPlace : "", StandardCharsets.UTF_8)
                + "&location=" + URLEncoder.encode(location, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
    public String getMainDesign() { return mainDesign; } public void setMainDesign(String v) { this.mainDesign = v; }
    public String getMainFont() { return mainFont; } public void setMainFont(String v) { this.mainFont = v; }

    private static final java.util.Map<String, String> MAIN_FONT_CSS = java.util.Map.ofEntries(
        java.util.Map.entry("noto",         "'Noto Serif KR', serif"),
        java.util.Map.entry("playfair",     "'Playfair Display', 'Noto Serif KR', serif"),
        java.util.Map.entry("eb_garamond",  "'EB Garamond', 'Cormorant Garamond', serif"),
        java.util.Map.entry("cormorant",    "'Cormorant Garamond', serif"),
        java.util.Map.entry("dancing",      "'Dancing Script', cursive"),
        java.util.Map.entry("nanum",        "'Nanum Myeongjo', 'Noto Serif KR', serif"),
        java.util.Map.entry("gowun_batang", "'Gowun Batang', 'Noto Serif KR', serif"),
        java.util.Map.entry("song_myung",   "'Song Myung', 'Noto Serif KR', serif"),
        java.util.Map.entry("gowun_dodum",  "'Gowun Dodum', 'Noto Sans KR', sans-serif"),
        java.util.Map.entry("nanum_pen",    "'Nanum Pen Script', 'Noto Sans KR', cursive"),
        // 아래 셋은 구글폰트에 없다 — MAIN_FONT_CDN_CSS 로 따로 로드한다
        java.util.Map.entry("pretendard",     "'Pretendard', 'Noto Sans KR', sans-serif"),
        java.util.Map.entry("maru_buri",      "'Maru Buri', 'Noto Serif KR', serif"),
        java.util.Map.entry("gyeonggi_batang","'Gyeonggi Batang', 'Noto Serif KR', serif")
    );
    /** 청첩장 전역 글꼴(mainFont) 선택값을 실제 CSS font-family 스택으로 변환 — 테마 고유 타이틀 서체는 이 값과 무관하게 고정 */
    public String getMainFontCss() { return MAIN_FONT_CSS.getOrDefault(mainFont != null ? mainFont : "noto", MAIN_FONT_CSS.get("noto")); }

    /**
     * 한글 폰트는 구글폰트 CSS 한 벌이 50~90KB씩 한다(영문은 다 합쳐도 35KB).
     * 그래서 항상 로드하는 기본 묶음에는 넣지 않고, 그 청첩장이 실제로 고른 것만 덧붙인다.
     * 안 고른 하객은 한 바이트도 받지 않는다.
     *
     * 여기 없는 값(noto·영문 계열)은 기본 묶음에 이미 들어 있으므로 빈 문자열.
     */
    private static final java.util.Map<String, String> MAIN_FONT_GF_FAMILY = java.util.Map.of(
        "nanum",        "Nanum+Myeongjo:wght@400;700",
        "gowun_batang", "Gowun+Batang:wght@400;700",
        "song_myung",   "Song+Myung",
        "gowun_dodum",  "Gowun+Dodum",
        "nanum_pen",    "Nanum+Pen+Script"
    );
    /**
     * 기본 묶음에 없는 폰트 중 이 청첩장이 실제로 필요로 하는 것만 `&family=...` 로 이어 붙인다.
     *  - 고른 메인 글꼴 (한글 계열일 때만)
     *  - 방명록을 켰다면 손글씨체 — 방명록 이름/본문에만 쓰인다(57KB)
     * 둘 다 나눔손글씨펜인 경우가 있어 중복은 제거한다(같은 family 를 두 번 넘기면 URL 이 지저분해진다).
     */
    public String getExtraFontFamilies() {
        java.util.LinkedHashSet<String> fams = new java.util.LinkedHashSet<>();
        String sel = MAIN_FONT_GF_FAMILY.get(mainFont != null ? mainFont : "noto");
        if (sel != null) fams.add(sel);
        // null 은 "안 보임"이 아니라 "보임"이다 — invitation.html 이
        // `guestbookVisible != null and !guestbookVisible` 일 때만 숨기므로,
        // 토글을 한 번도 만지지 않은 기존 청첩장은 방명록이 그대로 뜬다.
        // 여기서 TRUE.equals 로 걸러버리면 그 청첩장들만 손글씨체가 폴백된다.
        if (!Boolean.FALSE.equals(guestbookVisible)) fams.add("Nanum+Pen+Script");

        StringBuilder sb = new StringBuilder();
        for (String f : fams) sb.append("&family=").append(f);
        return sb.toString();
    }

    /**
     * 구글폰트에 없어 별도 CDN 에서 받아야 하는 글꼴.
     * 여기 쓰는 CSS 는 구글폰트와 같은 방식이다 — unicode-range 로 쪼갠 subset 목록이라
     * 브라우저가 청첩장에 실제로 나온 글자가 든 조각만 내려받는다(조각당 20~35KB).
     * CSS 자체는 압축 전송 기준 24~27KB.
     *
     * 프리텐다드는 여기 없다 — invitation.css 가 본문 기본 글꼴로 이미 쓰고 있어
     * 모든 청첩장이 어차피 받는다. 목록에 넣는 비용이 0이라 넣었다.
     */
    private static final java.util.Map<String, String> MAIN_FONT_CDN_CSS = java.util.Map.of(
        "maru_buri",
        "https://cdn.jsdelivr.net/gh/fonts-archive/MaruBuri/subsets/MaruBuri-dynamic-subset.css",
        "gyeonggi_batang",
        "https://cdn.jsdelivr.net/gh/fonts-archive/GyeonggiBatang/subsets/GyeonggiBatang-dynamic-subset.css"
    );
    /** 하객 화면용 — 이 청첩장이 고른 글꼴이 별도 CDN 을 쓸 때만 한 줄. 아니면 빈 목록. */
    public java.util.Collection<String> getExtraFontStylesheets() {
        String href = MAIN_FONT_CDN_CSS.get(mainFont != null ? mainFont : "noto");
        return href == null ? java.util.List.of() : java.util.List.of(href);
    }
    /** 편집기 미리보기용 — 글꼴을 바꿔가며 보므로 미리 다 받아둔다. */
    public static java.util.Collection<String> getAllFontStylesheets() {
        return MAIN_FONT_CDN_CSS.values();
    }
    public String getMainFontSize() { return mainFontSize; } public void setMainFontSize(String v) { this.mainFontSize = v; }
    public String getMainFontColor() { return mainFontColor; } public void setMainFontColor(String v) { this.mainFontColor = v; }
    public String getColorEffect() { return colorEffect; } public void setColorEffect(String v) { this.colorEffect = v; }
    public String getMainEffect() { return mainEffect; } public void setMainEffect(String v) { this.mainEffect = v; }
    public String getBgm() { return bgm; } public void setBgm(String v) { this.bgm = v; }
    public Boolean getRsvpEnabled() { return rsvpEnabled; } public void setRsvpEnabled(Boolean v) { this.rsvpEnabled = v; }
    public Boolean getGuestbookVisible() { return guestbookVisible; } public void setGuestbookVisible(Boolean v) { this.guestbookVisible = v; }
    public String getSectionOrder() { return sectionOrder; } public void setSectionOrder(String v) { this.sectionOrder = v; }
    public java.util.List<com.example.weddingexam.account.AccountDto> getAccounts() { return accounts; }
    public void setAccounts(java.util.List<com.example.weddingexam.account.AccountDto> v) { this.accounts = v; }

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
        public Builder groomBaptism(String v) { d.groomBaptism=v; return this; }
        public Builder brideBaptism(String v) { d.brideBaptism=v; return this; }
        public Builder groomFatherBaptism(String v) { d.groomFatherBaptism=v; return this; }
        public Builder groomMotherBaptism(String v) { d.groomMotherBaptism=v; return this; }
        public Builder brideFatherBaptism(String v) { d.brideFatherBaptism=v; return this; }
        public Builder brideMotherBaptism(String v) { d.brideMotherBaptism=v; return this; }
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
        public Builder mapVisible(Boolean v) { d.mapVisible=v; return this; }
        public Builder mapLocked(Boolean v) { d.mapLocked=v; return this; }
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
        public Builder mainPhotoPosX(Double v) { d.mainPhotoPosX=v; return this; }
        public Builder mainPhotoPosY(Double v) { d.mainPhotoPosY=v; return this; }
        public Builder mainPhotoScale(Double v) { d.mainPhotoScale=v; return this; }
        public Builder rsvpEnabled(Boolean v) { d.rsvpEnabled=v; return this; }
        public Builder guestbookVisible(Boolean v) { d.guestbookVisible=v; return this; }
        public Builder sectionOrder(String v) { d.sectionOrder=v; return this; }
        public WeddingDto build() { return d; }
    }
}
