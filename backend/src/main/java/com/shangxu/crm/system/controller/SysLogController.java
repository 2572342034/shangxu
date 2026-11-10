package com.shangxu.crm.system.controller;

import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.system.entity.SysLog;
import com.shangxu.crm.system.service.SysLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统管理-操作日志")
@RestController
@RequestMapping("/api/system/logs")
@RequiredArgsConstructor
public class SysLogController {

    private final SysLogService logService;

    @GetMapping
    @PreAuthorize("@perm.has('system:log:list')")
    public R<PageResult<SysLog>> page(PageQuery query,
                                      @RequestParam(required = false) String operName,
                                      @RequestParam(required = false) Integer status) {
        return R.ok(logService.page(query, operName, status));
    }

    @DeleteMapping
    @PreAuthorize("@perm.has('system:log:list')")
    public R<Void> clear() {
        logService.clear();
        return R.ok("日志已清空", null);
    }
}
