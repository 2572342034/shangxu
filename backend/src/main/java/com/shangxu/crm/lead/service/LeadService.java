package com.shangxu.crm.lead.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.customer.entity.Contact;
import com.shangxu.crm.customer.entity.Customer;
import com.shangxu.crm.customer.mapper.ContactMapper;
import com.shangxu.crm.customer.service.CustomerService;
import com.shangxu.crm.lead.dto.LeadConvertRequest;
import com.shangxu.crm.lead.entity.Lead;
import com.shangxu.crm.lead.mapper.LeadMapper;
import com.shangxu.crm.opportunity.entity.Opportunity;
import com.shangxu.crm.opportunity.mapper.OpportunityMapper;
import com.shangxu.crm.security.DataScopeHelper;
import com.shangxu.crm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;

@Service
@RequiredArgsConstructor
public class LeadService {
    private final com.shangxu.crm.common.sequence.SequenceService sequenceService;


    private final LeadMapper leadMapper;
    private final CustomerService customerService;
    private final ContactMapper contactMapper;
    private final OpportunityMapper opportunityMapper;

    public PageResult<Lead> page(PageQuery query, String status, String source) {
        Page<Lead> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<Lead> result = leadMapper.selectPageList(page, query.getKeyword(), status, source,
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
        return PageResult.of(result);
    }

    public Lead detail(Long id) {
        Lead lead = leadMapper.selectById(id);
        if (lead == null) {
            throw new BizException("线索不存在");
        }
        return lead;
    }

    public Long create(Lead lead) {
        lead.setId(null);
        lead.setLeadNo(sequenceService.nextNo("lead", "XS"));
        if (lead.getOwnerUserId() == null) {
            lead.setOwnerUserId(SecurityUtils.getUserId());
            lead.setDeptId(SecurityUtils.getDeptId());
        }
        if (lead.getStatus() == null) {
            lead.setStatus("NEW");
        }
        leadMapper.insert(lead);
        return lead.getId();
    }

    public void update(Lead lead) {
        lead.setLeadNo(null);
        leadMapper.updateById(lead);
    }

    public void delete(Long id) {
        leadMapper.deleteById(id);
    }

    /**
     * 线索转化：生成客户 + 联系人（可选商机）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long convert(Long id, LeadConvertRequest request) {
        Lead lead = leadMapper.selectById(id);
        if (lead == null) {
            throw new BizException("线索不存在");
        }
        if ("CONVERTED".equals(lead.getStatus())) {
            throw new BizException("该线索已转化，请勿重复操作");
        }

        Customer customer = new Customer();
        customer.setName(request.customerName() == null || request.customerName().isBlank()
                ? (lead.getCompany() == null ? lead.getName() : lead.getCompany()) : request.customerName());
        customer.setShortName(customer.getName());
        customer.setIndustry(lead.getIndustry());
        customer.setSource(lead.getSource());
        customer.setPhone(lead.getMobile());
        customer.setOwnerUserId(lead.getOwnerUserId());
        customer.setDeptId(lead.getDeptId());
        customer.setLevel(request.level() == null ? "B" : request.level());
        customer.setStatus("FOLLOWING");
        customer.setRemark("由线索【" + lead.getName() + "】转化生成");
        Long customerId = customerService.create(customer);

        if (lead.getContactName() != null && !lead.getContactName().isBlank()) {
            Contact contact = new Contact();
            contact.setCustomerId(customerId);
            contact.setName(lead.getContactName());
            contact.setMobile(lead.getMobile());
            contact.setEmail(lead.getEmail());
            contact.setIsPrimary(1);
            contact.setRemark("线索转化自动创建");
            contactMapper.insert(contact);
        }

        if (request.createOpportunity()) {
            Opportunity opportunity = new Opportunity();
            opportunity.setOppNo(sequenceService.nextNo("opportunity", "SJ"));
            opportunity.setName(request.oppName() == null || request.oppName().isBlank()
                    ? customer.getName() + "-商机" : request.oppName());
            opportunity.setCustomerId(customerId);
            opportunity.setCustomerName(customer.getName());
            opportunity.setAmount(request.oppAmount() == null ? java.math.BigDecimal.ZERO : request.oppAmount());
            opportunity.setStage("CONTACT");
            opportunity.setWinRate(10);
            opportunity.setStatus("OPEN");
            opportunity.setExpectDealDate(LocalDate.now().plusMonths(1));
            opportunity.setOwnerUserId(lead.getOwnerUserId());
            opportunity.setDeptId(lead.getDeptId());
            opportunity.setSource(lead.getSource());
            opportunityMapper.insert(opportunity);
        }

        Lead update = new Lead();
        update.setId(id);
        update.setStatus("CONVERTED");
        update.setConvertedCustomerId(customerId);
        update.setConvertedTime(LocalDateTime.now());
        leadMapper.updateById(update);
        return customerId;
    }
}
