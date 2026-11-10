package com.shangxu.crm.common.sequence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SequenceMapper {

    @Insert("INSERT INTO sys_sequence(seq_key, current_val) VALUES(#{key}, 0) "
            + "ON DUPLICATE KEY UPDATE seq_key = seq_key")
    int ensure(@Param("key") String key);

    @Update("UPDATE sys_sequence SET current_val = current_val + 1 WHERE seq_key = #{key}")
    int increment(@Param("key") String key);

    @Select("SELECT current_val FROM sys_sequence WHERE seq_key = #{key}")
    long currentValue(@Param("key") String key);
}
