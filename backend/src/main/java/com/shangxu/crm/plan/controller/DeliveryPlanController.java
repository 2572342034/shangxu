package com.shangxu.crm.plan.controller;

import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.log.OperLog;
import com.shangxu.crm.plan.entity.DeliveryPlan;
import com.shangxu.crm.plan.service.DeliveryPlanService;
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

@Tag(name = "发货计划")
@RestController
@RequestMapping("/api/delivery-plans")
@RequiredArgsConstructor
public class DeliveryPlanController {

    private final DeliveryPlanService planService;

    @GetMapping
    @PreAuthorize("@perm.has('crm:plan:list')")
    public R<PageResult<DeliveryPlan>> page(PageQuery query,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) Long orderId) {
        return R.ok(planService.page(query, status, orderId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('crm:plan:list')")
    public R<DeliveryPlan> detail(@PathVariable Long id) {
        return R.ok(planService.detail(id));
    }

    @Operation(summary = "由订单创建发货计划")
    @PostMapping("/from-order/{orderId}")
    @PreAuthorize("@perm.has('crm:plan:add')")
    @OperLog(title = "发货计划", type = "INSERT")
    public R<Long> fromOrder(@PathVariable Long orderId,
                             @RequestParam(required = false) String planDate,
                             @RequestParam(required = false) String warehouse,
                             @RequestParam(required = false) String remark) {
        LocalDate date = planDate == null || planDate.isBlank() ? null : LocalDate.parse(planDate);
        return R.ok("发货计划已创建", planService.createFromOrder(orderId, date, warehouse, remark));
    }

    @PostMapping
    @PreAuthorize("@perm.has('crm:plan:add')")
    @OperLog(title = "发货计划", type = "INSERT")
    public R<Long> create(@RequestBody DeliveryPlan plan) {
        return R.ok("创建成功", planService.create(plan));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('crm:plan:edit')")
    @OperLog(title = "发货计划", type = "UPDATE")
    public R<Void> update(@PathVariable Long id, @RequestBody DeliveryPlan plan) {
        plan.setId(id);
        planService.update(plan);
        return R.ok();
    }

    @Operation(summary = "按计划生成出库单")
    @PutMapping("/{id}/generate-delivery")
    @PreAuthorize("@perm.has('crm:plan:ship')")
    @OperLog(title = "计划发货", type = "UPDATE")
    public R<Long> generateDelivery(@PathVariable Long id) {
        return R.ok("出库单已生成", planService.generateDelivery(id));
    }

    @Operation(summary = "取消计划")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("@perm.has('crm:plan:edit')")
    @OperLog(title = "发货计划", type = "UPDATE")
    public R<Void> cancel(@PathVariable Long id) {
        planService.cancel(id);
        return R.ok("已取消", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('crm:plan:delete')")
    @OperLog(title = "发货计划", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return R.ok();
    }
}
