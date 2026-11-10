package com.shangxu.crm.customer.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_contact")
public class Contact extends BaseEntity {

    private Long customerId;
    private String name;
    private String post;
    private String mobile;
    private String phone;
    private String email;
    private String wechat;
    /** MALE / FEMALE */
    private String gender;
    private Integer isPrimary;
    private LocalDate birthday;
    private String remark;

    @TableField(exist = false)
    private String customerName;
}
