package com.shangxu.crm.opportunity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.opportunity.entity.Opportunity;
import com.shangxu.crm.opportunity.mapper.OpportunityMapper;
import com.shangxu.crm.security.DataScopeHelper;
import com.shangxu.crm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpportunityService {
    private final com.shangxu.crm.common.sequence.SequenceService sequenceService;


    /** 销售阶段及默认赢单率 */
    public static final Map<String, Integer> STAGES = new LinkedHashMap<>() {{
        put("CONTACT", 10);
        put("DEMAND", 30);
        put("QUOTE", 50);
        put("NEGOTIATION", 70);
        put("WON", 100);
        put("LOST", 0);
    }};

    private final OpportunityMapper opportunityMapper;

    public PageResult<Opportunity> page(PageQuery query, String stage, String status, Long customerId) {
        Page<Opportunity> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<Opportunity> result = opportunityMapper.selectPageList(page, query.getKeyword(), stage, status, customerId,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
        return PageResult.of(result);
    }

    public List<Opportunity> listByCustomer(Long customerId) {
        Page<Opportunity> page = new Page<>(1, 100);
        return opportunityMapper.selectPageList(page, null, null, null, customerId,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId()).getRecords();
    }

    /** 看板视图：按阶段分组 */
    public List<Map<String, Object>> kanban() {
        Page<Opportunity> page = new Page<>(1, 200);
        List<Opportunity> all = opportunityMapper.selectPageList(page, null, null, null, null,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId()).getRecords();
        List<Map<String, Object>> result = new ArrayList<>();
        STAGES.forEach((stage, rate) -> {
            if ("LOST".equals(stage)) {
                return;
            }
            List<Opportunity> items = all.stream().filter(o -> stage.equals(o.getStage())).toList();
            Map<String, Object> column = new LinkedHashMap<>();
            column.put("stage", stage);
            column.put("winRate", rate);
            column.put("count", items.size());
            column.put("amount", items.stream()
                    .map(Opportunity::getAmount)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
            column.put("items", items);
            result.add(column);
        });
        return result;
    }

    public Opportunity detail(Long id) {
        Opportunity opportunity = opportunityMapper.selectById(id);
        if (opportunity == null) {
            throw new BizException("商机不存在");
        }
        return opportunity;
    }

    public Long create(Opportunity opportunity) {
        if (opportunity.getCustomerId() == null) {
            throw new BizException("请选择关联客户");
        }
        opportunity.setId(null);
        opportunity.setOppNo(sequenceService.nextNo("opportunity", "SJ"));
        if (opportunity.getOwnerUserId() == null) {
            opportunity.setOwnerUserId(SecurityUtils.getUserId());
            opportunity.setDeptId(SecurityUtils.getDeptId());
        }
        applyStage(opportunity, opportunity.getStage() == null ? "CONTACT" : opportunity.getStage());
        opportunityMapper.insert(opportunity);
        return opportunity.getId();
    }

    public void update(Opportunity opportunity) {
        opportunity.setOppNo(null);
        if (opportunity.getStage() != null) {
            applyStage(opportunity, opportunity.getStage());
        }
        opportunityMapper.updateById(opportunity);
    }

    public void delete(Long id) {
        opportunityMapper.deleteById(id);
    }

    /** 推进销售阶段 */
    public void changeStage(Long id, String stage) {
        if (!STAGES.containsKey(stage)) {
            throw new BizException("无效的销售阶段");
        }
        Opportunity opportunity = new Opportunity();
        opportunity.setId(id);
        applyStage(opportunity, stage);
        opportunityMapper.updateById(opportunity);
    }

    private void applyStage(Opportunity opportunity, String stage) {
        opportunity.setStage(stage);
        opportunity.setWinRate(STAGES.getOrDefault(stage, 10));
        if ("WON".equals(stage)) {
            opportunity.setStatus("WON");
            opportunity.setActualDealDate(LocalDate.now());
        } else if ("LOST".equals(stage)) {
            opportunity.setStatus("LOST");
        } else {
            opportunity.setStatus("OPEN");
        }
    }
}
