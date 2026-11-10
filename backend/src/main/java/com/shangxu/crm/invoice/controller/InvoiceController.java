package com.shangxu.crm.invoice.controller;

import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.invoice.entity.Invoice;
import com.shangxu.crm.invoice.service.InvoiceService;
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

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "开票管理")
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @PreAuthorize("@perm.has('crm:invoice:list')")
    public R<PageResult<Invoice>> page(PageQuery query,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) String type) {
        return R.ok(invoiceService.page(query, status, type));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('crm:invoice:list')")
    public R<Invoice> detail(@PathVariable Long id) {
        return R.ok(invoiceService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('crm:invoice:add')")
    @OperLog(title = "开票管理", type = "INSERT")
    public R<Long> create(@RequestBody Invoice invoice) {
        return R.ok("开票申请已创建", invoiceService.create(invoice));
    }

    @Operation(summary = "由订单申请开票")
    @PostMapping("/from-order/{orderId}")
    @PreAuthorize("@perm.has('crm:invoice:add')")
    @OperLog(title = "开票管理", type = "INSERT")
    public R<Long> fromOrder(@PathVariable Long orderId) {
        return R.ok("开票申请已创建", invoiceService.createFromOrder(orderId));
    }

    @Operation(summary = "由合同申请开票")
    @PostMapping("/from-contract/{contractId}")
    @PreAuthorize("@perm.has('crm:invoice:add')")
    @OperLog(title = "开票管理", type = "INSERT")
    public R<Long> fromContract(@PathVariable Long contractId) {
        return R.ok("开票申请已创建", invoiceService.createFromContract(contractId));
    }

    @Operation(summary = "确认开票")
    @PutMapping("/{id}/issue")
    @PreAuthorize("@perm.has('crm:invoice:issue')")
    @OperLog(title = "确认开票", type = "UPDATE")
    public R<Void> issue(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String date = body.get("invoiceDate");
        invoiceService.issue(id, body.get("invoiceNo"),
                date == null || date.isBlank() ? null : LocalDate.parse(date));
        return R.ok("已确认开票", null);
    }

    @Operation(summary = "作废发票")
    @PutMapping("/{id}/void")
    @PreAuthorize("@perm.has('crm:invoice:issue')")
    @OperLog(title = "作废发票", type = "UPDATE")
    public R<Void> voidInvoice(@PathVariable Long id) {
        invoiceService.voidInvoice(id);
        return R.ok("已作废", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('crm:invoice:delete')")
    @OperLog(title = "开票管理", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return R.ok();
    }
}
