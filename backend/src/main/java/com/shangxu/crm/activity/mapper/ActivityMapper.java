package com.shangxu.crm.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shangxu.crm.activity.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {

    @Select("""
            <script>
            SELECT a.*, u.nickname AS owner_name
            FROM crm_activity a
            LEFT JOIN sys_user u ON u.id = a.owner_user_id
            WHERE a.deleted = 0
            <if test="bizType != null and bizType != ''"> AND a.biz_type = #{bizType} </if>
            <if test="bizId != null"> AND a.biz_id = #{bizId} </if>
            <if test="ownerId != null"> AND a.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND a.owner_user_id IN (SELECT id FROM sys_user WHERE dept_id = #{deptId}) </if>
            ORDER BY a.create_time DESC, a.id DESC
            </script>
            """)
    IPage<Activity> selectPageList(IPage<Activity> page,
                                   @Param("bizType") String bizType,
                                   @Param("bizId") Long bizId,
                                   @Param("ownerId") Long ownerId,
                                   @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT a.*, u.nickname AS owner_name
            FROM crm_activity a
            LEFT JOIN sys_user u ON u.id = a.owner_user_id
            WHERE a.deleted = 0
            <if test="ownerId != null"> AND a.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND a.owner_user_id IN (SELECT id FROM sys_user WHERE dept_id = #{deptId}) </if>
            ORDER BY a.create_time DESC, a.id DESC
            LIMIT #{limit}
            </script>
            """)
    List<Activity> selectRecent(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId, @Param("limit") int limit);
}
