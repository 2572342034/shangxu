package com.shangxu.crm.changelog.controller;

import com.shangxu.crm.changelog.entity.ChangeLog;
import com.shangxu.crm.changelog.service.ChangeLogService;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.customer.entity.Customer;
import com.shangxu.crm.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "修改记录")
@RestController
@RequestMapping("/api/change-logs")
@RequiredArgsConstructor
public class ChangeLogController {

    private final ChangeLogService changeLogService;
    private final CustomerService customerService;

    @Operation(summary = "全量修改记录（按模块/关键词筛选）")
    @GetMapping("/page")
    public R<PageResult<ChangeLog>> page(PageQuery query,
                                         @RequestParam(required = false) String bizType,
                                         @RequestParam(required = false) String keyword) {
        return R.ok(changeLogService.page(query, bizType, keyword));
    }

    @Operation(summary = "查询单据的字段级修改记录")
    @GetMapping
    public R<List<ChangeLog>> list(@RequestParam String bizType,
                                   @RequestParam Long bizId,
                                   @RequestParam(defaultValue = "50") int limit) {
        boolean readOnly = false;
        if ("CUSTOMER".equals(bizType)) {
            // 复用客户详情的数据范围校验：越权直接抛异常，历史负责人走脱敏
            Customer customer = customerService.detail(bizId);
            readOnly = Boolean.TRUE.equals(customer.getReadOnly());
        }
        List<ChangeLog> logs = changeLogService.list(bizType, bizId, limit);
        return R.ok(readOnly ? changeLogService.withoutSensitive(logs) : logs);
    }
}
