package com.shangxu.crm.notice.service;

import com.shangxu.crm.notice.entity.Notice;
import com.shangxu.crm.notice.mapper.NoticeMapper;
import com.shangxu.crm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeMapper noticeMapper;

    public List<Notice> mine(boolean onlyUnread, int limit) {
        return noticeMapper.selectMine(SecurityUtils.getUserId(), onlyUnread, Math.min(limit, 100));
    }

    public long unreadCount() {
        return noticeMapper.countUnread(SecurityUtils.getUserId());
    }

    public void read(Long id) {
        noticeMapper.markRead(id, SecurityUtils.getUserId());
    }

    public void readAll() {
        noticeMapper.markAllRead(SecurityUtils.getUserId());
    }

    /**
     * 发通知；同一接收人、同一业务单据当天只发一次，避免定时任务重复轰炸。
     */
    public void push(Long userId, String title, String content, String type,
                     String bizType, Long bizId, String bizNo) {
        if (userId == null) {
            return;
        }
        if (bizNo != null && noticeMapper.countToday(userId, type, bizNo) > 0) {
            return;
        }
        Notice notice = new Notice();
        notice.setUserId(userId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setType(type);
        notice.setBizType(bizType);
        notice.setBizId(bizId);
        notice.setBizNo(bizNo);
        notice.setReadFlag(0);
        notice.setCreateTime(LocalDateTime.now());
        noticeMapper.insert(notice);
    }
}
