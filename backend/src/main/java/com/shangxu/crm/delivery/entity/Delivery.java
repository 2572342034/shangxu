package com.shangxu.crm.delivery.entity;

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
@TableName("crm_delivery")
public class Delivery extends BaseEntity {

    private String deliveryNo;
    private Long orderId;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private LocalDate deliveryDate;
    private String warehouse;
    private BigDecimal totalQty;
    private BigDecimal amount;
    /** DRAFT / SHIPPED / SIGNED / CANCELLED */
    private String status;
    private String logisticsCompany;
    private String logisticsNo;
    private String receiver;
    private String receiverPhone;
    private String address;
    private Long ownerUserId;
    private Long deptId;
    private String remark;

    @TableField(exist = false)
    private String ownerName;

    @TableField(exist = false)
    private List<DeliveryItem> items;
}
