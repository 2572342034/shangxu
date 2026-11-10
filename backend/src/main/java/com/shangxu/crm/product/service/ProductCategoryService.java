package com.shangxu.crm.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.product.entity.ProductCategory;
import com.shangxu.crm.product.mapper.ProductCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 产品目录（树形分类）。
 */
@Service
@RequiredArgsConstructor
public class ProductCategoryService {

    private final ProductCategoryMapper categoryMapper;

    /** 带产品数量的目录树；产品数量同时汇总到父级 */
    public List<Map<String, Object>> tree() {
        List<ProductCategory> all = categoryMapper.selectList(
                Wrappers.<ProductCategory>lambdaQuery().orderByAsc(ProductCategory::getSort, ProductCategory::getId));

        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Map<String, Object> row : categoryMapper.countProductsGroupByCategory()) {
            counts.put(((Number) row.get("categoryId")).longValue(), ((Number) row.get("cnt")).longValue());
        }

        Map<Long, List<Map<String, Object>>> childrenMap = new LinkedHashMap<>();
        for (ProductCategory category : all) {
            childrenMap.computeIfAbsent(category.getParentId(), k -> new ArrayList<>());
        }
        Map<Long, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        for (ProductCategory category : all) {
            Map<String, Object> node = toNode(category, counts.getOrDefault(category.getId(), 0L));
            nodeMap.put(category.getId(), node);
        }
        for (ProductCategory category : all) {
            if (category.getParentId() != null && category.getParentId() != 0
                    && childrenMap.containsKey(category.getParentId())) {
                childrenMap.get(category.getParentId()).add(nodeMap.get(category.getId()));
            }
        }

        List<Map<String, Object>> roots = new ArrayList<>();
        for (ProductCategory category : all) {
            if (category.getParentId() == null || category.getParentId() == 0) {
                Map<String, Object> node = nodeMap.get(category.getId());
                List<Map<String, Object>> children = childrenMap.getOrDefault(category.getId(), new ArrayList<>());
                node.put("children", children);
                long total = 0;
                for (Map<String, Object> child : children) {
                    total += ((Number) child.get("productCount")).longValue();
                }
                long own = counts.getOrDefault(category.getId(), 0L);
                node.put("productCount", total + own);
                roots.add(node);
            }
        }
        return roots;
    }

    private Map<String, Object> toNode(ProductCategory category, long productCount) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", category.getId());
        node.put("parentId", category.getParentId());
        node.put("name", category.getName());
        node.put("code", category.getCode());
        node.put("sort", category.getSort());
        node.put("status", category.getStatus());
        node.put("remark", category.getRemark());
        node.put("productCount", productCount);
        node.put("children", new ArrayList<>());
        return node;
    }

    public Long create(ProductCategory category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new BizException("请填写分类名称");
        }
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        long count = categoryMapper.selectCount(Wrappers.<ProductCategory>lambdaQuery()
                .eq(ProductCategory::getName, category.getName())
                .eq(ProductCategory::getParentId, category.getParentId()));
        if (count > 0) {
            throw new BizException("同级下已存在同名分类");
        }
        category.setId(null);
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        categoryMapper.insert(category);
        return category.getId();
    }

    public void update(Long id, ProductCategory category) {
        ProductCategory exist = categoryMapper.selectById(id);
        if (exist == null) {
            throw new BizException("分类不存在");
        }
        if (category.getParentId() != null && category.getParentId().equals(id)) {
            throw new BizException("上级分类不能是自己");
        }
        category.setId(id);
        category.setVersion(exist.getVersion());
        categoryMapper.updateById(category);
    }

    public void delete(Long id) {
        ProductCategory exist = categoryMapper.selectById(id);
        if (exist == null) {
            throw new BizException("分类不存在");
        }
        if (categoryMapper.countChildren(id) > 0) {
            throw new BizException("请先删除子分类");
        }
        if (categoryMapper.countProducts(id) > 0) {
            throw new BizException("该分类下还有产品，请先移走产品");
        }
        categoryMapper.deleteById(id);
    }

    public void changeStatus(Long id, Integer status) {
        ProductCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException("分类不存在");
        }
        ProductCategory update = new ProductCategory();
        update.setId(id);
        update.setVersion(category.getVersion());
        update.setStatus(status == null ? 1 : status);
        categoryMapper.updateById(update);
    }

    /** 取某分类及其所有子分类 ID，用于列表筛选 */
    public List<Long> selfAndChildren(Long categoryId) {
        List<Long> result = new ArrayList<>();
        if (categoryId == null) {
            return result;
        }
        result.add(categoryId);
        List<ProductCategory> children = categoryMapper.selectList(Wrappers.<ProductCategory>lambdaQuery()
                .eq(ProductCategory::getParentId, categoryId));
        for (ProductCategory child : children) {
            result.add(child.getId());
        }
        return result;
    }

    public String nameOf(Long id) {
        if (id == null) {
            return null;
        }
        ProductCategory category = categoryMapper.selectById(id);
        return category == null ? null : category.getName();
    }

    /** 按分类名称找 ID（用于 CSV 导入时按名称匹配目录） */
    public Long idOfName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        List<ProductCategory> list = categoryMapper.selectList(Wrappers.<ProductCategory>lambdaQuery()
                .eq(ProductCategory::getName, name.trim())
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0).getId();
    }
}
