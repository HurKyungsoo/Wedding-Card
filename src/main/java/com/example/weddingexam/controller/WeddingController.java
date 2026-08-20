package com.example.weddingexam.controller;

import com.example.weddingexam.account.AccountDto;
import com.example.weddingexam.account.AccountService;
import com.example.weddingexam.dto.WeddingDto;
import com.example.weddingexam.security.CustomOAuth2User;
import com.example.weddingexam.service.WeddingService;
import com.example.weddingexam.viewlog.ViewLogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
public class WeddingController {

    private final WeddingService weddingService;
    private final AccountService accountService;
    private final ViewLogService viewLogService;

    @Value("${kakao.map.appkey:}")
    private String kakaoAppKey;

    @Value("${kakao.map.restkey:}")
    private String kakaoRestKey;

    public WeddingController(WeddingService weddingService, AccountService accountService,
                              ViewLogService viewLogService) {
        this.weddingService = weddingService;
        this.accountService = accountService;
        this.viewLogService = viewLogService;
    }

    /* ── 랜딩 페이지 ── */
    @GetMapping("/")
    public String landing(Model model) {
        // "샘플 보기" 링크용 — 가장 먼저 생성된(데모) 청첩장의 slug. 청첩장이 하나도 없으면 null.
        String sampleSlug = weddingService.findFirst().getSlug();
        model.addAttribute("sampleSlug", sampleSlug);
        return "index";
    }

    /* ── slug 기반 공개 청첩장 ── */
    @GetMapping("/w/{slug}")
    public String invitationBySlug(@PathVariable String slug,
                                   @RequestParam(name = "preview", required = false) String preview,
                                   @AuthenticationPrincipal CustomOAuth2User principal,
                                   Model model) {
        WeddingDto dto = weddingService.findBySlug(slug)
            .orElseThrow(() -> new IllegalArgumentException("청첩장을 찾을 수 없습니다: " + slug));

        // 편집기 미리보기(iframe·전체 미리보기)는 조회수/방문로그에서 제외한다.
        // 편집기를 열거나 미리보기를 새로고침할 때마다 본인 트래픽이 통계에 쌓이던 문제.
        // 로그인 상태에서만 제외 — 하객이 URL에 ?preview=1을 붙여 집계를 피하지 못하게.
        boolean editorPreview = "1".equals(preview) && principal != null;
        if (!editorPreview) {
            weddingService.incrementViewCount(dto.getId());
            viewLogService.recordView(dto.getId());  // 일별 방문 로그 기록
        }
        addFormattedFields(model, dto);
        List<AccountDto> accounts = accountService.findByWeddingId(dto.getId());
        model.addAttribute("accounts", accounts);
        model.addAttribute("kakaoAppKey", kakaoAppKey);
        return "invitation";
    }

    /* ── 내 청첩장 생성 ── */
    @GetMapping("/my/create")
    public String createWedding(@AuthenticationPrincipal CustomOAuth2User principal) {
        // 이미 청첩장이 있으면 편집기로 이동
        List<WeddingDto> existing = weddingService.findByUserId(principal.getUserId());
        if (!existing.isEmpty()) return "redirect:/my/edit";

        WeddingDto dto = weddingService.getDefaultDto();
        dto.setUserId(principal.getUserId());
        weddingService.save(dto);
        return "redirect:/my/edit";
    }

    /* ── 내 청첩장 편집 (GET) ── */
    @GetMapping("/my/edit")
    public String myEdit(@AuthenticationPrincipal CustomOAuth2User principal,
                         HttpSession session, Model model) {
        Long userId = principal.getUserId();

        List<WeddingDto> weddings = weddingService.findByUserId(userId);
        WeddingDto published;
        if (weddings.isEmpty()) {
            // 청첩장이 없으면 자동 생성
            WeddingDto newDto = weddingService.getDefaultDto();
            newDto.setUserId(userId);
            published = weddingService.save(newDto);
        } else {
            published = weddings.get(0);
        }

        session.setAttribute("myWeddingId", published.getId());

        WeddingService.EditableWedding editable = weddingService.getEditable(published.getId());
        model.addAttribute("wedding", editable.dto());
        model.addAttribute("hasDraft", editable.hasDraft());
        model.addAttribute("draftSavedAt", editable.draftSavedAt() != null ? editable.draftSavedAt().toString() : null);
        model.addAttribute("currentUser", principal.getEntity());
        model.addAttribute("accounts", editableAccounts(editable, published.getId()));
        model.addAttribute("kakaoAppKey", kakaoAppKey);
        return "admin/edit";
    }

