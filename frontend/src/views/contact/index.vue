<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索姓名 / 手机号 / 客户" clearable @keyup.enter="search" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button v-perm="'crm:contact:add'" type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon>新增联系人
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">联系人列表 <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ total }} 人</span></div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="姓名" width="150">
          <template #default="{ row }">
            <div class="name-cell">
              <el-avatar :size="28" class="mini-avatar">{{ row.name?.slice(-1) }}</el-avatar>
              <div>
                <div class="name">{{ row.name }}</div>
                <div class="post">{{ row.post || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="所属客户" min-width="220" show-overflow-tooltip />
        <el-table-column prop="mobile" label="手机号" width="130" />
        <el-table-column prop="phone" label="座机" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="性别" width="70" align="center">
          <template #default="{ row }">{{ row.gender === 'MALE' ? '男' : row.gender === 'FEMALE' ? '女' : '-' }}</template>
        </el-table-column>
        <el-table-column label="关键联系人" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isPrimary" type="danger" size="small" effect="light">关键人</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="info" size="small" title="查看修改记录" @click="$showChangeLog('CONTACT', row.id, row.name)">记录</el-button>
            <el-button v-perm="'crm:contact:edit'" link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button v-perm="'crm:contact:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无联系人" :image-size="90" /></template>
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑联系人' : '新增联系人'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属客户" prop="customerId">
          <el-select
            v-model="form.customerId"
            filterable
            remote
            reserve-keyword
            :remote-method="searchCustomers"
            :loading="customerLoading"
            placeholder="输入客户名称搜索"
            style="width: 100%"
          >
            <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职务">
              <el-input v-model="form.post" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.mobile" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="座机">
              <el-input v-model="form.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="微信">
              <el-input v-model="form.wechat" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio value="MALE">男</el-radio>
                <el-radio value="FEMALE">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关键联系人">
              <el-switch v-model="form.isPrimary" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createContact, deleteContact, pageContacts, pageCustomers, updateContact } from '@/api/customer'

const loading = ref(false)
const saving = ref(false)
const customerLoading = ref(false)
const list = ref([])
const total = ref(0)
const customerOptions = ref([])
const formRef = ref()
const formVisible = ref(false)

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })

const form = reactive({
  id: null,
  customerId: null,
  name: '',
  post: '',
  mobile: '',
  phone: '',
  email: '',
  wechat: '',
  gender: 'MALE',
  isPrimary: 0,
  remark: ''
})

const rules = {
  customerId: [{ required: true, message: '请选择所属客户', trigger: 'change' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const { data } = await pageContacts(query)
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
  query.keyword = ''
  query.pageNum = 1
  load()
}

async function searchCustomers(keyword) {
  customerLoading.value = true
  try {
    const { data } = await pageCustomers({ pageNum: 1, pageSize: 20, keyword })
    customerOptions.value = data.records || []
  } finally {
    customerLoading.value = false
  }
}

async function openForm(row) {
  Object.assign(form, {
    id: null,
    customerId: null,
    name: '',
    post: '',
    mobile: '',
    phone: '',
    email: '',
    wechat: '',
    gender: 'MALE',
    isPrimary: 0,
    remark: ''
  })
  if (row) {
    Object.assign(form, row)
    customerOptions.value = [{ id: row.customerId, name: row.customerName }]
  } else if (!customerOptions.value.length) {
    await searchCustomers('')
  }
  formVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateContact(form.id, form)
    } else {
      await createContact(form)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除联系人「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteContact(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mini-avatar {
  background: #eef3ff;
  color: #2f6bff;
  font-size: 12px;
  font-weight: 600;
}

.name {
  font-size: 13px;
  color: #2c3648;
  font-weight: 500;
}

.post {
  font-size: 11px;
  color: #a3adbd;
}
</style>
