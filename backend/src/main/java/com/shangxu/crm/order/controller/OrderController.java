package com.shangxu.crm.order.controller;

import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.log.OperLog;
import com.shangxu.crm.order.entity.SalesOrder;
import com.shangxu.crm.order.service.OrderService;
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

@Tag(name = "订单管理")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("@perm.has('crm:order:list')")
    public R<PageResult<SalesOrder>> page(PageQuery query,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String deliveryStatus,
                                          @RequestParam(required = false) Long customerId) {
        return R.ok(orderService.page(query, status, deliveryStatus, customerId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('crm:order:list')")
    public R<SalesOrder> detail(@PathVariable Long id) {
        return R.ok(orderService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('crm:order:add')")
    @OperLog(title = "订单管理", type = "INSERT")
    public R<Long> create(@RequestBody SalesOrder order) {
        return R.ok("订单创建成功", orderService.create(order));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('crm:order:edit')")
    @OperLog(title = "订单管理", type = "UPDATE")
    public R<Void> update(@PathVariable Long id, @RequestBody SalesOrder order) {
        order.setId(id);
        orderService.update(order);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('crm:order:delete')")
    @OperLog(title = "订单管理", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return R.ok();
    }

    @Operation(summary = "订单状态流转")
    @PutMapping("/{id}/status")
    @PreAuthorize("@perm.has('crm:order:audit')")
    @OperLog(title = "订单审批", type = "UPDATE")
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        orderService.changeStatus(id, status);
        return R.ok("状态已更新", null);
    }
}
