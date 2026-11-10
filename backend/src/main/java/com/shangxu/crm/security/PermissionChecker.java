package com.shangxu.crm.security;

import org.springframework.stereotype.Component;

/**
 * 权限校验器，配合 @PreAuthorize("@perm.has('xxx')") 使用
 */
@Component("perm")
public class PermissionChecker {

    public boolean has(String permission) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return loginUser != null && loginUser.hasPermission(permission);
    }

    public boolean hasAny(String... permissions) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            return false;
        }
        for (String permission : permissions) {
            if (loginUser.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }
}
