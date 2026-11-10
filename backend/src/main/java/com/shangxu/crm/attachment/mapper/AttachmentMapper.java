package com.shangxu.crm.attachment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shangxu.crm.attachment.entity.Attachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AttachmentMapper extends BaseMapper<Attachment> {

    @Select("""
            SELECT * FROM sys_attachment
            WHERE deleted = 0 AND biz_type = #{bizType} AND biz_id = #{bizId}
            ORDER BY id DESC
            """)
    List<Attachment> selectByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    @Select("SELECT COUNT(1) FROM sys_attachment WHERE deleted = 0 AND biz_type = #{bizType} AND biz_id = #{bizId}")
    long countByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);
}
