package com.shangxu.crm.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.product.entity.Product;
import com.shangxu.crm.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductCategoryService categoryService;
    private final com.shangxu.crm.common.sequence.SequenceService sequenceService;

    public PageResult<Product> page(PageQuery query, String category, Long categoryId, Integer status) {
        Page<Product> page = new Page<>(query.getPageNum(), query.getPageSize());
        List<Long> categoryIds = categoryId == null ? List.of() : categoryService.selfAndChildren(categoryId);
        IPage<Product> result = productMapper.selectPage(page, new LambdaQueryWrapper<Product>()
                .and(query.getKeyword() != null && !query.getKeyword().isBlank(), w -> w
                        .like(Product::getName, query.getKeyword())
                        .or().like(Product::getCode, query.getKeyword())
                        .or().like(Product::getSpec, query.getKeyword()))
                .eq(category != null && !category.isBlank(), Product::getCategory, category)
                .in(!categoryIds.isEmpty(), Product::getCategoryId, categoryIds)
                .eq(status != null, Product::getStatus, status)
                .orderByAsc(Product::getSort)
                .orderByDesc(Product::getId));
        return PageResult.of(result);
    }

    /**
     * 产品详情：基本信息 + 目录名 + 销售统计 + 最近订单明细。
     */
    public Map<String, Object> detail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException("产品不存在");
        }
        product.setCategoryName(categoryService.nameOf(product.getCategoryId()));
        Map<String, Object> stats = productMapper.selectSalesStats(id);
        if (stats != null) {
            product.setSaleQty((BigDecimal) stats.get("saleQty"));
            product.setSaleAmount((BigDecimal) stats.get("saleAmount"));
            product.setOrderCount(stats.get("orderCount") == null ? 0
                    : ((Number) stats.get("orderCount")).intValue());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("product", product);
        result.put("categoryName", product.getCategoryName());
        result.put("grossRate", grossRate(product.getPrice(), product.getCost()));
        result.put("recentOrders", productMapper.selectRecentOrders(id, 10));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(Product product) {
        validate(product, null);
        product.setId(null);
        // 编码留空时按序列自动生成：PRD + 4 位序号，与现有 PRD001~PRD008 同族
        if (product.getCode() == null || product.getCode().isBlank()) {
            product.setCode("PRD" + String.format("%04d", sequenceService.next("product")));
        }
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        if (product.getSort() == null) {
            product.setSort(0);
        }
        if (product.getTaxRate() == null) {
            product.setTaxRate(new BigDecimal("6.00"));
        }
        syncCategory(product);
        productMapper.insert(product);
        return product.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, Product product) {
        Product exist = productMapper.selectById(id);
        if (exist == null) {
            throw new BizException("产品不存在");
        }
        validate(product, id);
        product.setId(id);
        product.setVersion(exist.getVersion());
        // 编码不允许在编辑时清空
        if (product.getCode() == null || product.getCode().isBlank()) {
            product.setCode(exist.getCode());
        }
        syncCategory(product);
        if (productMapper.updateById(product) == 0) {
            throw new BizException("产品已被他人修改，请刷新后重试");
        }
    }

    public void changeStatus(Long id, Integer status) {
        Product exist = productMapper.selectById(id);
        if (exist == null) {
            throw new BizException("产品不存在");
        }
        Product update = new Product();
        update.setId(id);
        update.setVersion(exist.getVersion());
        update.setStatus(status == null ? 1 : status);
        if (productMapper.updateById(update) == 0) {
            throw new BizException("产品已被他人修改，请刷新后重试");
        }
    }

    public void delete(Long id) {
        Product exist = productMapper.selectById(id);
        if (exist == null) {
            throw new BizException("产品不存在");
        }
        if (productMapper.countOrderRefs(id) > 0) {
            throw new BizException("该产品已出现在订单明细中，不能删除；建议改为「停售」");
        }
        productMapper.deleteById(id);
    }

    private void validate(Product product, Long excludeId) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new BizException("请填写产品名称");
        }
        if (product.getPrice() != null && product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("销售价不能为负数");
        }
        if (product.getCost() != null && product.getCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("成本价不能为负数");
        }
        if (product.getMinPrice() != null && product.getPrice() != null
                && product.getMinPrice().compareTo(product.getPrice()) > 0) {
            throw new BizException("最低成交价不能高于销售价");
        }
        if (product.getCode() != null && !product.getCode().isBlank()) {
            Long count = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                    .eq(Product::getCode, product.getCode())
                    .ne(excludeId != null, Product::getId, excludeId));
            if (count != null && count > 0) {
                throw new BizException("产品编码已存在：" + product.getCode());
            }
        }
    }

    /** 目录 ID 与分类名称保持一致，兼容仍读 category 字段的下游 */
    private void syncCategory(Product product) {
        if (product.getCategoryId() == null) {
            return;
        }
        String name = categoryService.nameOf(product.getCategoryId());
        if (name != null) {
            product.setCategory(name);
        }
    }

    private BigDecimal grossRate(BigDecimal price, BigDecimal cost) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal c = cost == null ? BigDecimal.ZERO : cost;
        return price.subtract(c).multiply(new BigDecimal("100"))
                .divide(price, 1, RoundingMode.HALF_UP);
    }
}
