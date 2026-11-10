package com.shangxu.crm.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shangxu.crm.delivery.entity.Delivery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface DeliveryMapper extends BaseMapper<Delivery> {

    @Select("""
            <script>
            SELECT d.*, u.nickname AS owner_name
            FROM crm_delivery d
            LEFT JOIN sys_user u ON u.id = d.owner_user_id
            WHERE d.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (d.delivery_no LIKE CONCAT('%', #{keyword}, '%') OR d.order_no LIKE CONCAT('%', #{keyword}, '%')
                   OR d.customer_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null and status != ''"> AND d.status = #{status} </if>
            <if test="orderId != null"> AND d.order_id = #{orderId} </if>
            <if test="ownerId != null"> AND d.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND d.dept_id = #{deptId} </if>
            ORDER BY d.id DESC
            </script>
            """)
    IPage<Delivery> selectPageList(IPage<Delivery> page,
                                   @Param("keyword") String keyword,
                                   @Param("status") String status,
                                   @Param("orderId") Long orderId,
                                   @Param("ownerId") Long ownerId,
                                   @Param("deptId") Long deptId);

    @Select("SELECT IFNULL(MAX(CAST(SUBSTRING(delivery_no, 7) AS UNSIGNED)), 0) FROM crm_delivery")
    int selectMaxNo();

    @Select("SELECT COUNT(1) FROM crm_delivery WHERE order_id = #{orderId} AND status <> 'CANCELLED' AND deleted = 0")
    long countActiveByOrder(@Param("orderId") Long orderId);

    @Select("""
            SELECT IFNULL(SUM(total_qty), 0) FROM crm_delivery
            WHERE order_id = #{orderId} AND status IN ('SHIPPED', 'SIGNED') AND deleted = 0
            """)
    BigDecimal sumShippedQty(@Param("orderId") Long orderId);

    @Select("""
            <script>
            SELECT COUNT(1) FROM crm_delivery WHERE deleted = 0 AND status = 'DRAFT'
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    long countPending(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT d.*, u.nickname AS owner_name
            FROM crm_delivery d
            LEFT JOIN sys_user u ON u.id = d.owner_user_id
            WHERE d.deleted = 0 AND d.status = 'DRAFT'
            <if test="ownerId != null"> AND d.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND d.dept_id = #{deptId} </if>
            ORDER BY d.create_time ASC
            LIMIT 8
            </script>
            """)
    List<Delivery> selectPendingList(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT d.delivery_no AS no, CONCAT('出库 · ', IFNULL(d.order_no, '')) AS title,
                   d.customer_name AS customerName, d.amount AS amount,
                   d.create_time AS time, u.nickname AS ownerName
            FROM crm_delivery d
            LEFT JOIN sys_user u ON u.id = d.owner_user_id
            WHERE d.deleted = 0 AND d.status IN ('SHIPPED', 'SIGNED')
            <if test="ownerId != null"> AND d.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND d.dept_id = #{deptId} </if>
            ORDER BY d.create_time DESC
            LIMIT #{limit}
            </script>
            """)
    List<java.util.Map<String, Object>> selectRecentShipped(@Param("ownerId") Long ownerId,
                                                            @Param("deptId") Long deptId,
                                                            @Param("limit") int limit);

    @Select("""
            <script>
            SELECT IFNULL(SUM(amount), 0) FROM crm_delivery
            WHERE deleted = 0 AND status IN ('SHIPPED', 'SIGNED')
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    java.math.BigDecimal sumShippedAmount(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT COUNT(1) FROM crm_delivery
            WHERE deleted = 0 AND status IN ('SHIPPED', 'SIGNED')
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    long countShipped(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);
}
