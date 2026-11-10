package com.shangxu.crm.common.dict;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 统一「码值 → 中文」显示服务。
 * 数据来源：sys_dict 数据字典（运行时加载、带缓存）+ 内置字典（未入库的枚举，如性别、布尔、收款方式）。
 * 用途：修改记录展示、CSV 导出展示、CSV 导入把中文反向解析回码值。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictDisplayService {

    private final JdbcTemplate jdbcTemplate;

    private static final long TTL = 60_000L;
    private volatile Map<String, String> labels = Map.of();
    private volatile Map<String, String> codes = Map.of();
    private volatile long loadedAt = 0L;

    /** 数据库里没有、但系统里在用的枚举 */
    private static final Map<String, Map<String, String>> BUILTIN = Map.of(
            "gender", Map.of("MALE", "男", "FEMALE", "女", "UNKNOWN", "未知"),
            "boolean", Map.of("0", "否", "1", "是"),
            "product_status", Map.of("0", "停售", "1", "在售"),
            "payment_method", Map.of("BANK", "银行转账", "ACCEPTANCE", "银行承兑",
                    "ONLINE", "在线支付", "CASH", "现金", "OTHER", "其他"),
            "payment_record_status", Map.of("CONFIRMED", "已确认", "CANCELLED", "已作废"),
            "data_scope", Map.of("ALL", "全部数据", "DEPT", "本部门", "SELF", "仅本人"),
            "customer_source", Map.of("IMPORT", "批量导入"),
            "biz_type", Map.ofEntries(
                    Map.entry("CUSTOMER", "客户"), Map.entry("CONTACT", "联系人"),
                    Map.entry("LEAD", "线索"), Map.entry("OPPORTUNITY", "商机"),
                    Map.entry("CONTRACT", "合同"), Map.entry("ORDER", "订单"),
                    Map.entry("DELIVERY", "出库单"), Map.entry("DELIVERY_PLAN", "发货计划"),
                    Map.entry("INVOICE", "发票"), Map.entry("RECEIVABLE", "应收单"),
                    Map.entry("PAYMENT", "收款单"), Map.entry("RETURN", "退货单"),
                    Map.entry("PRODUCT", "产品"), Map.entry("PRODUCT_CATEGORY", "产品目录"),
                    Map.entry("ACTIVITY", "跟进记录"), Map.entry("ATTACHMENT", "附件"))
    );

    /** 跨模块通用：字段名 → 字典类型 */
    private static final Map<String, String> FIELD_DICT = Map.ofEntries(
            Map.entry("gender", "gender"),
            Map.entry("is_primary", "boolean"),
            Map.entry("pool_type", "pool_type"),
            Map.entry("level", "customer_level"),
            Map.entry("industry", "industry"),
            Map.entry("scale", "customer_scale"),
            Map.entry("source", "customer_source"),
            Map.entry("stage", "opp_stage"),
            Map.entry("source_type", "receivable_source"),
            Map.entry("method", "payment_method"),
            Map.entry("data_scope", "data_scope"),
            Map.entry("biz_type", "biz_type"));

    /** 精确：业务类型 + 字段名 → 字典类型（status/type 在不同模块含义不同，必须区分） */
    private static final Map<String, String> BIZ_FIELD_DICT = Map.ofEntries(
            Map.entry("CUSTOMER.status", "customer_status"),
            Map.entry("LEAD.status", "lead_status"),
            Map.entry("CONTRACT.status", "contract_status"),
            Map.entry("ORDER.status", "order_status"),
            Map.entry("DELIVERY.status", "delivery_status"),
            Map.entry("DELIVERY_PLAN.status", "plan_status"),
            Map.entry("INVOICE.status", "invoice_status"),
            Map.entry("INVOICE.type", "invoice_type"),
            Map.entry("RECEIVABLE.status", "receivable_status"),
            Map.entry("RETURN.status", "return_status"),
            Map.entry("PAYMENT.status", "payment_record_status"),
            Map.entry("PAYMENT.method", "payment_method"),
            Map.entry("OPPORTUNITY.stage", "opp_stage"),
            Map.entry("OPPORTUNITY.status", "opp_stage"),
            Map.entry("ACTIVITY.type", "activity_type"),
            Map.entry("PRODUCT.status", "product_status"),
            Map.entry("CONTACT.gender", "gender"),
            Map.entry("CONTACT.is_primary", "boolean"));

    /** 码值兜底：只用于 sys_dict 与内置字典都没命中时 */
    private static final Map<String, String> FALLBACK = Map.ofEntries(
            Map.entry("PENDING", "待处理"), Map.entry("APPROVED", "已通过"),
            Map.entry("REJECTED", "已驳回"), Map.entry("CANCELLED", "已作废"),
            Map.entry("CONFIRMED", "已确认"), Map.entry("PAID", "已结清"),
            Map.entry("UNPAID", "未收款"), Map.entry("PART", "部分收款"),
            Map.entry("FOLLOWING", "跟进中"), Map.entry("DEAL", "已成交"),
            Map.entry("INVALID", "无效"), Map.entry("DRAFT", "草稿"),
            Map.entry("ISSUED", "已开具"), Map.entry("SHIPPED", "已出库"),
            Map.entry("REFUNDED", "已退款"), Map.entry("SIGNED", "已签收"),
            Map.entry("VOID", "已作废"), Map.entry("WON", "赢单"),
            Map.entry("LOST", "输单"), Map.entry("NEW", "新线索"),
            Map.entry("QUALIFIED", "已确认"), Map.entry("CONVERTED", "已转化"));

    private static String key(String type, String value) {
        return type + "\u0001" + (value == null ? "" : value.trim().toUpperCase(Locale.ROOT));
    }

    private static String rkey(String type, String label) {
        return type + "\u0002" + (label == null ? "" : label.trim());
    }

    private void ensureLoaded() {
        if (!labels.isEmpty() && System.currentTimeMillis() - loadedAt < TTL) {
            return;
        }
        synchronized (this) {
            if (!labels.isEmpty() && System.currentTimeMillis() - loadedAt < TTL) {
                return;
            }
            Map<String, String> l = new HashMap<>();
            Map<String, String> c = new HashMap<>();
            BUILTIN.forEach((type, entries) -> entries.forEach((code, label) -> {
                l.put(key(type, code), label);
                c.put(rkey(type, label), code);
            }));
            try {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT dict_type, dict_label, dict_value FROM sys_dict WHERE deleted = 0 AND status = 1");
                for (Map<String, Object> row : rows) {
                    String type = str(row.get("dict_type"));
                    String label = str(row.get("dict_label"));
                    String code = str(row.get("dict_value"));
                    if (type.isEmpty() || code.isEmpty()) {
                        continue;
                    }
                    l.put(key(type, code), label);
                    c.put(rkey(type, label), code);
                }
            } catch (Exception e) {
                log.warn("加载数据字典失败，使用内置字典: {}", e.getMessage());
            }
            labels = Map.copyOf(l);
            codes = Map.copyOf(c);
            loadedAt = System.currentTimeMillis();
        }
    }

    /** 指定字典类型：码值 → 中文；找不到返回原值 */
    public String label(String dictType, String value) {
        if (value == null || value.isBlank()) {
            return value == null ? "" : value;
        }
        ensureLoaded();
        String hit = labels.get(key(dictType, value));
        if (hit != null) {
            return hit;
        }
        return FALLBACK.getOrDefault(value.trim().toUpperCase(Locale.ROOT), value);
    }

    /** 指定字典类型：中文或码值 → 码值（供导入使用，兼容用户直接填码值） */
    public String value(String dictType, String labelOrCode) {
        if (labelOrCode == null || labelOrCode.isBlank()) {
            return labelOrCode == null ? "" : labelOrCode;
        }
        ensureLoaded();
        String text = labelOrCode.trim();
        if (labels.containsKey(key(dictType, text))) {
            return text;
        }
        String hit = codes.get(rkey(dictType, text));
        return hit == null ? text : hit;
    }

    /**
     * 综合展示：按「业务类型 + 字段」找字典，再按「字段」找通用字典，
     * 最后用内置状态兜底。用于修改记录等不知道具体字典类型的场景。
     */
    public String display(String bizType, String field, String value) {
        if (value == null || value.isBlank()) {
            return value == null ? "" : value;
        }
        String column = field == null ? "" : field.toLowerCase(Locale.ROOT);
        String type = BIZ_FIELD_DICT.get((bizType == null ? "" : bizType.toUpperCase(Locale.ROOT)) + "." + column);
        if (type == null) {
            type = FIELD_DICT.get(column);
        }
        if (type != null) {
            ensureLoaded();
            String hit = labels.get(key(type, value));
            if (hit != null) {
                return hit;
            }
        }
        return FALLBACK.getOrDefault(value.trim().toUpperCase(Locale.ROOT), value);
    }

    /** 展示时把外键 ID 换成名称（负责人、部门、客户） */
    public String foreignName(String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String sql = switch (field == null ? "" : field.toLowerCase(Locale.ROOT)) {
            case "owner_user_id" -> "SELECT nickname FROM sys_user WHERE id = ?";
            case "dept_id" -> "SELECT name FROM sys_dept WHERE id = ?";
            case "customer_id" -> "SELECT name FROM crm_customer WHERE id = ?";
            case "category_id" -> "SELECT name FROM crm_product_category WHERE id = ?";
            default -> null;
        };
        if (sql == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(sql, String.class, Long.valueOf(value.trim()));
        } catch (Exception e) {
            return null;
        }
    }

    private String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
