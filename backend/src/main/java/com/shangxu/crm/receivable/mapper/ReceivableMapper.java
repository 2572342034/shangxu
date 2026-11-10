package com.shangxu.crm.receivable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shangxu.crm.receivable.entity.Receivable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReceivableMapper extends BaseMapper<Receivable> {

    @Select("""
            <script>
            SELECT r.*, u.nickname AS owner_name,
                   CASE WHEN r.status = 'PAID' THEN 0
                        ELSE GREATEST(DATEDIFF(CURDATE(), r.due_date), 0) END AS overdue_days
            FROM crm_receivable r
            LEFT JOIN sys_user u ON u.id = r.owner_user_id
            WHERE r.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (r.receivable_no LIKE CONCAT('%', #{keyword}, '%')
                   OR r.customer_name LIKE CONCAT('%', #{keyword}, '%')
                   OR r.source_no LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null and status != ''"> AND r.status = #{status} </if>
            <if test="overdue != null and overdue"> AND r.status &lt;&gt; 'PAID' AND r.due_date &lt; CURDATE() </if>
            <if test="customerId != null"> AND r.customer_id = #{customerId} </if>
            <if test="ownerId != null"> AND r.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND r.dept_id = #{deptId} </if>
            ORDER BY r.id DESC
            </script>
            """)
    IPage<Receivable> selectPageList(IPage<Receivable> page,
                                     @Param("keyword") String keyword,
                                     @Param("status") String status,
                                     @Param("overdue") Boolean overdue,
                                     @Param("customerId") Long customerId,
                                     @Param("ownerId") Long ownerId,
                                     @Param("deptId") Long deptId);

    @Select("SELECT IFNULL(MAX(CAST(SUBSTRING(receivable_no, 7) AS UNSIGNED)), 0) FROM crm_receivable")
    int selectMaxNo();

    @Select("""
            <script>
            SELECT IFNULL(SUM(amount), 0) FROM crm_receivable WHERE deleted = 0
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    BigDecimal sumAmount(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT IFNULL(SUM(received_amount), 0) FROM crm_receivable WHERE deleted = 0
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    BigDecimal sumReceived(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT IFNULL(SUM(amount - received_amount), 0) FROM crm_receivable
            WHERE deleted = 0 AND status &lt;&gt; 'PAID' AND due_date &lt; CURDATE()
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    BigDecimal sumOverdue(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT COUNT(1) FROM crm_receivable
            WHERE deleted = 0 AND status &lt;&gt; 'PAID' AND due_date &lt; CURDATE()
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    long countOverdue(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT r.*, u.nickname AS owner_name,
                   CASE WHEN r.status = 'PAID' THEN 0
                        ELSE GREATEST(DATEDIFF(CURDATE(), r.due_date), 0) END AS overdue_days
            FROM crm_receivable r
            LEFT JOIN sys_user u ON u.id = r.owner_user_id
            WHERE r.deleted = 0 AND r.status &lt;&gt; 'PAID'
            <if test="ownerId != null"> AND r.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND r.dept_id = #{deptId} </if>
            ORDER BY r.due_date ASC
            LIMIT 8
            </script>
            """)
    List<Receivable> selectPendingList(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("SELECT COUNT(1) FROM crm_receivable WHERE source_type = #{type} AND source_id = #{sourceId} AND deleted = 0")
    long countBySource(@Param("type") String type, @Param("sourceId") Long sourceId);

    @Select("""
            <script>
            SELECT r.receivable_no AS no, CONCAT('收款 · ', IFNULL(r.source_no, '')) AS title,
                   r.customer_name AS customerName, r.received_amount AS amount,
                   IFNULL(r.receive_time, r.update_time) AS time, u.nickname AS ownerName
            FROM crm_receivable r
            LEFT JOIN sys_user u ON u.id = r.owner_user_id
            WHERE r.deleted = 0 AND r.received_amount &gt; 0
            <if test="ownerId != null"> AND r.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND r.dept_id = #{deptId} </if>
            ORDER BY IFNULL(r.receive_time, r.update_time) DESC
            LIMIT #{limit}
            </script>
            """)
    List<Map<String, Object>> selectRecentReceived(@Param("ownerId") Long ownerId,
                                                   @Param("deptId") Long deptId,
                                                   @Param("limit") int limit);

    @Select("""
            <script>
            SELECT DATE_FORMAT(IFNULL(receive_time, update_time), '%m') AS name, IFNULL(SUM(received_amount), 0) AS value
            FROM crm_receivable
            WHERE deleted = 0 AND received_amount &gt; 0 AND YEAR(IFNULL(receive_time, update_time)) = YEAR(CURDATE())
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            GROUP BY DATE_FORMAT(IFNULL(receive_time, update_time), '%m')
            </script>
            """)
    List<Map<String, Object>> sumReceivedMonthly(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            SELECT IFNULL(SUM(r.received_amount), 0)
            FROM crm_receivable r
            WHERE r.deleted = 0 AND (
              (r.source_type = 'CONTRACT' AND r.source_id = #{contractId})
              OR (r.source_type = 'ORDER' AND r.source_id IN
                  (SELECT o.id FROM crm_order o WHERE o.contract_id = #{contractId} AND o.deleted = 0))
            )
            """)
    BigDecimal sumReceivedByContract(@Param("contractId") Long contractId);

    @Select("""
            SELECT * FROM crm_receivable
            WHERE deleted = 0 AND status <> 'PAID' AND source_type = 'CONTRACT' AND source_id = #{contractId}
            ORDER BY due_date ASC, id ASC
            """)
    List<Receivable> selectOpenByContract(@Param("contractId") Long contractId);

    @Select("""
            SELECT * FROM crm_receivable
            WHERE deleted = 0 AND status <> 'PAID' AND due_date < CURDATE() AND owner_user_id IS NOT NULL
            ORDER BY due_date ASC
            """)
    List<Receivable> selectOverdueAll();
}
