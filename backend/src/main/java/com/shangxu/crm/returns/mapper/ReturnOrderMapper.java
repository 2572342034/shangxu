package com.shangxu.crm.returns.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shangxu.crm.returns.entity.ReturnOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReturnOrderMapper extends BaseMapper<ReturnOrder> {

    @Select("""
            <script>
            SELECT r.*, u.nickname AS owner_name
            FROM crm_return r
            LEFT JOIN sys_user u ON u.id = r.owner_user_id
            WHERE r.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (r.return_no LIKE CONCAT('%', #{keyword}, '%') OR r.order_no LIKE CONCAT('%', #{keyword}, '%')
                   OR r.customer_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null and status != ''"> AND r.status = #{status} </if>
            <if test="ownerId != null"> AND r.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND r.dept_id = #{deptId} </if>
            ORDER BY r.id DESC
            </script>
            """)
    IPage<ReturnOrder> selectPageList(IPage<ReturnOrder> page,
                                      @Param("keyword") String keyword,
                                      @Param("status") String status,
                                      @Param("ownerId") Long ownerId,
                                      @Param("deptId") Long deptId);

    @Select("SELECT IFNULL(MAX(CAST(RIGHT(return_no, 4) AS UNSIGNED)), 0) FROM crm_return")
    int selectMaxNo();

    @Select("""
            <script>
            SELECT r.return_no AS no, CONCAT('退款 · ', IFNULL(r.order_no, '')) AS title,
                   r.customer_name AS customerName, r.refund_amount AS amount,
                   IFNULL(r.refund_time, r.create_time) AS time, u.nickname AS ownerName
            FROM crm_return r
            LEFT JOIN sys_user u ON u.id = r.owner_user_id
            WHERE r.deleted = 0 AND r.status = 'REFUNDED'
            <if test="ownerId != null"> AND r.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND r.dept_id = #{deptId} </if>
            ORDER BY IFNULL(r.refund_time, r.create_time) DESC
            LIMIT #{limit}
            </script>
            """)
    List<Map<String, Object>> selectRecentRefunded(@Param("ownerId") Long ownerId,
                                                   @Param("deptId") Long deptId,
                                                   @Param("limit") int limit);
}
