package com.shangxu.crm.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shangxu.crm.product.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {

    @Select("SELECT category_id AS categoryId, COUNT(1) AS cnt FROM crm_product "
            + "WHERE deleted = 0 AND category_id IS NOT NULL GROUP BY category_id")
    List<Map<String, Object>> countProductsGroupByCategory();

    @Select("SELECT COUNT(1) FROM crm_product WHERE deleted = 0 AND category_id = #{id}")
    long countProducts(@Param("id") Long id);

    @Select("SELECT COUNT(1) FROM crm_product_category WHERE deleted = 0 AND parent_id = #{id}")
    long countChildren(@Param("id") Long id);
}
