package com.example.weddingexam.controller;

import com.example.weddingexam.rsvp.RsvpService;
import com.example.weddingexam.service.WeddingService;
import com.example.weddingexam.user.UserService;
import com.example.weddingexam.viewlog.ViewLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Controller
@RequestMapping("/superadmin")
public class SuperAdminController {

    private final WeddingService weddingService;
    private final UserService userService;
    private final ViewLogService viewLogService;
    private final RsvpService rsvpService;

    public SuperAdminController(WeddingService weddingService, UserService userService,
                                 ViewLogService viewLogService, RsvpService rsvpService) {
        this.weddingService = weddingService;
        this.userService = userService;
        this.viewLogService = viewLogService;
        this.rsvpService = rsvpService;
    }

    @GetMapping
    public String dashboard(Model model) {
        var weddings = weddingService.findAll();
        var users = userService.findAll();

        long totalViews = weddings.stream()
            .mapToLong(w -> w.getViewCount() != null ? w.getViewCount() : 0L)
            .sum();

        // ── 일별 방문 차트 (최근 14일) ──
        ViewLogService.DailyChartData chart = viewLogService.getDailyTotals(14);

        // ── 테마별 사용 비율 ──
        Map<String, Long> themeCounts = new LinkedHashMap<>();
        themeCounts.put("기본형", 0L);
        themeCounts.put("Our story", 0L);
        themeCounts.put("Our story 핑크", 0L);
        themeCounts.put("Getting Married", 0L);
        themeCounts.put("Together forever", 0L);
        for (var w : weddings) {
            String d = w.getMainDesign() == null ? "basic" : w.getMainDesign();
            switch (d) {
                case "our_story"      -> themeCounts.merge("Our story", 1L, Long::sum);
                case "our_story_pink" -> themeCounts.merge("Our story 핑크", 1L, Long::sum);
                case "married"        -> themeCounts.merge("Getting Married", 1L, Long::sum);
                case "forever"        -> themeCounts.merge("Together forever", 1L, Long::sum);
                default               -> themeCounts.merge("기본형", 1L, Long::sum);
            }
        }

        // ── 최근 7일 가입자 / 청첩장 생성 추이 ──
        List<String> growthLabels = new ArrayList<>();
        List<Long> signupData  = new ArrayList<>();
        List<Long> weddingData = new ArrayList<>();
        LocalDate from = LocalDate.now().minusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d");

        Map<LocalDate, Long> signupByDay  = countByDay(users,    u -> u.getCreatedAt());
        Map<LocalDate, Long> weddingByDay = countByDay(weddings, w -> w.getCreatedAt());

        for (int i = 0; i < 7; i++) {
            LocalDate d = from.plusDays(i);
            growthLabels.add(d.format(fmt));
            signupData.add(signupByDay.getOrDefault(d, 0L));
            weddingData.add(weddingByDay.getOrDefault(d, 0L));
        }

        // ── RSVP 참석 집계 ──
        RsvpService.RsvpSummary rsvp = rsvpService.summary();

        model.addAttribute("weddings", weddings);
        model.addAttribute("users", users);
        model.addAttribute("totalWeddings", weddings.size());
        model.addAttribute("totalUsers", userService.countAll());
        model.addAttribute("proUsers", userService.countPro());
        model.addAttribute("totalViews", totalViews);

        // 방문 차트
        model.addAttribute("chartLabels", chart.labels());
        model.addAttribute("chartData", chart.data());
        model.addAttribute("themeLabels", themeCounts.keySet());
        model.addAttribute("themeData", themeCounts.values());

        // 가입/생성 추이 차트
        model.addAttribute("growthLabels", growthLabels);
        model.addAttribute("signupData", signupData);
        model.addAttribute("weddingData", weddingData);

        // RSVP 집계
        model.addAttribute("rsvpTotal", rsvp.total());
        model.addAttribute("rsvpAttend", rsvp.attend());
        model.addAttribute("rsvpDecline", rsvp.decline());
        model.addAttribute("rsvpMeals", rsvp.totalMeals());

        // 오늘 방문 수 (차트 마지막 값)
        long todayViews = chart.data().isEmpty() ? 0L : chart.data().get(chart.data().size() - 1);
        model.addAttribute("todayViews", todayViews);

        return "superadmin/dashboard";
    }

    /** createdAt 기준 날짜별 카운트 (최근 7일 범위만) */
    private <T> Map<LocalDate, Long> countByDay(List<T> list, Function<T, LocalDateTime> getter) {
        LocalDate from = LocalDate.now().minusDays(6);
        Map<LocalDate, Long> map = new LinkedHashMap<>();
        for (T item : list) {
            LocalDateTime created = getter.apply(item);
            if (created == null) continue;
            LocalDate d = created.toLocalDate();
            if (d.isBefore(from)) continue;
            map.merge(d, 1L, Long::sum);
        }
        return map;
    }

    @PostMapping("/wedding/delete/{id}")
    public String deleteWedding(@PathVariable Long id) {
        weddingService.delete(id);
        return "redirect:/superadmin";
    }
}
