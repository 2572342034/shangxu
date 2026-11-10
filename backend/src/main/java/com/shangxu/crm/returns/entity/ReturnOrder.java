package com.shangxu.crm.returns.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_return")
public class ReturnOrder extends BaseEntity {

    private String returnNo;
    private Long orderId;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private LocalDate returnDate;
    private BigDecimal returnQty;
    private BigDecimal returnAmount;
    private BigDecimal refundAmount;
    private String reason;
    /** PENDING / APPROVED / REJECTED / REFUNDED */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime refundTime;

    private Long ownerUserId;
    private Long deptId;
    private String remark;

    @TableField(exist = false)
    private String ownerName;
}
