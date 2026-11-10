package com.shangxu.crm.plan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shangxu.crm.plan.entity.DeliveryPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeliveryPlanMapper extends BaseMapper<DeliveryPlan> {

    @Select("""
            <script>
            SELECT p.*, u.nickname AS owner_name
            FROM crm_delivery_plan p
            LEFT JOIN sys_user u ON u.id = p.owner_user_id
            WHERE p.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (p.plan_no LIKE CONCAT('%', #{keyword}, '%') OR p.order_no LIKE CONCAT('%', #{keyword}, '%')
                   OR p.customer_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null and status != ''"> AND p.status = #{status} </if>
            <if test="orderId != null"> AND p.order_id = #{orderId} </if>
            <if test="ownerId != null"> AND p.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND p.dept_id = #{deptId} </if>
            ORDER BY p.id DESC
            </script>
            """)
    IPage<DeliveryPlan> selectPageList(IPage<DeliveryPlan> page,
                                       @Param("keyword") String keyword,
                                       @Param("status") String status,
                                       @Param("orderId") Long orderId,
                                       @Param("ownerId") Long ownerId,
                                       @Param("deptId") Long deptId);

    @Select("SELECT IFNULL(MAX(CAST(RIGHT(plan_no, 4) AS UNSIGNED)), 0) FROM crm_delivery_plan")
    int selectMaxNo();

    @Select("SELECT COUNT(1) FROM crm_delivery_plan WHERE order_id = #{orderId} AND status = 'PENDING' AND deleted = 0")
    long countPendingByOrder(@Param("orderId") Long orderId);
}
