package com.shangxu.crm.opportunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shangxu.crm.opportunity.entity.Opportunity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OpportunityMapper extends BaseMapper<Opportunity> {

    @Select("""
            <script>
            SELECT o.*, u.nickname AS owner_name
            FROM crm_opportunity o
            LEFT JOIN sys_user u ON u.id = o.owner_user_id
            WHERE o.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (o.name LIKE CONCAT('%', #{keyword}, '%') OR o.customer_name LIKE CONCAT('%', #{keyword}, '%')
                   OR o.opp_no LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="stage != null and stage != ''"> AND o.stage = #{stage} </if>
            <if test="status != null and status != ''"> AND o.status = #{status} </if>
            <if test="customerId != null"> AND o.customer_id = #{customerId} </if>
            <if test="ownerId != null"> AND o.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND o.dept_id = #{deptId} </if>
            ORDER BY o.id DESC
            </script>
            """)
    IPage<Opportunity> selectPageList(IPage<Opportunity> page,
                                      @Param("keyword") String keyword,
                                      @Param("stage") String stage,
                                      @Param("status") String status,
                                      @Param("customerId") Long customerId,
                                      @Param("ownerId") Long ownerId,
                                      @Param("deptId") Long deptId);

    @Select("SELECT IFNULL(MAX(CAST(SUBSTRING(opp_no, 7) AS UNSIGNED)), 0) FROM crm_opportunity")
    int selectMaxNo();

    @Select("""
            <script>
            SELECT o.stage AS stage, COUNT(1) AS total, IFNULL(SUM(o.amount), 0) AS amount
            FROM crm_opportunity o
            WHERE o.deleted = 0
            <if test="ownerId != null"> AND o.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND o.dept_id = #{deptId} </if>
            GROUP BY o.stage
            </script>
            """)
    List<Map<String, Object>> countByStage(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT COUNT(1) FROM crm_opportunity WHERE deleted = 0 AND status = 'OPEN'
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    long countOpen(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT IFNULL(SUM(amount), 0) FROM crm_opportunity WHERE deleted = 0 AND status = 'OPEN'
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    java.math.BigDecimal sumOpenAmount(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT COUNT(1) FROM crm_opportunity WHERE deleted = 0 AND status = 'WON'
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    long countWon(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT COUNT(1) FROM crm_opportunity WHERE deleted = 0 AND status = 'LOST'
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    long countLost(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT DATE_FORMAT(IFNULL(o.actual_deal_date, o.update_time), '%m') AS name, IFNULL(SUM(o.amount), 0) AS value
            FROM crm_opportunity o
            WHERE o.deleted = 0 AND o.status = 'WON' AND YEAR(IFNULL(o.actual_deal_date, o.update_time)) = YEAR(CURDATE())
            <if test="ownerId != null"> AND o.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND o.dept_id = #{deptId} </if>
            GROUP BY DATE_FORMAT(IFNULL(o.actual_deal_date, o.update_time), '%m')
            </script>
            """)
    List<Map<String, Object>> sumWonMonthly(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT u.nickname AS name, COUNT(1) AS total, IFNULL(SUM(o.amount), 0) AS amount
            FROM crm_opportunity o
            LEFT JOIN sys_user u ON u.id = o.owner_user_id
            WHERE o.deleted = 0 AND o.status = 'WON'
            <if test="ownerId != null"> AND o.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND o.dept_id = #{deptId} </if>
            GROUP BY o.owner_user_id, u.nickname
            ORDER BY amount DESC
            LIMIT 8
            </script>
            """)
    List<Map<String, Object>> rankByOwner(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);
}