    /**
     * 편집기에 보여줄 계좌 목록 — 임시저장본에 계좌가 들어 있으면 그것을, 없으면 게시본을 쓴다.
     * 계좌도 다른 섹션처럼 초안 상태를 유지해야 하므로, 편집기를 다시 열었을 때
     * 게시된 계좌가 아니라 편집 중이던 계좌가 보여야 한다.
     */
    private List<AccountDto> editableAccounts(WeddingService.EditableWedding editable, Long weddingId) {
        if (editable != null && editable.hasDraft() && editable.dto().getAccounts() != null) {
            return editable.dto().getAccounts();
        }
        return accountService.findByWeddingId(weddingId);
    }

    /* ── 내 청첩장 저장 (POST) ── */
    @PostMapping("/my/edit")
    public String myUpdate(@ModelAttribute WeddingDto dto,
                           @AuthenticationPrincipal CustomOAuth2User principal) {
        Long userId = principal.getUserId();
        List<WeddingDto> weddings = weddingService.findByUserId(userId);
        if (weddings.isEmpty()) return "redirect:/my/edit";

        Long weddingId = weddings.get(0).getId();
        dto.setUserId(userId);
        weddingService.update(weddingId, dto);
        return "redirect:/my/edit?saved=true";
    }

    /* ── 기존 단일 청첩장 어드민 (하위 호환) ── */
    @GetMapping("/admin/edit")
    public String editForm(Model model) {
        WeddingDto published = weddingService.findFirst();
        WeddingService.EditableWedding editable = null;
        if (published.getId() != null) {
            editable = weddingService.getEditable(published.getId());
            model.addAttribute("wedding", editable.dto());
            model.addAttribute("hasDraft", editable.hasDraft());
            model.addAttribute("draftSavedAt", editable.draftSavedAt() != null ? editable.draftSavedAt().toString() : null);
        } else {
            model.addAttribute("wedding", published);
            model.addAttribute("hasDraft", false);
            model.addAttribute("draftSavedAt", null);
        }
        model.addAttribute("accounts", published.getId() != null
                ? editableAccounts(editable, published.getId())
                : accountService.findAll());
        model.addAttribute("kakaoAppKey", kakaoAppKey);
        return "admin/edit";
    }

    @PostMapping("/admin/edit")
    public String updateWedding(@ModelAttribute WeddingDto dto) {
        if (dto.getId() != null) weddingService.update(dto.getId(), dto);
        else weddingService.save(dto);
        return "redirect:/admin/edit?saved=true";
    }

    /* ── ID 기반 공개 청첩장 (하위 호환) ── */
    @GetMapping("/wedding/{id}")
    public String invitationById(@PathVariable Long id, Model model) {
        WeddingDto dto = weddingService.findById(id);
        addFormattedFields(model, dto);
        List<AccountDto> accounts = accountService.findByWeddingId(id);
        model.addAttribute("accounts", accounts);
        model.addAttribute("kakaoAppKey", kakaoAppKey);
        return "invitation";
    }

