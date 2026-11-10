package com.shangxu.crm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.system.entity.SysLog;
import com.shangxu.crm.system.mapper.SysLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysLogService {

    private final SysLogMapper logMapper;

    @Async
    public void saveAsync(SysLog sysLog) {
        try {
            logMapper.insert(sysLog);
        } catch (Exception e) {
            log.warn("操作日志写入失败: {}", e.getMessage(), e);
        }
    }

    public PageResult<SysLog> page(PageQuery query, String operName, Integer status) {
        Page<SysLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<SysLog> result = logMapper.selectPage(page, new LambdaQueryWrapper<SysLog>()
                .like(operName != null && !operName.isBlank(), SysLog::getOperName, operName)
                .eq(status != null, SysLog::getStatus, status)
                .orderByDesc(SysLog::getId));
        return PageResult.of(result);
    }

    public void clear() {
        logMapper.delete(new LambdaQueryWrapper<SysLog>().lt(SysLog::getId, Long.MAX_VALUE));
    }
}
