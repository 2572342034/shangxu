package com.shangxu.crm.invoice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_invoice")
public class Invoice extends BaseEntity {

    private String invoiceNo;
    private String title;
    private String taxNo;
    private Long customerId;
    private String customerName;
    /** ORDER / CONTRACT */
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    /** 价税合计 */
    private BigDecimal amount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    /** SPECIAL / NORMAL / E_INVOICE */
    private String type;
    /** PENDING / ISSUED / VOID */
    private String status;
    private LocalDate invoiceDate;
    private Long ownerUserId;
    private Long deptId;
    private String remark;

    @TableField(exist = false)
    private String ownerName;
}
