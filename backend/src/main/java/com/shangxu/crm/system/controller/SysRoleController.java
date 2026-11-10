package com.shangxu.crm.system.controller;

import com.shangxu.crm.common.R;
import com.shangxu.crm.log.OperLog;
import com.shangxu.crm.system.entity.SysRole;
import com.shangxu.crm.system.service.SysRoleService;
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

@Tag(name = "系统管理-角色")
@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @GetMapping
    public R<List<SysRole>> list(@RequestParam(required = false) String keyword) {
        return R.ok(roleService.list(keyword));
    }

    @PostMapping
    @PreAuthorize("@perm.has('system:role:add')")
    @OperLog(title = "系统角色", type = "INSERT")
    public R<Long> create(@RequestBody SysRole role) {
        return R.ok("新增成功", roleService.create(role));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('system:role:edit')")
    @OperLog(title = "系统角色", type = "UPDATE")
    public R<Void> update(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        roleService.update(role);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('system:role:delete')")
    @OperLog(title = "系统角色", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }
}
