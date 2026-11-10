package com.shangxu.crm.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shangxu.crm.notice.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {

    @Select("""
            <script>
            SELECT * FROM sys_notice
            WHERE deleted = 0 AND user_id = #{userId}
            <if test="onlyUnread"> AND read_flag = 0 </if>
            ORDER BY id DESC
            LIMIT #{limit}
            </script>
            """)
    List<Notice> selectMine(@Param("userId") Long userId,
                            @Param("onlyUnread") boolean onlyUnread,
                            @Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM sys_notice WHERE deleted = 0 AND user_id = #{userId} AND read_flag = 0")
    long countUnread(@Param("userId") Long userId);

    @Update("UPDATE sys_notice SET read_flag = 1, read_time = NOW() WHERE id = #{id} AND user_id = #{userId} AND read_flag = 0")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE sys_notice SET read_flag = 1, read_time = NOW() WHERE user_id = #{userId} AND read_flag = 0")
    int markAllRead(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(1) FROM sys_notice
            WHERE deleted = 0 AND user_id = #{userId} AND type = #{type}
              AND IFNULL(biz_no, '') = IFNULL(#{bizNo}, '')
              AND DATE(create_time) = CURDATE()
            """)
    long countToday(@Param("userId") Long userId, @Param("type") String type, @Param("bizNo") String bizNo);
}
