package com.shangxu.crm.payment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.sequence.SequenceService;
import com.shangxu.crm.contract.entity.Contract;
import com.shangxu.crm.contract.mapper.ContractMapper;
import com.shangxu.crm.order.service.OrderService;
import com.shangxu.crm.payment.dto.PaymentCreateRequest;
import com.shangxu.crm.payment.entity.PaymentAllocation;
import com.shangxu.crm.payment.entity.PaymentRecord;
import com.shangxu.crm.payment.mapper.PaymentAllocationMapper;
import com.shangxu.crm.payment.mapper.PaymentRecordMapper;
import com.shangxu.crm.receivable.entity.Receivable;
import com.shangxu.crm.receivable.mapper.ReceivableMapper;
import com.shangxu.crm.security.DataScopeHelper;
import com.shangxu.crm.security.SecurityUtils;
import com.shangxu.crm.system.entity.SysUser;
import com.shangxu.crm.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 收款与核销。
 * <p>
 * 记账规则：
 * <ul>
 *   <li>收款单是唯一资金入口，金额、日期、凭证可追溯；</li>
 *   <li>核销明细把收款金额分配到具体应收单，一张收款单可冲多张应收；</li>
 *   <li>未核销部分保留为预收款，后续可再次核销；</li>
 *   <li>应收余额、订单收款状态、合同回款状态均由核销结果推导，不再手工累加。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRecordMapper recordMapper;
    private final PaymentAllocationMapper allocationMapper;
    private final ReceivableMapper receivableMapper;
    private final ContractMapper contractMapper;
    private final SysUserMapper userMapper;
    private final OrderService orderService;
    private final SequenceService sequenceService;
    private final com.shangxu.crm.order.mapper.SalesOrderMapper salesOrderMapper;

    public PageResult<PaymentRecord> page(PageQuery query, String method, String status, Long customerId,
                                          LocalDate startDate, LocalDate endDate) {
        Page<PaymentRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<PaymentRecord> result = recordMapper.selectPageList(page, query.getKeyword(), method, status,
                customerId, startDate, endDate, DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
        return PageResult.of(result);
    }

    public Map<String, Object> detail(Long id) {
        PaymentRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BizException("收款单不存在");
        }
        if (record.getOwnerUserId() != null) {
            SysUser owner = userMapper.selectById(record.getOwnerUserId());
            record.setOwnerName(owner == null ? null : owner.getNickname());
        }
        List<PaymentAllocation> allocations = allocationMapper.selectByRecordId(id);
        record.setAllocations(allocations);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("record", record);
        result.put("allocations", allocations);
        return result;
    }

    public Map<String, Object> summary() {
        Long owner = DataScopeHelper.scopeOwnerId();
        Long dept = DataScopeHelper.scopeDeptId();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalReceived", recordMapper.sumConfirmed(owner, dept));
        result.put("unallocated", recordMapper.sumUnallocated(owner, dept));
        result.put("unallocatedCount", recordMapper.countUnallocated(owner, dept));
        return result;
    }

    /** 可核销的应收单（未结清），用于收款弹窗选择 */
    public List<Receivable> openReceivables(Long customerId) {
        Page<Receivable> page = new Page<>(1, 200);
        return receivableMapper.selectPageList(page, null, null, null, customerId,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId()).getRecords().stream()
                .filter(r -> !"PAID".equals(r.getStatus()))
                .toList();
    }

    /**
     * 登记收款并核销。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(PaymentCreateRequest req) {
        if (req.getCustomerId() == null) {
            throw new BizException("请选择客户");
        }
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("收款金额必须大于 0");
        }

        List<PaymentCreateRequest.AllocationItem> items = req.getAllocations() == null
                ? List.of() : req.getAllocations();
        BigDecimal allocated = BigDecimal.ZERO;
        List<Receivable> locked = new ArrayList<>();
        for (PaymentCreateRequest.AllocationItem item : items) {
            if (item.getReceivableId() == null) {
                throw new BizException("核销明细缺少应收单");
            }
            if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("核销金额必须大于 0");
            }
            Receivable receivable = receivableMapper.selectById(item.getReceivableId());
            if (receivable == null) {
                throw new BizException("应收单不存在：" + item.getReceivableId());
            }
            if (!req.getCustomerId().equals(receivable.getCustomerId())) {
                throw new BizException("不能跨客户核销：" + receivable.getReceivableNo());
            }
            if (!"APPROVED".equals(receivable.getAuditStatus())) {
                throw new BizException("应收单 " + receivable.getReceivableNo() + " 尚未确认，不能核销");
            }
            if ("PAID".equals(receivable.getStatus())) {
                throw new BizException("应收单已结清：" + receivable.getReceivableNo());
            }
            BigDecimal remaining = nz(receivable.getAmount()).subtract(nz(receivable.getReceivedAmount()));
            if (item.getAmount().compareTo(remaining) > 0) {
                throw new BizException("核销金额超过应收余额：" + receivable.getReceivableNo()
                        + "，余额 " + remaining.toPlainString());
            }
            for (Receivable exist : locked) {
                if (exist.getId().equals(receivable.getId())) {
                    throw new BizException("同一应收单不能重复核销：" + receivable.getReceivableNo());
                }
            }
            locked.add(receivable);
            allocated = allocated.add(item.getAmount());
        }
        if (allocated.compareTo(req.getAmount()) > 0) {
            throw new BizException("核销总额不能超过收款金额");
        }

        PaymentRecord record = new PaymentRecord();
        record.setRecordNo(sequenceService.nextNo("payment", "SR"));
        record.setCustomerId(req.getCustomerId());
        record.setCustomerName(req.getCustomerName() == null ? locked.isEmpty()
                ? "未知客户" : locked.get(0).getCustomerName() : req.getCustomerName());
        record.setAmount(req.getAmount());
        record.setAllocatedAmount(allocated);
        record.setUnallocatedAmount(req.getAmount().subtract(allocated));
        record.setReceiveDate(req.getReceiveDate() == null ? LocalDate.now() : req.getReceiveDate());
        record.setMethod(req.getMethod() == null ? "BANK" : req.getMethod());
        record.setBankAccount(req.getBankAccount());
        record.setVoucherNo(req.getVoucherNo());
        record.setSourceType(req.getSourceType() == null ? "MANUAL" : req.getSourceType());
        record.setSourceId(req.getSourceId());
        record.setSourceNo(req.getSourceNo());
        record.setStatus("CONFIRMED");
        record.setOwnerUserId(SecurityUtils.getUserId());
        record.setDeptId(SecurityUtils.getDeptId());
        record.setRemark(req.getRemark());
        recordMapper.insert(record);

        for (int i = 0; i < items.size(); i++) {
            applyAllocation(record, locked.get(i), items.get(i).getAmount());
        }
        return record.getId();
    }

    /**
     * 把一笔已登记的收款（预收余额）继续核销到应收单。
     */
    @Transactional(rollbackFor = Exception.class)
    public void allocate(Long recordId, Long receivableId, BigDecimal amount) {
        PaymentRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BizException("收款单不存在");
        }
        if ("CANCELLED".equals(record.getStatus())) {
            throw new BizException("已作废的收款单不能核销");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("核销金额必须大于 0");
        }
        BigDecimal unallocated = nz(record.getUnallocatedAmount());
        if (amount.compareTo(unallocated) > 0) {
            throw new BizException("核销金额超过预收余额 " + unallocated.toPlainString());
        }
        Receivable receivable = receivableMapper.selectById(receivableId);
        if (receivable == null) {
            throw new BizException("应收单不存在");
        }
        if (!record.getCustomerId().equals(receivable.getCustomerId())) {
            throw new BizException("不能跨客户核销");
        }
        if ("PAID".equals(receivable.getStatus())) {
            throw new BizException("应收单已结清");
        }
        BigDecimal remaining = nz(receivable.getAmount()).subtract(nz(receivable.getReceivedAmount()));
        if (amount.compareTo(remaining) > 0) {
            throw new BizException("核销金额超过应收余额 " + remaining.toPlainString());
        }

        applyAllocation(record, receivable, amount);

        PaymentRecord update = new PaymentRecord();
        update.setId(recordId);
        update.setVersion(record.getVersion());
        update.setAllocatedAmount(nz(record.getAllocatedAmount()).add(amount));
        update.setUnallocatedAmount(unallocated.subtract(amount));
        if (recordMapper.updateById(update) == 0) {
            throw new BizException("收款单已被他人修改，请刷新后重试");
        }
    }

    /**
     * 作废收款单：逐条反向冲销，应收余额回到核销前。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        PaymentRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BizException("收款单不存在");
        }
        if ("CANCELLED".equals(record.getStatus())) {
            throw new BizException("收款单已作废");
        }
        List<PaymentAllocation> allocations = allocationMapper.selectByRecordId(id);
        for (PaymentAllocation allocation : allocations) {
            Receivable receivable = receivableMapper.selectById(allocation.getReceivableId());
            if (receivable == null) {
                continue;
            }
            BigDecimal received = nz(receivable.getReceivedAmount()).subtract(nz(allocation.getAmount()));
            if (received.compareTo(BigDecimal.ZERO) < 0) {
                received = BigDecimal.ZERO;
            }
            Receivable update = new Receivable();
            update.setId(receivable.getId());
            update.setVersion(receivable.getVersion());
            update.setReceivedAmount(received);
            update.setStatus(calcStatus(received, receivable.getAmount()));
            update.setAllocatedCount(Math.max(0, (receivable.getAllocatedCount() == null ? 0
                    : receivable.getAllocatedCount()) - 1));
            if (receivableMapper.updateById(update) == 0) {
                throw new BizException("应收单 " + receivable.getReceivableNo() + " 已被他人修改，请刷新后重试");
            }
            refreshSource(receivable, received);
            allocationMapper.deleteById(allocation.getId());
        }
        PaymentRecord update = new PaymentRecord();
        update.setId(id);
        update.setVersion(record.getVersion());
        update.setStatus("CANCELLED");
        update.setAllocatedAmount(BigDecimal.ZERO);
        update.setUnallocatedAmount(BigDecimal.ZERO);
        if (recordMapper.updateById(update) == 0) {
            throw new BizException("收款单已被他人修改，请刷新后重试");
        }
    }

    /** 写一条核销明细并回写应收余额 */
    private void applyAllocation(PaymentRecord record, Receivable receivable, BigDecimal amount) {
        PaymentAllocation allocation = new PaymentAllocation();
        allocation.setRecordId(record.getId());
        allocation.setReceivableId(receivable.getId());
        allocation.setReceivableNo(receivable.getReceivableNo());
        allocation.setAmount(amount);
        allocationMapper.insert(allocation);

        BigDecimal received = nz(receivable.getReceivedAmount()).add(amount);
        Receivable update = new Receivable();
        update.setId(receivable.getId());
        update.setVersion(receivable.getVersion());
        update.setReceivedAmount(received);
        update.setReceiveTime(record.getReceiveDate().atTime(LocalDateTime.now().toLocalTime()));
        update.setStatus(calcStatus(received, receivable.getAmount()));
        update.setAllocatedCount((receivable.getAllocatedCount() == null ? 0 : receivable.getAllocatedCount()) + 1);
        if (receivableMapper.updateById(update) == 0) {
            throw new BizException("应收单 " + receivable.getReceivableNo() + " 已被他人修改，请刷新后重试");
        }
        refreshSource(receivable, received);
    }

    /** 回写来源单据状态：订单收款状态 / 合同回款状态 */
    private void refreshSource(Receivable receivable, BigDecimal received) {
        if ("ORDER".equals(receivable.getSourceType()) && receivable.getSourceId() != null) {
            orderService.refreshReceivableStatus(receivable.getSourceId(), received, receivable.getAmount());
            // 订单若挂在合同下，同步刷新合同回款状态，否则合同会一直停在「未回款」
            com.shangxu.crm.order.entity.SalesOrder order = salesOrderMapper.selectById(receivable.getSourceId());
            if (order != null && order.getContractId() != null) {
                refreshContract(order.getContractId());
            }
            return;
        }
        if ("CONTRACT".equals(receivable.getSourceType()) && receivable.getSourceId() != null) {
            refreshContract(receivable.getSourceId());
        }
    }

    /** 按合同下全部应收单重算回款状态 */
    public void refreshContract(Long contractId) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            return;
        }
        BigDecimal received = nz(receivableMapper.sumReceivedByContract(contractId));
        BigDecimal total = nz(contract.getAmount());
        Contract update = new Contract();
        update.setId(contractId);
        update.setVersion(contract.getVersion());
        if (received.compareTo(BigDecimal.ZERO) <= 0) {
            update.setPaymentStatus("UNPAID");
        } else if (total.compareTo(BigDecimal.ZERO) > 0 && received.compareTo(total) >= 0) {
            update.setPaymentStatus("PAID");
        } else {
            update.setPaymentStatus("PART");
        }
        contractMapper.updateById(update);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 核销状态推导：0 未收、等于应收已收、否则部分收款。包级可见以便单测。 */
    static String calcStatus(BigDecimal received, BigDecimal total) {
        if (received == null || received.compareTo(BigDecimal.ZERO) <= 0) {
            return "UNPAID";
        }
        return total != null && received.compareTo(total) >= 0 ? "PAID" : "PART";
    }
}
