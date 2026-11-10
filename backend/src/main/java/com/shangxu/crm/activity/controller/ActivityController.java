package com.shangxu.crm.activity.controller;

import com.shangxu.crm.activity.entity.Activity;
import com.shangxu.crm.activity.service.ActivityService;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.log.OperLog;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "跟进记录")
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    @PreAuthorize("@perm.has('crm:activity:list')")
    public R<PageResult<Activity>> page(PageQuery query,
                                        @RequestParam(required = false) String bizType,
                                        @RequestParam(required = false) Long bizId) {
        return R.ok(activityService.page(query, bizType, bizId));
    }

    @GetMapping("/recent")
    public R<List<Activity>> recent(@RequestParam(defaultValue = "10") int limit) {
        return R.ok(activityService.recent(limit));
    }

    @PostMapping
    @PreAuthorize("@perm.has('crm:activity:add')")
    @OperLog(title = "跟进记录", type = "INSERT")
    public R<Long> create(@RequestBody Activity activity) {
        return R.ok("跟进已记录", activityService.create(activity));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('crm:activity:delete')")
    @OperLog(title = "跟进记录", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        activityService.delete(id);
        return R.ok();
    }
}
