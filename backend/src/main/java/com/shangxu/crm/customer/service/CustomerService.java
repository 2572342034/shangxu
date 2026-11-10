package com.shangxu.crm.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.changelog.service.ChangeLogService;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.customer.entity.Contact;
import com.shangxu.crm.customer.entity.Customer;
import com.shangxu.crm.customer.mapper.ContactMapper;
import com.shangxu.crm.customer.mapper.CustomerMapper;
import com.shangxu.crm.security.DataScopeHelper;
import com.shangxu.crm.security.LoginUser;
import com.shangxu.crm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final com.shangxu.crm.common.sequence.SequenceService sequenceService;
    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final com.shangxu.crm.system.mapper.SysUserMapper userMapper;
    private final com.shangxu.crm.system.mapper.SysDeptMapper deptMapper;
    private final com.shangxu.crm.opportunity.service.OpportunityService opportunityService;
    private final com.shangxu.crm.contract.service.ContractService contractService;
    private final com.shangxu.crm.activity.service.ActivityService activityService;
    private final ChangeLogService changeLogService;

    /** 客户 360 详情：基本信息 + 联系人 + 商机 + 合同 + 跟进记录。历史负责人不下发关联数据。 */
    public java.util.Map<String, Object> aggregate(Long id) {
        Customer customer = detail(id);
        boolean readOnly = Boolean.TRUE.equals(customer.getReadOnly());

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("customer", customer);
        result.put("readOnly", readOnly);
        result.put("masked", readOnly);
        if (readOnly) {
            result.put("contacts", List.of());
            result.put("opportunities", List.of());
            result.put("contracts", List.of());
            result.put("activities", List.of());
            result.put("opportunityAmount", BigDecimal.ZERO);
            result.put("contractAmount", BigDecimal.ZERO);
            return result;
        }

        List<com.shangxu.crm.opportunity.entity.Opportunity> opportunities = opportunityService.listByCustomer(id);
        List<com.shangxu.crm.contract.entity.Contract> contracts = contractService.listByCustomer(id);
        PageQuery activityQuery = new PageQuery();
        activityQuery.setPageNum(1);
        activityQuery.setPageSize(50);
        List<com.shangxu.crm.activity.entity.Activity> activities =
                activityService.page(activityQuery, "CUSTOMER", id).getRecords();

        BigDecimal opportunityAmount = opportunities.stream()
                .map(o -> o.getAmount() == null ? BigDecimal.ZERO : o.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal contractAmount = contracts.stream()
                .map(c -> c.getAmount() == null ? BigDecimal.ZERO : c.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        result.put("contacts", contacts(id));
        result.put("opportunities", opportunities);
        result.put("contracts", contracts);
        result.put("activities", activities);
        result.put("opportunityAmount", opportunityAmount);
        result.put("contractAmount", contractAmount);
        return result;
    }

    public PageResult<Customer> page(PageQuery query, String level, String status, String industry, String poolType) {
        // 历史客户：曾负责过、现已移交，只返回脱敏摘要
        if ("HISTORY".equals(poolType)) {
            IPage<Customer> history = new Page<>(query.getPageNum(), query.getPageSize());
            IPage<Customer> result = customerMapper.selectHistoryPage(history,
                    SecurityUtils.getUserId(), query.getKeyword());
            result.getRecords().forEach(this::mask);
            return PageResult.of(result);
        }

        Page<Customer> page = new Page<>(query.getPageNum(), query.getPageSize());
        Long ownerId = null;
        Long deptId = null;
        // 公海对所有销售可见，不受数据权限限制
        if (!"PUBLIC".equals(poolType)) {
            ownerId = DataScopeHelper.scopeOwnerId();
            deptId = DataScopeHelper.scopeDeptId();
        }
        IPage<Customer> result = customerMapper.selectPageList(page, query.getKeyword(), level, status,
                industry, poolType, ownerId, deptId);
        return PageResult.of(result);
    }

    public Customer detail(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BizException("客户不存在或已删除");
        }
        applyReadScope(customer);
        if (customer.getOwnerUserId() != null) {
            com.shangxu.crm.system.entity.SysUser owner = userMapper.selectById(customer.getOwnerUserId());
            customer.setOwnerName(owner == null ? null : owner.getNickname());
        }
        if (customer.getDeptId() != null) {
            com.shangxu.crm.system.entity.SysDept dept = deptMapper.selectById(customer.getDeptId());
            customer.setDeptName(dept == null ? null : dept.getName());
        }
        return customer;
    }

    /** 读权限：ALL 全量 / DEPT 本部门 / SELF 仅本人；有历史轨迹者看脱敏摘要，其余拒绝。 */
    private void applyReadScope(Customer customer) {
        LoginUser me = SecurityUtils.getLoginUser();
        if (me == null) {
            throw new BizException("未登录");
        }
        if ("ALL".equals(me.getDataScope()) || "PUBLIC".equals(customer.getPoolType())) {
            return;
        }
        boolean owner = me.getId().equals(customer.getOwnerUserId());
        boolean sameDept = "DEPT".equals(me.getDataScope()) && me.getDeptId() != null
                && me.getDeptId().equals(customer.getDeptId());
        if (owner || sameDept) {
            return;
        }
        if (changeLogService.hasOwnerHistory(customer.getId(), me.getId())) {
            mask(customer);
            return;
        }
        throw new BizException("无权访问该客户");
    }

    /** 写权限：历史负责人一律拦截，避免从编辑/转移/删除接口绕过只读。 */
    private Customer requireWritable(Long id) {
        Customer exist = customerMapper.selectById(id);
        if (exist == null) {
            throw new BizException("客户不存在");
        }
        LoginUser me = SecurityUtils.getLoginUser();
        if (me == null) {
            throw new BizException("未登录");
        }
        if (!"ALL".equals(me.getDataScope())) {
            boolean owner = me.getId().equals(exist.getOwnerUserId());
            boolean sameDept = "DEPT".equals(me.getDataScope()) && me.getDeptId() != null
                    && me.getDeptId().equals(exist.getDeptId());
            if (!owner && !sameDept) {
                if (changeLogService.hasOwnerHistory(id, me.getId())) {
                    throw new BizException("该客户已移交，仅可查看摘要，不能修改");
                }
                throw new BizException("无权操作该客户");
            }
        }
        return exist;
    }

    private void mask(Customer c) {
        c.setPhone(null);
        c.setWebsite(null);
        c.setAddress(null);
        c.setProvince(null);
        c.setCity(null);
        c.setRemark(null);
        c.setScale(null);
        c.setShortName(null);
        c.setNextFollowTime(null);
        c.setLastFollowTime(null);
        c.setReadOnly(true);
        c.setMasked(true);
    }

    public List<Contact> contacts(Long customerId) {
        Customer customer = detail(customerId);
        if (Boolean.TRUE.equals(customer.getReadOnly())) {
            return List.of();
        }
        return contactMapper.selectList(new LambdaQueryWrapper<Contact>()
                .eq(Contact::getCustomerId, customerId)
                .orderByDesc(Contact::getIsPrimary)
                .orderByAsc(Contact::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(Customer customer) {
        if (customer.getName() == null || customer.getName().isBlank()) {
            throw new BizException("请填写客户名称");
        }
        Long exist = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getName, customer.getName()));
        if (exist != null && exist > 0) {
            throw new BizException("同名客户已存在，请确认是否重复创建");
        }
        customer.setId(null);
        customer.setCustomerNo(nextCustomerNo());
        if (customer.getOwnerUserId() == null) {
            customer.setOwnerUserId(SecurityUtils.getUserId());
            customer.setDeptId(SecurityUtils.getDeptId());
        }
        if (customer.getPoolType() == null) {
            customer.setPoolType("PRIVATE");
        }
        if (customer.getStatus() == null) {
            customer.setStatus("FOLLOWING");
        }
        if (customer.getLevel() == null) {
            customer.setLevel("C");
        }
        customerMapper.insert(customer);
        return customer.getId();
    }

    public void update(Customer customer) {
        requireWritable(customer.getId());
        customer.setCustomerNo(null);
        customer.setPoolType(null);
        customerMapper.updateById(customer);
    }

    public void delete(Long id) {
        requireWritable(id);
        customerMapper.deleteById(id);
    }

    /** 转移客户负责人 */
    public void transfer(Long id, Long ownerUserId, Long deptId) {
        if (ownerUserId == null) {
            throw new BizException("请选择新的负责人");
        }
        requireWritable(id);
        Customer customer = new Customer();
        customer.setId(id);
        customer.setOwnerUserId(ownerUserId);
        customer.setDeptId(deptId);
        customer.setPoolType("PRIVATE");
        customerMapper.updateById(customer);
    }

    /** 放入公海 */
    public void toPublic(Long id) {
        requireWritable(id);
        // updateById 默认忽略 null 字段，清空负责人必须用 LambdaUpdateWrapper 显式 set
        customerMapper.update(null, new LambdaUpdateWrapper<Customer>()
                .eq(Customer::getId, id)
                .set(Customer::getOwnerUserId, null)
                .set(Customer::getDeptId, null)
                .set(Customer::getPoolType, "PUBLIC"));
    }

    /** 领取公海客户：条件更新防止两人同时抢到同一条 */
    public void claim(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BizException("客户不存在");
        }
        if (!"PUBLIC".equals(customer.getPoolType())) {
            throw new BizException("该客户已被领取");
        }
        Customer update = new Customer();
        update.setId(id);
        update.setOwnerUserId(SecurityUtils.getUserId());
        update.setDeptId(SecurityUtils.getDeptId());
        update.setPoolType("PRIVATE");
        update.setLastFollowTime(LocalDateTime.now());
        int rows = customerMapper.update(update, new LambdaUpdateWrapper<Customer>()
                .eq(Customer::getId, id)
                .eq(Customer::getPoolType, "PUBLIC"));
        if (rows == 0) {
            throw new BizException("该客户已被他人领取，请刷新后重试");
        }
    }

    public long countAll() {
        return customerMapper.countAll(DataScopeHelper.scopeOwnerId(), DataScopeHelper.scopeDeptId());
    }

    private String nextCustomerNo() {
        return sequenceService.nextNo("customer", "KH");
    }
}
