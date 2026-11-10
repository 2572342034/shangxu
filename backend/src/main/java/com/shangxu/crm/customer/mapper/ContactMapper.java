package com.shangxu.crm.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shangxu.crm.customer.entity.Contact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ContactMapper extends BaseMapper<Contact> {

    @Select("""
            <script>
            SELECT ct.*, c.name AS customer_name
            FROM crm_contact ct
            LEFT JOIN crm_customer c ON c.id = ct.customer_id AND c.deleted = 0
            WHERE ct.deleted = 0
            <if test="customerId != null"> AND ct.customer_id = #{customerId} </if>
            <if test="keyword != null and keyword != ''">
              AND (ct.name LIKE CONCAT('%', #{keyword}, '%') OR ct.mobile LIKE CONCAT('%', #{keyword}, '%')
                   OR c.name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="ownerId != null"> AND c.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND c.dept_id = #{deptId} </if>
            ORDER BY ct.is_primary DESC, ct.id DESC
            </script>
            """)
    IPage<Contact> selectPageList(IPage<Contact> page,
                                  @Param("customerId") Long customerId,
                                  @Param("keyword") String keyword,
                                  @Param("ownerId") Long ownerId,
                                  @Param("deptId") Long deptId);
}
