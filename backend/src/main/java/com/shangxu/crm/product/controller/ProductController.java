package com.shangxu.crm.product.controller;

import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.log.OperLog;
import com.shangxu.crm.product.entity.Product;
import com.shangxu.crm.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "产品管理")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("@perm.has('crm:product:list')")
    public R<PageResult<Product>> page(PageQuery query,
                                       @RequestParam(required = false) String category,
                                       @RequestParam(required = false) Long categoryId,
                                       @RequestParam(required = false) Integer status) {
        return R.ok(productService.page(query, category, categoryId, status));
    }

    @Operation(summary = "产品详情（含销售统计与最近订单）")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('crm:product:list')")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(productService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('crm:product:add')")
    @OperLog(title = "产品管理", type = "INSERT")
    public R<Long> create(@RequestBody Product product) {
        return R.ok("新增成功", productService.create(product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('crm:product:edit')")
    @OperLog(title = "产品管理", type = "UPDATE")
    public R<Void> update(@PathVariable Long id, @RequestBody Product product) {
        productService.update(id, product);
        return R.ok();
    }

    @Operation(summary = "上架 / 停售")
    @PutMapping("/{id}/status")
    @PreAuthorize("@perm.has('crm:product:edit')")
    @OperLog(title = "产品上下架", type = "UPDATE")
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        productService.changeStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('crm:product:delete')")
    @OperLog(title = "产品管理", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return R.ok();
    }
}
