package com.shangxu.crm.attachment.service;

import com.shangxu.crm.attachment.entity.Attachment;
import com.shangxu.crm.attachment.mapper.AttachmentMapper;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 附件中心：合同扫描件、发票 PDF、营业执照等统一挂在业务对象上。
 * <p>
 * 安全要点：
 * <ul>
 *   <li>存储文件名用 UUID，杜绝路径穿越与可执行后缀；</li>
 *   <li>下载走鉴权接口，不做静态目录暴露；</li>
 *   <li>扩展名白名单 + 单文件大小上限。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private static final Set<String> ALLOWED_EXT = Set.of(
            "pdf", "png", "jpg", "jpeg", "gif", "webp", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip", "ofd");

    private final AttachmentMapper attachmentMapper;

    @Value("${crm.upload.dir:./data/uploads}")
    private String uploadDir;

    public List<Attachment> list(String bizType, Long bizId) {
        return attachmentMapper.selectByBiz(bizType, bizId);
    }

    public Attachment upload(String bizType, Long bizId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        String original = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
        original = original.replace("\\", "/");
        if (original.contains("..") || original.contains("/")) {
            original = original.substring(original.lastIndexOf('/') + 1);
        }
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException("不支持的文件类型：" + (ext.isEmpty() ? "未知" : ext));
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(uploadDir, datePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(storedName).toFile());
        } catch (IOException e) {
            log.error("附件保存失败", e);
            throw new BizException("附件保存失败，请稍后重试");
        }

        Attachment attachment = new Attachment();
        attachment.setBizType(bizType);
        attachment.setBizId(bizId);
        attachment.setOriginalName(original);
        attachment.setFileName(storedName);
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setStoragePath(datePath + "/" + storedName);
        attachment.setUploaderId(SecurityUtils.getUserId());
        attachment.setUploaderName(SecurityUtils.getNickname());
        attachmentMapper.insert(attachment);
        return attachment;
    }

    public Resource download(Long id) {
        Attachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw new BizException("附件不存在");
        }
        Path path = Paths.get(uploadDir, attachment.getStoragePath()).toAbsolutePath().normalize();
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!path.startsWith(root) || !Files.exists(path)) {
            throw new BizException("附件文件已丢失");
        }
        return new FileSystemResource(path);
    }

    public Attachment get(Long id) {
        Attachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw new BizException("附件不存在");
        }
        return attachment;
    }

    public void delete(Long id) {
        Attachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw new BizException("附件不存在");
        }
        attachmentMapper.deleteById(id);
        try {
            Files.deleteIfExists(Paths.get(uploadDir, attachment.getStoragePath()).toAbsolutePath().normalize());
        } catch (IOException e) {
            log.warn("附件文件删除失败: {}", attachment.getStoragePath(), e);
        }
    }
}
