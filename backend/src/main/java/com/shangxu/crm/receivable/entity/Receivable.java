package com.shangxu.crm.receivable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_receivable")
public class Receivable extends BaseEntity {

    private String receivableNo;
    private Long customerId;
    private String customerName;
    /** ORDER / CONTRACT */
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private BigDecimal amount;
    private BigDecimal receivedAmount;

    /** 核销笔数 */
    private Integer allocatedCount;
    /** 最近收款时间 */
    private java.time.LocalDateTime receiveTime;
    private LocalDate dueDate;
    /** UNPAID / PART / PAID */
    private String status;

    /** 审批状态：PENDING 待确认 / APPROVED 已确认 / REJECTED 已驳回 */
    private String auditStatus;
    private Long ownerUserId;
    private Long deptId;
    private String remark;

    @TableField(exist = false)
    private String ownerName;

    /** 逾期天数，未逾期为 0 */
    @TableField(exist = false)
    private Integer overdueDays;
}
