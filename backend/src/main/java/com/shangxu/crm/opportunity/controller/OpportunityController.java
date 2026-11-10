package com.shangxu.crm.opportunity.controller;

import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.log.OperLog;
import com.shangxu.crm.opportunity.entity.Opportunity;
import com.shangxu.crm.opportunity.service.OpportunityService;
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

@Tag(name = "商机管理")
@RestController
@RequestMapping("/api/opportunities")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;

    @GetMapping
    @PreAuthorize("@perm.has('crm:opportunity:list')")
    public R<PageResult<Opportunity>> page(PageQuery query,
                                           @RequestParam(required = false) String stage,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) Long customerId) {
        return R.ok(opportunityService.page(query, stage, status, customerId));
    }

    @Operation(summary = "商机看板")
    @GetMapping("/kanban")
    @PreAuthorize("@perm.has('crm:opportunity:list')")
    public R<List<Map<String, Object>>> kanban() {
        return R.ok(opportunityService.kanban());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('crm:opportunity:list')")
    public R<Opportunity> detail(@PathVariable Long id) {
        return R.ok(opportunityService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('crm:opportunity:add')")
    @OperLog(title = "商机管理", type = "INSERT")
    public R<Long> create(@RequestBody Opportunity opportunity) {
        return R.ok("新增成功", opportunityService.create(opportunity));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('crm:opportunity:edit')")
    @OperLog(title = "商机管理", type = "UPDATE")
    public R<Void> update(@PathVariable Long id, @RequestBody Opportunity opportunity) {
        opportunity.setId(id);
        opportunityService.update(opportunity);
        return R.ok();
    }

    @Operation(summary = "推进销售阶段")
    @PutMapping("/{id}/stage")
    @PreAuthorize("@perm.has('crm:opportunity:stage')")
    @OperLog(title = "商机阶段", type = "UPDATE")
    public R<Void> changeStage(@PathVariable Long id, @RequestParam String stage) {
        opportunityService.changeStage(id, stage);
        return R.ok("阶段更新成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('crm:opportunity:delete')")
    @OperLog(title = "商机管理", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        opportunityService.delete(id);
        return R.ok();
    }
}
