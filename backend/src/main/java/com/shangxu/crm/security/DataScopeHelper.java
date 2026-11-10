package com.shangxu.crm.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

/**
 * 数据权限拼接工具
 * <p>ALL：全部数据；DEPT：本部门数据；SELF：仅本人数据</p>
 */
public final class DataScopeHelper {

    private DataScopeHelper() {
    }

    public static <T> void apply(QueryWrapper<T> wrapper, String ownerColumn) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            wrapper.eq(ownerColumn, -1L);
            return;
        }
        String scope = loginUser.getDataScope();
        if ("ALL".equals(scope)) {
            return;
        }
        if ("DEPT".equals(scope) && loginUser.getDeptId() != null) {
            wrapper.inSql(ownerColumn,
                    "SELECT id FROM sys_user WHERE dept_id = " + loginUser.getDeptId() + " AND deleted = 0");
            return;
        }
        wrapper.eq(ownerColumn, loginUser.getId());
    }

    /**
     * 生成仪表盘统计用的过滤条件（配合 Mapper 的 <if> 使用）
     */
    public static Long scopeOwnerId() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            return -1L;
        }
        return "SELF".equals(loginUser.getDataScope()) ? loginUser.getId() : null;
    }

    public static Long scopeDeptId() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            return -1L;
        }
        return "DEPT".equals(loginUser.getDataScope()) ? loginUser.getDeptId() : null;
    }
}
