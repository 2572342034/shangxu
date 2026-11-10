package com.shangxu.crm.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("crm_delivery_item")
public class DeliveryItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deliveryId;
    private String productName;
    private String unit;
    private BigDecimal quantity;
    private String remark;
}
