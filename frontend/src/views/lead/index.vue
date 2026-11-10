<template>
  <div class="page-container">
    <div class="status-cards">
      <div
        v-for="item in statusCards"
        :key="item.value"
        class="status-card"
        :class="{ active: query.status === item.value }"
        @click="switchStatus(item.value)"
      >
        <div class="status-label">{{ item.label }}</div>
        <div class="status-value">{{ item.count }}</div>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索线索 / 公司 / 联系人" clearable @keyup.enter="search" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.source" placeholder="线索来源" clearable @change="search">
        <el-option v-for="item in dictStore.options('customer_source')" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <el-button link type="primary" @click="$router.push({ path: '/system/data-export', query: { module: 'LEAD' } })">批量导出</el-button>
      <div class="spacer"></div>
      <el-button v-perm="'crm:lead:add'" type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon>新建线索
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">线索列表 <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ total }} 条</span></div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="线索名称" min-width="230" fixed="left">
          <template #default="{ row }">
            <div class="lead-name">{{ row.name }}</div>
            <div class="cell-sub">{{ row.leadNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="company" label="公司" min-width="200" show-overflow-tooltip />
        <el-table-column label="联系人" width="140">
          <template #default="{ row }">
            <div>{{ row.contactName || '-' }}</div>
            <div class="cell-sub">{{ row.mobile || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="110">
          <template #default="{ row }">{{ dictStore.label('customer_source', row.source) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }"><DictTag type="lead_status" :value="row.status" /></template>
        </el-table-column>
        <el-table-column prop="ownerName" label="负责人" width="90">
          <template #default="{ row }">{{ row.ownerName || '-' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="120">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="info" size="small" title="查看修改记录" @click="$showChangeLog('LEAD', row.id, row.leadNo)">记录</el-button>
            <el-button
              v-if="row.status !== 'CONVERTED'"
              v-perm="'crm:lead:convert'"
              link
              type="success"
              size="small"
              @click="openConvert(row)"
            >
              转化
            </el-button>
            <el-button v-if="row.status !== 'CONVERTED'" v-perm="'crm:lead:edit'" link type="primary" size="small" @click="openForm(row)">
              编辑
            </el-button>
            <el-button v-perm="'crm:lead:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无线索" :image-size="90" /></template>
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

    <!-- 线索表单 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑线索' : '新建线索'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="14">
          <el-col :span="24">
            <el-form-item label="线索名称" prop="name">
              <el-input v-model="form.name" placeholder="如：XX 公司 CRM 采购需求" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公司名称">
              <el-input v-model="form.company" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="form.contactName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.mobile" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="线索来源">
              <el-select v-model="form.source" style="width: 100%" clearable>
                <el-option v-for="item in dictStore.options('customer_source')" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属行业">
              <el-select v-model="form.industry" style="width: 100%" clearable>
                <el-option v-for="item in dictStore.options('industry')" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-select v-model="form.ownerUserId" filterable clearable style="width: 100%" placeholder="默认当前用户">
                <el-option v-for="user in users" :key="user.id" :label="user.nickname" :value="user.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="线索状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="新线索" value="NEW" />
                <el-option label="跟进中" value="FOLLOWING" />
                <el-option label="已确认" value="QUALIFIED" />
                <el-option label="无效" value="INVALID" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 转化 -->
    <el-dialog v-model="convertVisible" title="线索转化" width="520px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
        转化后将自动创建客户与联系人，可选择同步创建商机。
      </el-alert>
      <el-form :model="convertForm" label-width="100px">
        <el-form-item label="客户名称">
          <el-input v-model="convertForm.customerName" />
        </el-form-item>
        <el-form-item label="客户级别">
          <el-radio-group v-model="convertForm.level">
            <el-radio-button value="A">A</el-radio-button>
            <el-radio-button value="B">B</el-radio-button>
            <el-radio-button value="C">C</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="创建商机">
          <el-switch v-model="convertForm.createOpportunity" />
        </el-form-item>
        <template v-if="convertForm.createOpportunity">
          <el-form-item label="商机名称">
            <el-input v-model="convertForm.oppName" placeholder="默认：客户名称-商机" />
          </el-form-item>
          <el-form-item label="预计金额">
            <el-input-number v-model="convertForm.oppAmount" :min="0" :step="10000" style="width: 100%" controls-position="right" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="convertVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitConvert">确认转化</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import DictTag from '@/components/DictTag.vue'
import { convertLead, createLead, deleteLead, pageLeads, updateLead } from '@/api/lead'
import { userOptions } from '@/api/system'
import { useDictStore } from '@/store/dict'
import { formatDate } from '@/utils/format'

const router = useRouter()
const route = useRoute()
const dictStore = useDictStore()

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const users = ref([])
const formRef = ref()
const formVisible = ref(false)
const convertVisible = ref(false)

const statusCards = ref([
  { label: '全部线索', value: '', count: 0 },
  { label: '新线索', value: 'NEW', count: 0 },
  { label: '跟进中', value: 'FOLLOWING', count: 0 },
  { label: '已确认', value: 'QUALIFIED', count: 0 },
  { label: '已转化', value: 'CONVERTED', count: 0 }
])

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '', source: '' })

const form = reactive({
  id: null,
  name: '',
  company: '',
  contactName: '',
  mobile: '',
  email: '',
  source: '',
  industry: '',
  status: 'NEW',
  ownerUserId: null,
  remark: ''
})

const rules = { name: [{ required: true, message: '请输入线索名称', trigger: 'blur' }] }

const convertForm = reactive({
  id: null,
  customerName: '',
  level: 'B',
  createOpportunity: true,
  oppName: '',
  oppAmount: 100000
})

async function load() {
  loading.value = true
  try {
    const { data } = await pageLeads(query)
    list.value = data.records || []
    total.value = data.total || 0
    await loadStats()
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const { data } = await pageLeads({ pageNum: 1, pageSize: 1 })
  statusCards.value[0].count = data.total || 0
  const statuses = ['NEW', 'FOLLOWING', 'QUALIFIED', 'CONVERTED']
  const results = await Promise.all(statuses.map((s) => pageLeads({ pageNum: 1, pageSize: 1, status: s })))
  results.forEach((res, index) => {
    statusCards.value[index + 1].count = res.data.total || 0
  })
}

function switchStatus(status) {
  query.status = status
  query.pageNum = 1
  load()
}

function search() {
  query.pageNum = 1
  load()
}

function resetQuery() {
  Object.assign(query, { keyword: '', source: '', status: '', pageNum: 1 })
  load()
}

async function ensureUsers() {
  if (!users.value.length) {
    const { data } = await userOptions()
    users.value = data || []
  }
}

async function openForm(row) {
  await ensureUsers()
  Object.assign(form, {
    id: null,
    name: '',
    company: '',
    contactName: '',
    mobile: '',
    email: '',
    source: '',
    industry: '',
    status: 'NEW',
    ownerUserId: null,
    remark: ''
  })
  if (row) Object.assign(form, row)
  formVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateLead(form.id, form)
    } else {
      await createLead(form)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openConvert(row) {
  Object.assign(convertForm, {
    id: row.id,
    customerName: row.company || row.name,
    level: 'B',
    createOpportunity: true,
    oppName: `${row.company || row.name}-商机`,
    oppAmount: 100000
  })
  convertVisible.value = true
}

async function submitConvert() {
  saving.value = true
  try {
    await convertLead(convertForm.id, {
      customerName: convertForm.customerName,
      level: convertForm.level,
      createOpportunity: convertForm.createOpportunity,
      oppName: convertForm.oppName,
      oppAmount: convertForm.oppAmount
    })
    ElMessage.success('转化成功，已生成客户')
    convertVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除线索「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteLead(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  await dictStore.load()
  await load()
  if (route.query.create) {
    openForm()
    router.replace({ query: {} })
  }
})
</script>

<style scoped>
.status-cards {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.status-card {
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.15s;
}

.status-card:hover {
  border-color: #b9cdff;
}

.status-card.active {
  border-color: #2f6bff;
  box-shadow: 0 4px 14px rgba(47, 107, 255, 0.12);
  background: linear-gradient(180deg, #f7faff, #fff);
}

.status-label {
  font-size: 12.5px;
  color: #7a8699;
}

.status-value {
  font-size: 22px;
  font-weight: 700;
  color: #16233b;
  margin-top: 4px;
}

.lead-name {
  font-size: 13px;
  font-weight: 500;
  color: #2c3648;
}

.cell-sub {
  font-size: 11px;
  color: #a3adbd;
  margin-top: 2px;
}
</style>
