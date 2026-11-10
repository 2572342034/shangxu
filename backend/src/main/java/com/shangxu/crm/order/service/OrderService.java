package com.shangxu.crm.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.order.entity.OrderItem;
import com.shangxu.crm.order.entity.SalesOrder;
import com.shangxu.crm.order.mapper.OrderItemMapper;
import com.shangxu.crm.order.mapper.SalesOrderMapper;
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
public class OrderService {
    private final com.shangxu.crm.common.sequence.SequenceService sequenceService;


    private final SalesOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final com.shangxu.crm.system.mapper.SysUserMapper userMapper;
    private final com.shangxu.crm.contract.mapper.ContractMapper contractMapper;

    public PageResult<SalesOrder> page(PageQuery query, String status, String deliveryStatus, Long customerId) {
        Page<SalesOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<SalesOrder> result = orderMapper.selectPageList(page, query.getKeyword(), status, deliveryStatus, customerId,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
        return PageResult.of(result);
    }

    public List<SalesOrder> listByCustomer(Long customerId) {
        Page<SalesOrder> page = new Page<>(1, 100);
        return orderMapper.selectPageList(page, null, null, null, customerId,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId()).getRecords();
    }

    public SalesOrder detail(Long id) {
        SalesOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        order.setItems(orderItemMapper.selectByOrderId(id));
        if (order.getOwnerUserId() != null) {
            com.shangxu.crm.system.entity.SysUser owner = userMapper.selectById(order.getOwnerUserId());
            order.setOwnerName(owner == null ? null : owner.getNickname());
        }
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(SalesOrder order) {
        if (order.getCustomerId() == null) {
            throw new BizException("请选择关联客户");
        }
        order.setId(null);
        order.setOrderNo(sequenceService.nextNo("order", "SO"));
        if (order.getOwnerUserId() == null) {
            order.setOwnerUserId(SecurityUtils.getUserId());
            order.setDeptId(SecurityUtils.getDeptId());
        }
        if (order.getStatus() == null) {
            order.setStatus("DRAFT");
        }
        order.setDeliveryStatus("UNDELIVERED");
        order.setReceivableStatus("UNPAID");
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDate.now());
        }
        order.setAmount(calcAmount(order.getItems()));
        bindContract(order, order.getAmount(), null);
        orderMapper.insert(order);
        saveItems(order.getId(), order.getItems());
        return order.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SalesOrder order) {
        SalesOrder exist = orderMapper.selectById(order.getId());
        if (exist == null) {
            throw new BizException("订单不存在");
        }
        if (!"DRAFT".equals(exist.getStatus()) && !"REJECTED".equals(exist.getStatus())) {
            throw new BizException("只有草稿或已驳回的订单可以修改");
        }
        order.setOrderNo(null);
        // 状态只能通过状态流转接口变更，编辑表单不得覆盖
        order.setStatus(null);
        order.setDeliveryStatus(null);
        order.setReceivableStatus(null);
        if (order.getItems() != null) {
            order.setAmount(calcAmount(order.getItems()));
        }
        // 前端未传合同时沿用原关联，避免编辑把关联清空
        if (order.getContractId() == null && (order.getContractNo() == null || order.getContractNo().isBlank())) {
            order.setContractId(exist.getContractId());
            order.setContractNo(exist.getContractNo());
        }
        BigDecimal checkAmount = order.getAmount() != null ? order.getAmount() : exist.getAmount();
        bindContract(order, checkAmount, order.getId());
        orderMapper.updateById(order);
        if (order.getItems() != null) {
            orderItemMapper.deleteByOrderId(order.getId());
            saveItems(order.getId(), order.getItems());
        }
    }

    public void delete(Long id) {
        SalesOrder order = orderMapper.selectById(id);
        if (order == null) {
            return;
        }
        if ("APPROVED".equals(order.getStatus())) {
            throw new BizException("已审核的订单不能删除，请先取消");
        }
        orderMapper.deleteById(id);
        orderItemMapper.deleteByOrderId(id);
    }

