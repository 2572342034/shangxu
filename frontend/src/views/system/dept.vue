<template>
  <div class="page-container">
    <div class="filter-bar">
      <div class="text-muted">部门层级决定数据权限中「本部门数据」的范围</div>
      <div class="spacer"></div>
      <el-button v-perm="'system:dept:add'" type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon>新增部门
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">组织架构</div>
      </div>
      <el-table
        v-loading="loading"
        :data="tree"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
      >
        <el-table-column prop="name" label="部门名称" min-width="260">
          <template #default="{ row }">
            <div class="dept-name">
              <el-icon><Grid /></el-icon>{{ row.name }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="leader" label="负责人" width="140">
          <template #default="{ row }">{{ row.leader || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'system:dept:add'" link type="primary" size="small" @click="openForm(null, row.id)">新增下级</el-button>
            <el-button v-perm="'system:dept:edit'" link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button v-perm="'system:dept:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑部门' : '新增部门'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="不选则为一级部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leader" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" style="width: 100%" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createDept, deleteDept, deptTree, updateDept } from '@/api/system'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const tree = ref([])
const formRef = ref()
const formVisible = ref(false)

const form = reactive({ id: null, parentId: 0, name: '', leader: '', sort: 0, status: 1 })

const rules = { name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }

const parentOptions = computed(() => [
  { id: 0, name: '顶级部门', children: tree.value }
])

async function load() {
  loading.value = true
  try {
    const { data } = await deptTree()
    tree.value = data || []
  } finally {
    loading.value = false
  }
}

function openForm(row, parentId) {
  Object.assign(form, { id: null, parentId: parentId ?? 0, name: '', leader: '', sort: 0, status: 1 })
  if (row) Object.assign(form, row)
  formVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateDept(form.id, form)
    } else {
      await createDept(form)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除部门「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteDept(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.dept-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: #2c3648;
  vertical-align: middle;
}
</style>
