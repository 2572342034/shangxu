package com.shangxu.crm.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shangxu.crm.contract.entity.PaymentPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface PaymentPlanMapper extends BaseMapper<PaymentPlan> {

    @Select("""
            SELECT p.*, c.name AS contract_name, c.customer_name
            FROM crm_payment_plan p
            LEFT JOIN crm_contract c ON c.id = p.contract_id
            WHERE p.contract_id = #{contractId} AND p.deleted = 0
            ORDER BY p.period ASC
            """)
    List<PaymentPlan> selectByContractId(@Param("contractId") Long contractId);

    @Select("""
            <script>
            SELECT IFNULL(SUM(p.actual_amount), 0)
            FROM crm_payment_plan p
            INNER JOIN crm_contract c ON c.id = p.contract_id
            WHERE p.deleted = 0 AND c.deleted = 0
            <if test="ownerId != null"> AND c.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND c.dept_id = #{deptId} </if>
            </script>
            """)
    BigDecimal sumReceived(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT p.*, c.name AS contract_name, c.customer_name
            FROM crm_payment_plan p
            INNER JOIN crm_contract c ON c.id = p.contract_id AND c.deleted = 0
            WHERE p.deleted = 0 AND p.status = 'UNPAID'
            <if test="ownerId != null"> AND c.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND c.dept_id = #{deptId} </if>
            ORDER BY p.plan_date ASC
            LIMIT 10
            </script>
            """)
    List<PaymentPlan> selectPending(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);
}
