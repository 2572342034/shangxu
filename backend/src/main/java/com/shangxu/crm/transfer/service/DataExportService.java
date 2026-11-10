package com.shangxu.crm.transfer.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.common.csv.CsvUtils;
import com.shangxu.crm.common.dict.DictDisplayService;
import com.shangxu.crm.customer.entity.Customer;
import com.shangxu.crm.customer.mapper.CustomerMapper;
import com.shangxu.crm.lead.entity.Lead;
import com.shangxu.crm.lead.mapper.LeadMapper;
import com.shangxu.crm.product.entity.Product;
import com.shangxu.crm.product.mapper.ProductMapper;
import com.shangxu.crm.receivable.entity.Receivable;
import com.shangxu.crm.receivable.mapper.ReceivableMapper;
import com.shangxu.crm.security.DataScopeHelper;
import com.shangxu.crm.transfer.dto.ExportQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 批量导出中心：先按条件查询预览，再按选中的行与列导出 CSV。
 * 所有码值统一走 DictDisplayService 转中文，与修改记录、快速导出口径一致。
 */
@Service
@RequiredArgsConstructor
public class DataExportService {

    private final CustomerMapper customerMapper;
    private final LeadMapper leadMapper;
    private final ReceivableMapper receivableMapper;
    private final ProductMapper productMapper;
    private final DictDisplayService dict;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_ROWS = 10000;

    private static final Map<String, String> MODULE_LABEL = Map.of(
            "CUSTOMER", "客户", "LEAD", "线索", "RECEIVABLE", "应收单", "PRODUCT", "产品");
    private static final Map<String, String> STATUS_DICT = Map.of(
            "CUSTOMER", "customer_status", "LEAD", "lead_status",
            "RECEIVABLE", "receivable_status", "PRODUCT", "product_status");

    private static final Map<String, LinkedHashMap<String, String>> COLUMNS = Map.of(
            "CUSTOMER", cols("customerNo", "客户编号", "name", "客户名称", "shortName", "简称",
                    "level", "级别", "industry", "行业", "source", "来源", "scale", "规模",
                    "phone", "电话", "website", "官网", "province", "省份", "city", "城市",
                    "address", "地址", "ownerName", "负责人", "poolType", "客户池",
                    "status", "状态", "createTime", "创建时间"),
            "LEAD", cols("leadNo", "线索编号", "name", "线索名称", "company", "公司",
                    "contactName", "联系人", "mobile", "手机", "email", "邮箱",
                    "source", "来源", "industry", "行业", "status", "状态",
                    "ownerName", "负责人", "remark", "备注", "createTime", "创建时间"),
            "RECEIVABLE", cols("receivableNo", "应收单号", "customerName", "客户",
                    "sourceType", "来源类型", "sourceNo", "来源单号", "amount", "应收金额",
                    "receivedAmount", "已收金额", "unpaid", "未收金额", "dueDate", "应收日期",
                    "overdueDays", "账龄(天)", "status", "状态", "ownerName", "负责人",
                    "createTime", "创建时间"),
            "PRODUCT", cols("code", "产品编码", "name", "产品名称", "category", "所属目录",
                    "spec", "规格型号", "unit", "单位", "price", "销售价", "cost", "成本价",
                    "minPrice", "最低成交价", "taxRate", "税率(%)", "sort", "排序",
                    "status", "状态", "description", "产品描述", "remark", "备注"));

