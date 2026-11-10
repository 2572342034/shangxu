package com.shangxu.crm.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shangxu.crm.invoice.entity.Invoice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface InvoiceMapper extends BaseMapper<Invoice> {

    @Select("""
            <script>
            SELECT i.*, u.nickname AS owner_name
            FROM crm_invoice i
            LEFT JOIN sys_user u ON u.id = i.owner_user_id
            WHERE i.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (i.invoice_no LIKE CONCAT('%', #{keyword}, '%') OR i.title LIKE CONCAT('%', #{keyword}, '%')
                   OR i.source_no LIKE CONCAT('%', #{keyword}, '%') OR i.customer_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null and status != ''"> AND i.status = #{status} </if>
            <if test="type != null and type != ''"> AND i.type = #{type} </if>
            <if test="ownerId != null"> AND i.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND i.dept_id = #{deptId} </if>
            ORDER BY i.id DESC
            </script>
            """)
    IPage<Invoice> selectPageList(IPage<Invoice> page,
                                  @Param("keyword") String keyword,
                                  @Param("status") String status,
                                  @Param("type") String type,
                                  @Param("ownerId") Long ownerId,
                                  @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT IFNULL(SUM(amount), 0) FROM crm_invoice
            WHERE deleted = 0 AND status = 'ISSUED'
            <if test="ownerId != null"> AND owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            </script>
            """)
    BigDecimal sumIssued(@Param("ownerId") Long ownerId, @Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT i.invoice_no AS no, CONCAT('开票 · ', IFNULL(i.source_no, '')) AS title,
                   i.customer_name AS customerName, i.amount AS amount,
                   IFNULL(i.invoice_date, i.create_time) AS time, u.nickname AS ownerName
            FROM crm_invoice i
            LEFT JOIN sys_user u ON u.id = i.owner_user_id
            WHERE i.deleted = 0 AND i.status = 'ISSUED'
            <if test="ownerId != null"> AND i.owner_user_id = #{ownerId} </if>
            <if test="deptId != null"> AND i.dept_id = #{deptId} </if>
            ORDER BY IFNULL(i.invoice_date, i.create_time) DESC
            LIMIT #{limit}
            </script>
            """)
    List<Map<String, Object>> selectRecentIssued(@Param("ownerId") Long ownerId,
                                                 @Param("deptId") Long deptId,
                                                 @Param("limit") int limit);

    /** 某来源单已开票金额（排除作废），用于防超开 */
    @Select("""
            SELECT IFNULL(SUM(amount), 0) FROM crm_invoice
            WHERE source_type = #{sourceType} AND source_id = #{sourceId}
              AND status <> 'VOID' AND deleted = 0
            """)
    BigDecimal sumIssuedBySource(@Param("sourceType") String sourceType, @Param("sourceId") Long sourceId);
}
