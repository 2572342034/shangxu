package com.shangxu.crm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.system.entity.SysRole;
import com.shangxu.crm.system.mapper.SysRoleMapper;
import com.shangxu.crm.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final ObjectMapper objectMapper;

    public List<SysRole> list(String keyword) {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .like(keyword != null && !keyword.isBlank(), SysRole::getName, keyword)
                .orderByAsc(SysRole::getSort));
        roles.forEach(this::fillPermissionList);
        return roles;
    }

    public Long create(SysRole role) {
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, role.getCode()));
        if (count != null && count > 0) {
            throw new BizException("角色编码已存在");
        }
        role.setId(null);
        role.setPermissions(writePermissions(role));
        roleMapper.insert(role);
        return role.getId();
    }

    public void update(SysRole role) {
        if (role.getId() != null && role.getId() == 1L) {
            throw new BizException("超级管理员角色不允许修改");
        }
        role.setPermissions(writePermissions(role));
        roleMapper.updateById(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id != null && id == 1L) {
            throw new BizException("超级管理员角色不允许删除");
        }
        long userCount = roleMapper.countUserByRoleId(id);
        if (userCount > 0) {
            throw new BizException("该角色下还有 " + userCount + " 个用户，无法删除");
        }
        roleMapper.deleteById(id);
        userRoleMapper.deleteByRoleId(id);
    }

    private void fillPermissionList(SysRole role) {
        if (role.getPermissions() == null || role.getPermissions().isBlank()) {
            role.setPermissionList(List.of());
            return;
        }
        try {
            role.setPermissionList(objectMapper.readValue(role.getPermissions(), new TypeReference<List<String>>() {
            }));
        } catch (Exception e) {
            log.warn("角色 {} 权限解析失败", role.getCode(), e);
            role.setPermissionList(List.of());
        }
    }

    private String writePermissions(SysRole role) {
        if (role.getPermissionList() == null) {
            return role.getPermissions();
        }
        try {
            return objectMapper.writeValueAsString(role.getPermissionList());
        } catch (Exception e) {
            throw new BizException("权限数据格式错误");
        }
    }
}
