package com.shangxu.crm.lead.controller;

import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.lead.dto.LeadConvertRequest;
import com.shangxu.crm.lead.entity.Lead;
import com.shangxu.crm.lead.service.LeadService;
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

@Tag(name = "线索管理")
@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    @PreAuthorize("@perm.has('crm:lead:list')")
    public R<PageResult<Lead>> page(PageQuery query,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String source) {
        return R.ok(leadService.page(query, status, source));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('crm:lead:list')")
    public R<Lead> detail(@PathVariable Long id) {
        return R.ok(leadService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('crm:lead:add')")
    @OperLog(title = "线索管理", type = "INSERT")
    public R<Long> create(@RequestBody Lead lead) {
        return R.ok("新增成功", leadService.create(lead));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('crm:lead:edit')")
    @OperLog(title = "线索管理", type = "UPDATE")
    public R<Void> update(@PathVariable Long id, @RequestBody Lead lead) {
        lead.setId(id);
        leadService.update(lead);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('crm:lead:delete')")
    @OperLog(title = "线索管理", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        leadService.delete(id);
        return R.ok();
    }

    @Operation(summary = "线索转化为客户")
    @PostMapping("/{id}/convert")
    @PreAuthorize("@perm.has('crm:lead:convert')")
    @OperLog(title = "线索转化", type = "UPDATE")
    public R<Long> convert(@PathVariable Long id, @RequestBody LeadConvertRequest request) {
        return R.ok("转化成功", leadService.convert(id, request));
    }
}
