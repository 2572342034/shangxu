package com.shangxu.crm.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shangxu.crm.security.SecurityUtils;
import com.shangxu.crm.system.entity.SysLog;
import com.shangxu.crm.system.service.SysLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final SysLogService sysLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        SysLog entity = new SysLog();
        entity.setTitle(operLog.title());
        entity.setBusinessType(operLog.type());
        entity.setMethod(joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());
        entity.setOperName(SecurityUtils.getNickname() == null ? "匿名" : SecurityUtils.getNickname());
        entity.setOperTime(LocalDateTime.now());

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            entity.setRequestMethod(request.getMethod());
            entity.setOperUrl(request.getRequestURI());
            entity.setOperIp(clientIp(request));
        }
        entity.setOperParam(parseParams(joinPoint.getArgs()));

        try {
            Object result = joinPoint.proceed();
            entity.setStatus(1);
            return result;
        } catch (Throwable throwable) {
            entity.setStatus(0);
            String message = throwable.getMessage();
            entity.setErrorMsg(message == null ? throwable.getClass().getSimpleName() : truncate(message, 1000));
            throw throwable;
        } finally {
            entity.setCostTime(System.currentTimeMillis() - start);
            sysLogService.saveAsync(entity);
        }
    }

    private String parseParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            Object[] filtered = Arrays.stream(args)
                    .filter(arg -> !(arg instanceof HttpServletRequest)
                            && !(arg instanceof HttpServletResponse)
                            && !(arg instanceof MultipartFile))
                    .toArray();
            return truncate(objectMapper.writeValueAsString(filtered), 2000);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
