package com.shangxu.crm.activity.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_activity")
public class Activity extends BaseEntity {

    /** CUSTOMER / LEAD / OPPORTUNITY / CONTRACT */
    private String bizType;
    private Long bizId;
    private String bizName;
    /** CALL / VISIT / WECHAT / EMAIL / MEETING / OTHER */
    private String type;
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextFollowTime;

    private Long ownerUserId;

    @TableField(exist = false)
    private String ownerName;
}
