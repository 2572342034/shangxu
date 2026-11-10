package com.shangxu.crm.lead.dto;

import java.math.BigDecimal;

/**
 * 线索转化参数
 */
public record LeadConvertRequest(
        String customerName,
        String level,
        boolean createOpportunity,
        String oppName,
        BigDecimal oppAmount
) {
}
