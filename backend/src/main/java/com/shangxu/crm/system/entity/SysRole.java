package com.shangxu.crm.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private String name;
    private String code;
    /** 数据权限 ALL / DEPT / SELF */
    private String dataScope;
    /** JSON 字符串数组 */
    private String permissions;
    private Integer sort;
    private Integer status;
    private String remark;

    @TableField(exist = false)
    private List<String> permissionList;
}
