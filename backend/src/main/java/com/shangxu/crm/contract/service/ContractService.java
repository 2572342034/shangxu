package com.shangxu.crm.contract.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.contract.entity.Contract;
import com.shangxu.crm.contract.entity.ContractItem;
import com.shangxu.crm.contract.entity.PaymentPlan;
import com.shangxu.crm.contract.mapper.ContractMapper;
import com.shangxu.crm.contract.mapper.PaymentPlanMapper;
import com.shangxu.crm.payment.dto.PaymentCreateRequest;
import com.shangxu.crm.payment.service.PaymentService;
import com.shangxu.crm.receivable.entity.Receivable;
import com.shangxu.crm.receivable.mapper.ReceivableMapper;
import com.shangxu.crm.receivable.service.ReceivableService;
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
public class ContractService {

    private final ContractMapper contractMapper;
    private final com.shangxu.crm.order.mapper.SalesOrderMapper salesOrderMapper;
    private final com.shangxu.crm.contract.mapper.ContractItemMapper contractItemMapper;
    private final PaymentPlanMapper paymentPlanMapper;
    private final com.shangxu.crm.system.mapper.SysUserMapper userMapper;
    private final ReceivableMapper receivableMapper;
    private final ReceivableService receivableService;
    private final PaymentService paymentService;
    private final com.shangxu.crm.common.sequence.SequenceService sequenceService;

    public PageResult<Contract> page(PageQuery query, String status, String paymentStatus, Long customerId) {
        Page<Contract> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<Contract> result = contractMapper.selectPageList(page, query.getKeyword(), status, paymentStatus, customerId,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
        return PageResult.of(result);
    }

    public List<Contract> listByCustomer(Long customerId) {
        Page<Contract> page = new Page<>(1, 100);
        return contractMapper.selectPageList(page, null, null, null, customerId,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId()).getRecords();
    }

    public Contract detail(Long id) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BizException("合同不存在");
        }
        if (contract.getOwnerUserId() != null) {
            com.shangxu.crm.system.entity.SysUser owner = userMapper.selectById(contract.getOwnerUserId());
            contract.setOwnerName(owner == null ? null : owner.getNickname());
        }
        List<PaymentPlan> plans = paymentPlanMapper.selectByContractId(id);
        contract.setPaymentPlans(plans);
        // 实收以「应收单核销结果」为准，回款计划只表达计划，不参与记账
        BigDecimal received = receivableMapper.sumReceivedByContract(id);
        contract.setReceivedAmount(received == null ? BigDecimal.ZERO : received);

        // 下单额度：合同金额 - 该合同下未取消订单合计
        BigDecimal ordered = salesOrderMapper.sumAmountByContract(id, null);
        if (ordered == null) {
            ordered = BigDecimal.ZERO;
        }
        BigDecimal amount = contract.getAmount() == null ? BigDecimal.ZERO : contract.getAmount();
        contract.setOrderedAmount(ordered);
        contract.setOrderCount((int) salesOrderMapper.countByContract(id));
        contract.setItems(contractItemMapper.selectByContractId(id));
        contract.setRemainAmount(amount.subtract(ordered));
        return contract;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(Contract contract) {
        if (contract.getCustomerId() == null) {
            throw new BizException("请选择关联客户");
        }
        contract.setId(null);
        contract.setContractNo(sequenceService.nextNo("contract", "HT"));
        if (contract.getOwnerUserId() == null) {
            contract.setOwnerUserId(SecurityUtils.getUserId());
            contract.setDeptId(SecurityUtils.getDeptId());
        }
        if (contract.getStatus() == null) {
            contract.setStatus("DRAFT");
        }
        contract.setPaymentStatus("UNPAID");
        java.math.BigDecimal manualAmount = contract.getAmount();
        contractMapper.insert(contract);

        // 有明细时合同金额以明细合计为准；没有明细（框架合同）保留手填金额
        java.math.BigDecimal total = saveItems(contract.getId(), contract.getItems(), manualAmount);
        if (contract.getAmount() == null || contract.getAmount().compareTo(total) != 0) {
            Contract amountUpdate = new Contract();
            amountUpdate.setId(contract.getId());
            amountUpdate.setAmount(total);
            contractMapper.updateById(amountUpdate);
            contract.setAmount(total);
        }

        // 默认按 5:5 生成两期回款计划
        if (contract.getAmount() != null && contract.getAmount().compareTo(BigDecimal.ZERO) > 0
                && (contract.getPaymentPlans() == null || contract.getPaymentPlans().isEmpty())) {
            BigDecimal half = contract.getAmount().divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
            LocalDate signDate = contract.getSignDate() == null ? LocalDate.now() : contract.getSignDate();
            PaymentPlan first = new PaymentPlan();
            first.setContractId(contract.getId());
            first.setPeriod(1);
            first.setPlanAmount(half);
            first.setPlanDate(signDate.plusDays(15));
            first.setActualAmount(BigDecimal.ZERO);
            first.setStatus("UNPAID");
            first.setRemark("首付款 50%");
            paymentPlanMapper.insert(first);

            PaymentPlan second = new PaymentPlan();
            second.setContractId(contract.getId());
            second.setPeriod(2);
            second.setPlanAmount(contract.getAmount().subtract(half));
            second.setPlanDate(signDate.plusMonths(3));
            second.setActualAmount(BigDecimal.ZERO);
            second.setStatus("UNPAID");
            second.setRemark("尾款 50%");
            paymentPlanMapper.insert(second);
        }
        return contract.getId();
    }

