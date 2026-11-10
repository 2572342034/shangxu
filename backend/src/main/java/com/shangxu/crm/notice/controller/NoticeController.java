package com.shangxu.crm.notice.controller;

import com.shangxu.crm.common.R;
import com.shangxu.crm.notice.entity.Notice;
import com.shangxu.crm.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "站内通知")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public R<List<Notice>> mine(@RequestParam(defaultValue = "false") boolean onlyUnread,
                                @RequestParam(defaultValue = "30") int limit) {
        return R.ok(noticeService.mine(onlyUnread, limit));
    }

    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(noticeService.unreadCount());
    }

    @PutMapping("/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        noticeService.read(id);
        return R.ok();
    }

    @PutMapping("/read-all")
    public R<Void> readAll() {
        noticeService.readAll();
        return R.ok();
    }
}