    /* ── 캘린더 담기 (.ics 다운로드) — 하객이 예식 일정을 본인 캘린더에 추가 ── */
    @GetMapping("/api/wedding/{id}/calendar.ics")
    public ResponseEntity<byte[]> calendarIcs(@PathVariable Long id) {
        WeddingDto dto;
        try {
            dto = weddingService.findById(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
        String ics = buildIcs(dto);
        if (ics == null) return ResponseEntity.badRequest().build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/calendar;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("wedding.ics", StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(ics.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }

    private String buildIcs(WeddingDto dto) {
        if (dto.getWeddingDate() == null || dto.getWeddingDate().isBlank()
                || dto.getWeddingTime() == null || dto.getWeddingTime().isBlank()) {
            return null;
        }
        LocalDateTime startKst;
        try {
            startKst = LocalDateTime.parse(dto.getWeddingDate() + "T" + dto.getWeddingTime());
        } catch (Exception e) {
            return null;
        }
        ZonedDateTime startUtc = startKst.atZone(ZoneId.of("Asia/Seoul")).withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime endUtc = startUtc.plusHours(1);
        DateTimeFormatter icsFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

        String summary = icsEscape(
            (dto.getGroomName() != null ? dto.getGroomName() : "신랑") + " ♥ " +
            (dto.getBrideName() != null ? dto.getBrideName() : "신부") + " 결혼식");
        String location = icsEscape(joinNonBlank(", ", dto.getWeddingPlace(), dto.getMapAddressRoad()));
        String description = icsEscape(joinNonBlank(" ", dto.getWeddingPlace(), dto.getMapAddress()));

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//WeddingCard//KO\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("BEGIN:VEVENT\r\n");
        sb.append("UID:wedding-").append(dto.getId()).append("@weddingcard\r\n");
        sb.append("DTSTAMP:").append(ZonedDateTime.now(ZoneOffset.UTC).format(icsFmt)).append("\r\n");
        sb.append("DTSTART:").append(startUtc.format(icsFmt)).append("\r\n");
        sb.append("DTEND:").append(endUtc.format(icsFmt)).append("\r\n");
        sb.append("SUMMARY:").append(summary).append("\r\n");
        if (!location.isEmpty()) sb.append("LOCATION:").append(location).append("\r\n");
        if (!description.isEmpty()) sb.append("DESCRIPTION:").append(description).append("\r\n");
        sb.append("END:VEVENT\r\n");
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private static String icsEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }

    private static String joinNonBlank(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }

    /* ── 카카오맵 Static Map 프록시 ── */
    @GetMapping("/api/map/staticmap")
    public ResponseEntity<byte[]> staticMap(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "480") int w,
            @RequestParam(defaultValue = "280") int h,
            @RequestParam(defaultValue = "3")   int level) {
        String key = (kakaoRestKey != null && !kakaoRestKey.isEmpty()) ? kakaoRestKey : kakaoAppKey;
        try {
            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + key);
            String url = String.format(
                "https://dapi.kakao.com/v2/maps/staticmap?center=%f,%f&level=%d&w=%d&h=%d&markers=marker_b_%f_%f",
                lng, lat, level, w, h, lng, lat);
            ResponseEntity<byte[]> resp = rt.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), byte[].class);
            HttpHeaders out = new HttpHeaders();
            out.setContentType(MediaType.IMAGE_PNG);
            out.setCacheControl("max-age=3600");
            return new ResponseEntity<>(resp.getBody(), out, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    /* ── 카카오맵 키워드 검색 프록시 ──
       편집기가 브라우저에서 dapi.kakao.com을 직접 호출하면 REST 키를 JS에 박아야 하고
       (= public 레포에 그대로 노출) 서버가 대신 호출해 결과만 넘겨준다. */
    @GetMapping("/api/map/search")
    @ResponseBody
    public ResponseEntity<String> searchPlace(@RequestParam String query) {
        return proxyKakaoLocal("keyword", query);
    }

    /* ── 카카오맵 주소 검색 프록시 — 키워드 검색 결과가 없을 때의 폴백 ── */
    @GetMapping("/api/map/address")
    @ResponseBody
    public ResponseEntity<String> searchAddressProxy(@RequestParam String query) {
        return proxyKakaoLocal("address", query);
    }

    /** 카카오 로컬 API(keyword/address) 호출 — 실패해도 편집기가 깨지지 않도록 빈 결과를 반환 */
    private ResponseEntity<String> proxyKakaoLocal(String kind, String query) {
        String key = (kakaoRestKey != null && !kakaoRestKey.isEmpty()) ? kakaoRestKey : kakaoAppKey;
        String empty = "{\"documents\":[],\"meta\":{\"total_count\":0}}";
        if (key == null || key.isEmpty()) {
            return ResponseEntity.ok()
                .header("Content-Type", "application/json; charset=UTF-8")
                .body(empty);
        }
        try {
            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + key);
            String url = "https://dapi.kakao.com/v2/local/search/" + kind + ".json?query="
                       + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&size=15";
            ResponseEntity<String> resp = rt.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
            return ResponseEntity.ok()
                .header("Content-Type", "application/json; charset=UTF-8")
                .body(resp.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok()
                .header("Content-Type", "application/json; charset=UTF-8")
                .body(empty);
        }
    }

    /* ── 저장된 메인 사진 반환 ── */
    @GetMapping("/api/admin/photo")
    @ResponseBody
    public ResponseEntity<byte[]> getMainPhoto() {
        try {
            WeddingDto dto = weddingService.findFirst();
            if (dto.getMainPhotoBase64() == null || dto.getMainPhotoBase64().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            byte[] bytes = java.util.Base64.getDecoder().decode(dto.getMainPhotoBase64());
            return ResponseEntity.ok()
                .header("Content-Type","image/jpeg")
                .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /* ── AJAX 임시저장 — 게스트 화면(게시본)에는 반영되지 않음 ── */
    @PostMapping("/api/admin/autosave")
    @ResponseBody
    public ResponseEntity<Map<String,Object>> autoSave(
            @RequestBody WeddingDto dto,
            @AuthenticationPrincipal CustomOAuth2User principal,
            HttpSession session) {
        try {
            normalizeToggleDefaults(dto);
            Long weddingId = resolveWeddingId(dto, principal, session);

            if (weddingId != null) {
                dto.setId(weddingId);
                if (principal != null) dto.setUserId(principal.getUserId());
                weddingService.saveDraft(weddingId, dto);
                return ResponseEntity.ok(Map.of("success", true, "draftSavedAt", LocalDateTime.now().toString()));
            } else {
                weddingService.save(dto);
                return ResponseEntity.ok(Map.of("success", true));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /* ── AJAX 게시 — 임시저장된 내용을 게스트 화면에 실제로 반영 ── */
    @PostMapping("/api/admin/publish")
    @ResponseBody
    public ResponseEntity<Map<String,Object>> publish(
            @RequestBody WeddingDto dto,
            @AuthenticationPrincipal CustomOAuth2User principal,
            HttpSession session) {
        try {
            normalizeToggleDefaults(dto);
            Long weddingId = resolveWeddingId(dto, principal, session);

            if (weddingId != null) {
                dto.setId(weddingId);
                if (principal != null) dto.setUserId(principal.getUserId());
                weddingService.publish(weddingId, dto);
                // 계좌도 이 시점에 반영 — 다른 섹션과 동일하게 "게시해야 하객 화면에 보인다"
                if (dto.getAccounts() != null) {
                    accountService.saveAllForWedding(weddingId, dto.getAccounts());
                }
            } else {
                weddingService.save(dto);
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** 폼에서 체크박스가 빠져 null로 들어오는 토글 필드의 기본값 보정 */
    private void normalizeToggleDefaults(WeddingDto dto) {
        if (dto.getMapNaviKakao() == null) dto.setMapNaviKakao(false);
        if (dto.getMapNaviTmap()  == null) dto.setMapNaviTmap(false);
        if (dto.getMapNaviNaver() == null) dto.setMapNaviNaver(false);
        if (dto.getAccountVisible()  == null) dto.setAccountVisible(false);
        if (dto.getGalleryVisible()  == null) dto.setGalleryVisible(false);
        if (dto.getMapVisible()      == null) dto.setMapVisible(false);
        if (dto.getGreetingVisible() == null) dto.setGreetingVisible(false);
        if (dto.getHostsVisible()    == null) dto.setHostsVisible(false);
        if (dto.getCalendarVisible() == null) dto.setCalendarVisible(false);
        if (dto.getDdayVisible()     == null) dto.setDdayVisible(false);
        if (dto.getDisplayOrder()    == null) dto.setDisplayOrder("groom");
        if (dto.getContactPopupEnabled() == null) dto.setContactPopupEnabled(true);
    }

    /**
     * 현재 편집 중인 청첩장 ID를 확인 — 로그인한 사용자의 소유 청첩장을 최우선으로 사용해
     * 요청 본문의 id를 신뢰해 다른 사용자의 청첩장을 덮어쓰는 것을 방지한다.
     */
    private Long resolveWeddingId(WeddingDto dto, CustomOAuth2User principal, HttpSession session) {
        if (principal != null) {
            List<WeddingDto> weddings = weddingService.findByUserId(principal.getUserId());
            if (!weddings.isEmpty()) return weddings.get(0).getId();
        }
        Long weddingId = dto.getId();
        if (weddingId == null) {
            Long sessionId = (Long) session.getAttribute("myWeddingId");
            if (sessionId != null) weddingId = sessionId;
        }
        return weddingId;
    }

    /* ── REST API (하위 호환) ── */
    @GetMapping("/api/wedding/{id}")
    @ResponseBody
    public ResponseEntity<WeddingDto> getWedding(@PathVariable Long id) {
        return ResponseEntity.ok(weddingService.findById(id));
    }

    @PostMapping("/api/wedding")
    @ResponseBody
    public ResponseEntity<WeddingDto> createWeddingApi(@RequestBody WeddingDto dto) {
        return ResponseEntity.ok(weddingService.save(dto));
    }

    @PutMapping("/api/wedding/{id}")
    @ResponseBody
    public ResponseEntity<WeddingDto> updateWeddingApi(@PathVariable Long id, @RequestBody WeddingDto dto) {
        return ResponseEntity.ok(weddingService.update(id, dto));
    }

    @DeleteMapping("/api/wedding/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteWedding(@PathVariable Long id) {
        weddingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void addFormattedFields(Model model, WeddingDto dto) {
        model.addAttribute("wedding", dto);
        model.addAttribute("greetingTitleHtml",
                dto.getGreetingTitle() != null
                        ? HtmlUtils.htmlEscape(dto.getGreetingTitle()).replace("\n", "<br>") : "");
        model.addAttribute("greetingTextHtml",
                dto.getGreetingText() != null
                        ? HtmlUtils.htmlEscape(dto.getGreetingText()).replace("\n", "<br>") : "");
    }
}
