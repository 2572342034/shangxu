package com.shangxu.crm.changelog.aspect;

import com.shangxu.crm.changelog.service.ChangeLogService;
import com.shangxu.crm.log.OperLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用字段级修改记录切面。
 * 拦截所有带 @OperLog 且类型为 INSERT/UPDATE/DELETE 的 Controller 方法，
 * 用 JdbcTemplate 取更新前后的行快照做 diff，业务代码零侵入即可覆盖全部核心模块。
 * 记录失败只告警，不影响主流程。
 */
@Slf4j
@Aspect
@Component
@Order(30)
@RequiredArgsConstructor
public class ChangeLogAspect {

    private final JdbcTemplate jdbcTemplate;
    private final ChangeLogService changeLogService;

    private static final Pattern URI = Pattern.compile("^/api/([A-Za-z0-9-]+)/(\\d+)(?:/.*)?$");
    private static final Pattern INSERT_URI = Pattern.compile("^/api/([A-Za-z0-9-]+)/?$");

    /** URI 模块段 -> {业务类型, 表名} */
    private static final Map<String, String[]> MODULES = new LinkedHashMap<>();
    static {
        MODULES.put("customers", new String[]{"CUSTOMER", "crm_customer"});
        MODULES.put("contacts", new String[]{"CONTACT", "crm_contact"});
        MODULES.put("leads", new String[]{"LEAD", "crm_lead"});
        MODULES.put("opportunities", new String[]{"OPPORTUNITY", "crm_opportunity"});
        MODULES.put("contracts", new String[]{"CONTRACT", "crm_contract"});
        MODULES.put("orders", new String[]{"ORDER", "crm_order"});
        MODULES.put("deliveries", new String[]{"DELIVERY", "crm_delivery"});
        MODULES.put("delivery-plans", new String[]{"DELIVERY_PLAN", "crm_delivery_plan"});
        MODULES.put("invoices", new String[]{"INVOICE", "crm_invoice"});
        MODULES.put("receivables", new String[]{"RECEIVABLE", "crm_receivable"});
        MODULES.put("payments", new String[]{"PAYMENT", "crm_payment_record"});
        MODULES.put("returns", new String[]{"RETURN", "crm_return"});
        MODULES.put("products", new String[]{"PRODUCT", "crm_product"});
        MODULES.put("product-categories", new String[]{"PRODUCT_CATEGORY", "crm_product_category"});
        MODULES.put("activities", new String[]{"ACTIVITY", "crm_activity"});
        MODULES.put("attachments", new String[]{"ATTACHMENT", "sys_attachment"});
    }

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        String action = operLog.type();
        boolean tracked = "UPDATE".equals(action) || "DELETE".equals(action) || "INSERT".equals(action);
        if (!tracked) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }
        String uri = request.getRequestURI();
        String[] target = resolve(uri);
        if (target == null) {
            return joinPoint.proceed();
        }

        Matcher m = URI.matcher(uri);
        Long bizId = m.matches() ? Long.valueOf(m.group(2)) : null;
        String bizType = target[0];
        String table = target[1];

        Map<String, Object> before = bizId == null
                ? Collections.emptyMap() : snapshot(table, bizId);

        Object result = joinPoint.proceed();

        Long id = bizId;
        if (id == null && "INSERT".equals(action)) {
            id = extractId(result);
        }
        if (id == null) {
            return result;
        }
        try {
            Map<String, Object> after = "DELETE".equals(action)
                    ? Collections.emptyMap() : snapshot(table, id);
            changeLogService.diffAndSave(bizType, id,
                    "INSERT".equals(action) ? "CREATE" : action, before, after, operLog.title());
        } catch (Exception e) {
            log.warn("记录变更轨迹失败: {} {} id={}", bizType, action, id, e);
        }
        return result;
    }

    private String[] resolve(String uri) {
        Matcher m = URI.matcher(uri);
        if (m.matches()) {
            return MODULES.get(m.group(1));
        }
        Matcher ins = INSERT_URI.matcher(uri);
        if (ins.matches()) {
            return MODULES.get(ins.group(1));
        }
        return null;
    }

    private Map<String, Object> snapshot(String table, Long id) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT * FROM `" + table + "` WHERE id = ?", id);
            Map<String, Object> normalized = new LinkedHashMap<>();
            row.forEach((k, v) -> normalized.put(k.toLowerCase(), v));
            return normalized;
        } catch (EmptyResultDataAccessException e) {
            return Collections.emptyMap();
        } catch (Exception e) {
            log.debug("快照读取失败: {} id={}", table, id);
            return Collections.emptyMap();
        }
    }

    private Long extractId(Object result) {
        try {
            if (result == null) {
                return null;
            }
            Object data = result.getClass().getMethod("getData").invoke(result);
            return data instanceof Number n ? n.longValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes s ? s.getRequest() : null;
    }
}
