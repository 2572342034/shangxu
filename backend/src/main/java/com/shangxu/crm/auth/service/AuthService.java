package com.shangxu.crm.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shangxu.crm.auth.dto.LoginRequest;
import com.shangxu.crm.auth.dto.LoginResponse;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.security.JwtTokenProvider;
import com.shangxu.crm.security.LoginUser;
import com.shangxu.crm.security.SecurityUtils;
import com.shangxu.crm.security.TokenService;
import com.shangxu.crm.system.entity.SysLog;
import com.shangxu.crm.system.entity.SysUser;
import com.shangxu.crm.system.mapper.SysUserMapper;
import com.shangxu.crm.system.service.SysLogService;
import com.shangxu.crm.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final SysUserMapper userMapper;
    private final SysUserService userService;
    private final SysLogService sysLogService;
    private final PasswordEncoder passwordEncoder;
    private final com.shangxu.crm.security.UserDetailsServiceImpl userDetailsService;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException e) {
            log.warn("登录失败: {}", e.getMessage(), e);
            recordLoginLog(request.username(), 0, "账号或密码错误");
            throw new BizException(401, "账号或密码错误");
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = jwtTokenProvider.createToken(loginUser.getId(), loginUser.getUsername());
        tokenService.save(loginUser);
        userService.updateLoginTime(loginUser.getId());
        recordLoginLog(loginUser.getNickname(), 1, null);
        return new LoginResponse(token, jwtTokenProvider.getExpire(), loginUser.getId(), loginUser.getUsername(),
                loginUser.getNickname(), loginUser.getAvatar(), loginUser.getDeptName(),
                loginUser.getRoles(), loginUser.getPermissions());
    }

    public LoginUser info() {
        LoginUser current = SecurityUtils.requireLoginUser();
        // 从数据库刷新，保证角色/权限变更后及时生效
        SysUser user = userMapper.selectById(current.getId());
        if (user == null) {
            throw new BizException(401, "账号不存在");
        }
        LoginUser loginUser = userDetailsService.buildLoginUser(user);
        tokenService.refresh(loginUser);
        return loginUser;
    }

    public void logout() {
        Long userId = SecurityUtils.getUserId();
        if (userId != null) {
            tokenService.remove(userId);
        }
    }

    public void changePassword(String oldPassword, String newPassword) {
        LoginUser loginUser = SecurityUtils.requireLoginUser();
        SysUser user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            throw new BizException("账号不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BizException("新密码长度不能少于 6 位");
        }
        userService.resetPassword(user.getId(), newPassword);
        tokenService.remove(user.getId());
    }

    public void updateProfile(String nickname, String mobile, String email, String avatar) {
        Long userId = SecurityUtils.getUserId();
        SysUser user = new SysUser();
        user.setId(userId);
        user.setNickname(nickname);
        user.setMobile(mobile);
        user.setEmail(email);
        user.setAvatar(avatar);
        userMapper.updateById(user);
        LoginUser loginUser = SecurityUtils.requireLoginUser();
        loginUser.setNickname(nickname);
        loginUser.setMobile(mobile);
        loginUser.setAvatar(avatar);
        tokenService.refresh(loginUser);
    }

    private void recordLoginLog(String operName, int status, String errorMsg) {
        SysLog sysLog = new SysLog();
        sysLog.setTitle("用户登录");
        sysLog.setBusinessType("LOGIN");
        sysLog.setRequestMethod("POST");
        sysLog.setOperName(operName);
        sysLog.setOperUrl("/api/auth/login");
        sysLog.setStatus(status);
        sysLog.setErrorMsg(errorMsg);
        sysLog.setCostTime(0L);
        sysLog.setOperTime(LocalDateTime.now());
        sysLogService.saveAsync(sysLog);
    }
}
