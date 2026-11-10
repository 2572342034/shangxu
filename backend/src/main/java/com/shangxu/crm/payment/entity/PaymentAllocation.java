package com.shangxu.crm.payment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 核销明细：一笔收款冲销某张应收单的金额。
 * 一张收款单可拆成多条核销明细，实现「一笔款冲多张单」。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_payment_allocation")
public class PaymentAllocation extends BaseEntity {

    private Long recordId;

    private Long receivableId;

    private String receivableNo;

    private BigDecimal amount;

    /** 应收单应收总额（展示用） */
    @TableField(exist = false)
    private BigDecimal receivableAmount;

    /** 应收单已收金额（展示用） */
    @TableField(exist = false)
    private BigDecimal receivableReceived;
}
