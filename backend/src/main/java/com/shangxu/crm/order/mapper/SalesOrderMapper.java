package com.shangxu.crm.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shangxu.crm.order.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {

    @Select("""
            <script>
            SELECT o.*, u.nickname AS owner_name,
                   (SELECT COUNT(1) FROM crm_delivery d
                     WHERE d.order_id = o.id AND d.status &lt;&gt; 'CANCELLED' AND d.deleted = 0) AS delivery_count
            FROM crm_order o
            LEFT JOIN sys_user u ON u.id = o.owner_user_id
            WHERE o.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (o.title LIKE CONCAT('%', #{keyword}, '%') OR o.order_no LIKE CONCAT('%', #{keyword}, '%')
                   OR o.customer_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null and status != ''"> AND o.status = #{status} </if>
            <if test="deliveryStatus != null and deliveryStatus != ''"> AND o.delivery_status = #{deliveryStatus} </if>
            <if test="customerId != null"> AND o.customer_id = #{customerId} </if>
            <if test="ownerId != null"> AND o.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND o.dept_id = #{deptId} </if>
            ORDER BY o.id DESC
            </script>
            """)
    IPage<SalesOrder> selectPageList(IPage<SalesOrder> page,
                                     @Param("keyword") String keyword,
                                     @Param("status") String status,
                                     @Param("deliveryStatus") String deliveryStatus,
                                     @Param("customerId") Long customerId,
                                     @Param("ownerId") Long ownerId,
                                     @Param("deptId") Long deptId);

    @Select("SELECT IFNULL(MAX(CAST(SUBSTRING(order_no, 7) AS UNSIGNED)), 0) FROM crm_order")
    int selectMaxNo();

    @Select("""
            <script>
            SELECT COUNT(1) FROM crm_order
            WHERE deleted = 0 AND YEAR(order_date) = YEAR(CURDATE()) AND MONTH(order_date) = MONTH(CURDATE())
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    long countThisMonth(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT IFNULL(SUM(amount), 0) FROM crm_order
            WHERE deleted = 0 AND status = 'APPROVED'
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    BigDecimal sumApprovedAmount(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT COUNT(1) FROM crm_order
            WHERE deleted = 0 AND status = 'APPROVED' AND delivery_status &lt;&gt; 'DELIVERED'
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    long countPendingDelivery(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT o.*, u.nickname AS owner_name
            FROM crm_order o
            LEFT JOIN sys_user u ON u.id = o.owner_user_id
            WHERE o.deleted = 0 AND o.status = 'APPROVED' AND o.delivery_status &lt;&gt; 'DELIVERED'
            <if test="ownerId != null"> AND o.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND o.dept_id = #{deptId} </if>
            ORDER BY o.order_date ASC
            LIMIT 8
            </script>
            """)
    List<SalesOrder> selectPendingDelivery(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT status AS name, COUNT(1) AS value FROM crm_order WHERE deleted = 0
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            GROUP BY status
            </script>
            """)
    List<Map<String, Object>> countByStatus(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT o.order_no AS no, o.title AS title, o.customer_name AS customerName,
                   o.amount AS amount, o.create_time AS time, u.nickname AS ownerName
            FROM crm_order o
            LEFT JOIN sys_user u ON u.id = o.owner_user_id
            WHERE o.deleted = 0 AND o.status = 'APPROVED'
            <if test="ownerId != null"> AND o.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND o.dept_id = #{deptId} </if>
            ORDER BY o.create_time DESC
            LIMIT #{limit}
            </script>
            """)
    List<Map<String, Object>> selectRecentApproved(@Param("ownerId") Long ownerId,
                                                   @Param("deptId") Long deptId,
                                                   @Param("limit") int limit);

    @Select("""
            <script>
            SELECT o.customer_name AS name, IFNULL(SUM(o.amount), 0) AS amount, COUNT(1) AS total
            FROM crm_order o
            WHERE o.deleted = 0 AND o.status = 'APPROVED'
            <if test="ownerId != null"> AND o.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND o.dept_id = #{deptId} </if>
            GROUP BY o.customer_id, o.customer_name
            ORDER BY amount DESC
            LIMIT 8
            </script>
            """)
    List<Map<String, Object>> selectCustomerRank(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT DATE_FORMAT(order_date, '%m') AS name, IFNULL(SUM(amount), 0) AS value
            FROM crm_order
            WHERE deleted = 0 AND status = 'APPROVED' AND order_date IS NOT NULL
              AND YEAR(order_date) = YEAR(CURDATE())
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            GROUP BY DATE_FORMAT(order_date, '%m')
            </script>
            """)
    List<Map<String, Object>> sumOrderMonthly(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT COUNT(1) FROM crm_order WHERE deleted = 0 AND status = 'APPROVED'
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    long countApproved(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    /** 某合同下有效订单金额合计（排除已取消；excludeId 用于编辑时排除自己） */
    @Select("""
            <script>
            SELECT IFNULL(SUM(amount), 0) FROM crm_order
            WHERE contract_id = #{contractId} AND deleted = 0 AND status &lt;&gt; 'CANCELLED'
            <if test="excludeId != null"> AND id &lt;&gt; #{excludeId} </if>
            </script>
            """)
    BigDecimal sumAmountByContract(@Param("contractId") Long contractId,
                                   @Param("excludeId") Long excludeId);

    @Select("""
            SELECT COUNT(1) FROM crm_order
            WHERE contract_id = #{contractId} AND deleted = 0 AND status <> 'CANCELLED'
            """)
    long countByContract(@Param("contractId") Long contractId);
}
