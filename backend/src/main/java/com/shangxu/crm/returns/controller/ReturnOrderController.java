package com.shangxu.crm.returns.controller;

import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.log.OperLog;
import com.shangxu.crm.returns.entity.ReturnOrder;
import com.shangxu.crm.returns.service.ReturnOrderService;
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

@Tag(name = "退货退款")
@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnOrderController {

    private final ReturnOrderService returnService;

    @GetMapping
    @PreAuthorize("@perm.has('crm:return:list')")
    public R<PageResult<ReturnOrder>> page(PageQuery query, @RequestParam(required = false) String status) {
        return R.ok(returnService.page(query, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('crm:return:list')")
    public R<ReturnOrder> detail(@PathVariable Long id) {
        return R.ok(returnService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('crm:return:add')")
    @OperLog(title = "退货退款", type = "INSERT")
    public R<Long> create(@RequestBody ReturnOrder returnOrder) {
        return R.ok("退货单已创建", returnService.create(returnOrder));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('crm:return:edit')")
    @OperLog(title = "退货退款", type = "UPDATE")
    public R<Void> update(@PathVariable Long id, @RequestBody ReturnOrder returnOrder) {
        returnOrder.setId(id);
        returnService.update(returnOrder);
        return R.ok();
    }

    @Operation(summary = "审核退货单")
    @PutMapping("/{id}/audit")
    @PreAuthorize("@perm.has('crm:return:audit')")
    @OperLog(title = "退货审核", type = "UPDATE")
    public R<Void> audit(@PathVariable Long id, @RequestParam String status) {
        returnService.audit(id, status);
        return R.ok("审核完成", null);
    }

    @Operation(summary = "确认退款")
    @PutMapping("/{id}/refund")
    @PreAuthorize("@perm.has('crm:return:audit')")
    @OperLog(title = "确认退款", type = "UPDATE")
    public R<Void> refund(@PathVariable Long id) {
        returnService.refund(id);
        return R.ok("已退款", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('crm:return:delete')")
    @OperLog(title = "退货退款", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        returnService.delete(id);
        return R.ok();
    }
}