    private static LinkedHashMap<String, String> cols(String... kv) {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    private String module(ExportQuery q) {
        String m = q.getModule() == null ? "CUSTOMER" : q.getModule().toUpperCase();
        if (!COLUMNS.containsKey(m)) {
            throw new BizException("不支持的导出类型：" + m);
        }
        return m;
    }

    public Map<String, Object> meta(String module) {
        String m = module == null ? "CUSTOMER" : module.toUpperCase();
        if (!COLUMNS.containsKey(m)) {
            throw new BizException("不支持的导出类型：" + m);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("module", m);
        result.put("label", MODULE_LABEL.get(m));
        result.put("statusDict", STATUS_DICT.get(m));
        List<Map<String, String>> columns = new ArrayList<>();
        COLUMNS.get(m).forEach((k, v) -> columns.add(Map.of("key", k, "label", v)));
        result.put("columns", columns);
        return result;
    }

    public Map<String, Object> preview(ExportQuery q) {
        String m = module(q);
        int pageNum = q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum();
        int pageSize = q.getPageSize() == null || q.getPageSize() < 1 ? 10 : Math.min(q.getPageSize(), 100);
        SearchResult found = search(m, q, new Page<>(pageNum, pageSize));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", found.total);
        result.put("records", found.rows);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    public byte[] csv(ExportQuery q) {
        String m = module(q);
        SearchResult found = search(m, q, new Page<>(1, MAX_ROWS));
        LinkedHashMap<String, String> defs = COLUMNS.get(m);
        List<String> keys = selectedKeys(q, defs);
        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtils.line(keys.stream().map(defs::get).toArray()));
        for (Map<String, Object> row : found.rows) {
            sb.append(CsvUtils.line(keys.stream().map(row::get).toArray()));
        }
        return CsvUtils.withBom(sb.toString());
    }

    public String filename(ExportQuery q) {
        return MODULE_LABEL.get(module(q)) + "-" + LocalDate.now() + ".csv";
    }

    private List<String> selectedKeys(ExportQuery q, LinkedHashMap<String, String> defs) {
        if (q.getFields() == null || q.getFields().isBlank()) {
            return new ArrayList<>(defs.keySet());
        }
        Set<String> allowed = defs.keySet();
        return Arrays.stream(q.getFields().split(","))
                .map(String::trim).filter(allowed::contains).distinct().toList();
    }

    private static class SearchResult {
        long total;
        List<Map<String, Object>> rows = new ArrayList<>();
    }

    private SearchResult search(String module, ExportQuery q, Page<?> page) {
        return switch (module) {
            case "CUSTOMER" -> customers(q, page);
            case "LEAD" -> leads(q, page);
            case "RECEIVABLE" -> receivables(q, page);
            default -> products(q, page);
        };
    }

    private SearchResult customers(ExportQuery q, Page<?> page) {
        QueryWrapper<Customer> w = new QueryWrapper<>();
        common(w, q);
        if (notBlank(q.getLevel())) w.eq("level", q.getLevel());
        if (notBlank(q.getIndustry())) w.eq("industry", q.getIndustry());
        if (notBlank(q.getSource())) w.eq("source", q.getSource());
        if (notBlank(q.getPoolType())) w.eq("pool_type", q.getPoolType());
        if (notBlank(q.getKeyword())) {
            String k = q.getKeyword().trim();
            w.and(x -> x.like("name", k).or().like("customer_no", k).or().like("short_name", k));
        }
        w.orderByDesc("id");
        IPage<Customer> p = customerMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), w);
        SearchResult r = new SearchResult();
        r.total = p.getTotal();
        for (Customer c : p.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId());
            row.put("customerNo", c.getCustomerNo());
            row.put("name", c.getName());
            row.put("shortName", c.getShortName());
            row.put("level", dict.label("customer_level", c.getLevel()));
            row.put("industry", dict.label("industry", c.getIndustry()));
            row.put("source", dict.label("customer_source", c.getSource()));
            row.put("scale", dict.label("customer_scale", c.getScale()));
            row.put("phone", c.getPhone());
            row.put("website", c.getWebsite());
            row.put("province", c.getProvince());
            row.put("city", c.getCity());
            row.put("address", c.getAddress());
            row.put("ownerName", c.getOwnerName());
            row.put("poolType", dict.label("pool_type", c.getPoolType()));
            row.put("status", dict.label("customer_status", c.getStatus()));
            row.put("createTime", c.getCreateTime() == null ? "" : c.getCreateTime().format(DT));
            r.rows.add(row);
        }
        return r;
    }

    private SearchResult leads(ExportQuery q, Page<?> page) {
        QueryWrapper<Lead> w = new QueryWrapper<>();
        common(w, q);
        if (notBlank(q.getIndustry())) w.eq("industry", q.getIndustry());
        if (notBlank(q.getSource())) w.eq("source", q.getSource());
        if (notBlank(q.getKeyword())) {
            String k = q.getKeyword().trim();
            w.and(x -> x.like("name", k).or().like("company", k).or().like("contact_name", k).or().like("lead_no", k));
        }
        w.orderByDesc("id");
        IPage<Lead> p = leadMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), w);
        SearchResult r = new SearchResult();
        r.total = p.getTotal();
        for (Lead l : p.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", l.getId());
            row.put("leadNo", l.getLeadNo());
            row.put("name", l.getName());
            row.put("company", l.getCompany());
            row.put("contactName", l.getContactName());
            row.put("mobile", l.getMobile());
            row.put("email", l.getEmail());
            row.put("source", dict.label("customer_source", l.getSource()));
            row.put("industry", dict.label("industry", l.getIndustry()));
            row.put("status", dict.label("lead_status", l.getStatus()));
            row.put("ownerName", l.getOwnerName());
            row.put("remark", l.getRemark());
            row.put("createTime", l.getCreateTime() == null ? "" : l.getCreateTime().format(DT));
            r.rows.add(row);
        }
        return r;
    }

    private SearchResult receivables(ExportQuery q, Page<?> page) {
        QueryWrapper<Receivable> w = new QueryWrapper<>();
        common(w, q);
        if (notBlank(q.getKeyword())) {
            String k = q.getKeyword().trim();
            w.and(x -> x.like("receivable_no", k).or().like("customer_name", k).or().like("source_no", k));
        }
        w.orderByDesc("id");
        IPage<Receivable> p = receivableMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), w);
        SearchResult r = new SearchResult();
        r.total = p.getTotal();
        for (Receivable v : p.getRecords()) {
            BigDecimal amount = nz(v.getAmount());
            BigDecimal received = nz(v.getReceivedAmount());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", v.getId());
            row.put("receivableNo", v.getReceivableNo());
            row.put("customerName", v.getCustomerName());
            row.put("sourceType", dict.label("receivable_source", v.getSourceType()));
            row.put("sourceNo", v.getSourceNo());
            row.put("amount", amount);
            row.put("receivedAmount", received);
            row.put("unpaid", amount.subtract(received));
            row.put("dueDate", v.getDueDate());
            row.put("overdueDays", v.getOverdueDays());
            row.put("status", dict.label("receivable_status", v.getStatus()));
            row.put("ownerName", v.getOwnerName());
            row.put("createTime", v.getCreateTime() == null ? "" : v.getCreateTime().format(DT));
            r.rows.add(row);
        }
        return r;
    }

    private SearchResult products(ExportQuery q, Page<?> page) {
        QueryWrapper<Product> w = new QueryWrapper<>();
        dateRange(w, q);
        idsIn(w, q);
        if (q.getCategoryId() != null) w.eq("category_id", q.getCategoryId());
        if (notBlank(q.getStatus())) w.eq("status", q.getStatus());
        if (notBlank(q.getKeyword())) {
            String k = q.getKeyword().trim();
            w.and(x -> x.like("name", k).or().like("code", k).or().like("spec", k));
        }
        w.orderByAsc("sort").orderByAsc("id");
        IPage<Product> p = productMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), w);
        SearchResult r = new SearchResult();
        r.total = p.getTotal();
        for (Product v : p.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", v.getId());
            row.put("code", v.getCode());
            row.put("name", v.getName());
            row.put("category", v.getCategory());
            row.put("spec", v.getSpec());
            row.put("unit", v.getUnit());
            row.put("price", v.getPrice());
            row.put("cost", v.getCost());
            row.put("minPrice", v.getMinPrice());
            row.put("taxRate", v.getTaxRate());
            row.put("sort", v.getSort());
            row.put("status", dict.label("product_status", String.valueOf(v.getStatus())));
            row.put("description", v.getDescription());
            row.put("remark", v.getRemark());
            r.rows.add(row);
        }
        return r;
    }

    private <T> void common(QueryWrapper<T> w, ExportQuery q) {
        if (notBlank(q.getStatus())) w.eq("status", q.getStatus());
        dateRange(w, q);
        idsIn(w, q);
        Long ownerId = DataScopeHelper.scopeOwnerId();
        if (ownerId != null) w.eq("owner_user_id", ownerId);
        Long deptId = DataScopeHelper.scopeDeptId();
        if (deptId != null) {
            w.inSql("owner_user_id", "SELECT id FROM sys_user WHERE dept_id = " + deptId + " AND deleted = 0");
        }
    }

    private <T> void dateRange(QueryWrapper<T> w, ExportQuery q) {
        if (notBlank(q.getDateFrom())) w.ge("create_time", q.getDateFrom().trim() + " 00:00:00");
        if (notBlank(q.getDateTo())) w.le("create_time", q.getDateTo().trim() + " 23:59:59");
    }

    private <T> void idsIn(QueryWrapper<T> w, ExportQuery q) {
        if (!notBlank(q.getIds())) return;
        List<Long> ids = Arrays.stream(q.getIds().split(","))
                .map(String::trim).filter(s -> s.matches("\\d+")).map(Long::valueOf).toList();
        if (!ids.isEmpty()) w.in("id", ids);
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
