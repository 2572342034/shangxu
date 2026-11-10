package com.shangxu.crm.payment.controller;

import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.log.OperLog;
import com.shangxu.crm.payment.dto.PaymentCreateRequest;
import com.shangxu.crm.payment.entity.PaymentRecord;
import com.shangxu.crm.payment.service.PaymentService;
import com.shangxu.crm.receivable.entity.Receivable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "收款核销")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @PreAuthorize("@perm.has('crm:payment:list')")
    public R<PageResult<PaymentRecord>> page(PageQuery query,
                                             @RequestParam(required = false) String method,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) Long customerId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(paymentService.page(query, method, status, customerId, startDate, endDate));
    }

    @Operation(summary = "收款汇总")
    @GetMapping("/summary")
    @PreAuthorize("@perm.has('crm:payment:list')")
    public R<Map<String, Object>> summary() {
        return R.ok(paymentService.summary());
    }

    @Operation(summary = "收款单详情（含核销明细）")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('crm:payment:list')")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(paymentService.detail(id));
    }

    @Operation(summary = "可核销应收单")
    @GetMapping("/open-receivables")
    @PreAuthorize("@perm.has('crm:payment:list')")
    public R<List<Receivable>> openReceivables(@RequestParam Long customerId) {
        return R.ok(paymentService.openReceivables(customerId));
    }

    @PostMapping
    @PreAuthorize("@perm.has('crm:payment:add')")
    @OperLog(title = "收款登记", type = "INSERT")
    public R<Long> create(@RequestBody PaymentCreateRequest request) {
        return R.ok("收款登记成功", paymentService.create(request));
    }

    @Operation(summary = "预收余额继续核销")
    @PutMapping("/{id}/allocate")
    @PreAuthorize("@perm.has('crm:payment:add')")
    @OperLog(title = "收款核销", type = "UPDATE")
    public R<Void> allocate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long receivableId = Long.valueOf(String.valueOf(body.get("receivableId")));
        BigDecimal amount = new BigDecimal(String.valueOf(body.get("amount")));
        paymentService.allocate(id, receivableId, amount);
        return R.ok("核销成功", null);
    }

    @Operation(summary = "作废收款单并反向冲销")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("@perm.has('crm:payment:cancel')")
    @OperLog(title = "收款作废", type = "UPDATE")
    public R<Void> cancel(@PathVariable Long id) {
        paymentService.cancel(id);
        return R.ok("已作废并冲销", null);
    }
}
