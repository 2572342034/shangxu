package com.shangxu.crm.payment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 收款状态推导单测：这是账务正确性的核心判定，必须锁死。
 */
class PaymentServiceStatusTest {

    @Test
    @DisplayName("未收款：金额为 0 或 null 时状态为 UNPAID")
    void unpaid() {
        assertEquals("UNPAID", PaymentService.calcStatus(BigDecimal.ZERO, new BigDecimal("1000")));
        assertEquals("UNPAID", PaymentService.calcStatus(null, new BigDecimal("1000")));
    }

    @Test
    @DisplayName("部分收款：大于 0 且小于应收总额")
    void partial() {
        assertEquals("PART", PaymentService.calcStatus(new BigDecimal("1"), new BigDecimal("1000")));
        assertEquals("PART", PaymentService.calcStatus(new BigDecimal("999.99"), new BigDecimal("1000")));
    }

    @Test
    @DisplayName("已收款：等于或大于应收总额")
    void paid() {
        assertEquals("PAID", PaymentService.calcStatus(new BigDecimal("1000"), new BigDecimal("1000")));
        assertEquals("PAID", PaymentService.calcStatus(new BigDecimal("1000.01"), new BigDecimal("1000")));
    }

    @Test
    @DisplayName("小数精度：0.1 + 0.2 场景不应误判为部分收款")
    void decimalPrecision() {
        BigDecimal received = new BigDecimal("0.1").add(new BigDecimal("0.2"));
        assertEquals("PAID", PaymentService.calcStatus(received, new BigDecimal("0.3")));
    }
}
