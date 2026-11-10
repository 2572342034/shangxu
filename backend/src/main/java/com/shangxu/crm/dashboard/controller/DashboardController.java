package com.shangxu.crm.dashboard.controller;

import com.shangxu.crm.common.R;
import com.shangxu.crm.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "工作台")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(dashboardService.overview());
    }

    @GetMapping("/funnel")
    public R<List<Map<String, Object>>> funnel() {
        return R.ok(dashboardService.funnel());
    }

    @GetMapping("/trend")
    public R<Map<String, Object>> trend() {
        return R.ok(dashboardService.trend());
    }

    @GetMapping("/rank")
    public R<List<Map<String, Object>>> rank() {
        return R.ok(dashboardService.rank());
    }

    @GetMapping("/source")
    public R<List<Map<String, Object>>> source() {
        return R.ok(dashboardService.source());
    }

    @GetMapping("/level")
    public R<List<Map<String, Object>>> level() {
        return R.ok(dashboardService.level());
    }

    @GetMapping("/todo")
    public R<Map<String, Object>> todo() {
        return R.ok(dashboardService.todo());
    }
}
