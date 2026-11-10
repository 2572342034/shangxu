package com.shangxu.crm.system.controller;

import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.log.OperLog;
import com.shangxu.crm.system.entity.SysUser;
import com.shangxu.crm.system.service.SysUserService;
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

@Tag(name = "系统管理-用户")
@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @GetMapping
    @PreAuthorize("@perm.has('system:user:list')")
    public R<PageResult<SysUser>> page(PageQuery query,
                                       @RequestParam(required = false) Long deptId,
                                       @RequestParam(required = false) Integer status) {
        return R.ok(userService.page(query, deptId, status));
    }

    /** 下拉选项：所有登录用户可见 */
    @GetMapping("/options")
    public R<List<SysUser>> options() {
        return R.ok(userService.options());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('system:user:list')")
    public R<SysUser> detail(@PathVariable Long id) {
        return R.ok(userService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('system:user:add')")
    @OperLog(title = "系统用户", type = "INSERT")
    public R<Long> create(@RequestBody SysUser user) {
        return R.ok("新增成功", userService.create(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('system:user:edit')")
    @OperLog(title = "系统用户", type = "UPDATE")
    public R<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        userService.update(user);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('system:user:delete')")
    @OperLog(title = "系统用户", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("@perm.has('system:user:reset')")
    @OperLog(title = "重置密码", type = "UPDATE")
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body) {
        userService.resetPassword(id, body == null ? null : body.get("password"));
        return R.ok("密码已重置", null);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@perm.has('system:user:edit')")
    @OperLog(title = "用户状态", type = "UPDATE")
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.changeStatus(id, status);
        return R.ok();
    }
}
