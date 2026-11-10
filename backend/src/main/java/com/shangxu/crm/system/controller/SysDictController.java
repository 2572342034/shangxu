package com.shangxu.crm.system.controller;

import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.common.R;
import com.shangxu.crm.log.OperLog;
import com.shangxu.crm.system.entity.SysDict;
import com.shangxu.crm.system.service.SysDictService;
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

@Tag(name = "系统管理-字典")
@RestController
@RequestMapping("/api/system/dicts")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService dictService;

    @GetMapping
    @PreAuthorize("@perm.has('system:dict:list')")
    public R<PageResult<SysDict>> page(PageQuery query, @RequestParam(required = false) String dictType) {
        return R.ok(dictService.page(query, dictType));
    }

    @GetMapping("/type/{dictType}")
    public R<List<SysDict>> listByType(@PathVariable String dictType) {
        return R.ok(dictService.listByType(dictType));
    }

    /** 前端启动时一次性拉取全部字典 */
    @GetMapping("/all")
    public R<Map<String, List<SysDict>>> all() {
        return R.ok(dictService.all());
    }

    @PostMapping
    @PreAuthorize("@perm.has('system:dict:add')")
    @OperLog(title = "数据字典", type = "INSERT")
    public R<Long> create(@RequestBody SysDict dict) {
        return R.ok("新增成功", dictService.create(dict));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('system:dict:edit')")
    @OperLog(title = "数据字典", type = "UPDATE")
    public R<Void> update(@PathVariable Long id, @RequestBody SysDict dict) {
        dict.setId(id);
        dictService.update(dict);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('system:dict:delete')")
    @OperLog(title = "数据字典", type = "DELETE")
    public R<Void> delete(@PathVariable Long id) {
        dictService.delete(id);
        return R.ok();
    }
}
