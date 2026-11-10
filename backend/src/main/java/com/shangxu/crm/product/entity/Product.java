package com.shangxu.crm.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shangxu.crm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_product")
public class Product extends BaseEntity {

    private String code;

    private String name;

    /** 所属目录 ID */
    private Long categoryId;

    /** 兼容旧数据的分类名称，保存时由目录自动带出 */
    private String category;

    private String unit;

    /** 规格型号，如「20 账号 / 年」 */
    private String spec;

    private BigDecimal price;

    private BigDecimal cost;

    /** 默认税率（%） */
    private BigDecimal taxRate;

    /** 最低成交价，用于折扣管控 */
    private BigDecimal minPrice;

    private Integer sort;

    private String imageUrl;

    /** 1 在售 / 0 停售 */
    private Integer status;

    private String remark;

    /** 产品详细描述 */
    private String description;

    @TableField(exist = false)
    private String categoryName;

    /** 累计销量（详情页展示） */
    @TableField(exist = false)
    private BigDecimal saleQty;

    /** 累计销售额（详情页展示） */
    @TableField(exist = false)
    private BigDecimal saleAmount;

    /** 关联订单数 */
    @TableField(exist = false)
    private Integer orderCount;
}
