package com.shangxu.crm.lead.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_lead")
public class Lead extends BaseEntity {

    private String leadNo;
    private String name;
    private String company;
    private String contactName;
    private String mobile;
    private String email;
    private String source;
    private String industry;
    /** NEW / FOLLOWING / QUALIFIED / INVALID / CONVERTED */
    private String status;
    private Long ownerUserId;
    private Long deptId;
    private Long convertedCustomerId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime convertedTime;

    private String remark;

    @TableField(exist = false)
    private String ownerName;
}
