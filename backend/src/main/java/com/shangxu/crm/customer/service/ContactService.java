package com.shangxu.crm.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.customer.entity.Contact;
import com.shangxu.crm.customer.mapper.ContactMapper;
import com.shangxu.crm.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactMapper contactMapper;

    public PageResult<Contact> page(PageQuery query, Long customerId) {
        Page<Contact> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<Contact> result = contactMapper.selectPageList(page, customerId, query.getKeyword(),
                DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
        return PageResult.of(result);
    }

    public List<Contact> listByCustomer(Long customerId) {
        return contactMapper.selectList(new LambdaQueryWrapper<Contact>()
                .eq(Contact::getCustomerId, customerId)
                .orderByDesc(Contact::getIsPrimary)
                .orderByAsc(Contact::getId));
    }

    public Long create(Contact contact) {
        if (contact.getCustomerId() == null) {
            throw new BizException("请选择所属客户");
        }
        contact.setId(null);
        if (contact.getIsPrimary() == null) {
            contact.setIsPrimary(0);
        }
        contactMapper.insert(contact);
        return contact.getId();
    }

    public void update(Contact contact) {
        contactMapper.updateById(contact);
    }

    public void delete(Long id) {
        contactMapper.deleteById(id);
    }
}
