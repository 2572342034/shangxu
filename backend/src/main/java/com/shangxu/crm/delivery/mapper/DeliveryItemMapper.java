package com.shangxu.crm.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shangxu.crm.delivery.entity.DeliveryItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeliveryItemMapper extends BaseMapper<DeliveryItem> {

    @Select("SELECT * FROM crm_delivery_item WHERE delivery_id = #{deliveryId} ORDER BY id ASC")
    List<DeliveryItem> selectByDeliveryId(@Param("deliveryId") Long deliveryId);

    @Delete("DELETE FROM crm_delivery_item WHERE delivery_id = #{deliveryId}")
    int deleteByDeliveryId(@Param("deliveryId") Long deliveryId);
}
