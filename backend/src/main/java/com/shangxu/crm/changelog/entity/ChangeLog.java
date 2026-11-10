package com.shangxu.crm.changelog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/** 通用业务变更轨迹：一条记录 = 一个字段的一次变更 */
@Data
@TableName("sys_change_log")
public class ChangeLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String action;
    private String fieldKey;
    private String fieldLabel;
    private String oldValue;
    private String newValue;
    private Long oldOwnerId;
    private Long operatorId;
    private String operatorName;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
