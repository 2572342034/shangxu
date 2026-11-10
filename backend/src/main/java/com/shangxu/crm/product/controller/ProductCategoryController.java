package com.shangxu.crm.product.controller;

import com.shangxu.crm.common.R;
import com.shangxu.crm.log.OperLog;
import com.shangxu.crm.product.entity.ProductCategory;
import com.shangxu.crm.product.service.ProductCategoryService;
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

import java.util.List;
import java.util.Map;

@Tag(name = "产品目录")
@RestController
@RequestMapping("/api/product-categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService categoryService;

    @Operation(summary = "目录树（含各节点产品数）")
    @GetMapping("/tree")
    @PreAuthorize("@perm.has('crm:product:list')")
    public R<List<Map<String, Object>>> tree() {
        return R.ok(categoryService.tree());
    }

    @PostMapping
    @PreAuthorize("@perm.has('crm:product:add')")
    @OperLog(title = "产品目录", type = "INSERT")
    public R<Long> create(@RequestBody ProductCategory category) {
        return R.ok("分类创建成功", categoryService.create(category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('crm:product:edit')")
    @OperLog(title = "产品目录", type = "UPDATE")
    public R<Void> update(@PathVariable Long id, @RequestBody ProductCategory category) {
        categoryService.update(id, category);
        return R.ok();
    }

    @Operation(summary = "启用 / 停用")
    @PutMapping("/{id}/status")
    @PreAuthorize("@perm.has('crm:product:edit')")
    @OperLog(title = "产品目录", type = "UPDATE")
    public R<Void> status(@PathVariable Long id, @RequestParam Integer status) {
        categoryService.changeStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('crm:product:delete')")
    @OperLog(title = "产品目录", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }
}
