package com.shangxu.crm.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shangxu.crm.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            <script>
            SELECT u.*, d.name AS dept_name
            FROM sys_user u
            LEFT JOIN sys_dept d ON d.id = u.dept_id AND d.deleted = 0
            WHERE u.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                OR u.mobile LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="deptId != null"> AND u.dept_id = #{deptId} </if>
            <if test="status != null"> AND u.status = #{status} </if>
            ORDER BY u.id ASC
            </script>
            """)
    IPage<SysUser> selectUserPage(IPage<SysUser> page,
                                  @Param("keyword") String keyword,
                                  @Param("deptId") Long deptId,
                                  @Param("status") Integer status);

    @Select("""
            SELECT r.* FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.deleted = 0
            ORDER BY r.sort ASC
            """)
    List<com.shangxu.crm.system.entity.SysRole> selectRolesByUserId(@Param("userId") Long userId);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM sys_user WHERE dept_id = #{deptId} AND deleted = 0")
    long countByDeptId(@Param("deptId") Long deptId);

    @Select("SELECT id FROM sys_user WHERE dept_id = #{deptId} AND deleted = 0")
    List<Long> selectUserIdsByDeptId(@Param("deptId") Long deptId);
}
