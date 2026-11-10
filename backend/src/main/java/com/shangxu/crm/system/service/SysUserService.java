package com.shangxu.crm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.system.entity.SysDept;
import com.shangxu.crm.system.entity.SysUser;
import com.shangxu.crm.system.entity.SysUserRole;
import com.shangxu.crm.system.mapper.SysDeptMapper;
import com.shangxu.crm.system.mapper.SysUserMapper;
import com.shangxu.crm.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<SysUser> page(PageQuery query, Long deptId, Integer status) {
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<SysUser> result = userMapper.selectUserPage(page, query.getKeyword(), deptId, status);
        result.getRecords().forEach(user -> {
            user.setRoleIds(userMapper.selectRoleIdsByUserId(user.getId()));
            user.setRoleNames(userMapper.selectRolesByUserId(user.getId())
                    .stream().map(com.shangxu.crm.system.entity.SysRole::getName).toList());
        });
        return PageResult.of(result);
    }

    public SysUser detail(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setRoleIds(userMapper.selectRoleIdsByUserId(id));
        user.setRoleNames(userMapper.selectRolesByUserId(id)
                .stream().map(com.shangxu.crm.system.entity.SysRole::getName).toList());
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            user.setDeptName(dept == null ? null : dept.getName());
        }
        return user;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(SysUser user) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername()));
        if (count != null && count > 0) {
            throw new BizException("登录账号已存在");
        }
        user.setId(null);
        String rawPassword = user.getPassword() == null || user.getPassword().isBlank()
                ? DEFAULT_PASSWORD : user.getPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userMapper.insert(user);
        saveRoles(user.getId(), user.getRoleIds());
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysUser user) {
        SysUser exist = userMapper.selectById(user.getId());
        if (exist == null) {
            throw new BizException("用户不存在");
        }
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        user.setUsername(null);
        userMapper.updateById(user);
        if (user.getRoleIds() != null) {
            userRoleMapper.deleteByUserId(user.getId());
            saveRoles(user.getId(), user.getRoleIds());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id != null && id == 1L) {
            throw new BizException("超级管理员账号不允许删除");
        }
        userMapper.deleteById(id);
        userRoleMapper.deleteByUserId(id);
    }

    public void resetPassword(Long id, String password) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(password == null || password.isBlank() ? DEFAULT_PASSWORD : password));
        userMapper.updateById(user);
    }

    public void changeStatus(Long id, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public List<SysUser> options() {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .select(SysUser::getId, SysUser::getNickname, SysUser::getUsername, SysUser::getDeptId)
                .orderByAsc(SysUser::getId));
        Map<Long, String> deptNames = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName));
        users.forEach(u -> u.setDeptName(deptNames.get(u.getDeptId())));
        return users;
    }

    public void updateLoginTime(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setLastLoginTime(java.time.LocalDateTime.now());
        userMapper.updateById(user);
    }

    private void saveRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        roleIds.stream().distinct().forEach(roleId ->
                userRoleMapper.insert(new SysUserRole(userId, roleId)));
    }
}
