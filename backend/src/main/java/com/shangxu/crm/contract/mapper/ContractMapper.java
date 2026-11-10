package com.shangxu.crm.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shangxu.crm.contract.entity.Contract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface ContractMapper extends BaseMapper<Contract> {

    @Select("""
            <script>
            SELECT c.*, u.nickname AS owner_name,
                   (SELECT IFNULL(SUM(p.actual_amount), 0) FROM crm_payment_plan p
                     WHERE p.contract_id = c.id AND p.deleted = 0) AS received_amount
            FROM crm_contract c
            LEFT JOIN sys_user u ON u.id = c.owner_user_id
            WHERE c.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (c.name LIKE CONCAT('%', #{keyword}, '%') OR c.contract_no LIKE CONCAT('%', #{keyword}, '%')
                   OR c.customer_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null and status != ''"> AND c.status = #{status} </if>
            <if test="paymentStatus != null and paymentStatus != ''"> AND c.payment_status = #{paymentStatus} </if>
            <if test="customerId != null"> AND c.customer_id = #{customerId} </if>
            <if test="ownerId != null"> AND c.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND c.dept_id = #{deptId} </if>
            ORDER BY c.id DESC
            </script>
            """)
    IPage<Contract> selectPageList(IPage<Contract> page,
                                   @Param("keyword") String keyword,
                                   @Param("status") String status,
                                   @Param("paymentStatus") String paymentStatus,
                                   @Param("customerId") Long customerId,
                                   @Param("ownerId") Long ownerId,
                                   @Param("deptId") Long deptId);

    @Select("SELECT IFNULL(MAX(CAST(SUBSTRING(contract_no, 7) AS UNSIGNED)), 0) FROM crm_contract")
    int selectMaxNo();

    @Select("""
            <script>
            SELECT c.*, u.nickname AS owner_name
            FROM crm_contract c
            LEFT JOIN sys_user u ON u.id = c.owner_user_id
            WHERE c.deleted = 0 AND c.status = 'PENDING'
            <if test="ownerId != null"> AND c.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND c.dept_id = #{deptId} </if>
            ORDER BY c.update_time DESC
            LIMIT 8
            </script>
            """)
    List<Contract> selectPendingList(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT IFNULL(SUM(amount), 0) FROM crm_contract
            WHERE deleted = 0 AND status IN ('SIGNED', 'APPROVED')
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    BigDecimal sumSignedAmount(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT COUNT(1) FROM crm_contract
            WHERE deleted = 0 AND status IN ('SIGNED', 'APPROVED')
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    long countSigned(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT status AS name, COUNT(1) AS value FROM crm_contract WHERE deleted = 0
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            GROUP BY status
            </script>
            """)
    List<Map<String, Object>> countByStatus(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT DATE_FORMAT(sign_date, '%m') AS name, IFNULL(SUM(amount), 0) AS value
            FROM crm_contract
            WHERE deleted = 0 AND sign_date IS NOT NULL AND status IN ('SIGNED', 'APPROVED')
              AND YEAR(sign_date) = YEAR(CURDATE())
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            GROUP BY DATE_FORMAT(sign_date, '%m')
            </script>
            """)
    List<Map<String, Object>> sumSignedMonthly(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT c.contract_no AS no, c.name AS title, c.customer_name AS customerName,
                   c.amount AS amount, c.create_time AS time, u.nickname AS ownerName
            FROM crm_contract c
            LEFT JOIN sys_user u ON u.id = c.owner_user_id
            WHERE c.deleted = 0 AND c.status IN ('SIGNED', 'APPROVED')
            <if test="ownerId != null"> AND c.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND c.dept_id = #{deptId} </if>
            ORDER BY c.create_time DESC
            LIMIT #{limit}
            </script>
            """)
    List<Map<String, Object>> selectRecentSigned(@Param("ownerId") Long ownerId,
                                                 @Param("deptId") Long deptId,
                                                 @Param("limit") int limit);
}
