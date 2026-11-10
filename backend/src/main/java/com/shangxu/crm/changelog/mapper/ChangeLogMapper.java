package com.shangxu.crm.changelog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shangxu.crm.changelog.entity.ChangeLog;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ChangeLogMapper extends BaseMapper<ChangeLog> {

    @Select("""
            SELECT * FROM sys_change_log
            WHERE biz_type = #{bizType} AND biz_id = #{bizId}
            ORDER BY id DESC LIMIT #{limit}
            """)
    List<ChangeLog> selectByBiz(@Param("bizType") String bizType,
                                @Param("bizId") Long bizId,
                                @Param("limit") int limit);

    /** 该用户是否曾是这条客户的负责人 */
    @Select("""
            SELECT COUNT(1) FROM sys_change_log
            WHERE biz_type = 'CUSTOMER' AND biz_id = #{bizId}
              AND field_key = 'owner_user_id' AND old_owner_id = #{userId}
            """)
    long countOwnerHistory(@Param("bizId") Long bizId, @Param("userId") Long userId);

    @Select("""
            <script>
            SELECT * FROM sys_change_log
            WHERE 1 = 1
            <if test="bizType != null and bizType != ''"> AND biz_type = #{bizType} </if>
            <if test="keyword != null and keyword != ''">
              AND (biz_no LIKE CONCAT('%', #{keyword}, '%')
                   OR field_label LIKE CONCAT('%', #{keyword}, '%')
                   OR operator_name LIKE CONCAT('%', #{keyword}, '%')
                   OR old_value LIKE CONCAT('%', #{keyword}, '%')
                   OR new_value LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY id DESC
            </script>
            """)
    IPage<ChangeLog> selectPageList(IPage<ChangeLog> page,
                                    @Param("bizType") String bizType,
                                    @Param("keyword") String keyword);
}
