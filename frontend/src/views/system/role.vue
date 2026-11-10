<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input v-model="keyword" placeholder="搜索角色名称" clearable style="width: 220px" @keyup.enter="load" @clear="load">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" plain @click="load">查询</el-button>
      <div class="spacer"></div>
      <el-button v-perm="'system:role:add'" type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon>新增角色
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">角色权限</div>
      </div>
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="name" label="角色名称" width="160">
          <template #default="{ row }">
            <div class="role-name">
              <el-icon><Key /></el-icon>{{ row.name }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="角色编码" width="150">
          <template #default="{ row }"><code class="code">{{ row.code }}</code></template>
        </el-table-column>
        <el-table-column label="数据权限" width="150">
          <template #default="{ row }">
            <el-tag :type="scopeTag(row.dataScope)" size="small" effect="light">{{ scopeLabel(row.dataScope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限范围" min-width="260">
          <template #default="{ row }">
            <span v-if="row.permissionList?.includes('*:*:*')" class="all-perm">全部权限</span>
            <span v-else class="text-muted">{{ row.permissionList?.length || 0 }} 项权限</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'system:role:edit'" link type="primary" size="small" :disabled="row.id === 1" @click="openForm(row)">编辑</el-button>
            <el-button
              v-if="row.id !== 1"
              v-perm="'system:role:delete'"
              link
              type="danger"
              size="small"
              @click="remove(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑角色' : '新增角色'" width="760px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="角色名称" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色编码" prop="code">
              <el-input v-model="form.code" placeholder="如 sales_manager" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="数据权限">
              <el-radio-group v-model="form.dataScope">
                <el-radio-button value="ALL">全部数据</el-radio-button>
                <el-radio-button value="DEPT">本部门数据</el-radio-button>
                <el-radio-button value="SELF">仅本人数据</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="功能权限">
              <div class="perm-wrap">
                <div class="perm-toolbar">
                  <el-button size="small" @click="checkAll(true)">全选</el-button>
                  <el-button size="small" @click="checkAll(false)">清空</el-button>
                  <span class="text-muted">已选 {{ checkedCount }} 项</span>
                </div>
                <el-tree
                  ref="treeRef"
                  :data="permissionTree"
                  show-checkbox
                  node-key="id"
                  default-expand-all
                  :props="{ label: 'label', children: 'children' }"
                  class="perm-tree"
                  @check="updateCount"
                />
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createRole, deleteRole, listRoles, updateRole } from '@/api/system'

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const list = ref([])
const formRef = ref()
const treeRef = ref()
const formVisible = ref(false)
const checkedCount = ref(0)

const permissionTree = [
  { id: 'g-dashboard', label: '工作台', children: [{ id: 'dashboard:view', label: '查看工作台' }] },
  {
    id: 'g-customer',
    label: '客户管理',
    children: [
      { id: 'crm:customer:list', label: '查看客户' },
      { id: 'crm:customer:add', label: '新增客户' },
      { id: 'crm:customer:edit', label: '编辑客户' },
      { id: 'crm:customer:delete', label: '删除客户' },
      { id: 'crm:customer:transfer', label: '转移/公海' },
      { id: 'crm:contact:list', label: '查看联系人' },
      { id: 'crm:contact:add', label: '新增联系人' },
      { id: 'crm:contact:edit', label: '编辑联系人' },
      { id: 'crm:contact:delete', label: '删除联系人' }
    ]
  },
  {
    id: 'g-lead',
    label: '线索管理',
    children: [
      { id: 'crm:lead:list', label: '查看线索' },
      { id: 'crm:lead:add', label: '新增线索' },
      { id: 'crm:lead:edit', label: '编辑线索' },
      { id: 'crm:lead:delete', label: '删除线索' },
      { id: 'crm:lead:convert', label: '线索转化' }
    ]
  },
  {
    id: 'g-opp',
    label: '商机管理',
    children: [
      { id: 'crm:opportunity:list', label: '查看商机' },
      { id: 'crm:opportunity:add', label: '新增商机' },
      { id: 'crm:opportunity:edit', label: '编辑商机' },
      { id: 'crm:opportunity:delete', label: '删除商机' },
      { id: 'crm:opportunity:stage', label: '推进阶段' }
    ]
  },
  {
    id: 'g-trade',
    label: '交易管理',
    children: [{ id: 'crm:trade:view', label: '交易总览' }]
  },
  {
    id: 'g-contract',
    label: '合同回款',
    children: [
      { id: 'crm:contract:list', label: '查看合同' },
      { id: 'crm:contract:add', label: '新增合同' },
      { id: 'crm:contract:edit', label: '编辑合同' },
      { id: 'crm:contract:delete', label: '删除合同' },
      { id: 'crm:contract:audit', label: '合同审批' },
      { id: 'crm:payment:list', label: '查看回款' },
      { id: 'crm:payment:add', label: '回款登记' }
    ]
  },
  {
    id: 'g-order',
    label: '订单管理',
    children: [
      { id: 'crm:order:list', label: '查看订单' },
      { id: 'crm:order:add', label: '新增订单' },
      { id: 'crm:order:edit', label: '编辑订单' },
      { id: 'crm:order:delete', label: '删除订单' },
      { id: 'crm:order:audit', label: '订单审批' },
      { id: 'crm:order:deliver', label: '订单出库' }
    ]
  },
  {
    id: 'g-delivery',
    label: '出库管理',
    children: [
      { id: 'crm:delivery:list', label: '查看出库单' },
      { id: 'crm:delivery:add', label: '生成出库单' },
      { id: 'crm:delivery:edit', label: '编辑出库单' },
      { id: 'crm:delivery:delete', label: '删除出库单' },
      { id: 'crm:delivery:ship', label: '出库/签收' }
    ]
  },
  {
    id: 'g-receivable',
    label: '应收管理',
    children: [
      { id: 'crm:receivable:list', label: '查看应收' },
      { id: 'crm:receivable:add', label: '新增应收' },
      { id: 'crm:receivable:receive', label: '收款登记' },
      { id: 'crm:receivable:delete', label: '删除应收' }
    ]
  },
  {
    id: 'g-payment',
    label: '收款核销',
    children: [
      { id: 'crm:payment:list', label: '查看收款单' },
      { id: 'crm:payment:add', label: '登记收款/核销' },
      { id: 'crm:payment:cancel', label: '作废冲销' }
    ]
  },
  {
    id: 'g-plan',
    label: '发货计划',
    children: [
      { id: 'crm:plan:list', label: '查看计划' },
      { id: 'crm:plan:add', label: '新建计划' },
      { id: 'crm:plan:edit', label: '编辑/取消' },
      { id: 'crm:plan:delete', label: '删除计划' },
      { id: 'crm:plan:ship', label: '按计划发货' }
    ]
  },
  {
    id: 'g-invoice',
    label: '开票管理',
    children: [
      { id: 'crm:invoice:list', label: '查看发票' },
      { id: 'crm:invoice:add', label: '申请开票' },
      { id: 'crm:invoice:edit', label: '编辑发票' },
      { id: 'crm:invoice:delete', label: '删除发票' },
      { id: 'crm:invoice:issue', label: '确认开票/作废' }
    ]
  },
  {
    id: 'g-return',
    label: '退货退款',
    children: [
      { id: 'crm:return:list', label: '查看退货' },
      { id: 'crm:return:add', label: '新建退货' },
      { id: 'crm:return:edit', label: '编辑退货' },
      { id: 'crm:return:delete', label: '删除退货' },
      { id: 'crm:return:audit', label: '审核/退款' }
    ]
  },
  {
    id: 'g-product',
    label: '产品管理',
    children: [
      { id: 'crm:product:list', label: '查看产品' },
      { id: 'crm:product:add', label: '新增产品' },
      { id: 'crm:product:edit', label: '编辑产品' },
      { id: 'crm:product:delete', label: '删除产品' }
    ]
  },
  {
    id: 'g-activity',
    label: '跟进记录',
    children: [
      { id: 'crm:activity:list', label: '查看跟进' },
      { id: 'crm:activity:add', label: '新增跟进' },
      { id: 'crm:activity:delete', label: '删除跟进' }
    ]
  },
  {
    id: 'g-system',
    label: '系统管理',
    children: [
      { id: 'system:user:list', label: '用户-查看' },
      { id: 'system:user:add', label: '用户-新增' },
      { id: 'system:user:edit', label: '用户-编辑' },
      { id: 'system:user:delete', label: '用户-删除' },
      { id: 'system:user:reset', label: '用户-重置密码' },
      { id: 'system:role:list', label: '角色-查看' },
      { id: 'system:role:add', label: '角色-新增' },
      { id: 'system:role:edit', label: '角色-编辑' },
      { id: 'system:role:delete', label: '角色-删除' },
      { id: 'system:dept:list', label: '部门-查看' },
      { id: 'system:dept:add', label: '部门-新增' },
      { id: 'system:dept:edit', label: '部门-编辑' },
      { id: 'system:dept:delete', label: '部门-删除' },
      { id: 'system:dict:list', label: '字典-查看' },
      { id: 'system:dict:add', label: '字典-新增' },
      { id: 'system:dict:edit', label: '字典-编辑' },
      { id: 'system:dict:delete', label: '字典-删除' },
      { id: 'system:log:list', label: '操作日志' }
    ]
  }
]

const leafKeys = computed(() => {
  const keys = []
  const walk = (nodes) => {
    nodes.forEach((node) => {
      if (node.children) walk(node.children)
      else keys.push(node.id)
    })
  }
  walk(permissionTree)
  return keys
})

const form = reactive({
  id: null,
  name: '',
  code: '',
  dataScope: 'SELF',
  remark: '',
  permissionList: []
})

const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

function scopeLabel(scope) {
  return { ALL: '全部数据', DEPT: '本部门数据', SELF: '仅本人数据' }[scope] || scope
}

function scopeTag(scope) {
  return { ALL: 'danger', DEPT: 'warning', SELF: 'info' }[scope] || 'info'
}

async function load() {
  loading.value = true
  try {
    const { data } = await listRoles({ keyword: keyword.value })
    list.value = data || []
  } finally {
    loading.value = false
  }
}

async function openForm(row) {
  Object.assign(form, {
    id: null,
    name: '',
    code: '',
    dataScope: 'SELF',
    remark: '',
    permissionList: []
  })
  if (row) {
    Object.assign(form, row)
  }
  formVisible.value = true
  await nextTick()
  treeRef.value?.setCheckedKeys((row?.permissionList || []).filter((key) => leafKeys.value.includes(key)))
  updateCount()
}

function checkAll(checked) {
  treeRef.value?.setCheckedKeys(checked ? leafKeys.value : [])
  updateCount()
}

function updateCount() {
  checkedCount.value = treeRef.value?.getCheckedKeys().filter((key) => leafKeys.value.includes(key)).length || 0
}

async function submit() {
  await formRef.value.validate()
  const checked = treeRef.value.getCheckedKeys().filter((key) => leafKeys.value.includes(key))
  if (!checked.length) {
    ElMessage.warning('请至少选择一项功能权限')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, permissionList: checked }
    if (form.id) {
      await updateRole(form.id, payload)
    } else {
      await createRole(payload)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除角色「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.role-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: #2c3648;
}

.code {
  font-family: Consolas, monospace;
  font-size: 11.5px;
  background: #f4f6fa;
  padding: 2px 6px;
  border-radius: 4px;
  color: #5b6779;
}

.all-perm {
  color: #f56c6c;
  font-weight: 500;
}

.perm-wrap {
  width: 100%;
  border: 1px solid #eef1f6;
  border-radius: 8px;
  padding: 8px 12px;
  max-height: 300px;
  overflow-y: auto;
}

.perm-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #eef1f6;
  margin-bottom: 6px;
}

.perm-tree {
  --el-tree-node-content-height: 28px;
}
</style>
