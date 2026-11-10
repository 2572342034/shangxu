package com.shangxu.crm.transfer.controller;

import com.shangxu.crm.common.R;
import com.shangxu.crm.log.OperLog;
import com.shangxu.crm.transfer.dto.ExportQuery;
import com.shangxu.crm.transfer.service.DataExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Tag(name = "批量导出中心")
@RestController
@RequestMapping("/api/data/exports")
@RequiredArgsConstructor
public class DataExportController {

    private final DataExportService dataExportService;

    @Operation(summary = "导出类型的列定义与状态字典")
    @GetMapping("/meta")
    public R<Map<String, Object>> meta(@RequestParam(defaultValue = "CUSTOMER") String module) {
        return R.ok(dataExportService.meta(module));
    }

    @Operation(summary = "按条件查询导出预览（分页）")
    @GetMapping("/preview")
    public R<Map<String, Object>> preview(ExportQuery query) {
        return R.ok(dataExportService.preview(query));
    }

    @Operation(summary = "按条件与选中列导出 CSV")
    @GetMapping("/download")
    @OperLog(title = "批量导出", type = "EXPORT")
    public ResponseEntity<byte[]> download(ExportQuery query) {
        byte[] body = dataExportService.csv(query);
        String encoded = URLEncoder.encode(dataExportService.filename(query), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
