package com.shangxu.crm.receivable.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.contract.entity.Contract;
import com.shangxu.crm.contract.mapper.ContractMapper;
import com.shangxu.crm.order.entity.SalesOrder;
import com.shangxu.crm.order.mapper.SalesOrderMapper;
import com.shangxu.crm.order.service.OrderService;
import com.shangxu.crm.receivable.entity.Receivable;
import com.shangxu.crm.receivable.mapper.ReceivableMapper;
import com.shangxu.crm.payment.dto.PaymentCreateRequest;
import com.shangxu.crm.payment.service.PaymentService;
import com.shangxu.crm.security.DataScopeHelper;
import com.shangxu.crm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReceivableService {

    private final ReceivableMapper receivableMapper;
    private final SalesOrderMapper orderMapper;
    private final ContractMapper contractMapper;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final com.shangxu.crm.common.sequence.SequenceService sequenceService;

    public PageResult<Receivable> page(PageQuery query, String status, Boolean overdue, Long customerId) {
        Page<Receivable> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<Receivable> result = receivableMapper.selectPageList(page, query.getKeyword(), status, overdue, customerId,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
        return PageResult.of(result);
    }

    public Map<String, Object> summary() {
        Long owner = DataScopeHelper.scopeOwnerId();
        Long dept = DataScopeHelper.scopeDeptId();
        BigDecimal total = receivableMapper.sumAmount(owner, dept);
        BigDecimal received = receivableMapper.sumReceived(owner, dept);
        BigDecimal overdue = receivableMapper.sumOverdue(owner, dept);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalAmount", total);
        result.put("receivedAmount", received);
        result.put("unpaidAmount", total.subtract(received));
        result.put("overdueAmount", overdue);
        result.put("overdueCount", receivableMapper.countOverdue(owner, dept));
        return result;
    }

    public List<Receivable> pending() {
        return receivableMapper.selectPendingList(DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
    }

    /** 由订单生成应收单 */
    @Transactional(rollbackFor = Exception.class)
    public Long createFromOrder(Long orderId) {
        SalesOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (!"APPROVED".equals(order.getStatus())) {
            throw new BizException("只有已审核的订单才能生成应收单");
        }
        if (receivableMapper.countBySource("ORDER", orderId) > 0) {
            throw new BizException("该订单已生成应收单");
        }
        Receivable receivable = new Receivable();
        receivable.setCustomerId(order.getCustomerId());
        receivable.setCustomerName(order.getCustomerName());
        receivable.setSourceType("ORDER");
        receivable.setSourceId(orderId);
        receivable.setSourceNo(order.getOrderNo());
        receivable.setAmount(order.getAmount());
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setDueDate(order.getOrderDate() == null ? LocalDate.now().plusDays(30)
                : order.getOrderDate().plusDays(30));
        receivable.setOwnerUserId(order.getOwnerUserId());
        receivable.setDeptId(order.getDeptId());
        receivable.setRemark("由订单 " + order.getOrderNo() + " 生成");
        save(receivable);

        orderService.refreshReceivableStatus(orderId, BigDecimal.ZERO, order.getAmount());
        return receivable.getId();
    }

    /** 由合同生成应收单 */
    @Transactional(rollbackFor = Exception.class)
    public Long createFromContract(Long contractId) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BizException("合同不存在");
        }
        if (receivableMapper.countBySource("CONTRACT", contractId) > 0) {
            throw new BizException("该合同已生成应收单");
        }
        Receivable receivable = new Receivable();
        receivable.setCustomerId(contract.getCustomerId());
        receivable.setCustomerName(contract.getCustomerName());
        receivable.setSourceType("CONTRACT");
        receivable.setSourceId(contractId);
        receivable.setSourceNo(contract.getContractNo());
        receivable.setAmount(contract.getAmount());
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setDueDate(contract.getSignDate() == null ? LocalDate.now().plusDays(30)
                : contract.getSignDate().plusDays(30));
        receivable.setOwnerUserId(contract.getOwnerUserId());
        receivable.setDeptId(contract.getDeptId());
        receivable.setRemark("由合同 " + contract.getContractNo() + " 生成");
        save(receivable);
        return receivable.getId();
    }

    public Long create(Receivable receivable) {
        if (receivable.getCustomerId() == null) {
            throw new BizException("请选择客户");
        }
        if (receivable.getAmount() == null || receivable.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("应收金额必须大于 0");
        }
        if (receivable.getReceivedAmount() == null) {
            receivable.setReceivedAmount(BigDecimal.ZERO);
        }
        if (receivable.getDueDate() == null) {
            receivable.setDueDate(LocalDate.now().plusDays(30));
        }
        if (receivable.getOwnerUserId() == null) {
            receivable.setOwnerUserId(SecurityUtils.getUserId());
            receivable.setDeptId(SecurityUtils.getDeptId());
        }
        receivable.setSourceType(receivable.getSourceType() == null ? "ORDER" : receivable.getSourceType());
        receivable.setStatus(calcStatus(receivable.getReceivedAmount(), receivable.getAmount()));
        save(receivable);
        return receivable.getId();
    }

    /**
     * 收款登记：统一生成收款单并核销，应收余额、订单/合同状态由核销结果推导。
     * 保留此接口是为了兼容旧调用方，实际记账逻辑在 PaymentService。
     */
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long id, BigDecimal amount, LocalDate receiveDate) {
        Receivable receivable = receivableMapper.selectById(id);
        if (receivable == null) {
            throw new BizException("应收单不存在");
        }
        if ("PAID".equals(receivable.getStatus())) {
            throw new BizException("该应收单已结清");
        }
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setCustomerId(receivable.getCustomerId());
        request.setCustomerName(receivable.getCustomerName());
        request.setAmount(amount);
        request.setReceiveDate(receiveDate);
        request.setMethod("BANK");
        request.setSourceType(receivable.getSourceType());
        request.setSourceId(receivable.getSourceId());
        request.setSourceNo(receivable.getSourceNo());
        request.setRemark("应收单 " + receivable.getReceivableNo() + " 收款");

        PaymentCreateRequest.AllocationItem item = new PaymentCreateRequest.AllocationItem();
        item.setReceivableId(id);
        item.setAmount(amount);
        request.setAllocations(List.of(item));
        paymentService.create(request);
    }

    public void delete(Long id) {
        receivableMapper.deleteById(id);
    }

    private void save(Receivable receivable) {
        receivable.setId(null);
        if (receivable.getAuditStatus() == null) {
            receivable.setAuditStatus("PENDING");
        }
        receivable.setReceivableNo(sequenceService.nextNo("receivable", "AR"));
        if (receivable.getStatus() == null) {
            receivable.setStatus("UNPAID");
        }
        receivableMapper.insert(receivable);
    }

    /** 应收确认：待确认 -> 已确认 / 已驳回；已确认后才能登记收款 */
    public void audit(Long id, String status) {
        Receivable receivable = receivableMapper.selectById(id);
        if (receivable == null) {
            throw new BizException("应收单不存在");
        }
        if (!"PENDING".equals(receivable.getAuditStatus())) {
            throw new BizException("该应收单已确认或已驳回，不能重复操作");
        }
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new BizException("无效的确认状态");
        }
        Receivable update = new Receivable();
        update.setId(id);
        update.setAuditStatus(status);
        receivableMapper.updateById(update);
    }

    private String calcStatus(BigDecimal received, BigDecimal total) {
        if (received == null || received.compareTo(BigDecimal.ZERO) <= 0) {
            return "UNPAID";
        }
        return total != null && received.compareTo(total) >= 0 ? "PAID" : "PART";
    }
}
