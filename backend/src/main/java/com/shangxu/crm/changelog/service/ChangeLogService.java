package com.shangxu.crm.changelog.service;

import com.shangxu.crm.changelog.entity.ChangeLog;
import com.shangxu.crm.changelog.mapper.ChangeLogMapper;
import com.shangxu.crm.common.dict.DictDisplayService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** 通用字段级变更记录：对比更新前后的行快照，逐字段落库 */
@Service
@RequiredArgsConstructor
public class ChangeLogService {

    private final ChangeLogMapper changeLogMapper;
    private final DictDisplayService dictDisplay;
    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Set<String> IGNORE = Set.of(
            "id", "create_time", "update_time", "create_by", "version", "deleted");

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("name", "名称"), Map.entry("customer_name", "客户"),
            Map.entry("gender", "性别"), Map.entry("is_primary", "关键联系人"),
            Map.entry("wechat", "微信"), Map.entry("post", "职务"),
            Map.entry("birthday", "生日"), Map.entry("contact_name", "联系人"),
            Map.entry("customer_no", "客户编号"), Map.entry("lead_no", "线索编号"),
            Map.entry("opp_no", "商机编号"), Map.entry("contract_no", "合同编号"),
            Map.entry("order_no", "订单编号"), Map.entry("delivery_no", "出库单号"),
            Map.entry("receivable_no", "应收单号"), Map.entry("record_no", "收款单号"),
            Map.entry("invoice_no", "发票号"), Map.entry("return_no", "退货单号"),
            Map.entry("plan_no", "计划单号"), Map.entry("code", "编码"),
            Map.entry("title", "标题"), Map.entry("short_name", "简称"),
            Map.entry("amount", "金额"), Map.entry("received_amount", "已收金额"),
            Map.entry("allocated_amount", "已核销金额"), Map.entry("unallocated_amount", "预收余额"),
            Map.entry("refund_amount", "退款金额"), Map.entry("return_amount", "退货金额"),
            Map.entry("tax_amount", "税额"), Map.entry("tax_rate", "税率"),
            Map.entry("price", "单价"), Map.entry("cost", "成本"), Map.entry("min_price", "最低成交价"),
            Map.entry("quantity", "数量"), Map.entry("return_qty", "退货数量"),
            Map.entry("status", "状态"), Map.entry("stage", "阶段"), Map.entry("pool_type", "客户池"),
            Map.entry("owner_user_id", "负责人"), Map.entry("dept_id", "部门"),
            Map.entry("customer_id", "客户"), Map.entry("category_id", "所属目录"),
            Map.entry("category", "分类"), Map.entry("spec", "规格型号"), Map.entry("unit", "单位"),
            Map.entry("phone", "电话"), Map.entry("mobile", "手机"), Map.entry("email", "邮箱"),
            Map.entry("website", "网站"), Map.entry("address", "地址"),
            Map.entry("province", "省份"), Map.entry("city", "城市"),
            Map.entry("industry", "行业"), Map.entry("level", "级别"),
            Map.entry("source", "来源"), Map.entry("scale", "规模"),
            Map.entry("reason", "原因"), Map.entry("remark", "备注"),
            Map.entry("description", "描述"), Map.entry("due_date", "应收日期"),
            Map.entry("receive_date", "收款日期"), Map.entry("invoice_date", "开票日期"),
            Map.entry("delivery_date", "发货日期"), Map.entry("return_date", "退货日期"),
            Map.entry("next_follow_time", "下次跟进时间"), Map.entry("last_follow_time", "最近跟进时间"),
            Map.entry("sort", "排序"), Map.entry("method", "收款方式"),
            Map.entry("voucher_no", "凭证号"), Map.entry("warehouse", "仓库"),
            Map.entry("logistics_no", "物流单号"), Map.entry("allocated_count", "核销笔数"),
            Map.entry("probability", "赢单率"), Map.entry("expected_date", "预计成交日期"));

    private static final Map<String, String> VALUE_LABELS = Map.ofEntries(
            Map.entry("PENDING", "待审核"), Map.entry("APPROVED", "已通过"),
            Map.entry("REJECTED", "已驳回"), Map.entry("CANCELLED", "已作废"),
            Map.entry("CONFIRMED", "已确认"), Map.entry("PAID", "已结清"),
            Map.entry("UNPAID", "未收款"), Map.entry("PARTIAL", "部分收款"),
            Map.entry("PART", "部分收款"), Map.entry("PRIVATE", "私海"),
            Map.entry("PUBLIC", "公海"), Map.entry("FOLLOWING", "跟进中"),
            Map.entry("DEAL", "已成交"), Map.entry("INVALID", "无效"),
            Map.entry("DRAFT", "草稿"), Map.entry("ISSUED", "已开具"),
            Map.entry("SHIPPED", "已出库"), Map.entry("REFUNDED", "已退款"));

    public PageResult<ChangeLog> page(PageQuery query, String bizType, String keyword) {
        IPage<ChangeLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(changeLogMapper.selectPageList(page, bizType, keyword));
    }

    public List<ChangeLog> list(String bizType, Long bizId, int limit) {
        return changeLogMapper.selectByBiz(bizType, bizId, Math.min(Math.max(limit, 1), 200));
    }

    public boolean hasOwnerHistory(Long customerId, Long userId) {
        return userId != null && changeLogMapper.countOwnerHistory(customerId, userId) > 0;
    }

    /** 定时任务等非 Controller 入口的负责人变更记录 */
    public void recordOwnerChange(Long customerId, String customerNo, Long oldOwnerId,
                                  String action, String remark) {
        ChangeLog log = base("CUSTOMER", customerId, customerNo, action, null, "系统任务", remark);
        log.setFieldKey("owner_user_id");
        log.setFieldLabel("负责人");
        log.setOldValue(display("CUSTOMER", "owner_user_id", oldOwnerId == null ? "" : String.valueOf(oldOwnerId)));
        log.setNewValue("");
        log.setOldOwnerId(oldOwnerId);
        changeLogMapper.insert(log);
    }

    private static final Set<String> SENSITIVE = Set.of(
            "phone", "mobile", "email", "website", "address", "province", "city",
            "remark", "description", "scale", "contact_name", "contact_phone",
            "next_follow_time", "last_follow_time");

    /** 历史负责人只读视图：不暴露敏感字段的历史值，避免绕过脱敏 */
    public List<ChangeLog> withoutSensitive(List<ChangeLog> logs) {
        return logs.stream()
                .filter(l -> l.getFieldKey() == null || !SENSITIVE.contains(l.getFieldKey()))
                .toList();
    }

    /** 对比前后快照并落库；CREATE/DELETE 只记一条汇总 */
    public void diffAndSave(String bizType, Long bizId, String action,
                            Map<String, Object> before, Map<String, Object> after, String remark) {
        Long userId = SecurityUtils.getUserId();
        String userName = SecurityUtils.getNickname();
        String bizNo = bizNo(before, after);

        if (before.isEmpty() || after.isEmpty()) {
            ChangeLog log = base(bizType, bizId, bizNo, action, userId, userName, remark);
            if (after.isEmpty() && !before.isEmpty()) {
                log.setFieldKey("deleted");
                log.setFieldLabel("记录状态");
                log.setOldValue("正常");
                log.setNewValue("已删除");
            } else if (!after.isEmpty()) {
                log.setFieldKey("id");
                log.setFieldLabel("记录");
                log.setOldValue("");
                log.setNewValue("新建");
            }
            changeLogMapper.insert(log);
            return;
        }

        Set<String> keys = new TreeSet<>(before.keySet());
        keys.addAll(after.keySet());
        List<ChangeLog> logs = new ArrayList<>();
        for (String key : keys) {
            String column = key.toLowerCase();
            if (IGNORE.contains(column)) {
                continue;
            }
            String oldVal = str(before.get(key));
            String newVal = str(after.get(key));
            if (oldVal.equals(newVal)) {
                continue;
            }
            ChangeLog log = base(bizType, bizId, bizNo, action, userId, userName, remark);
            log.setFieldKey(column);
            log.setFieldLabel(LABELS.getOrDefault(column, column));
            log.setOldValue(display(bizType, column, oldVal));
            log.setNewValue(display(bizType, column, newVal));
            if ("owner_user_id".equals(column)) {
                log.setOldOwnerId(parseLong(oldVal));
            }
            logs.add(log);
        }
        for (ChangeLog log : logs) {
            changeLogMapper.insert(log);
        }
    }

    private ChangeLog base(String bizType, Long bizId, String bizNo, String action,
                           Long userId, String userName, String remark) {
        ChangeLog log = new ChangeLog();
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setBizNo(bizNo);
        log.setAction(action);
        log.setOperatorId(userId);
        log.setOperatorName(userName);
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        return log;
    }

    private String bizNo(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> src = after.isEmpty() ? before : after;
        for (Map.Entry<String, Object> e : src.entrySet()) {
            String k = e.getKey().toLowerCase();
            if (k.endsWith("_no") && e.getValue() != null) {
                return str(e.getValue());
            }
        }
        for (String key : new String[]{"code", "name", "title"}) {
            for (Map.Entry<String, Object> e : src.entrySet()) {
                if (e.getKey().equalsIgnoreCase(key) && e.getValue() != null) {
                    return str(e.getValue());
                }
            }
        }
        return null;
    }

    private String str(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal b) {
            return b.stripTrailingZeros().toPlainString();
        }
        if (value instanceof Timestamp t) {
            return t.toLocalDateTime().format(TIME);
        }
        if (value instanceof byte[]) {
            return "[二进制]";
        }
        String s = String.valueOf(value);
        return s.length() > 450 ? s.substring(0, 450) + "..." : s;
    }

    /** 外键 ID 转名称，其余码值统一交给 DictDisplayService 转中文 */
    private String display(String bizType, String field, String value) {
        String name = dictDisplay.foreignName(field, value);
        return name != null ? name : dictDisplay.display(bizType, field, value);
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
