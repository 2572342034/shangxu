package com.shangxu.crm.receivable.controller;

import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.log.OperLog;
import com.shangxu.crm.receivable.entity.Receivable;
import com.shangxu.crm.receivable.service.ReceivableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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

@Tag(name = "应收管理")
@RestController
@RequestMapping("/api/receivables")
@RequiredArgsConstructor
public class ReceivableController {

    private final ReceivableService receivableService;

    @GetMapping
    @PreAuthorize("@perm.has('crm:receivable:list')")
    public R<PageResult<Receivable>> page(PageQuery query,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) Boolean overdue,
                                          @RequestParam(required = false) Long customerId) {
        return R.ok(receivableService.page(query, status, overdue, customerId));
    }

    @Operation(summary = "应收汇总")
    @GetMapping("/summary")
    @PreAuthorize("@perm.has('crm:receivable:list')")
    public R<Map<String, Object>> summary() {
        return R.ok(receivableService.summary());
    }

    @GetMapping("/pending")
    @PreAuthorize("@perm.has('crm:receivable:list')")
    public R<List<Receivable>> pending() {
        return R.ok(receivableService.pending());
    }

    @PostMapping
    @PreAuthorize("@perm.has('crm:receivable:add')")
    @OperLog(title = "应收管理", type = "INSERT")
    public R<Long> create(@RequestBody Receivable receivable) {
        return R.ok("应收单创建成功", receivableService.create(receivable));
    }

    @Operation(summary = "确认应收（财务确认债权）")
    @PutMapping("/{id}/audit")
    @PreAuthorize("@perm.has('crm:receivable:add')")
    @OperLog(title = "应收确认", type = "UPDATE")
    public R<Void> audit(@PathVariable Long id, @RequestParam String status) {
        receivableService.audit(id, status);
        return R.ok("APPROVED".equals(status) ? "应收已确认" : "已驳回", null);
    }

    @Operation(summary = "由订单生成应收单")
    @PostMapping("/from-order/{orderId}")
    @PreAuthorize("@perm.has('crm:receivable:add')")
    @OperLog(title = "应收管理", type = "INSERT")
    public R<Long> fromOrder(@PathVariable Long orderId) {
        return R.ok("应收单已生成", receivableService.createFromOrder(orderId));
    }

    @Operation(summary = "由合同生成应收单")
    @PostMapping("/from-contract/{contractId}")
    @PreAuthorize("@perm.has('crm:receivable:add')")
    @OperLog(title = "应收管理", type = "INSERT")
    public R<Long> fromContract(@PathVariable Long contractId) {
        return R.ok("应收单已生成", receivableService.createFromContract(contractId));
    }

    @Operation(summary = "收款登记")
    @PutMapping("/{id}/receive")
    @PreAuthorize("@perm.has('crm:receivable:receive')")
    @OperLog(title = "收款登记", type = "UPDATE")
    public R<Void> receive(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal amount = body.get("amount") == null ? null : new BigDecimal(body.get("amount").toString());
        LocalDate date = body.get("receiveDate") == null || String.valueOf(body.get("receiveDate")).isBlank()
                ? LocalDate.now() : LocalDate.parse(String.valueOf(body.get("receiveDate")));
        receivableService.receive(id, amount, date);
        return R.ok("收款登记成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('crm:receivable:delete')")
    @OperLog(title = "应收管理", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        receivableService.delete(id);
        return R.ok();
    }
}
