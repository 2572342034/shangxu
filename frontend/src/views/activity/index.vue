<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-select v-model="query.bizType" placeholder="业务类型" clearable @change="search">
        <el-option label="客户" value="CUSTOMER" />
        <el-option label="线索" value="LEAD" />
        <el-option label="商机" value="OPPORTUNITY" />
        <el-option label="合同" value="CONTRACT" />
      </el-select>
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button v-perm="'crm:activity:add'" type="primary" @click="openForm">
        <el-icon><Plus /></el-icon>记录跟进
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">跟进记录 <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ total }} 条</span></div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="跟进时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="方式" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="light" :type="typeTag(row.type)">
              {{ dictStore.label('activity_type', row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联对象" width="200">
          <template #default="{ row }">
            <div class="biz-name">{{ row.bizName || '-' }}</div>
            <div class="cell-sub">{{ bizTypeLabel(row.bizType) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="跟进内容" min-width="320" show-overflow-tooltip />
        <el-table-column label="下次跟进" width="150">
          <template #default="{ row }">{{ formatDateTime(row.nextFollowTime) }}</template>
        </el-table-column>
        <el-table-column label="跟进人" width="100">
          <template #default="{ row }">{{ row.ownerName || row.createBy }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="info" size="small" title="查看修改记录" @click="$showChangeLog('ACTIVITY', row.id, '')">记录</el-button>
            <el-button v-perm="'crm:activity:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无跟进记录" :image-size="90" /></template>
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

    <el-dialog v-model="formVisible" title="记录跟进" width="560px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="业务类型">
          <el-radio-group v-model="form.bizType" @change="loadBizOptions">
            <el-radio-button value="CUSTOMER">客户</el-radio-button>
            <el-radio-button value="LEAD">线索</el-radio-button>
            <el-radio-button value="OPPORTUNITY">商机</el-radio-button>
            <el-radio-button value="CONTRACT">合同</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关联对象" required>
          <el-select v-model="form.bizId" filterable placeholder="请选择" style="width: 100%" :loading="bizLoading">
            <el-option v-for="item in bizOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进方式">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="item in dictStore.options('activity_type')" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进内容" required>
          <el-input v-model="form.content" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="下次跟进">
          <el-date-picker
            v-model="form.nextFollowTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createActivity, deleteActivity, pageActivities } from '@/api/activity'
import { pageCustomers } from '@/api/customer'
import { pageLeads } from '@/api/lead'
import { pageOpportunities } from '@/api/opportunity'
import { pageContracts } from '@/api/contract'
import { useDictStore } from '@/store/dict'
import { formatDateTime } from '@/utils/format'

const dictStore = useDictStore()

const loading = ref(false)
const saving = ref(false)
const bizLoading = ref(false)
const list = ref([])
const total = ref(0)
const formVisible = ref(false)
const bizOptions = ref([])

const query = reactive({ pageNum: 1, pageSize: 10, bizType: '' })

const form = reactive({
  bizType: 'CUSTOMER',
  bizId: null,
  type: 'CALL',
  content: '',
  nextFollowTime: ''
})

function typeTag(type) {
  return { CALL: 'primary', VISIT: 'success', MEETING: 'warning', EMAIL: 'info', WECHAT: 'success' }[type] || 'info'
}

function bizTypeLabel(type) {
  return { CUSTOMER: '客户', LEAD: '线索', OPPORTUNITY: '商机', CONTRACT: '合同' }[type] || '-'
}

async function load() {
  loading.value = true
  try {
    const { data } = await pageActivities(query)
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
  query.bizType = ''
  query.pageNum = 1
  load()
}

async function loadBizOptions() {
  bizLoading.value = true
  form.bizId = null
  try {
    if (form.bizType === 'CUSTOMER') {
      const { data } = await pageCustomers({ pageNum: 1, pageSize: 100 })
      bizOptions.value = data.records || []
    } else if (form.bizType === 'LEAD') {
      const { data } = await pageLeads({ pageNum: 1, pageSize: 100 })
      bizOptions.value = data.records || []
    } else if (form.bizType === 'OPPORTUNITY') {
      const { data } = await pageOpportunities({ pageNum: 1, pageSize: 100 })
      bizOptions.value = data.records || []
    } else {
      const { data } = await pageContracts({ pageNum: 1, pageSize: 100 })
      bizOptions.value = data.records || []
    }
  } finally {
    bizLoading.value = false
  }
}

async function openForm() {
  Object.assign(form, { bizType: 'CUSTOMER', bizId: null, type: 'CALL', content: '', nextFollowTime: '' })
  formVisible.value = true
  await loadBizOptions()
}

async function submit() {
  if (!form.bizId) {
    ElMessage.warning('请选择关联对象')
    return
  }
  if (!form.content) {
    ElMessage.warning('请填写跟进内容')
    return
  }
  saving.value = true
  try {
    await createActivity(form)
    ElMessage.success('跟进已记录')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm('确定删除这条跟进记录吗？', '提示', { type: 'warning' })
  await deleteActivity(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  await dictStore.load()
  await load()
})
</script>

<style scoped>
.biz-name {
  font-size: 13px;
  color: #2c3648;
}

.cell-sub {
  font-size: 11px;
  color: #a3adbd;
  margin-top: 2px;
}
</style>
