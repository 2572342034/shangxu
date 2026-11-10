package com.shangxu.crm.order.entity;

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
@TableName("crm_order")
public class SalesOrder extends BaseEntity {

    private String orderNo;
    private String title;
    private Long customerId;
    private String customerName;
    private Long contractId;
    private String contractNo;
    private BigDecimal amount;
    /** DRAFT / PENDING / APPROVED / REJECTED / CANCELLED */
    private String status;
    private LocalDate orderDate;
    /** UNDELIVERED / PART / DELIVERED */
    private String deliveryStatus;
    /** UNPAID / PART / PAID */
    private String receivableStatus;
    private Long ownerUserId;
    private Long deptId;
    private String remark;

    @TableField(exist = false)
    private String ownerName;

    @TableField(exist = false)
    private List<OrderItem> items;

    /** 已生成的出库单数量（含草稿） */
    @TableField(exist = false)
    private Integer deliveryCount;
}
