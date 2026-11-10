package com.shangxu.crm.customer.controller;

import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.customer.entity.Contact;
import com.shangxu.crm.customer.entity.Customer;
import com.shangxu.crm.customer.service.CustomerService;
import com.shangxu.crm.log.OperLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "客户管理")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "客户分页列表")
    @GetMapping
    @PreAuthorize("@perm.has('crm:customer:list')")
    public R<PageResult<Customer>> page(PageQuery query,
                                        @RequestParam(required = false) String level,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String industry,
                                        @RequestParam(required = false) String poolType) {
        return R.ok(customerService.page(query, level, status, industry, poolType));
    }

    @Operation(summary = "客户详情")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('crm:customer:list')")
    public R<Customer> detail(@PathVariable Long id) {
        return R.ok(customerService.detail(id));
    }

    @Operation(summary = "客户 360 视图")
    @GetMapping("/{id}/detail")
    @PreAuthorize("@perm.has('crm:customer:list')")
    public R<Map<String, Object>> aggregate(@PathVariable Long id) {
        return R.ok(customerService.aggregate(id));
    }

    @Operation(summary = "客户联系人")
    @GetMapping("/{id}/contacts")
    public R<List<Contact>> contacts(@PathVariable Long id) {
        return R.ok(customerService.contacts(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('crm:customer:add')")
    @OperLog(title = "客户管理", type = "INSERT")
    public R<Long> create(@RequestBody Customer customer) {
        return R.ok("客户创建成功", customerService.create(customer));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('crm:customer:edit')")
    @OperLog(title = "客户管理", type = "UPDATE")
    public R<Void> update(@PathVariable Long id, @RequestBody Customer customer) {
        customer.setId(id);
        customerService.update(customer);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('crm:customer:delete')")
    @OperLog(title = "客户管理", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return R.ok();
    }

    @Operation(summary = "转移客户负责人")
    @PutMapping("/{id}/transfer")
    @PreAuthorize("@perm.has('crm:customer:transfer')")
    @OperLog(title = "客户转移", type = "UPDATE")
    public R<Void> transfer(@PathVariable Long id,
                            @RequestParam Long ownerUserId,
                            @RequestParam(required = false) Long deptId) {
        customerService.transfer(id, ownerUserId, deptId);
        return R.ok("转移成功", null);
    }

    @Operation(summary = "放入公海")
    @PutMapping("/{id}/to-public")
    @PreAuthorize("@perm.has('crm:customer:transfer')")
    @OperLog(title = "客户公海", type = "UPDATE")
    public R<Void> toPublic(@PathVariable Long id) {
        customerService.toPublic(id);
        return R.ok("已放入公海", null);
    }

    @Operation(summary = "领取公海客户")
    @PutMapping("/{id}/claim")
    @PreAuthorize("@perm.has('crm:customer:edit')")
    @OperLog(title = "领取客户", type = "UPDATE")
    public R<Void> claim(@PathVariable Long id) {
        customerService.claim(id);
        return R.ok("领取成功", null);
    }
}
