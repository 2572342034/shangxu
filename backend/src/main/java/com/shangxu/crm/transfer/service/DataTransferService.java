package com.shangxu.crm.transfer.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.csv.CsvUtils;
import com.shangxu.crm.common.dict.DictDisplayService;
import com.shangxu.crm.common.sequence.SequenceService;
import com.shangxu.crm.customer.entity.Customer;
import com.shangxu.crm.customer.mapper.CustomerMapper;
import com.shangxu.crm.lead.entity.Lead;
import com.shangxu.crm.lead.mapper.LeadMapper;
import com.shangxu.crm.receivable.entity.Receivable;
import com.shangxu.crm.receivable.mapper.ReceivableMapper;
import com.shangxu.crm.security.DataScopeHelper;
import com.shangxu.crm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据导入导出。导出为带 BOM 的 CSV，Excel 可直接打开；导入接受同结构 CSV。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataTransferService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CustomerMapper customerMapper;
    private final LeadMapper leadMapper;
    private final ReceivableMapper receivableMapper;
    private final SequenceService sequenceService;
    private final com.shangxu.crm.product.mapper.ProductMapper productMapper;
    private final com.shangxu.crm.product.service.ProductService productService;
    private final com.shangxu.crm.product.service.ProductCategoryService categoryService;
    private final DictDisplayService dict;

    public byte[] exportCustomers() {
        List<Customer> list = customerMapper.selectPageList(new Page<>(1, 5000), null, null, null, null, null,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId()).getRecords();
        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtils.line("客户编号", "客户名称", "简称", "级别", "行业", "来源", "规模",
                "电话", "网站", "省份", "城市", "地址", "负责人", "状态", "创建时间"));
        for (Customer c : list) {
            sb.append(CsvUtils.line(c.getCustomerNo(), c.getName(), c.getShortName(),
                    dict.label("customer_level", c.getLevel()), dict.label("industry", c.getIndustry()),
                    dict.label("customer_source", c.getSource()), dict.label("customer_scale", c.getScale()),
                    c.getPhone(), c.getWebsite(),
                    c.getProvince(), c.getCity(), c.getAddress(), c.getOwnerName(),
                    dict.label("customer_status", c.getStatus()),
                    c.getCreateTime() == null ? "" : c.getCreateTime().format(DT)));
        }
        return CsvUtils.withBom(sb.toString());
    }

    public byte[] exportLeads() {
        List<Lead> list = leadMapper.selectPageList(new Page<>(1, 5000), null, null, null,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId()).getRecords();
        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtils.line("线索编号", "线索名称", "公司", "联系人", "手机", "来源", "行业", "状态", "负责人", "创建时间"));
        for (Lead l : list) {
            sb.append(CsvUtils.line(l.getLeadNo(), l.getName(), l.getCompany(), l.getContactName(),
                    l.getMobile(), dict.label("customer_source", l.getSource()),
                    dict.label("industry", l.getIndustry()), dict.label("lead_status", l.getStatus()), l.getOwnerName(),
                    l.getCreateTime() == null ? "" : l.getCreateTime().format(DT)));
        }
        return CsvUtils.withBom(sb.toString());
    }

    public byte[] exportReceivables() {
        List<Receivable> list = receivableMapper.selectPageList(new Page<>(1, 5000), null, null, null, null,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId()).getRecords();
        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtils.line("应收单号", "客户", "来源单号", "应收金额", "已收金额", "未收金额",
                "应收日期", "账龄(天)", "状态", "负责人"));
        for (Receivable r : list) {
            BigDecimal unpaid = (r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())
                    .subtract(r.getReceivedAmount() == null ? BigDecimal.ZERO : r.getReceivedAmount());
            sb.append(CsvUtils.line(r.getReceivableNo(), r.getCustomerName(), r.getSourceNo(),
                    r.getAmount(), r.getReceivedAmount(), unpaid, r.getDueDate(),
                    r.getOverdueDays(), dict.label("receivable_status", r.getStatus()), r.getOwnerName()));
        }
        return CsvUtils.withBom(sb.toString());
    }

    /**
     * 导入客户。CSV 首行为表头，列顺序：客户名称,级别,行业,来源,电话,城市,地址,备注
     * 重名客户跳过，逐行返回结果。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importCustomers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.shangxu.crm.common.BizException("请选择要导入的文件");
        }
        String text;
        try {
            text = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new com.shangxu.crm.common.BizException("文件读取失败");
        }
        List<String[]> rows = CsvUtils.parse(text);
        int success = 0;
        List<String> errors = new ArrayList<>();
        int start = 0;
        if (!rows.isEmpty() && rows.get(0).length > 0 && rows.get(0)[0].contains("客户名称")) {
            start = 1;
        }
        for (int i = start; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String name = cell(row, 0);
            if (name.isEmpty()) {
                errors.add("第 " + (i + 1) + " 行：客户名称为空");
                continue;
            }
            if (customerMapper.countByName(name) > 0) {
                errors.add("第 " + (i + 1) + " 行：客户「" + name + "」已存在，已跳过");
                continue;
            }
            Customer customer = new Customer();
            customer.setCustomerNo(sequenceService.nextNo("customer", "KH"));
            customer.setName(name);
            customer.setLevel(dict.value("customer_level", defaultIfEmpty(cell(row, 1), "C")));
            customer.setIndustry(dict.value("industry", cell(row, 2)));
            customer.setSource(dict.value("customer_source", defaultIfEmpty(cell(row, 3), "IMPORT")));
            customer.setPhone(cell(row, 4));
            customer.setCity(cell(row, 5));
            customer.setAddress(cell(row, 6));
            customer.setRemark(cell(row, 7));
            customer.setStatus("FOLLOWING");
            customer.setPoolType("PRIVATE");
            customer.setOwnerUserId(SecurityUtils.getUserId());
            customer.setDeptId(SecurityUtils.getDeptId());
            customerMapper.insert(customer);
            success++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", Math.max(0, rows.size() - start));
        result.put("success", success);
        result.put("failed", errors.size());
        result.put("errors", errors.size() > 20 ? errors.subList(0, 20) : errors);
        return result;
    }

    /** 产品导出：列顺序与导入模板完全一致，导出的文件可直接改完再导回来 */
    public byte[] exportProducts() {
        List<com.shangxu.crm.product.entity.Product> list = productMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                        com.shangxu.crm.product.entity.Product>()
                        .orderByAsc(com.shangxu.crm.product.entity.Product::getSort)
                        .orderByAsc(com.shangxu.crm.product.entity.Product::getId));
        StringBuilder sb = new StringBuilder();
        sb.append(productHeader());
        for (com.shangxu.crm.product.entity.Product p : list) {
            sb.append(CsvUtils.line(p.getCode(), p.getName(), p.getCategory(), p.getSpec(), p.getUnit(),
                    p.getPrice(), p.getCost(), p.getMinPrice(), p.getTaxRate(), p.getSort(),
                    p.getStatus() != null && p.getStatus() == 1 ? "在售" : "停售",
                    p.getDescription(), p.getRemark()));
        }
        return CsvUtils.withBom(sb.toString());
    }

    /** 只有表头和一行示例，供用户填写后导入 */
    public byte[] exportProductTemplate() {
        return CsvUtils.withBom(productHeader()
                + CsvUtils.line("", "示例：商序云 旗舰版（年）", "订阅许可", "100 账号 / 年", "套/年",
                "298000", "50000", "268000", "6", "9", "在售", "面向大型团队的旗舰版本", ""));
    }

    private String productHeader() {
        return CsvUtils.line("产品编码", "产品名称", "所属目录", "规格型号", "单位", "销售价", "成本价",
                "最低成交价", "税率(%)", "排序", "状态", "产品描述", "备注");
    }

    /**
     * 产品批量导入。产品编码留空时由序列自动生成；目录按名称匹配；重名或目录不存在则跳过该行。
     */
    public Map<String, Object> importProducts(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.shangxu.crm.common.BizException("请选择要导入的文件");
        }
        String text;
        try {
            text = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new com.shangxu.crm.common.BizException("文件读取失败");
        }
        List<String[]> rows = CsvUtils.parse(text);
        int start = 0;
        if (!rows.isEmpty() && rows.get(0).length > 0
                && (rows.get(0)[0].contains("产品编码")
                || (rows.get(0).length > 1 && rows.get(0)[1].contains("产品名称")))) {
            start = 1;
        }
        int success = 0;
        List<String> errors = new ArrayList<>();
        for (int i = start; i < rows.size(); i++) {
            String[] row = rows.get(i);
            try {
                String name = cell(row, 1);
                if (name.isEmpty()) {
                    errors.add("第 " + (i + 1) + " 行：产品名称为空");
                    continue;
                }
                String code = cell(row, 0);
                if (!code.isEmpty() && productMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                com.shangxu.crm.product.entity.Product>()
                                .eq(com.shangxu.crm.product.entity.Product::getCode, code)) > 0) {
                    errors.add("第 " + (i + 1) + " 行：产品编码「" + code + "」已存在，已跳过");
                    continue;
                }
                if (productMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                com.shangxu.crm.product.entity.Product>()
                                .eq(com.shangxu.crm.product.entity.Product::getName, name)) > 0) {
                    errors.add("第 " + (i + 1) + " 行：产品「" + name + "」已存在，已跳过");
                    continue;
                }
                String categoryName = cell(row, 2);
                Long categoryId = null;
                if (!categoryName.isEmpty()) {
                    categoryId = categoryService.idOfName(categoryName);
                    if (categoryId == null) {
                        errors.add("第 " + (i + 1) + " 行：目录「" + categoryName + "」不存在，请先在产品目录页创建");
                        continue;
                    }
                }

                com.shangxu.crm.product.entity.Product product = new com.shangxu.crm.product.entity.Product();
                product.setCode(code.isEmpty() ? null : code);
                product.setName(name);
                product.setCategoryId(categoryId);
                product.setSpec(cell(row, 3));
                product.setUnit(cell(row, 4));
                product.setPrice(decimal(cell(row, 5), BigDecimal.ZERO));
                product.setCost(decimal(cell(row, 6), BigDecimal.ZERO));
                String minPrice = cell(row, 7);
                product.setMinPrice(minPrice.isEmpty() ? null : decimal(minPrice, null));
                product.setTaxRate(decimal(cell(row, 8), new BigDecimal("6.00")));
                product.setSort(Integer.parseInt(cell(row, 9).isEmpty() ? "0" : cell(row, 9)));
                String status = cell(row, 10);
                product.setStatus("0".equals(dict.value("product_status", status)) ? 0 : 1);
                product.setDescription(cell(row, 11));
                product.setRemark(cell(row, 12));
                productService.create(product);
                success++;
            } catch (Exception e) {
                String msg = e instanceof com.shangxu.crm.common.BizException
                        ? e.getMessage() : "数据格式有误：" + e.getMessage();
                errors.add("第 " + (i + 1) + " 行：" + msg);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", Math.max(0, rows.size() - start));
        result.put("success", success);
        result.put("failed", errors.size());
        result.put("errors", errors.size() > 20 ? errors.subList(0, 20) : errors);
        return result;
    }

    private BigDecimal decimal(String value, BigDecimal fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        try {
            return new BigDecimal(value.replace(",", "").replace("¥", "").trim());
        } catch (NumberFormatException e) {
            throw new com.shangxu.crm.common.BizException("金额格式不正确：" + value);
        }
    }

    private String cell(String[] row, int index) {
        return index < row.length && row[index] != null ? row[index].trim() : "";
    }

    private String defaultIfEmpty(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
