package com.shangxu.crm.invoice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.contract.entity.Contract;
import com.shangxu.crm.contract.mapper.ContractMapper;
import com.shangxu.crm.invoice.entity.Invoice;
import com.shangxu.crm.invoice.mapper.InvoiceMapper;
import com.shangxu.crm.order.entity.SalesOrder;
import com.shangxu.crm.order.mapper.SalesOrderMapper;
import com.shangxu.crm.security.DataScopeHelper;
import com.shangxu.crm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceMapper invoiceMapper;
    private final SalesOrderMapper orderMapper;
    private final ContractMapper contractMapper;

    public PageResult<Invoice> page(PageQuery query, String status, String type) {
        Page<Invoice> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<Invoice> result = invoiceMapper.selectPageList(page, query.getKeyword(), status, type,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
        return PageResult.of(result);
    }

    public Invoice detail(Long id) {
        Invoice invoice = invoiceMapper.selectById(id);
        if (invoice == null) {
            throw new BizException("发票不存在");
        }
        return invoice;
    }

    public Long createFromOrder(Long orderId) {
        SalesOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (!"APPROVED".equals(order.getStatus())) {
            throw new BizException("只有已审核的订单才能申请开票");
        }
        Invoice invoice = new Invoice();
        invoice.setCustomerId(order.getCustomerId());
        invoice.setCustomerName(order.getCustomerName());
        invoice.setTitle(order.getCustomerName());
        invoice.setSourceType("ORDER");
        invoice.setSourceId(orderId);
        invoice.setSourceNo(order.getOrderNo());
        invoice.setAmount(order.getAmount());
        invoice.setType("SPECIAL");
        invoice.setOwnerUserId(order.getOwnerUserId());
        invoice.setDeptId(order.getDeptId());
        invoice.setRemark("由订单 " + order.getOrderNo() + " 生成");
        checkIssuable("ORDER", orderId, order.getAmount());
        return save(invoice);
    }

    public Long createFromContract(Long contractId) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BizException("合同不存在");
        }
        Invoice invoice = new Invoice();
        invoice.setCustomerId(contract.getCustomerId());
        invoice.setCustomerName(contract.getCustomerName());
        invoice.setTitle(contract.getCustomerName());
        invoice.setSourceType("CONTRACT");
        invoice.setSourceId(contractId);
        invoice.setSourceNo(contract.getContractNo());
        invoice.setAmount(contract.getAmount());
        invoice.setType("SPECIAL");
        invoice.setOwnerUserId(contract.getOwnerUserId());
        invoice.setDeptId(contract.getDeptId());
        invoice.setRemark("由合同 " + contract.getContractNo() + " 生成");
        checkIssuable("CONTRACT", contractId, contract.getAmount());
        return save(invoice);
    }

    public Long create(Invoice invoice) {
        if (invoice.getAmount() == null || invoice.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("开票金额必须大于 0");
        }
        if (invoice.getCustomerId() == null) {
            throw new BizException("请选择客户");
        }
        if (invoice.getTitle() == null || invoice.getTitle().isBlank()) {
            invoice.setTitle(invoice.getCustomerName());
        }
        if (invoice.getSourceType() == null) {
            invoice.setSourceType("ORDER");
        }
        if (invoice.getOwnerUserId() == null) {
            invoice.setOwnerUserId(SecurityUtils.getUserId());
            invoice.setDeptId(SecurityUtils.getDeptId());
        }
        return save(invoice);
    }

    /** 确认开票：录入发票号与开票日期 */
    public void issue(Long id, String invoiceNo, LocalDate invoiceDate) {
        Invoice invoice = invoiceMapper.selectById(id);
        if (invoice == null) {
            throw new BizException("发票不存在");
        }
        if (!"PENDING".equals(invoice.getStatus())) {
            throw new BizException("只有待开票的记录可以确认开票");
        }
        if (invoiceNo == null || invoiceNo.isBlank()) {
            throw new BizException("请填写发票号码");
        }
        Invoice update = new Invoice();
        update.setId(id);
        update.setInvoiceNo(invoiceNo.trim());
        update.setInvoiceDate(invoiceDate == null ? LocalDate.now() : invoiceDate);
        update.setStatus("ISSUED");
        invoiceMapper.updateById(update);
    }

    public void voidInvoice(Long id) {
        Invoice invoice = invoiceMapper.selectById(id);
        if (invoice == null) {
            throw new BizException("发票不存在");
        }
        if ("VOID".equals(invoice.getStatus())) {
            throw new BizException("该发票已作废");
        }
        Invoice update = new Invoice();
        update.setId(id);
        update.setStatus("VOID");
        invoiceMapper.updateById(update);
    }

    public void delete(Long id) {
        Invoice invoice = invoiceMapper.selectById(id);
        if (invoice == null) {
            return;
        }
        if ("ISSUED".equals(invoice.getStatus())) {
            throw new BizException("已开票的记录不能删除，请先作废");
        }
        invoiceMapper.deleteById(id);
    }

    /** 防超开：累计已开票（不含作废）+ 本次 ≤ 来源单金额 */
    private void checkIssuable(String sourceType, Long sourceId, BigDecimal baseAmount) {
        BigDecimal base = baseAmount == null ? BigDecimal.ZERO : baseAmount;
        BigDecimal issued = invoiceMapper.sumIssuedBySource(sourceType, sourceId);
        if (issued == null) {
            issued = BigDecimal.ZERO;
        }
        if (issued.compareTo(base) >= 0) {
            throw new BizException("该单据已开票 " + issued.toPlainString() + "，已达上限 "
                    + base.toPlainString() + "，不能重复开票");
        }
    }

    private Long save(Invoice invoice) {
        invoice.setId(null);
        if (invoice.getStatus() == null) {
            invoice.setStatus("PENDING");
        }
        if (invoice.getTaxRate() == null) {
            invoice.setTaxRate(new BigDecimal("6.00"));
        }
        if (invoice.getType() == null) {
            invoice.setType("SPECIAL");
        }
        invoice.setTaxAmount(calcTax(invoice.getAmount(), invoice.getTaxRate()));
        invoiceMapper.insert(invoice);
        return invoice.getId();
    }

    /** 价税合计倒算税额：amount / (1 + rate) * rate */
    private BigDecimal calcTax(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal divisor = BigDecimal.ONE.add(rate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        return amount.divide(divisor, 2, RoundingMode.HALF_UP)
                .multiply(rate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
