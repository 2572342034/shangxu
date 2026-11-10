package com.shangxu.crm.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 合同明细：合同标的行 */
@Data
@TableName("crm_contract_item")
public class ContractItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private Long productId;
    private String productName;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private String remark;
}
