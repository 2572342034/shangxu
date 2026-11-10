package com.shangxu.crm.trade.controller;

import com.shangxu.crm.common.R;
import com.shangxu.crm.trade.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "交易中心")
@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @Operation(summary = "交易概览")
    @GetMapping("/overview")
    @PreAuthorize("@perm.has('crm:trade:view')")
    public R<Map<String, Object>> overview() {
        return R.ok(tradeService.overview());
    }

    @Operation(summary = "交易趋势")
    @GetMapping("/trend")
    @PreAuthorize("@perm.has('crm:trade:view')")
    public R<Map<String, Object>> trend() {
        return R.ok(tradeService.trend());
    }

    @Operation(summary = "交易流水")
    @GetMapping("/flow")
    @PreAuthorize("@perm.has('crm:trade:view')")
    public R<List<Map<String, Object>>> flow(@RequestParam(defaultValue = "20") int limit) {
        return R.ok(tradeService.flow(limit));
    }

    @Operation(summary = "客户交易排行")
    @GetMapping("/rank")
    @PreAuthorize("@perm.has('crm:trade:view')")
    public R<List<Map<String, Object>>> rank() {
        return R.ok(tradeService.rank());
    }
}
