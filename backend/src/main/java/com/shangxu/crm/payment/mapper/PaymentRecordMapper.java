package com.shangxu.crm.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shangxu.crm.payment.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {

    @Select("""
            <script>
            SELECT p.*, u.nickname AS owner_name
            FROM crm_payment_record p
            LEFT JOIN sys_user u ON u.id = p.owner_user_id
            WHERE p.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (p.record_no LIKE CONCAT('%', #{keyword}, '%')
                   OR p.customer_name LIKE CONCAT('%', #{keyword}, '%')
                   OR p.voucher_no LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="method != null and method != ''"> AND p.method = #{method} </if>
            <if test="status != null and status != ''"> AND p.status = #{status} </if>
            <if test="customerId != null"> AND p.customer_id = #{customerId} </if>
            <if test="startDate != null"> AND p.receive_date &gt;= #{startDate} </if>
            <if test="endDate != null"> AND p.receive_date &lt;= #{endDate} </if>
            <if test="ownerId != null"> AND p.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND p.dept_id = #{deptId} </if>
            ORDER BY p.receive_date DESC, p.id DESC
            </script>
            """)
    IPage<PaymentRecord> selectPageList(IPage<PaymentRecord> page,
                                        @Param("keyword") String keyword,
                                        @Param("method") String method,
                                        @Param("status") String status,
                                        @Param("customerId") Long customerId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("ownerId") Long ownerId,
                                        @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT IFNULL(SUM(amount), 0) FROM crm_payment_record
            WHERE deleted = 0 AND status = 'CONFIRMED'
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    BigDecimal sumConfirmed(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT IFNULL(SUM(unallocated_amount), 0) FROM crm_payment_record
            WHERE deleted = 0 AND status = 'CONFIRMED'
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    BigDecimal sumUnallocated(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT COUNT(1) FROM crm_payment_record
            WHERE deleted = 0 AND status = 'CONFIRMED' AND unallocated_amount &gt; 0
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    long countUnallocated(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT DATE_FORMAT(receive_date, '%m') AS name, IFNULL(SUM(amount), 0) AS value
            FROM crm_payment_record
            WHERE deleted = 0 AND status = 'CONFIRMED' AND YEAR(receive_date) = YEAR(CURDATE())
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            GROUP BY DATE_FORMAT(receive_date, '%m')
            </script>
            """)
    List<Map<String, Object>> sumMonthly(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);
}
