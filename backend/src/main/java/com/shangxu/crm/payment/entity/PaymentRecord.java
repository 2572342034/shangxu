package com.shangxu.crm.payment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 收款单：资金流水的唯一记账入口。
 * 合同回款计划、订单应收单都不再直接累加实收金额，统一由收款单核销产生。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_payment_record")
public class PaymentRecord extends BaseEntity {

    private String recordNo;

    private Long customerId;

    private String customerName;

    /** 收款金额 */
    private BigDecimal amount;

    /** 已核销金额 */
    private BigDecimal allocatedAmount;

    /** 未核销金额（预收） */
    private BigDecimal unallocatedAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate receiveDate;

    /** BANK / CASH / ACCEPTANCE / ONLINE / OTHER */
    private String method;

    private String bankAccount;

    /** 银行流水号 / 凭证号 */
    private String voucherNo;

    /** MANUAL / ORDER / CONTRACT */
    private String sourceType;

    private Long sourceId;

    private String sourceNo;

    /** CONFIRMED / CANCELLED */
    private String status;

    private Long ownerUserId;

    private Long deptId;

    private String remark;

    @TableField(exist = false)
    private String ownerName;

    @TableField(exist = false)
    private List<PaymentAllocation> allocations;
}
