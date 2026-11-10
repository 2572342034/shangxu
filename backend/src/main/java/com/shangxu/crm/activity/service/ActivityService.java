package com.shangxu.crm.activity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.activity.entity.Activity;
import com.shangxu.crm.activity.mapper.ActivityMapper;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.contract.entity.Contract;
import com.shangxu.crm.contract.mapper.ContractMapper;
import com.shangxu.crm.customer.entity.Customer;
import com.shangxu.crm.customer.mapper.CustomerMapper;
import com.shangxu.crm.lead.entity.Lead;
import com.shangxu.crm.lead.mapper.LeadMapper;
import com.shangxu.crm.opportunity.entity.Opportunity;
import com.shangxu.crm.opportunity.mapper.OpportunityMapper;
import com.shangxu.crm.security.DataScopeHelper;
import com.shangxu.crm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityMapper activityMapper;
    private final CustomerMapper customerMapper;
    private final LeadMapper leadMapper;
    private final OpportunityMapper opportunityMapper;
    private final ContractMapper contractMapper;

    public PageResult<Activity> page(PageQuery query, String bizType, Long bizId) {
        Page<Activity> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<Activity> result = activityMapper.selectPageList(page, bizType, bizId,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
        return PageResult.of(result);
    }

    public List<Activity> recent(int limit) {
        return activityMapper.selectRecent(DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId(), limit);
    }

    public Long create(Activity activity) {
        if (activity.getBizId() == null || activity.getBizType() == null) {
            throw new BizException("请指定跟进对象");
        }
        if (activity.getContent() == null || activity.getContent().isBlank()) {
            throw new BizException("请填写跟进内容");
        }
        activity.setId(null);
        activity.setBizName(resolveBizName(activity.getBizType(), activity.getBizId()));
        activity.setOwnerUserId(SecurityUtils.getUserId());
        if (activity.getType() == null) {
            activity.setType("CALL");
        }
        activityMapper.insert(activity);

        // 同步更新客户的最近/下次跟进时间
        if ("CUSTOMER".equals(activity.getBizType())) {
            Customer customer = new Customer();
            customer.setId(activity.getBizId());
            customer.setLastFollowTime(LocalDateTime.now());
            customer.setNextFollowTime(activity.getNextFollowTime());
            customerMapper.updateById(customer);
        }
        return activity.getId();
    }

    public void delete(Long id) {
        activityMapper.deleteById(id);
    }

    private String resolveBizName(String bizType, Long bizId) {
        return switch (bizType) {
            case "CUSTOMER" -> {
                Customer customer = customerMapper.selectById(bizId);
                yield customer == null ? null : customer.getName();
            }
            case "LEAD" -> {
                Lead lead = leadMapper.selectById(bizId);
                yield lead == null ? null : lead.getName();
            }
            case "OPPORTUNITY" -> {
                Opportunity opportunity = opportunityMapper.selectById(bizId);
                yield opportunity == null ? null : opportunity.getName();
            }
            case "CONTRACT" -> {
                Contract contract = contractMapper.selectById(bizId);
                yield contract == null ? null : contract.getName();
            }
            default -> null;
        };
    }
}
