package com.shangxu.crm.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.system.entity.SysDept;
import com.shangxu.crm.system.entity.SysRole;
import com.shangxu.crm.system.entity.SysUser;
import com.shangxu.crm.system.mapper.SysDeptMapper;
import com.shangxu.crm.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 登录用户加载：用户 + 角色 + 权限 + 数据权限
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final ObjectMapper objectMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(403, "账号已被停用，请联系管理员");
        }
        return buildLoginUser(user);
    }

    public LoginUser buildLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setNickname(user.getNickname());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setMobile(user.getMobile());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setPost(user.getPost());

        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            loginUser.setDeptName(dept == null ? null : dept.getName());
        }

        List<SysRole> roles = userMapper.selectRolesByUserId(user.getId());
        Set<String> permissions = new LinkedHashSet<>();
        List<String> roleCodes = new ArrayList<>();
        List<String> roleNames = new ArrayList<>();
        String dataScope = "SELF";
        for (SysRole role : roles) {
            roleCodes.add(role.getCode());
            roleNames.add(role.getName());
            if ("ALL".equals(role.getDataScope())) {
                dataScope = "ALL";
            } else if ("DEPT".equals(role.getDataScope()) && !"ALL".equals(dataScope)) {
                dataScope = "DEPT";
            }
            if (role.getPermissions() != null && !role.getPermissions().isBlank()) {
                try {
                    List<String> perms = objectMapper.readValue(role.getPermissions(), new TypeReference<List<String>>() {
                    });
                    permissions.addAll(perms);
                } catch (Exception e) {
                    log.warn("角色 {} 权限配置解析失败", role.getCode(), e);
                }
            }
        }
        loginUser.setPermissions(new ArrayList<>(permissions));
        loginUser.setRoles(roleCodes);
        loginUser.setRoleNames(roleNames);
        loginUser.setDataScope(dataScope);
        return loginUser;
    }
}
