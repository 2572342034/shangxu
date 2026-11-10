package com.shangxu.crm.customer.controller;

import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.customer.entity.Contact;
import com.shangxu.crm.customer.service.ContactService;
import com.shangxu.crm.log.OperLog;
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

@Tag(name = "联系人管理")
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    @PreAuthorize("@perm.has('crm:contact:list')")
    public R<PageResult<Contact>> page(PageQuery query, @RequestParam(required = false) Long customerId) {
        return R.ok(contactService.page(query, customerId));
    }

    @PostMapping
    @PreAuthorize("@perm.has('crm:contact:add')")
    @OperLog(title = "联系人管理", type = "INSERT")
    public R<Long> create(@RequestBody Contact contact) {
        return R.ok("新增成功", contactService.create(contact));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('crm:contact:edit')")
    @OperLog(title = "联系人管理", type = "UPDATE")
    public R<Void> update(@PathVariable Long id, @RequestBody Contact contact) {
        contact.setId(id);
        contactService.update(contact);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('crm:contact:delete')")
    @OperLog(title = "联系人管理", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        contactService.delete(id);
        return R.ok();
    }
}
