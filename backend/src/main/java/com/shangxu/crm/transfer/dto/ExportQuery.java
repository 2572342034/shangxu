package com.shangxu.crm.transfer.dto;

import lombok.Data;

/** 批量导出：查询条件 + 导出范围 + 导出列 */
@Data
public class ExportQuery {
    private String module = "CUSTOMER";
    private String keyword;
    private String status;
    private String level;
    private String industry;
    private String source;
    private String poolType;
    private Long categoryId;
    private String dateFrom;
    private String dateTo;
    /** 逗号分隔：只导出勾选的行 */
    private String ids;
    /** 逗号分隔：只导出勾选的列，为空表示全选 */
    private String fields;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
