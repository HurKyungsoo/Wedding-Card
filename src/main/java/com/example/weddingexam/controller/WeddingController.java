package com.example.weddingexam.controller;

import com.example.weddingexam.account.AccountDto;
import com.example.weddingexam.account.AccountService;
import com.example.weddingexam.demo.DemoPhotos;
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
        WeddingDto sample = weddingService.findFirst();
        model.addAttribute("sampleSlug", sample.getSlug());
        // 기능 소개의 "오시는 길" 카드 — 샘플 청첩장과 같은 위치를 진짜 카카오 지도로 그린다.
        // 키나 좌표가 없으면 템플릿이 그려 둔 예시 그림을 그대로 둔다.
        model.addAttribute("kakaoAppKey", kakaoAppKey);
        model.addAttribute("sampleMapLat", sample.getMapLat());
        model.addAttribute("sampleMapLng", sample.getMapLng());
        String place = sample.getMapPlaceName() != null && !sample.getMapPlaceName().isBlank()
            ? sample.getMapPlaceName() : sample.getWeddingPlace();
        model.addAttribute("sampleMapPlace", place);
        // 카카오톡·슬랙 링크 미리보기(og:image)는 절대 주소여야 한다
        model.addAttribute("baseUrl",
            org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString());
        return "index";
    }

    /* ── 체험용 샘플 청첩장 — 랜딩의 체험 폰·테마 미리보기가 iframe 으로 띄운다 ──
       데모 청첩장(가장 먼저 만들어진 샘플)을 바탕으로 사진·갤러리만 예시로 채워 보여준다.
       저장하지 않고, 조회수·방문 로그도 남기지 않는다(랜딩 방문마다 샘플 통계가 오르던 것 방지).
       방명록·참석 응답은 화면만 보이고 제출은 막는다(템플릿의 demo 스크립트). */
    @GetMapping("/demo")
    public String demo(@RequestParam(name = "theme", required = false) String theme, Model model) {
        WeddingDto dto = weddingService.findFirst();
        String design = DemoPhotos.isDesign(theme) ? theme : "our_story";
        dto.setMainDesign(design);
        dto.setMainPhotoBase64(DemoPhotos.photoUrl(design));   // 파일 URL — HTML 에 사진을 박지 않는다
        dto.setMainPhotoPosX(null);
        dto.setMainPhotoPosY(null);
        dto.setMainPhotoScale(null);
        dto.setPhotoFilter(null);
        dto.setGalleryImages(DemoPhotos.GALLERY);
        dto.setGalleryVisible(true);

        addFormattedFields(model, dto);
        model.addAttribute("accounts", accountService.findByWeddingId(dto.getId()));
        model.addAttribute("kakaoAppKey", kakaoAppKey);
        model.addAttribute("loadAllFonts", false);
        model.addAttribute("editorPreview", false);
        model.addAttribute("demo", true);
        return "invitation";
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
        // 편집기 미리보기에서는 글꼴을 새로고침 없이 바꿔가며 보므로 전부 로드해야 한다.
        // 하객 화면은 그 청첩장이 고른 것만 받는다(한글 폰트 CSS가 한 벌에 50~90KB).
        // 인증과 무관 — 폰트를 더 받는 것뿐이라 preview 파라미터만 본다.
        model.addAttribute("loadAllFonts", "1".equals(preview));
        // 편집기 미리보기 여부 — 실제 청첩장에서는 빈 칸·편집용 안내("사진을 업로드하세요")를 숨긴다
        model.addAttribute("editorPreview", "1".equals(preview));
        return "invitation";
    }

    /* ── 마이페이지 — 내 청첩장 목록 ── */
    @GetMapping("/my")
    public String myPage(@AuthenticationPrincipal CustomOAuth2User principal,
                         @RequestParam(name = "error", required = false) String error,
                         Model model) {
        Long userId = principal.getUserId();
        List<WeddingDto> weddings = weddingService.findByUserIdOrderByCreatedAtDesc(userId);

        // 목록에 "작성 중 / 게시됨"을 보여주기 위해 청첩장별 임시저장 상태를 함께 넘긴다
        List<MyWeddingRow> rows = weddings.stream().map(w -> {
            WeddingService.EditableWedding editable = weddingService.getEditable(w.getId());
            return new MyWeddingRow(editable.dto(), editable.hasDraft(), editable.draftSavedAt());
        }).toList();

        model.addAttribute("rows", rows);
        model.addAttribute("currentUser", principal.getEntity());
        model.addAttribute("canCreate", weddings.size() < WeddingService.MAX_WEDDINGS_PER_USER);
        model.addAttribute("maxWeddings", WeddingService.MAX_WEDDINGS_PER_USER);
        model.addAttribute("errorMessage", error);
        return "my/list";
    }

    /** 마이페이지 목록 한 줄 — 청첩장 내용 + 임시저장 상태 */
    public record MyWeddingRow(WeddingDto wedding, boolean hasDraft, LocalDateTime draftSavedAt) {}

    /* ── 랜딩 "이 테마로 만들기" 진입점 (비로그인 공개) ──
       /my/create 는 로그인이 필요하고, 로그인 성공 후에는 항상 /my/edit 로 보내도록 되어 있어
       로그인을 거치면 고른 테마가 사라졌다. 세션에 적어 두고 로그인 성공 처리에서 이어 준다. */
    @GetMapping("/start")
    public String start(@RequestParam(name = "design", required = false) String design,
                        @AuthenticationPrincipal CustomOAuth2User principal,
                        HttpSession session) {
        String d = DemoPhotos.isDesign(design) ? design : null;
        if (principal != null) {
            return "redirect:/my/create" + (d != null ? "?design=" + d : "");
        }
        if (d != null) session.setAttribute(PENDING_DESIGN, d);
        return "redirect:/my/create";
    }

    /** 로그인 전에 고른 테마 — SecurityConfig 의 로그인 성공 처리가 꺼내 쓴다 */
    public static final String PENDING_DESIGN = "pendingDesign";

    /* ── 새 청첩장 만들기 ── */
    @GetMapping("/my/create")
    public String createWedding(@AuthenticationPrincipal CustomOAuth2User principal,
                                @RequestParam(name = "design", required = false) String design) {
        try {
            // 랜딩에서 "이 테마로 만들기"로 왔으면 그 테마가 고른 상태로 편집 화면이 열린다
            WeddingDto created = weddingService.createForUser(principal.getUserId(),
                DemoPhotos.isDesign(design) ? design : null);
            return "redirect:/my/edit?id=" + created.getId();
        } catch (IllegalStateException e) {
            return "redirect:/my?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
    }

    /* ── 내 청첩장 삭제 ── */
    @PostMapping("/my/{id}/delete")
    public String deleteMyWedding(@PathVariable Long id,
                                  @AuthenticationPrincipal CustomOAuth2User principal) {
        weddingService.deleteForUser(id, principal.getUserId());
        return "redirect:/my";
    }

    /* ── 내 청첩장 편집 (GET) ── */
    @GetMapping("/my/edit")
    public String myEdit(@AuthenticationPrincipal CustomOAuth2User principal,
                         @RequestParam(name = "id", required = false) Long id,
                         HttpSession session, Model model) {
        Long userId = principal.getUserId();
        List<WeddingDto> weddings = weddingService.findByUserIdOrderByCreatedAtDesc(userId);

        WeddingDto published;
        if (id != null) {
            // 남의 청첩장을 id로 열지 못하게 소유 확인
            if (!weddingService.isOwnedByUser(id, userId)) return "redirect:/my";
            published = weddingService.findById(id);
        } else if (weddings.size() == 1) {
            published = weddings.get(0);            // 하나뿐이면 바로 연다
        } else if (weddings.isEmpty()) {
            published = weddingService.createForUser(userId);
        } else {
            return "redirect:/my";                  // 여러 개면 어느 것인지 고르게 한다
        }

        session.setAttribute("myWeddingId", published.getId());

        WeddingService.EditableWedding editable = weddingService.getEditable(published.getId());
        model.addAttribute("wedding", editable.dto());
        model.addAttribute("hasDraft", editable.hasDraft());
        model.addAttribute("draftSavedAt", editable.draftSavedAt() != null ? editable.draftSavedAt().toString() : null);
        model.addAttribute("currentUser", principal.getEntity());
        model.addAttribute("accounts", editableAccounts(editable, published.getId()));
        model.addAttribute("kakaoAppKey", kakaoAppKey);
        model.addAttribute("fontChoices", WeddingDto.getFontChoices());
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
                           @AuthenticationPrincipal CustomOAuth2User principal,
                           HttpSession session) {
        Long userId = principal.getUserId();
        // 폼의 id를 쓰되 소유 확인을 거친다 — "첫 번째 청첩장"으로 고정하면
        // 여러 개일 때 엉뚱한 청첩장을 덮어쓴다
        Long weddingId = resolveWeddingId(dto, principal, session);
        if (weddingId == null) return "redirect:/my";

        dto.setUserId(userId);
        weddingService.update(weddingId, dto);
        return "redirect:/my/edit?id=" + weddingId + "&saved=true";
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
        model.addAttribute("fontChoices", WeddingDto.getFontChoices());
        return "admin/edit";
    }

    @PostMapping("/admin/edit")
    public String updateWedding(@ModelAttribute WeddingDto dto) {
        if (dto.getId() != null) weddingService.update(dto.getId(), dto);
        else weddingService.save(dto);
        return "redirect:/admin/edit?saved=true";
    }

    /* ── ID 기반 공개 청첩장 (하위 호환) ──
         slug 발급 전인 청첩장은 편집기 미리보기도 이 경로를 쓴다(`?preview=1`) */
    @GetMapping("/wedding/{id}")
    public String invitationById(@PathVariable Long id,
                                 @RequestParam(name = "preview", required = false) String preview,
                                 Model model) {
        WeddingDto dto = weddingService.findById(id);
        addFormattedFields(model, dto);
        List<AccountDto> accounts = accountService.findByWeddingId(id);
        model.addAttribute("accounts", accounts);
        model.addAttribute("kakaoAppKey", kakaoAppKey);
        model.addAttribute("loadAllFonts", "1".equals(preview));
        // 편집기 미리보기 여부 — 실제 청첩장에서는 빈 칸·편집용 안내("사진을 업로드하세요")를 숨긴다
        model.addAttribute("editorPreview", "1".equals(preview));
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

    /**
     * 카카오 로컬 API 주소 — 검색어를 정확히 한 번만 인코딩한 URI.
     * 예전엔 URLEncoder 로 인코딩한 문자열을 RestTemplate.exchange(String) 에 넘겼는데,
     * 문자열 버전은 그것을 URI 템플릿으로 보고 한 번 더 인코딩한다(% → %25).
     * 카카오는 "%EA%B7%B8..." 라는 글자 그대로를 검색해 한글 검색이 전부 0건이었다.
     */
    static java.net.URI kakaoLocalUri(String kind, String query) {
        return org.springframework.web.util.UriComponentsBuilder
            .fromHttpUrl("https://dapi.kakao.com/v2/local/search/" + kind + ".json")
            .queryParam("query", query)
            .queryParam("size", 15)
            .encode(StandardCharsets.UTF_8)
            .build()
            .toUri();
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
            ResponseEntity<String> resp = rt.exchange(kakaoLocalUri(kind, query), HttpMethod.GET,
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
            String src = WeddingDto.photoSrc(dto.getMainPhotoBase64());
            if (src.startsWith("/")) {   // 파일로 저장된 사진 — 그 주소로 보낸다
                return ResponseEntity.status(302).header("Location", src).build();
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
                return ResponseEntity.ok(Map.of("success", true, "draftSavedAt", LocalDateTime.now().toString(),
                        "photos", photoFields(dto)));
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
            return ResponseEntity.ok(Map.of("success", true, "photos", photoFields(dto)));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 저장하면서 파일로 바뀐 사진 값 — 편집기가 받아서 자기 입력값(base64)을 URL 로 바꿔 둔다.
     * 그래야 다음 자동저장부터는 사진을 다시 보내지 않는다.
     */
    private static Map<String, String> photoFields(WeddingDto dto) {
        Map<String, String> m = new java.util.HashMap<>();
        m.put("mainPhotoBase64",   nz(dto.getMainPhotoBase64()));
        m.put("endingPhotoBase64", nz(dto.getEndingPhotoBase64()));
        m.put("endingPhotos",      nz(dto.getEndingPhotos()));
        m.put("galleryImages",     nz(dto.getGalleryImages()));
        return m;
    }

    private static String nz(String v) { return v == null ? "" : v; }

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
        // 체크 안 된 체크박스는 폼에서 아예 빠지므로, 다른 토글과 동일하게 null이면 꺼짐으로 본다.
        // (기존 청첩장은 컬럼이 null이라 화면상 기본 표시 — 템플릿의 "null이면 보임" 규칙을 따른다)
        if (dto.getGuestbookVisible() == null) dto.setGuestbookVisible(false);
        if (dto.getEndingVisible() == null) dto.setEndingVisible(false);
    }

    /**
     * 지금 편집 중인 청첩장 ID를 확인한다.
     *
     * 요청 본문의 id를 쓰되 "그 청첩장이 요청자 소유인지"를 반드시 확인한다 —
     * 확인 없이 쓰면 남의 청첩장을 덮어쓸 수 있다.
     *
     * 예전에는 로그인 사용자면 본문 id를 무시하고 무조건 첫 번째 청첩장을 돌려줬다.
     * 청첩장이 하나뿐일 땐 문제가 없었지만, 여러 개를 만들 수 있게 되면
     * 두 번째 청첩장을 편집해도 첫 번째에 저장되는 심각한 버그가 된다.
     */
    private Long resolveWeddingId(WeddingDto dto, CustomOAuth2User principal, HttpSession session) {
        if (principal == null) return dto.getId();   // 로그인 전 레거시 경로

        Long userId = principal.getUserId();

        Long requested = dto.getId();
        if (requested != null && weddingService.isOwnedByUser(requested, userId)) return requested;

        Long sessionId = (Long) session.getAttribute("myWeddingId");
        if (sessionId != null && weddingService.isOwnedByUser(sessionId, userId)) return sessionId;

        // 마지막 안전망 — 소유한 청첩장이 하나뿐이면 그것으로 본다
        List<WeddingDto> weddings = weddingService.findByUserId(userId);
        if (weddings.size() == 1) return weddings.get(0).getId();

        return null;
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
        model.addAttribute("endingCaptionHtml",
                dto.getEndingCaption() != null
                        ? HtmlUtils.htmlEscape(dto.getEndingCaption()).replace("\n", "<br>") : "");
    }
}
