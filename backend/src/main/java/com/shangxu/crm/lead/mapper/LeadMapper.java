package com.shangxu.crm.lead.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shangxu.crm.lead.entity.Lead;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LeadMapper extends BaseMapper<Lead> {

    @Select("""
            <script>
            SELECT l.*, u.nickname AS owner_name
            FROM crm_lead l
            LEFT JOIN sys_user u ON u.id = l.owner_user_id
            WHERE l.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (l.name LIKE CONCAT('%', #{keyword}, '%') OR l.company LIKE CONCAT('%', #{keyword}, '%')
                   OR l.contact_name LIKE CONCAT('%', #{keyword}, '%') OR l.mobile LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null and status != ''"> AND l.status = #{status} </if>
            <if test="source != null and source != ''"> AND l.source = #{source} </if>
            <if test="ownerId != null"> AND l.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND l.dept_id = #{deptId} </if>
            ORDER BY l.id DESC
            </script>
            """)
    IPage<Lead> selectPageList(IPage<Lead> page,
                               @Param("keyword") String keyword,
                               @Param("status") String status,
                               @Param("source") String source,
                               @Param("ownerId") Long ownerId,
                               @Param("deptId") Long deptId);

    @Select("SELECT IFNULL(MAX(CAST(SUBSTRING(lead_no, 7) AS UNSIGNED)), 0) FROM crm_lead")
    int selectMaxNo();

    @Select("""
            <script>
            SELECT COUNT(1) FROM crm_lead WHERE deleted = 0
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    long countAll(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT COUNT(1) FROM crm_lead
            WHERE deleted = 0 AND YEAR(create_time) = YEAR(CURDATE()) AND MONTH(create_time) = MONTH(CURDATE())
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    long countNewThisMonth(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT COUNT(1) FROM crm_lead WHERE deleted = 0 AND status = 'CONVERTED'
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    long countConverted(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT status AS name, COUNT(1) AS value FROM crm_lead WHERE deleted = 0
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            GROUP BY status
            </script>
            """)
    List<java.util.Map<String, Object>> countByStatus(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);
}
