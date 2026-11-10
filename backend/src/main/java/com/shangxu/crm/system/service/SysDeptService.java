package com.shangxu.crm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shangxu.crm.common.BizException;
import com.shangxu.crm.system.entity.SysDept;
import com.shangxu.crm.system.mapper.SysDeptMapper;
import com.shangxu.crm.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysDeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    public List<SysDept> list() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getSort));
    }

    public List<SysDept> tree() {
        List<SysDept> all = list();
        Map<Long, List<SysDept>> grouped = all.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId()));
        all.forEach(d -> d.setChildren(grouped.getOrDefault(d.getId(), new ArrayList<>())));
        return all.stream().filter(d -> d.getParentId() == null || d.getParentId() == 0L).toList();
    }

    public Long create(SysDept dept) {
        dept.setId(null);
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        deptMapper.insert(dept);
        return dept.getId();
    }

    public void update(SysDept dept) {
        if (dept.getId() != null && dept.getId().equals(dept.getParentId())) {
            throw new BizException("上级部门不能是自己");
        }
        deptMapper.updateById(dept);
    }

    public void delete(Long id) {
        Long children = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (children != null && children > 0) {
            throw new BizException("存在下级部门，无法删除");
        }
        long users = userMapper.countByDeptId(id);
        if (users > 0) {
            throw new BizException("部门下还有 " + users + " 个用户，无法删除");
        }
        deptMapper.deleteById(id);
    }
}
