package com.shangxu.crm.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shangxu.crm.payment.entity.PaymentAllocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PaymentAllocationMapper extends BaseMapper<PaymentAllocation> {

    @Select("""
            SELECT a.*, r.amount AS receivable_amount, r.received_amount AS receivable_received
            FROM crm_payment_allocation a
            LEFT JOIN crm_receivable r ON r.id = a.receivable_id
            WHERE a.deleted = 0 AND a.record_id = #{recordId}
            ORDER BY a.id ASC
            """)
    List<PaymentAllocation> selectByRecordId(@Param("recordId") Long recordId);

    @Select("SELECT IFNULL(SUM(amount), 0) FROM crm_payment_allocation WHERE deleted = 0 AND receivable_id = #{receivableId}")
    java.math.BigDecimal sumByReceivable(@Param("receivableId") Long receivableId);
}
