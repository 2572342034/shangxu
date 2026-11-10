package com.shangxu.crm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shangxu.crm.common.PageQuery;
import com.shangxu.crm.common.PageResult;
import com.shangxu.crm.system.entity.SysDict;
import com.shangxu.crm.system.mapper.SysDictMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SysDictService {

    private final SysDictMapper dictMapper;

    public PageResult<SysDict> page(PageQuery query, String dictType) {
        Page<SysDict> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<SysDict> result = dictMapper.selectPage(page, new LambdaQueryWrapper<SysDict>()
                .eq(dictType != null && !dictType.isBlank(), SysDict::getDictType, dictType)
                .like(query.getKeyword() != null && !query.getKeyword().isBlank(), SysDict::getDictLabel, query.getKeyword())
                .orderByAsc(SysDict::getDictType)
                .orderByAsc(SysDict::getSort));
        return PageResult.of(result);
    }

    public List<SysDict> listByType(String dictType) {
        return dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, dictType)
                .eq(SysDict::getStatus, 1)
                .orderByAsc(SysDict::getSort));
    }

    /**
     * 一次性返回所有启用的字典，供前端缓存
     */
    public Map<String, List<SysDict>> all() {
        List<SysDict> list = dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getStatus, 1)
                .orderByAsc(SysDict::getSort));
        Map<String, List<SysDict>> result = new LinkedHashMap<>();
        list.forEach(dict -> result.computeIfAbsent(dict.getDictType(), k -> new java.util.ArrayList<>()).add(dict));
        return result;
    }

    public Long create(SysDict dict) {
        dict.setId(null);
        dictMapper.insert(dict);
        return dict.getId();
    }

    public void update(SysDict dict) {
        dictMapper.updateById(dict);
    }

    public void delete(Long id) {
        dictMapper.deleteById(id);
    }
}
