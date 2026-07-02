package com.example.weddingexam.controller;

import com.example.weddingexam.account.AccountDto;
import com.example.weddingexam.account.AccountService;
import com.example.weddingexam.dto.WeddingDto;
import com.example.weddingexam.security.CustomOAuth2User;
import com.example.weddingexam.service.WeddingService;
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
import java.util.List;
import java.util.Map;

@Controller
public class WeddingController {

    private final WeddingService weddingService;
    private final AccountService accountService;

    @Value("${kakao.map.appkey:502b99c57514360a34fdf5b9181ed284}")
    private String kakaoAppKey;

    @Value("${kakao.map.restkey:03a041000c72178b476cbb6e29431e81}")
    private String kakaoRestKey;

    public WeddingController(WeddingService weddingService, AccountService accountService) {
        this.weddingService = weddingService;
        this.accountService = accountService;
    }

    /* ── 랜딩 페이지 ── */
    @GetMapping("/")
    public String landing() {
        return "index";
    }

    /* ── slug 기반 공개 청첩장 ── */
    @GetMapping("/w/{slug}")
    public String invitationBySlug(@PathVariable String slug, Model model) {
        WeddingDto dto = weddingService.findBySlug(slug)
            .orElseThrow(() -> new IllegalArgumentException("청첩장을 찾을 수 없습니다: " + slug));
        weddingService.incrementViewCount(dto.getId());
        addFormattedFields(model, dto);
        List<AccountDto> accounts = accountService.findByWeddingId(dto.getId());
        if (accounts.isEmpty()) accounts = accountService.findAll();
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
        WeddingDto dto;
        if (weddings.isEmpty()) {
            // 청첩장이 없으면 자동 생성
            WeddingDto newDto = weddingService.getDefaultDto();
            newDto.setUserId(userId);
            dto = weddingService.save(newDto);
        } else {
            dto = weddings.get(0);
        }

        session.setAttribute("myWeddingId", dto.getId());
        model.addAttribute("wedding", dto);
        model.addAttribute("currentUser", principal.getEntity());

        List<AccountDto> accounts = accountService.findByWeddingId(dto.getId());
        if (accounts.isEmpty()) accounts = accountService.findAll();
        model.addAttribute("accounts", accounts);
        model.addAttribute("kakaoAppKey", kakaoAppKey);
        return "admin/edit";
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
        model.addAttribute("wedding", weddingService.findFirst());
        model.addAttribute("accounts", accountService.findAll());
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
        if (accounts.isEmpty()) accounts = accountService.findAll();
        model.addAttribute("accounts", accounts);
        model.addAttribute("kakaoAppKey", kakaoAppKey);
        return "invitation";
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

    /* ── 카카오맵 키워드 검색 프록시 ── */
    @GetMapping("/api/map/search")
    @ResponseBody
    public ResponseEntity<String> searchPlace(@RequestParam String query) {
        String key = (kakaoRestKey != null && !kakaoRestKey.isEmpty()) ? kakaoRestKey : kakaoAppKey;
        try {
            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + key);
            String url = "https://dapi.kakao.com/v2/local/search/keyword.json?query="
                       + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&size=15";
            ResponseEntity<String> resp = rt.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
            return ResponseEntity.ok()
                .header("Content-Type", "application/json; charset=UTF-8")
                .body(resp.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok()
                .header("Content-Type", "application/json; charset=UTF-8")
                .body("{\"documents\":[],\"meta\":{\"total_count\":0}}");
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

    /* ── AJAX 자동저장 ── */
    @PostMapping("/api/admin/autosave")
    @ResponseBody
    public ResponseEntity<Map<String,Object>> autoSave(
            @RequestBody WeddingDto dto,
            @AuthenticationPrincipal CustomOAuth2User principal,
            HttpSession session) {
        try {
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

            // 인증된 사용자의 청첩장 ID로 저장
            Long weddingId = dto.getId();
            if (weddingId == null && principal != null) {
                List<WeddingDto> weddings = weddingService.findByUserId(principal.getUserId());
                if (!weddings.isEmpty()) weddingId = weddings.get(0).getId();
            }
            if (weddingId == null) {
                Long sessionId = (Long) session.getAttribute("myWeddingId");
                if (sessionId != null) weddingId = sessionId;
            }

            if (weddingId != null) {
                dto.setId(weddingId);
                if (principal != null) dto.setUserId(principal.getUserId());
                weddingService.update(weddingId, dto);
            } else {
                weddingService.save(dto);
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
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
