<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索账号 / 姓名 / 手机号" clearable @keyup.enter="search" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-tree-select
        v-model="query.deptId"
        :data="deptTree"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        check-strictly
        clearable
        placeholder="所属部门"
        style="width: 180px"
        @change="search"
      />
      <el-select v-model="query.status" placeholder="账号状态" clearable style="width: 140px" @change="search">
        <el-option label="正常" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button v-perm="'system:user:add'" type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon>新增用户
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">用户列表 <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ total }} 人</span></div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" class="u-avatar">{{ row.nickname?.slice(-1) }}</el-avatar>
              <div>
                <div class="u-name">{{ row.nickname }}</div>
                <div class="u-account">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="deptName" label="所属部门" width="140">
          <template #default="{ row }">{{ row.deptName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="post" label="岗位" width="140">
          <template #default="{ row }">{{ row.post || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="name in row.roleNames" :key="name" size="small" effect="plain" style="margin-right: 4px">
              {{ name }}
            </el-tag>
            <span v-if="!row.roleNames?.length" class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="mobile" label="手机号" width="130" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="!canEdit || row.id === 1"
              @change="(val) => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="150">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-perm="'system:user:edit'" link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button v-perm="'system:user:reset'" link type="warning" size="small" @click="resetPwd(row)">重置密码</el-button>
            <el-button
              v-if="row.id !== 1"
              v-perm="'system:user:delete'"
              link
              type="danger"
              size="small"
              @click="remove(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无用户" :image-size="90" /></template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑用户' : '新增用户'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="登录账号" prop="username">
              <el-input v-model="form.username" :disabled="Boolean(form.id)" placeholder="英文、数字" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="nickname">
              <el-input v-model="form.nickname" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="form.id ? '新密码' : '初始密码'">
              <el-input v-model="form.password" type="password" show-password :placeholder="form.id ? '留空则不修改' : '默认 123456'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.mobile" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属部门">
              <el-tree-select
                v-model="form.deptId"
                :data="deptTree"
                :props="{ label: 'name', children: 'children' }"
                node-key="id"
                check-strictly
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位">
              <el-input v-model="form.post" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="停用" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色" prop="roleIds">
              <el-select v-model="form.roleIds" multiple style="width: 100%" placeholder="请选择角色">
                <el-option v-for="role in roles" :key="role.id" :label="role.name" :value="role.id" />
              </el-select>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  changeUserStatus,
  createUser,
  deleteUser,
  deptTree as fetchDeptTree,
  getUser,
  listRoles,
  pageUsers,
  resetUserPassword,
  updateUser
} from '@/api/system'
import { useUserStore } from '@/store/user'
import { formatDateTime } from '@/utils/format'

const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const roles = ref([])
const deptTree = ref([])
const formRef = ref()
const formVisible = ref(false)

const canEdit = computed(() => userStore.hasPerm('system:user:edit'))

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', deptId: null, status: null })

const form = reactive({
  id: null,
  username: '',
  nickname: '',
  password: '',
  mobile: '',
  email: '',
  deptId: null,
  post: '',
  status: 1,
  roleIds: []
})

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleIds: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    const { data } = await pageUsers(query)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function resetQuery() {
  Object.assign(query, { keyword: '', deptId: null, status: null, pageNum: 1 })
  load()
}

async function openForm(row) {
  Object.assign(form, {
    id: null,
    username: '',
    nickname: '',
    password: '',
    mobile: '',
    email: '',
    deptId: null,
    post: '',
    status: 1,
    roleIds: []
  })
  if (row) {
    const { data } = await getUser(row.id)
    Object.assign(form, data, { password: '' })
  }
  if (!roles.value.length) {
    const { data } = await listRoles()
    roles.value = data || []
  }
  formVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateUser(form.id, form)
    } else {
      await createUser(form)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row, status) {
  try {
    await changeUserStatus(row.id, status)
    row.status = status
    ElMessage.success(status === 1 ? '账号已启用' : '账号已停用')
  } catch (e) {
    load()
  }
}

async function resetPwd(row) {
  const { value } = await ElMessageBox.prompt(`请输入「${row.nickname}」的新密码`, '重置密码', {
    inputType: 'password',
    inputValue: '123456',
    inputValidator: (val) => (val && val.length >= 6 ? true : '密码长度不能少于 6 位')
  })
  await resetUserPassword(row.id, value)
  ElMessage.success('密码已重置')
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除用户「${row.nickname}」吗？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  const { data } = await fetchDeptTree()
  deptTree.value = data || []
  await load()
})
</script>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}

.u-avatar {
  background: #eef3ff;
  color: #2f6bff;
  font-size: 12px;
  font-weight: 600;
}

.u-name {
  font-size: 13px;
  font-weight: 500;
  color: #2c3648;
}

.u-account {
  font-size: 11px;
  color: #a3adbd;
  margin-top: 1px;
}
</style>
