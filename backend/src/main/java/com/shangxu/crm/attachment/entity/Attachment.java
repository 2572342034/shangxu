package com.shangxu.crm.attachment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_attachment")
public class Attachment extends BaseEntity {

    /** CUSTOMER / CONTACT / LEAD / OPPORTUNITY / CONTRACT / ORDER / INVOICE / RETURN / DELIVERY */
    private String bizType;

    private Long bizId;

    private String originalName;

    private String fileName;

    private Long fileSize;

    private String contentType;

    /** 相对存储路径，如 2026/11/uuid.pdf */
    private String storagePath;

    private Long uploaderId;

    private String uploaderName;
}
