package com.shangxu.crm.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shangxu.crm.customer.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    @Select("""
            <script>
            SELECT c.*, u.nickname AS owner_name, d.name AS dept_name,
                   (SELECT COUNT(1) FROM crm_contact ct WHERE ct.customer_id = c.id AND ct.deleted = 0) AS contact_count,
                   (SELECT COUNT(1) FROM crm_opportunity o WHERE o.customer_id = c.id AND o.deleted = 0) AS opportunity_count
            FROM crm_customer c
            LEFT JOIN sys_user u ON u.id = c.owner_user_id
            LEFT JOIN sys_dept d ON d.id = c.dept_id
            WHERE c.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (c.name LIKE CONCAT('%', #{keyword}, '%') OR c.customer_no LIKE CONCAT('%', #{keyword}, '%')
                   OR c.short_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="level != null and level != ''"> AND c.level = #{level} </if>
            <if test="status != null and status != ''"> AND c.status = #{status} </if>
            <if test="industry != null and industry != ''"> AND c.industry = #{industry} </if>
            <if test="poolType != null and poolType != ''"> AND c.pool_type = #{poolType} </if>
            <if test="ownerId != null"> AND c.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND c.dept_id = #{deptId} </if>
            ORDER BY c.id DESC
            </script>
            """)
    IPage<Customer> selectPageList(IPage<Customer> page,
                                   @Param("keyword") String keyword,
                                   @Param("level") String level,
                                   @Param("status") String status,
                                   @Param("industry") String industry,
                                   @Param("poolType") String poolType,
                                   @Param("ownerId") Long ownerId,
                                   @Param("deptId") Long deptId);

    @Select("SELECT IFNULL(MAX(CAST(SUBSTRING(customer_no, 7) AS UNSIGNED)), 0) FROM crm_customer")
    int selectMaxNo();

    @Select("""
            <script>
            SELECT c.*, u.nickname AS owner_name
            FROM crm_customer c
            LEFT JOIN sys_user u ON u.id = c.owner_user_id
            WHERE c.deleted = 0 AND c.next_follow_time IS NOT NULL
              AND c.next_follow_time &lt;= DATE_ADD(NOW(), INTERVAL 7 DAY)
            <if test="ownerId != null"> AND c.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND c.dept_id = #{deptId} </if>
            ORDER BY c.next_follow_time ASC
            LIMIT 8
            </script>
            """)
    List<Customer> selectTodoFollow(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("SELECT COUNT(1) FROM crm_customer WHERE deleted = 0 AND pool_type = 'PRIVATE'")
    long countPrivate();

    @Select("""
            SELECT COUNT(1) FROM crm_customer
            WHERE deleted = 0 AND YEAR(create_time) = YEAR(CURDATE()) AND MONTH(create_time) = MONTH(CURDATE())
            """)
    long countThisMonth();

    @Select("""
            <script>
            SELECT COUNT(1) FROM crm_customer
            WHERE deleted = 0
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    long countAll(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT COUNT(1) FROM crm_customer
            WHERE deleted = 0 AND YEAR(create_time) = YEAR(CURDATE()) AND MONTH(create_time) = MONTH(CURDATE())
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    long countNewThisMonth(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT DATE_FORMAT(create_time, '%m') AS name, COUNT(1) AS value
            FROM crm_customer
            WHERE deleted = 0 AND YEAR(create_time) = YEAR(CURDATE())
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            GROUP BY DATE_FORMAT(create_time, '%m')
            </script>
            """)
    List<java.util.Map<String, Object>> countMonthly(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT source AS name, COUNT(1) AS value
            FROM crm_customer
            WHERE deleted = 0 AND source IS NOT NULL
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            GROUP BY source
            </script>
            """)
    List<java.util.Map<String, Object>> countBySource(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT level AS name, COUNT(1) AS value
            FROM crm_customer
            WHERE deleted = 0
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            GROUP BY level ORDER BY level
            </script>
            """)
    List<java.util.Map<String, Object>> countByLevel(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            SELECT * FROM crm_customer
            WHERE deleted = 0 AND owner_user_id IS NOT NULL
              AND next_follow_time IS NOT NULL
              AND next_follow_time <= DATE_ADD(NOW(), INTERVAL #{hours} HOUR)
            ORDER BY next_follow_time ASC
            """)
    List<Customer> selectFollowDue(@Param("hours") int hours);

    @Select("""
            SELECT * FROM crm_customer
            WHERE deleted = 0 AND pool_type = 'PRIVATE' AND owner_user_id IS NOT NULL
              AND (last_follow_time IS NULL AND create_time < DATE_SUB(NOW(), INTERVAL #{days} DAY)
                   OR last_follow_time < DATE_SUB(NOW(), INTERVAL #{days} DAY))
            ORDER BY id ASC
            """)
    List<Customer> selectIdlePrivate(@Param("days") int days);

    @Select("SELECT COUNT(1) FROM crm_customer WHERE deleted = 0 AND name = #{name}")
    long countByName(@Param("name") String name);

    /**
     * 历史客户：当前用户曾经负责、但现在负责人已不是自己的客户。
     * 依据通用变更轨迹表中 owner_user_id 的旧值判断。
     */
    @Select("""
            <script>
            SELECT c.*, u.nickname AS owner_name
            FROM crm_customer c
            JOIN (
              SELECT DISTINCT biz_id FROM sys_change_log
              WHERE biz_type = 'CUSTOMER' AND field_key = 'owner_user_id'
                AND old_owner_id = #{userId}
            ) h ON h.biz_id = c.id
            LEFT JOIN sys_user u ON u.id = c.owner_user_id
            WHERE c.deleted = 0
              AND (c.owner_user_id IS NULL OR c.owner_user_id &lt;&gt; #{userId})
            <if test="keyword != null and keyword != ''">
              AND (c.name LIKE CONCAT('%', #{keyword}, '%') OR c.customer_no LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY c.id DESC
            </script>
            """)
    IPage<Customer> selectHistoryPage(IPage<Customer> page,
                                      @Param("userId") Long userId,
                                      @Param("keyword") String keyword);
}
