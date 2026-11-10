package com.shangxu.crm.transfer.controller;

import com.shangxu.crm.common.R;
import com.shangxu.crm.log.OperLog;
import com.shangxu.crm.transfer.service.DataTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

@Tag(name = "导入导出")
@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataTransferController {

    private final DataTransferService dataTransferService;

    @Operation(summary = "导出客户")
    @GetMapping("/export/customers")
    @PreAuthorize("@perm.has('crm:customer:list')")
    @OperLog(title = "导出客户", type = "EXPORT")
    public ResponseEntity<byte[]> exportCustomers() {
        return csv(dataTransferService.exportCustomers(), "customers-" + LocalDate.now() + ".csv");
    }

    @Operation(summary = "导出线索")
    @GetMapping("/export/leads")
    @PreAuthorize("@perm.has('crm:lead:list')")
    @OperLog(title = "导出线索", type = "EXPORT")
    public ResponseEntity<byte[]> exportLeads() {
        return csv(dataTransferService.exportLeads(), "leads-" + LocalDate.now() + ".csv");
    }

    @Operation(summary = "导出应收")
    @GetMapping("/export/receivables")
    @PreAuthorize("@perm.has('crm:receivable:list')")
    @OperLog(title = "导出应收", type = "EXPORT")
    public ResponseEntity<byte[]> exportReceivables() {
        return csv(dataTransferService.exportReceivables(), "receivables-" + LocalDate.now() + ".csv");
    }

    @Operation(summary = "导出产品")
    @GetMapping("/export/products")
    @PreAuthorize("@perm.has('crm:product:list')")
    @OperLog(title = "导出产品", type = "EXPORT")
    public ResponseEntity<byte[]> exportProducts() {
        return csv(dataTransferService.exportProducts(), "products-" + LocalDate.now() + ".csv");
    }

    @Operation(summary = "产品导入模板")
    @GetMapping("/export/product-template")
    @PreAuthorize("@perm.has('crm:product:add')")
    public ResponseEntity<byte[]> exportProductTemplate() {
        return csv(dataTransferService.exportProductTemplate(), "产品导入模板.csv");
    }

    @Operation(summary = "导入产品（CSV）")
    @PostMapping("/import/products")
    @PreAuthorize("@perm.has('crm:product:add')")
    @OperLog(title = "导入产品", type = "IMPORT")
    public R<Map<String, Object>> importProducts(@RequestParam("file") MultipartFile file) {
        return R.ok("导入完成", dataTransferService.importProducts(file));
    }

    @Operation(summary = "导入客户（CSV）")
    @PostMapping("/import/customers")
    @PreAuthorize("@perm.has('crm:customer:add')")
    @OperLog(title = "导入客户", type = "IMPORT")
    public R<Map<String, Object>> importCustomers(@RequestParam("file") MultipartFile file) {
        return R.ok("导入完成", dataTransferService.importCustomers(file));
    }

    private ResponseEntity<byte[]> csv(byte[] body, String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
