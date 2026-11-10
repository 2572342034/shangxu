package com.shangxu.crm.opportunity.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_opportunity")
public class Opportunity extends BaseEntity {

    private String oppNo;
    private String name;
    private Long customerId;
    private String customerName;
    private BigDecimal amount;
    /** CONTACT / DEMAND / QUOTE / NEGOTIATION / WON / LOST */
    private String stage;
    private Integer winRate;
    private LocalDate expectDealDate;
    private LocalDate actualDealDate;
    /** OPEN / WON / LOST */
    private String status;
    private String source;
    private Long ownerUserId;
    private Long deptId;
    private String remark;

    @TableField(exist = false)
    private String ownerName;

    @TableField(exist = false)
    private Integer activityCount;
}
