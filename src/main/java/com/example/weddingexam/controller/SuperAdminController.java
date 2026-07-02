package com.example.weddingexam.controller;

import com.example.weddingexam.service.WeddingService;
import com.example.weddingexam.user.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/superadmin")
public class SuperAdminController {

    private final WeddingService weddingService;
    private final UserService userService;

    public SuperAdminController(WeddingService weddingService, UserService userService) {
        this.weddingService = weddingService;
        this.userService = userService;
    }

    @GetMapping
    public String dashboard(Model model) {
        var weddings = weddingService.findAll();
        long totalViews = weddings.stream()
            .mapToLong(w -> w.getViewCount() != null ? w.getViewCount() : 0L)
            .sum();
        model.addAttribute("weddings", weddings);
        model.addAttribute("users", userService.findAll());
        model.addAttribute("totalWeddings", weddings.size());
        model.addAttribute("totalUsers", userService.countAll());
        model.addAttribute("proUsers", userService.countPro());
        model.addAttribute("totalViews", totalViews);
        return "superadmin/dashboard";
    }

    @PostMapping("/wedding/delete/{id}")
    public String deleteWedding(@PathVariable Long id) {
        weddingService.delete(id);
        return "redirect:/superadmin";
    }
}
