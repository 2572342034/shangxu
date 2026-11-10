package com.shangxu.crm.payment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 收款登记请求：一笔收款 + 若干条核销明细。
 */
@Data
public class PaymentCreateRequest {

    private Long customerId;

    private String customerName;

    /** 收款金额 */
    private BigDecimal amount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate receiveDate;

    /** BANK / CASH / ACCEPTANCE / ONLINE / OTHER */
    private String method;

    private String bankAccount;

    private String voucherNo;

    private String sourceType;

    private Long sourceId;

    private String sourceNo;

    private String remark;

    /** 核销明细；为空表示先记预收，后续再核销 */
    private List<AllocationItem> allocations;

    @Data
    public static class AllocationItem {
        private Long receivableId;
        private BigDecimal amount;
    }
}
