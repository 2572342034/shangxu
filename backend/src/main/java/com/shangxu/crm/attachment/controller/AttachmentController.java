package com.shangxu.crm.attachment.controller;

import com.shangxu.crm.attachment.entity.Attachment;
import com.shangxu.crm.attachment.service.AttachmentService;
import com.shangxu.crm.common.R;
import com.shangxu.crm.log.OperLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "附件中心")
@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping
    public R<List<Attachment>> list(@RequestParam String bizType, @RequestParam Long bizId) {
        return R.ok(attachmentService.list(bizType, bizId));
    }

    @PostMapping
    @OperLog(title = "附件上传", type = "INSERT")
    public R<Attachment> upload(@RequestParam String bizType,
                                @RequestParam Long bizId,
                                @RequestParam("file") MultipartFile file) {
        return R.ok("上传成功", attachmentService.upload(bizType, bizId, file));
    }

    @Operation(summary = "下载附件")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Attachment attachment = attachmentService.get(id);
        Resource resource = attachmentService.download(id);
        String encoded = URLEncoder.encode(attachment.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @OperLog(title = "附件删除", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id);
        return R.ok();
    }
}
