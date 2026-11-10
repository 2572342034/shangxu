package com.shangxu.crm.contract.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_contract")
public class Contract extends BaseEntity {

    private String contractNo;
    private String name;
    private Long customerId;
    private String customerName;
    private Long opportunityId;
    private BigDecimal amount;
    private LocalDate signDate;
    private LocalDate startDate;
    private LocalDate endDate;
    /** DRAFT / PENDING / APPROVED / REJECTED / SIGNED / VOID */
    private String status;
    /** UNPAID / PART / PAID */
    private String paymentStatus;
    private Long ownerUserId;
    private Long deptId;
    private String remark;

    @TableField(exist = false)
    private String ownerName;

    @TableField(exist = false)
    private BigDecimal receivedAmount;

    @TableField(exist = false)
    private List<PaymentPlan> paymentPlans;

    /** 已下单金额（该合同下未取消订单合计） */
    @TableField(exist = false)
    private BigDecimal orderedAmount;

    /** 剩余可下单金额 = 合同金额 - 已下单 */
    @TableField(exist = false)
    private BigDecimal remainAmount;

    @TableField(exist = false)
    private Integer orderCount;

    /** 合同明细（标的行），金额 = 明细合计 */
    @TableField(exist = false)
    private List<ContractItem> items;
}
