package com.shangxu.crm.plan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.delivery.service.DeliveryService;
import com.shangxu.crm.order.entity.OrderItem;
import com.shangxu.crm.order.entity.SalesOrder;
import com.shangxu.crm.order.mapper.OrderItemMapper;
import com.shangxu.crm.order.mapper.SalesOrderMapper;
import com.shangxu.crm.plan.entity.DeliveryPlan;
import com.shangxu.crm.plan.mapper.DeliveryPlanMapper;
import com.shangxu.crm.security.DataScopeHelper;
import com.shangxu.crm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryPlanService {
    private final com.shangxu.crm.common.sequence.SequenceService sequenceService;


    private final DeliveryPlanMapper planMapper;
    private final SalesOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final DeliveryService deliveryService;
    private final com.shangxu.crm.delivery.mapper.DeliveryMapper deliveryMapper;

    public PageResult<DeliveryPlan> page(PageQuery query, String status, Long orderId) {
        Page<DeliveryPlan> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<DeliveryPlan> result = planMapper.selectPageList(page, query.getKeyword(), status, orderId,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
        return PageResult.of(result);
    }

    public DeliveryPlan detail(Long id) {
        DeliveryPlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BizException("发货计划不存在");
        }
        return plan;
    }

    /** 由订单创建发货计划 */
    @Transactional(rollbackFor = Exception.class)
    public Long createFromOrder(Long orderId, LocalDate planDate, String warehouse, String remark) {
        SalesOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (!"APPROVED".equals(order.getStatus())) {
            throw new BizException("只有已审核的订单才能安排发货");
        }
        if (planMapper.countPendingByOrder(orderId) > 0) {
            throw new BizException("该订单已有待发货计划");
        }
        if (deliveryMapper.countActiveByOrder(orderId) > 0) {
            throw new BizException("该订单已生成出库单，无需再安排发货计划");
        }
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        BigDecimal totalQty = items.stream()
                .map(item -> item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DeliveryPlan plan = new DeliveryPlan();
        plan.setPlanNo(sequenceService.nextNo("plan", "DP"));
        plan.setOrderId(orderId);
        plan.setOrderNo(order.getOrderNo());
        plan.setCustomerId(order.getCustomerId());
        plan.setCustomerName(order.getCustomerName());
        plan.setPlanDate(planDate == null ? LocalDate.now().plusDays(3) : planDate);
        plan.setPlanQty(totalQty);
        plan.setAmount(order.getAmount());
        plan.setWarehouse(warehouse == null || warehouse.isBlank() ? "主仓" : warehouse);
        plan.setStatus("PENDING");
        plan.setOwnerUserId(order.getOwnerUserId());
        plan.setDeptId(order.getDeptId());
        plan.setRemark(remark);
        planMapper.insert(plan);
        return plan.getId();
    }

    public void update(DeliveryPlan plan) {
        DeliveryPlan exist = planMapper.selectById(plan.getId());
        if (exist == null) {
            throw new BizException("发货计划不存在");
        }
        if (!"PENDING".equals(exist.getStatus())) {
            throw new BizException("只有待发货的计划可以修改");
        }
        plan.setPlanNo(null);
        plan.setOrderId(null);
        planMapper.updateById(plan);
    }

    /** 按计划生成出库单 */
    @Transactional(rollbackFor = Exception.class)
    public Long generateDelivery(Long id) {
        DeliveryPlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BizException("发货计划不存在");
        }
        if (!"PENDING".equals(plan.getStatus())) {
            throw new BizException("该计划已处理，不能重复发货");
        }
        Long deliveryId = deliveryService.createFromOrder(plan.getOrderId());
        DeliveryPlan update = new DeliveryPlan();
        update.setId(id);
        update.setStatus("SHIPPED");
        update.setDeliveryId(deliveryId);
        planMapper.updateById(update);
        return deliveryId;
    }

    public void cancel(Long id) {
        DeliveryPlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BizException("发货计划不存在");
        }
        if (!"PENDING".equals(plan.getStatus())) {
            throw new BizException("只有待发货的计划可以取消");
        }
        DeliveryPlan update = new DeliveryPlan();
        update.setId(id);
        update.setStatus("CANCELLED");
        planMapper.updateById(update);
    }

    public void delete(Long id) {
        DeliveryPlan plan = planMapper.selectById(id);
        if (plan == null) {
            return;
        }
        if ("SHIPPED".equals(plan.getStatus())) {
            throw new BizException("已发货的计划不能删除");
        }
        planMapper.deleteById(id);
    }

    /** 手工创建（不关联订单） */
    public Long create(DeliveryPlan plan) {
        plan.setId(null);
        plan.setPlanNo(sequenceService.nextNo("plan", "DP"));
        if (plan.getOwnerUserId() == null) {
            plan.setOwnerUserId(SecurityUtils.getUserId());
            plan.setDeptId(SecurityUtils.getDeptId());
        }
        if (plan.getStatus() == null) {
            plan.setStatus("PENDING");
        }
        if (plan.getPlanDate() == null) {
            plan.setPlanDate(LocalDate.now().plusDays(3));
        }
        planMapper.insert(plan);
        return plan.getId();
    }
}
