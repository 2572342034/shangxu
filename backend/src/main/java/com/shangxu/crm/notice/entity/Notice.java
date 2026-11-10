package com.shangxu.crm.notice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class Notice extends BaseEntity {

    private Long userId;

    private String title;

    private String content;

    /** SYSTEM / RECEIVABLE / FOLLOWUP / APPROVAL / POOL */
    private String type;

    private String bizType;

    private Long bizId;

    private String bizNo;

    private Integer readFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;
}
