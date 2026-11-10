package com.shangxu.crm.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 登录用户上下文（同时写入 Redis 缓存）
 */
@Data
public class LoginUser implements UserDetails, Serializable {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String mobile;
    private Long deptId;
    private String deptName;
    private String post;
    /** 数据权限范围：ALL / DEPT / SELF */
    private String dataScope;
    /** 权限标识，如 crm:customer:add */
    private List<String> permissions = new ArrayList<>();
    /** 角色编码 */
    private List<String> roles = new ArrayList<>();
    /** 角色名称 */
    private List<String> roleNames = new ArrayList<>();

    public boolean hasPermission(String permission) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        return permissions.contains("*:*:*") || permissions.contains(permission);
    }

    public boolean isAdmin() {
        return permissions != null && permissions.contains("*:*:*");
    }

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> list = new ArrayList<>();
        if (permissions != null) {
            permissions.forEach(p -> list.add(new SimpleGrantedAuthority(p)));
        }
        return list;
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return true;
    }
}
