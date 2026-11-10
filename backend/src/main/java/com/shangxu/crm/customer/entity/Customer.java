package com.shangxu.crm.customer.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_customer")
public class Customer extends BaseEntity {

    private String customerNo;
    private String name;
    private String shortName;
    private String industry;
    /** A / B / C */
    private String level;
    private String source;
    private String scale;
    private String phone;
    private String website;
    private String province;
    private String city;
    private String address;
    private Long ownerUserId;
    private Long deptId;
    /** PRIVATE 私海 / PUBLIC 公海 */
    private String poolType;
    /** FOLLOWING 跟进中 / DEAL 已成交 / INVALID 无效 */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextFollowTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastFollowTime;

    private String remark;

    @TableField(exist = false)
    private String ownerName;

    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private Integer contactCount;

    @TableField(exist = false)
    private Integer opportunityCount;

    /** 只读视图标记：历史负责人只能看摘要 */
    @TableField(exist = false)
    private Boolean readOnly;

    @TableField(exist = false)
    private Boolean masked;
}