    /**
     * 合同关联与额度校验：
     * 1) 只传合同号时反查合同 ID，保证两个字段一致（修复历史只存单号的问题）
     * 2) 合同必须是已审批/已签订
     * 3) 订单客户必须与合同客户一致
     * 4) 订单金额不得超过「合同金额 - 该合同下其他订单合计」
     * 不关联合同的订单（小额直单）保持允许。
     */
    private void bindContract(SalesOrder order, BigDecimal orderAmount, Long excludeOrderId) {
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("订单金额必须大于 0，请先添加订单明细");
        }
        com.shangxu.crm.contract.entity.Contract contract = null;
        if (order.getContractId() != null) {
            contract = contractMapper.selectById(order.getContractId());
            if (contract == null) {
                throw new BizException("关联的合同不存在");
            }
        } else if (order.getContractNo() != null && !order.getContractNo().isBlank()) {
            contract = contractMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                            com.shangxu.crm.contract.entity.Contract>()
                            .eq(com.shangxu.crm.contract.entity.Contract::getContractNo,
                                    order.getContractNo().trim())
                            .last("LIMIT 1"));
            if (contract == null) {
                throw new BizException("合同号不存在：" + order.getContractNo());
            }
            order.setContractId(contract.getId());
        }
        if (contract == null) {
            return;
        }

        order.setContractNo(contract.getContractNo());
        if (!"APPROVED".equals(contract.getStatus()) && !"SIGNED".equals(contract.getStatus())) {
            throw new BizException("合同 " + contract.getContractNo() + " 当前状态为「"
                    + contractStatusText(contract.getStatus()) + "」，只有已审批或已签订的合同才能下单");
        }
        if (!contract.getCustomerId().equals(order.getCustomerId())) {
            throw new BizException("订单客户与合同客户不一致：合同属于「" + contract.getCustomerName() + "」");
        }

        BigDecimal contractAmount = contract.getAmount() == null ? BigDecimal.ZERO : contract.getAmount();
        if (contractAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal ordered = orderMapper.sumAmountByContract(contract.getId(), excludeOrderId);
        if (ordered == null) {
            ordered = BigDecimal.ZERO;
        }
        BigDecimal remain = contractAmount.subtract(ordered);
        if (orderAmount.compareTo(remain) > 0) {
            String detail = remain.compareTo(BigDecimal.ZERO) < 0
                    ? "（合同金额 " + contractAmount.toPlainString() + "，已下单 " + ordered.toPlainString()
                      + "，已超额 " + remain.negate().toPlainString() + "）"
                    : "（合同金额 " + contractAmount.toPlainString() + "，已下单 " + ordered.toPlainString()
                      + "，剩余 " + remain.toPlainString() + "）";
            throw new BizException("订单金额 " + orderAmount.toPlainString()
                    + " 超出合同剩余可下单金额 " + detail);
        }
    }

    private String contractStatusText(String status) {
        return switch (status == null ? "" : status) {
            case "DRAFT" -> "草稿";
            case "PENDING" -> "审批中";
            case "APPROVED" -> "已审批";
            case "REJECTED" -> "已驳回";
            case "SIGNED" -> "已签订";
            case "VOID" -> "已作废";
            default -> status;
        };
    }

    /** 订单状态流转：DRAFT → PENDING → APPROVED / REJECTED，APPROVED → CANCELLED */
    public void changeStatus(Long id, String status) {
        SalesOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        String current = order.getStatus();
        boolean allowed = switch (status) {
            case "PENDING" -> "DRAFT".equals(current) || "REJECTED".equals(current);
            case "APPROVED", "REJECTED" -> "PENDING".equals(current);
            case "CANCELLED" -> !"CANCELLED".equals(current);
            default -> false;
        };
        if (!allowed) {
            throw new BizException("当前状态不允许该操作");
        }
        SalesOrder update = new SalesOrder();
        update.setId(id);
        update.setStatus(status);
        orderMapper.updateById(update);
    }

    /** 出库后刷新订单出库状态 */
    public void refreshDeliveryStatus(Long orderId, BigDecimal shippedQty) {
        BigDecimal orderedQty = orderItemMapper.sumQuantity(orderId);
        String status;
        if (shippedQty == null || shippedQty.compareTo(BigDecimal.ZERO) <= 0) {
            status = "UNDELIVERED";
        } else if (orderedQty != null && shippedQty.compareTo(orderedQty) < 0) {
            status = "PART";
        } else {
            status = "DELIVERED";
        }
        SalesOrder update = new SalesOrder();
        update.setId(orderId);
        update.setDeliveryStatus(status);
        orderMapper.updateById(update);
    }

    /** 应收变化后刷新订单收款状态 */
    public void refreshReceivableStatus(Long orderId, BigDecimal received, BigDecimal total) {
        String status;
        if (received == null || received.compareTo(BigDecimal.ZERO) <= 0) {
            status = "UNPAID";
        } else if (total != null && received.compareTo(total) >= 0) {
            status = "PAID";
        } else {
            status = "PART";
        }
        SalesOrder update = new SalesOrder();
        update.setId(orderId);
        update.setReceivableStatus(status);
        orderMapper.updateById(update);
    }

    private BigDecimal calcAmount(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(item -> {
                    BigDecimal qty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
                    BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
                    BigDecimal amount = qty.multiply(price);
                    item.setAmount(amount);
                    return amount;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void saveItems(Long orderId, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (OrderItem item : items) {
            item.setId(null);
            item.setOrderId(orderId);
            if (item.getAmount() == null) {
                BigDecimal qty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
                BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
                item.setAmount(qty.multiply(price));
            }
            orderItemMapper.insert(item);
        }
    }
}
