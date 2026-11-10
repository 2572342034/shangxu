package com.shangxu.crm.system.controller;

import com.shangxu.crm.common.R;
import com.shangxu.crm.log.OperLog;
import com.shangxu.crm.system.entity.SysDept;
import com.shangxu.crm.system.service.SysDeptService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "系统管理-部门")
@RestController
@RequestMapping("/api/system/depts")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    @GetMapping("/tree")
    public R<List<SysDept>> tree() {
        return R.ok(deptService.tree());
    }

    @GetMapping
    public R<List<SysDept>> list() {
        return R.ok(deptService.list());
    }

    @PostMapping
    @PreAuthorize("@perm.has('system:dept:add')")
    @OperLog(title = "部门管理", type = "INSERT")
    public R<Long> create(@RequestBody SysDept dept) {
        return R.ok("新增成功", deptService.create(dept));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('system:dept:edit')")
    @OperLog(title = "部门管理", type = "UPDATE")
    public R<Void> update(@PathVariable Long id, @RequestBody SysDept dept) {
        dept.setId(id);
        deptService.update(dept);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('system:dept:delete')")
    @OperLog(title = "部门管理", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok();
    }
}
