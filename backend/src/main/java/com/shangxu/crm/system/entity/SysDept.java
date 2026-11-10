package com.shangxu.crm.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    private Long parentId;
    private String name;
    private String leader;
    private Integer sort;
    private Integer status;

    /** 非数据库字段：前端树形展示用 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private java.util.List<SysDept> children;
}
