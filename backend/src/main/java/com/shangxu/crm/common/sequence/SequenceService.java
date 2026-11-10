package com.shangxu.crm.common.sequence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * 单号生成器。
 * <p>
 * 用数据库序列表 + 行锁取号，替代原来的 {@code MAX(单号)+1}：
 * 后者在并发下会取到同一个值，导致单号重复。
 * 使用 REQUIRES_NEW 让取号独立提交，外层事务回滚不会复用已发出的号（宁可跳号，不可重号）。
 */
@Service
@RequiredArgsConstructor
public class SequenceService {

    private final SequenceMapper sequenceMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public long next(String key) {
        sequenceMapper.ensure(key);
        sequenceMapper.increment(key);
        return sequenceMapper.currentValue(key);
    }

    /**
     * 生成「前缀 + 年份 + 4 位序号」的业务单号，如 KH20260019。
     */
    public String nextNo(String key, String prefix) {
        return prefix + Year.now().getValue() + String.format("%04d", next(key));
    }
}
