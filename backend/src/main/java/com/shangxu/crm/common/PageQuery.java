package com.shangxu.crm.common;

import lombok.Data;

/**
 * 通用分页查询参数
 */
@Data
public class PageQuery {

    private long pageNum = 1;
    private long pageSize = 10;
    private String keyword;
    /** 排序字段（驼峰或下划线均可，仅允许白名单字段） */
    private String orderBy;
    private String orderDirection = "desc";

    public long getPageNum() {
        return pageNum < 1 ? 1 : pageNum;
    }

    public long getPageSize() {
        if (pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 200);
    }
}
