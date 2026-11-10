package com.shangxu.crm.returns.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.order.entity.SalesOrder;
import com.shangxu.crm.order.mapper.SalesOrderMapper;
import com.shangxu.crm.returns.entity.ReturnOrder;
import com.shangxu.crm.returns.mapper.ReturnOrderMapper;
import com.shangxu.crm.security.DataScopeHelper;
import com.shangxu.crm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;

@Service
@RequiredArgsConstructor
public class ReturnOrderService {
    private final com.shangxu.crm.common.sequence.SequenceService sequenceService;


    private final ReturnOrderMapper returnMapper;
    private final SalesOrderMapper orderMapper;

    public PageResult<ReturnOrder> page(PageQuery query, String status) {
        Page<ReturnOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<ReturnOrder> result = returnMapper.selectPageList(page, query.getKeyword(), status,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
        return PageResult.of(result);
    }

    public ReturnOrder detail(Long id) {
        ReturnOrder order = returnMapper.selectById(id);
        if (order == null) {
            throw new BizException("退货单不存在");
        }
        return order;
    }

    public Long create(ReturnOrder returnOrder) {
        if (returnOrder.getCustomerId() == null) {
            throw new BizException("请选择客户");
        }
        if (returnOrder.getReturnAmount() == null || returnOrder.getReturnAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("退货金额必须大于 0");
        }
        if (returnOrder.getRefundAmount() == null) {
            returnOrder.setRefundAmount(BigDecimal.ZERO);
        }
        if (returnOrder.getRefundAmount().compareTo(returnOrder.getReturnAmount()) > 0) {
            throw new BizException("退款金额不能大于退货金额");
        }
        if (returnOrder.getOrderId() != null) {
            SalesOrder order = orderMapper.selectById(returnOrder.getOrderId());
            if (order == null) {
                throw new BizException("关联订单不存在");
            }
            if (returnOrder.getCustomerId() == null) {
                returnOrder.setCustomerId(order.getCustomerId());
            }
            returnOrder.setOrderNo(order.getOrderNo());
            returnOrder.setCustomerName(order.getCustomerName());
            if (returnOrder.getOwnerUserId() == null) {
                returnOrder.setOwnerUserId(order.getOwnerUserId());
                returnOrder.setDeptId(order.getDeptId());
            }
        }
        if (returnOrder.getOwnerUserId() == null) {
            returnOrder.setOwnerUserId(SecurityUtils.getUserId());
            returnOrder.setDeptId(SecurityUtils.getDeptId());
        }
        returnOrder.setId(null);
        returnOrder.setReturnNo(sequenceService.nextNo("return", "RT"));
        returnOrder.setStatus("PENDING");
        if (returnOrder.getReturnDate() == null) {
            returnOrder.setReturnDate(LocalDate.now());
        }
        if (returnOrder.getReturnQty() == null) {
            returnOrder.setReturnQty(BigDecimal.ZERO);
        }
        returnMapper.insert(returnOrder);
        return returnOrder.getId();
    }

    public void update(ReturnOrder returnOrder) {
        ReturnOrder exist = returnMapper.selectById(returnOrder.getId());
        if (exist == null) {
            throw new BizException("退货单不存在");
        }
        if (!"PENDING".equals(exist.getStatus())) {
            throw new BizException("只有待审核的退货单可以修改");
        }
        returnOrder.setReturnNo(null);
        returnMapper.updateById(returnOrder);
    }

    /** 审核：通过或驳回 */
    public void audit(Long id, String status) {
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new BizException("无效的审核结果");
        }
        ReturnOrder order = returnMapper.selectById(id);
        if (order == null) {
            throw new BizException("退货单不存在");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BizException("该退货单已审核");
        }
        ReturnOrder update = new ReturnOrder();
        update.setId(id);
        update.setStatus(status);
        returnMapper.updateById(update);
    }

    /** 确认退款 */
    public void refund(Long id) {
        ReturnOrder order = returnMapper.selectById(id);
        if (order == null) {
            throw new BizException("退货单不存在");
        }
        if (!"APPROVED".equals(order.getStatus())) {
            throw new BizException("只有审核通过的退货单可以退款");
        }
        ReturnOrder update = new ReturnOrder();
        update.setId(id);
        update.setStatus("REFUNDED");
        update.setRefundTime(LocalDateTime.now());
        if (order.getRefundAmount() == null || order.getRefundAmount().compareTo(BigDecimal.ZERO) == 0) {
            update.setRefundAmount(order.getReturnAmount());
        }
        returnMapper.updateById(update);
    }

    public void delete(Long id) {
        ReturnOrder order = returnMapper.selectById(id);
        if (order == null) {
            return;
        }
        if ("REFUNDED".equals(order.getStatus())) {
            throw new BizException("已退款的单据不能删除");
        }
        returnMapper.deleteById(id);
    }
}
