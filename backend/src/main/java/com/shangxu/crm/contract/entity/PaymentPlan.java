package com.shangxu.crm.contract.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_payment_plan")
public class PaymentPlan extends BaseEntity {

    private Long contractId;
    private Integer period;
    private BigDecimal planAmount;
    private LocalDate planDate;
    private BigDecimal actualAmount;
    private LocalDate actualDate;
    /** UNPAID / PAID */
    private String status;
    private String remark;

    @TableField(exist = false)
    private String contractName;

    @TableField(exist = false)
    private String customerName;
}
