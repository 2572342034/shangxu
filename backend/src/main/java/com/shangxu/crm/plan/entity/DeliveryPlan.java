package com.shangxu.crm.plan.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_delivery_plan")
public class DeliveryPlan extends BaseEntity {

    private String planNo;
    private Long orderId;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private LocalDate planDate;
    private BigDecimal planQty;
    private BigDecimal amount;
    private String warehouse;
    /** PENDING / SHIPPED / CANCELLED */
    private String status;
    private Long deliveryId;
    private Long ownerUserId;
    private Long deptId;
    private String remark;

    @TableField(exist = false)
    private String ownerName;
}
