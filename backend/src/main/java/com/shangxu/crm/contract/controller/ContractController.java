package com.shangxu.crm.contract.controller;

import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.contract.entity.Contract;
import com.shangxu.crm.contract.entity.PaymentPlan;
import com.shangxu.crm.contract.service.ContractService;
import com.shangxu.crm.log.OperLog;
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

@Tag(name = "合同管理")
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @GetMapping
    @PreAuthorize("@perm.has('crm:contract:list')")
    public R<PageResult<Contract>> page(PageQuery query,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String paymentStatus,
                                        @RequestParam(required = false) Long customerId) {
        return R.ok(contractService.page(query, status, paymentStatus, customerId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('crm:contract:list')")
    public R<Contract> detail(@PathVariable Long id) {
        return R.ok(contractService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('crm:contract:add')")
    @OperLog(title = "合同管理", type = "INSERT")
    public R<Long> create(@RequestBody Contract contract) {
        return R.ok("合同创建成功", contractService.create(contract));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('crm:contract:edit')")
    @OperLog(title = "合同管理", type = "UPDATE")
    public R<Void> update(@PathVariable Long id, @RequestBody Contract contract) {
        contract.setId(id);
        contractService.update(contract);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('crm:contract:delete')")
    @OperLog(title = "合同管理", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return R.ok();
    }

    @Operation(summary = "合同审批/状态流转")
    @PutMapping("/{id}/status")
    @PreAuthorize("@perm.has('crm:contract:audit')")
    @OperLog(title = "合同审批", type = "UPDATE")
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        contractService.changeStatus(id, status);
        return R.ok("状态已更新", null);
    }

    @Operation(summary = "可下单合同（按客户，含剩余额度）")
    @GetMapping("/selectable")
    @PreAuthorize("@perm.has('crm:order:add') or @perm.has('crm:order:edit')")
    public R<List<Map<String, Object>>> selectable(@RequestParam Long customerId) {
        return R.ok(contractService.selectable(customerId));
    }

    @Operation(summary = "回款计划列表")
    @GetMapping("/{id}/payments")
    @PreAuthorize("@perm.has('crm:payment:list')")
    public R<List<PaymentPlan>> payments(@PathVariable Long id) {
        return R.ok(contractService.payments(id));
    }

    @Operation(summary = "新增回款计划")
    @PostMapping("/{id}/payments")
    @PreAuthorize("@perm.has('crm:payment:add')")
    @OperLog(title = "回款计划", type = "INSERT")
    public R<Long> addPayment(@PathVariable Long id, @RequestBody PaymentPlan plan) {
        return R.ok("新增成功", contractService.addPayment(id, plan));
    }

    @Operation(summary = "回款登记")
    @PutMapping("/payments/{paymentId}/receive")
    @PreAuthorize("@perm.has('crm:payment:add')")
    @OperLog(title = "回款登记", type = "UPDATE")
    public R<Void> receive(@PathVariable Long paymentId, @RequestBody Map<String, Object> body) {
        BigDecimal amount = body.get("amount") == null ? null : new BigDecimal(body.get("amount").toString());
        LocalDate date = body.get("actualDate") == null || String.valueOf(body.get("actualDate")).isBlank()
                ? LocalDate.now() : LocalDate.parse(String.valueOf(body.get("actualDate")));
        contractService.receive(paymentId, amount, date);
        return R.ok("回款登记成功", null);
    }
}
