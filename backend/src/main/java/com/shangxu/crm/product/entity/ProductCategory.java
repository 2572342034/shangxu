package com.shangxu.crm.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 产品目录节点，支持两级（可递归扩展）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_product_category")
public class ProductCategory extends BaseEntity {

    /** 父级 ID，0 表示根节点 */
    private Long parentId;

    private String name;

    private String code;

    private Integer sort;

    /** 1 启用 / 0 停用 */
    private Integer status;

    private String remark;
}