    /**
     * 编辑合同。明细采用「先删后插」，必须放在同一事务里：
     * 否则逐条插入时若某行校验失败，旧明细已删、新明细不完整，合同会落成残缺状态。
     */
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void update(Contract contract) {
        contract.setContractNo(null);
        if (contract.getItems() != null) {
            BigDecimal total = saveItems(contract.getId(), contract.getItems(), contract.getAmount());
            contract.setAmount(total);
        }
        contractMapper.updateById(contract);
    }

    /** 保存明细并返回合计；items 为 null 表示不动明细 */
    private BigDecimal saveItems(Long contractId, List<ContractItem> items, BigDecimal fallback) {
        if (items == null) {
            return fallback == null ? BigDecimal.ZERO : fallback;
        }
        contractItemMapper.deleteByContractId(contractId);
        if (items.isEmpty()) {
            return fallback == null ? BigDecimal.ZERO : fallback;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (ContractItem item : items) {
            if (item.getProductName() == null || item.getProductName().isBlank()) {
                throw new BizException("合同明细的产品名称不能为空");
            }
            BigDecimal qty = item.getQuantity() == null ? BigDecimal.ONE : item.getQuantity();
            BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("合同明细「" + item.getProductName() + "」数量必须大于 0");
            }
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("合同明细「" + item.getProductName() + "」单价不能为负");
            }
            item.setId(null);
            item.setContractId(contractId);
            item.setQuantity(qty);
            item.setPrice(price);
            item.setAmount(qty.multiply(price).setScale(2, java.math.RoundingMode.HALF_UP));
            contractItemMapper.insert(item);
            total = total.add(item.getAmount());
        }
        return total;
    }

    public void delete(Long id) {
        contractMapper.deleteById(id);
    }

    /** 合同状态流转：DRAFT -> PENDING -> APPROVED/REJECTED -> SIGNED */
    public void changeStatus(Long id, String status) {
        List<String> allowed = List.of("DRAFT", "PENDING", "APPROVED", "REJECTED", "SIGNED", "VOID");
        if (!allowed.contains(status)) {
            throw new BizException("无效的合同状态");
        }
        Contract contract = new Contract();
        contract.setId(id);
        contract.setStatus(status);
        if ("SIGNED".equals(status)) {
            contract.setSignDate(LocalDate.now());
        }
        contractMapper.updateById(contract);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long addPayment(Long contractId, PaymentPlan plan) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BizException("合同不存在");
        }
        plan.setId(null);
        plan.setContractId(contractId);
        if (plan.getActualAmount() == null) {
            plan.setActualAmount(BigDecimal.ZERO);
        }
        if (plan.getStatus() == null) {
            plan.setStatus("UNPAID");
        }
        paymentPlanMapper.insert(plan);
        return plan.getId();
    }

    /**
     * 回款登记（兼容旧入口）：转为「收款单 + 核销应收单」。
     * 若该合同还没有应收单，先按合同金额生成一张，再核销。
     */
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long paymentId, BigDecimal amount, LocalDate actualDate) {
        PaymentPlan plan = paymentPlanMapper.selectById(paymentId);
        if (plan == null) {
            throw new BizException("回款计划不存在");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("回款金额必须大于 0");
        }
        Long contractId = plan.getContractId();
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BizException("合同不存在");
        }
        List<Receivable> open = receivableMapper.selectOpenByContract(contractId);
        if (open.isEmpty()) {
            receivableService.createFromContract(contractId);
            open = receivableMapper.selectOpenByContract(contractId);
        }
        if (open.isEmpty()) {
            throw new BizException("该合同应收已全部结清");
        }

        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setCustomerId(contract.getCustomerId());
        request.setCustomerName(contract.getCustomerName());
        request.setAmount(amount);
        request.setReceiveDate(actualDate == null ? LocalDate.now() : actualDate);
        request.setMethod("BANK");
        request.setSourceType("CONTRACT");
        request.setSourceId(contractId);
        request.setSourceNo(contract.getContractNo());
        request.setRemark("合同 " + contract.getContractNo() + " 回款（计划第 " + plan.getPeriod() + " 期）");

        BigDecimal left = amount;
        List<PaymentCreateRequest.AllocationItem> items = new java.util.ArrayList<>();
        for (Receivable receivable : open) {
            if (left.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal remaining = receivable.getAmount()
                    .subtract(receivable.getReceivedAmount() == null ? BigDecimal.ZERO : receivable.getReceivedAmount());
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal alloc = remaining.min(left);
            PaymentCreateRequest.AllocationItem item = new PaymentCreateRequest.AllocationItem();
            item.setReceivableId(receivable.getId());
            item.setAmount(alloc);
            items.add(item);
            left = left.subtract(alloc);
        }
        if (left.compareTo(BigDecimal.ZERO) > 0) {
            throw new BizException("回款金额超过该合同应收余额，超出 " + left.toPlainString());
        }
        request.setAllocations(items);
        paymentService.create(request);
    }

    /** 该客户的全部合同 + 是否可下单 + 不可下单原因（下拉里置灰展示，避免"找不到合同"） */
    public List<java.util.Map<String, Object>> selectable(Long customerId) {
        List<Contract> list = contractMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract>()
                        .eq(Contract::getCustomerId, customerId)
                        .orderByDesc(Contract::getId));
        List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (Contract c : list) {
            BigDecimal amount = c.getAmount() == null ? BigDecimal.ZERO : c.getAmount();
            BigDecimal ordered = salesOrderMapper.sumAmountByContract(c.getId(), null);
            if (ordered == null) {
                ordered = BigDecimal.ZERO;
            }
            BigDecimal remain = amount.subtract(ordered);
            boolean statusOk = "APPROVED".equals(c.getStatus()) || "SIGNED".equals(c.getStatus());
            boolean amountOk = amount.compareTo(BigDecimal.ZERO) <= 0
                    || remain.compareTo(BigDecimal.ZERO) > 0;
            String reason = null;
            if (!statusOk) {
                reason = "合同" + contractStatusText(c.getStatus()) + "，需先审批或签订";
            } else if (!amountOk) {
                reason = "已超额 " + remain.negate().toPlainString();
            }
            java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("contractNo", c.getContractNo());
            item.put("name", c.getName());
            item.put("amount", amount);
            item.put("orderedAmount", ordered);
            item.put("remainAmount", remain);
            item.put("status", c.getStatus());
            item.put("selectable", statusOk && amountOk);
            item.put("reason", reason);
            result.add(item);
        }
        return result;
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

    public List<PaymentPlan> payments(Long contractId) {
        return paymentPlanMapper.selectByContractId(contractId);
    }

    /** 待回款计划（首页待办） */
    public List<PaymentPlan> pendingPayments() {
        return paymentPlanMapper.selectPending(DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
    }
}
