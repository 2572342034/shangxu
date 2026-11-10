package com.shangxu.crm.delivery.controller;

import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.delivery.entity.Delivery;
import com.shangxu.crm.delivery.service.DeliveryService;
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

import java.util.List;

@Tag(name = "出库管理")
@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping
    @PreAuthorize("@perm.has('crm:delivery:list')")
    public R<PageResult<Delivery>> page(PageQuery query,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) Long orderId) {
        return R.ok(deliveryService.page(query, status, orderId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('crm:delivery:list')")
    public R<Delivery> detail(@PathVariable Long id) {
        return R.ok(deliveryService.detail(id));
    }

    @GetMapping("/by-order/{orderId}")
    @PreAuthorize("@perm.has('crm:delivery:list')")
    public R<List<Delivery>> byOrder(@PathVariable Long orderId) {
        return R.ok(deliveryService.listByOrder(orderId));
    }

    @Operation(summary = "由订单生成出库单")
    @PostMapping("/from-order/{orderId}")
    @PreAuthorize("@perm.has('crm:delivery:add')")
    @OperLog(title = "出库管理", type = "INSERT")
    public R<Long> createFromOrder(@PathVariable Long orderId) {
        return R.ok("出库单已生成", deliveryService.createFromOrder(orderId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('crm:delivery:edit')")
    @OperLog(title = "出库管理", type = "UPDATE")
    public R<Void> update(@PathVariable Long id, @RequestBody Delivery delivery) {
        delivery.setId(id);
        deliveryService.update(delivery);
        return R.ok();
    }

    @Operation(summary = "出库单审批（提交审核 / 通过 / 驳回）")
    @PutMapping("/{id}/status")
    @PreAuthorize("@perm.has('crm:delivery:edit')")
    @OperLog(title = "出库审批", type = "UPDATE")
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        deliveryService.changeStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "确认出库")
    @PutMapping("/{id}/ship")
    @PreAuthorize("@perm.has('crm:delivery:ship')")
    @OperLog(title = "确认出库", type = "UPDATE")
    public R<Void> ship(@PathVariable Long id) {
        deliveryService.ship(id);
        return R.ok("已确认出库", null);
    }

    @Operation(summary = "客户签收")
    @PutMapping("/{id}/sign")
    @PreAuthorize("@perm.has('crm:delivery:ship')")
    @OperLog(title = "出库签收", type = "UPDATE")
    public R<Void> sign(@PathVariable Long id) {
        deliveryService.sign(id);
        return R.ok("已确认签收", null);
    }

    @Operation(summary = "作废出库单")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("@perm.has('crm:delivery:edit')")
    @OperLog(title = "作废出库单", type = "UPDATE")
    public R<Void> cancel(@PathVariable Long id) {
        deliveryService.cancel(id);
        return R.ok("已作废", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('crm:delivery:delete')")
    @OperLog(title = "出库管理", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        deliveryService.delete(id);
        return R.ok();
    }
}
