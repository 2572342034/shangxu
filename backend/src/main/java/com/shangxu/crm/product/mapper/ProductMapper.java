package com.shangxu.crm.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shangxu.crm.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select("""
            SELECT IFNULL(COUNT(DISTINCT oi.order_id), 0) AS orderCount,
                   IFNULL(SUM(oi.quantity), 0) AS saleQty,
                   IFNULL(SUM(oi.amount), 0) AS saleAmount
            FROM crm_order_item oi
            WHERE oi.product_id = #{productId}
            """)
    Map<String, Object> selectSalesStats(@Param("productId") Long productId);

    @Select("""
            SELECT oi.order_id AS orderId, o.order_no AS orderNo, o.customer_name AS customerName,
                   oi.quantity AS quantity, oi.price AS price, oi.amount AS amount,
                   o.order_date AS orderDate, o.status AS orderStatus
            FROM crm_order_item oi
            LEFT JOIN crm_order o ON o.id = oi.order_id
            WHERE oi.product_id = #{productId}
            ORDER BY oi.id DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> selectRecentOrders(@Param("productId") Long productId, @Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM crm_order_item WHERE product_id = #{productId}")
    long countOrderRefs(@Param("productId") Long productId);
}
