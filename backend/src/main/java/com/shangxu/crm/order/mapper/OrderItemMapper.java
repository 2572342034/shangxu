package com.shangxu.crm.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shangxu.crm.order.entity.OrderItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("SELECT * FROM crm_order_item WHERE order_id = #{orderId} ORDER BY id ASC")
    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);

    @Delete("DELETE FROM crm_order_item WHERE order_id = #{orderId}")
    int deleteByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT IFNULL(SUM(quantity), 0) FROM crm_order_item WHERE order_id = #{orderId}")
    BigDecimal sumQuantity(@Param("orderId") Long orderId);
}
