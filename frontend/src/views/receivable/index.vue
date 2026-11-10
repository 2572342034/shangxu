<template>
  <div class="page-container">
    <div class="summary-grid">
      <div class="summary-card">
        <div class="s-label">应收总额</div>
        <div class="s-value">{{ formatWan(summary.totalAmount) }}</div>
        <div class="s-sub">共 {{ total }} 张应收单</div>
      </div>
      <div class="summary-card green">
        <div class="s-label">已收款</div>
        <div class="s-value">{{ formatWan(summary.receivedAmount) }}</div>
        <div class="s-sub">回款率 {{ receivedRate }}%</div>
      </div>
      <div class="summary-card orange">
        <div class="s-label">未收款</div>
        <div class="s-value">{{ formatWan(summary.unpaidAmount) }}</div>
        <div class="s-sub">含逾期部分</div>
      </div>
      <div class="summary-card red">
        <div class="s-label">逾期未收</div>
        <div class="s-value">{{ formatWan(summary.overdueAmount) }}</div>
        <div class="s-sub">{{ summary.overdueCount || 0 }} 张已逾期</div>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索应收单号 / 客户 / 来源单号" clearable @keyup.enter="search" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.status" placeholder="收款状态" clearable @change="search">
        <el-option v-for="item in dictStore.options('receivable_status')" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-checkbox v-model="query.overdue" @change="search">只看逾期</el-checkbox>
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <el-button link type="primary" @click="$router.push({ path: '/system/data-export', query: { module: 'RECEIVABLE' } })">批量导出</el-button>
      <div class="spacer"></div>
      <el-button v-perm="'crm:receivable:add'" type="primary" @click="openForm">
        <el-icon><Plus /></el-icon>新增应收
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">应收单 <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ total }} 张</span></div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="应收单号" width="140" fixed="left">
          <template #default="{ row }">
            <div class="no">{{ row.receivableNo }}</div>
            <div class="cell-sub">{{ formatDate(row.createTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" min-width="170" show-overflow-tooltip />
        <el-table-column label="来源" width="140">
          <template #default="{ row }">
            <div class="source">{{ dictStore.label('receivable_source', row.sourceType) }}</div>
            <div class="cell-sub">{{ row.sourceNo || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="应收金额" width="120" align="right">
          <template #default="{ row }"><span class="amount">{{ formatMoney(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column label="已收金额" width="120" align="right">
          <template #default="{ row }"><span class="received">{{ formatMoney(row.receivedAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="未收金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(Number(row.amount) - Number(row.receivedAmount || 0)) }}</template>
        </el-table-column>
        <el-table-column label="应收日期" width="105">
          <template #default="{ row }">{{ formatDate(row.dueDate) }}</template>
        </el-table-column>
        <el-table-column label="账龄" width="95" align="center">
          <template #default="{ row }">
            <span v-if="row.status !== 'PAID' && row.overdueDays > 0" class="overdue">逾期 {{ row.overdueDays }} 天</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="确认" width="90" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.auditStatus === 'APPROVED' ? 'success' : row.auditStatus === 'REJECTED' ? 'danger' : 'warning'"
              size="small"
              effect="light"
            >
              {{ row.auditStatus === 'APPROVED' ? '已确认' : row.auditStatus === 'REJECTED' ? '已驳回' : '待确认' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><DictTag type="receivable_status" :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="info" size="small" title="查看修改记录" @click="$showChangeLog('RECEIVABLE', row.id, row.receivableNo)">记录</el-button>
            <el-button
              v-if="row.auditStatus === 'PENDING'"
              v-perm="'crm:receivable:add'"
              link
              type="success"
              size="small"
              @click="confirmReceivable(row)"
            >
              确认应收
            </el-button>
            <el-button
              v-if="row.status !== 'PAID' && row.auditStatus === 'APPROVED'"
              v-perm="'crm:receivable:receive'"
              link
              type="primary"
              size="small"
              @click="openReceive(row)"
            >
              收款登记
            </el-button>
            <el-button v-perm="'crm:receivable:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无应收单" :image-size="90" /></template>
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

    <!-- 收款登记 -->
    <el-dialog v-model="receiveVisible" title="收款登记" width="440px" destroy-on-close>
      <el-form :model="receiveForm" label-width="90px">
        <el-form-item label="客户">
          <span>{{ receiveForm.customerName }}</span>
        </el-form-item>
        <el-form-item label="应收金额">
          <span class="text-amount">{{ formatMoney(receiveForm.amount) }}</span>
        </el-form-item>
        <el-form-item label="已收金额">
          <span class="text-amount">{{ formatMoney(receiveForm.receivedAmount) }}</span>
        </el-form-item>
        <el-form-item label="本次收款">
          <el-input-number v-model="receiveForm.amountInput" :min="0" :step="1000" style="width: 100%" controls-position="right" />
        </el-form-item>
        <el-form-item label="收款日期">
          <el-date-picker v-model="receiveForm.receiveDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="receiveVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReceive">确认收款</el-button>
      </template>
    </el-dialog>

    <!-- 新增应收 -->
    <el-dialog v-model="formVisible" title="新增应收单" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="客户" prop="customerId">
          <el-select
            v-model="form.customerId"
            filterable
            remote
            reserve-keyword
            :remote-method="searchCustomers"
            :loading="customerLoading"
            placeholder="搜索客户"
            style="width: 100%"
            @change="onCustomerChange"
          >
            <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="应收金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0" :step="1000" style="width: 100%" controls-position="right" />
        </el-form-item>
        <el-form-item label="应收日期">
          <el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DictTag from '@/components/DictTag.vue'
import {
  createReceivable,
  deleteReceivable,
  pageReceivables,
  receivableSummary,
  receiveReceivable, auditReceivable } from '@/api/order'
import { pageCustomers } from '@/api/customer'
import { useDictStore } from '@/store/dict'
import { formatDate, formatMoney, formatWan } from '@/utils/format'

const dictStore = useDictStore()

const loading = ref(false)
const saving = ref(false)
const customerLoading = ref(false)
const list = ref([])
const total = ref(0)
const summary = ref({ totalAmount: 0, receivedAmount: 0, unpaidAmount: 0, overdueAmount: 0, overdueCount: 0 })
const customerOptions = ref([])
const receiveVisible = ref(false)
const formVisible = ref(false)
const formRef = ref()

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '', overdue: false })

const receiveForm = reactive({
  id: null,
  customerName: '',
  amount: 0,
  receivedAmount: 0,
  amountInput: 0,
  receiveDate: ''
})

const form = reactive({ customerId: null, customerName: '', amount: 0, dueDate: '', remark: '' })

const rules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  amount: [{ required: true, message: '请输入应收金额', trigger: 'blur' }]
}

const receivedRate = computed(() => {
  const totalAmount = Number(summary.value.totalAmount || 0)
  if (!totalAmount) return 0
  return Math.round((Number(summary.value.receivedAmount || 0) / totalAmount) * 100)
})

async function load() {
  loading.value = true
  try {
    const { data } = await pageReceivables(query)
    list.value = data.records || []
    total.value = data.total || 0
    const res = await receivableSummary()
    summary.value = res.data || {}
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function resetQuery() {
  Object.assign(query, { keyword: '', status: '', overdue: false, pageNum: 1 })
  load()
}

async function confirmReceivable(row) {
  try {
    await ElMessageBox.confirm(
      `确认应收单「${row.receivableNo}」金额 ${formatMoney(row.amount)} 吗？确认后才能登记收款。`,
      '确认应收',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await auditReceivable(row.id, 'APPROVED')
    ElMessage.success('应收已确认')
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '确认失败，请稍后重试')
  } finally {
    await load()
  }
}

function openReceive(row) {
  Object.assign(receiveForm, {
    id: row.id,
    customerName: row.customerName,
    amount: Number(row.amount),
    receivedAmount: Number(row.receivedAmount || 0),
    amountInput: Number(row.amount) - Number(row.receivedAmount || 0),
    receiveDate: new Date().toISOString().slice(0, 10)
  })
  receiveVisible.value = true
}

async function submitReceive() {
  if (!receiveForm.amountInput || receiveForm.amountInput <= 0) {
    ElMessage.warning('请输入本次收款金额')
    return
  }
  saving.value = true
  try {
    await receiveReceivable(receiveForm.id, {
      amount: receiveForm.amountInput,
      receiveDate: receiveForm.receiveDate
    })
    ElMessage.success('收款登记成功')
    receiveVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '收款登记失败，请稍后重试')
    receiveVisible.value = false
  } finally {
    saving.value = false
  }
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

function onCustomerChange(id) {
  const hit = customerOptions.value.find((c) => c.id === id)
  if (hit) form.customerName = hit.name
}

async function openForm() {
  Object.assign(form, { customerId: null, customerName: '', amount: 0, dueDate: '', remark: '' })
  formVisible.value = true
  if (!customerOptions.value.length) await searchCustomers('')
}

async function submitForm() {
  await formRef.value.validate()
  saving.value = true
  try {
    await createReceivable(form)
    ElMessage.success('应收单已创建')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除应收单「${row.receivableNo}」吗？`, '提示', { type: 'warning' })
  await deleteReceivable(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  await dictStore.load()
  await load()
})
</script>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.summary-card {
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  padding: 14px 18px;
  position: relative;
  overflow: hidden;
}

.summary-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: #2f6bff;
}

.summary-card.green::before {
  background: #22b07d;
}
.summary-card.orange::before {
  background: #f5a623;
}
.summary-card.red::before {
  background: #f56c6c;
}

.s-label {
  font-size: 12.5px;
  color: #7a8699;
}

.s-value {
  font-size: 22px;
  font-weight: 700;
  color: #16233b;
  margin: 6px 0 4px;
}

.s-sub {
  font-size: 11.5px;
  color: #a3adbd;
}

.no {
  font-size: 13px;
  font-weight: 500;
  color: #2c3648;
}

.cell-sub {
  font-size: 11px;
  color: #a3adbd;
  margin-top: 2px;
}

.source {
  font-size: 12.5px;
  color: #3c4759;
}

.amount {
  font-weight: 600;
  color: #1f2733;
}

.received {
  color: #22b07d;
  font-weight: 500;
}

.overdue {
  color: #f56c6c;
  font-size: 11.5px;
  font-weight: 500;
}
</style>
