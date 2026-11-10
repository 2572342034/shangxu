<template>
  <div class="page-container">
    <div class="summary-grid">
      <div class="summary-card green">
        <div class="s-label">累计收款</div>
        <div class="s-value">{{ formatWan(summary.totalReceived) }}</div>
        <div class="s-sub">仅统计有效收款单</div>
      </div>
      <div class="summary-card orange">
        <div class="s-label">预收未核销</div>
        <div class="s-value">{{ formatWan(summary.unallocated) }}</div>
        <div class="s-sub">{{ summary.unallocatedCount || 0 }} 笔待核销</div>
      </div>
      <div class="summary-card">
        <div class="s-label">收款单数</div>
        <div class="s-value">{{ total }}</div>
        <div class="s-sub">当前筛选范围</div>
      </div>
      <div class="summary-card red">
        <div class="s-label">作废单</div>
        <div class="s-value">{{ cancelledCount }}</div>
        <div class="s-sub">已反向冲销</div>
      </div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索收款单号 / 客户 / 凭证号"
        clearable
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.method" placeholder="收款方式" clearable @change="search">
        <el-option v-for="(label, value) in methodLabels" :key="value" :label="label" :value="value" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="收款开始日"
        end-placeholder="结束日"
        style="width: 250px"
        @change="search"
      />
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button v-perm="'crm:payment:add'" type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>登记收款
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">
          收款单
          <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ total }} 张</span>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="收款单号" width="140" fixed="left">
          <template #default="{ row }">
            <div class="no">{{ row.recordNo }}</div>
            <div class="cell-sub">{{ formatDate(row.receiveDate) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" min-width="170" show-overflow-tooltip />
        <el-table-column label="收款金额" width="120" align="right">
          <template #default="{ row }">
            <span class="money">{{ formatMoney(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="已核销" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.allocatedAmount) }}</template>
        </el-table-column>
        <el-table-column label="预收余额" width="110" align="right">
          <template #default="{ row }">
            <span :class="Number(row.unallocatedAmount) > 0 ? 'warn' : 'text-muted'">
              {{ formatMoney(row.unallocatedAmount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="方式" width="100" align="center">
          <template #default="{ row }">{{ methodLabels[row.method] || row.method }}</template>
        </el-table-column>
        <el-table-column label="凭证号" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.voucherNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'CANCELLED' ? 'info' : 'success'" size="small" effect="light">
              {{ row.status === 'CANCELLED' ? '已作废' : '已确认' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ownerName" label="经办人" width="90" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="info" size="small" title="查看修改记录" @click="$showChangeLog('PAYMENT', row.id, row.recordNo)">记录</el-button>
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status !== 'CANCELLED' && Number(row.unallocatedAmount) > 0"
              v-perm="'crm:payment:add'"
              link
              type="warning"
              size="small"
              @click="openAllocate(row)"
            >
              核销
            </el-button>
            <el-button
              v-if="row.status !== 'CANCELLED'"
              v-perm="'crm:payment:cancel'"
              link
              type="danger"
              size="small"
              @click="cancel(row)"
            >
              作废
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <!-- 登记收款 -->
    <el-dialog v-model="createVisible" title="登记收款" width="880px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户" prop="customerId">
              <el-select
                v-model="form.customerId"
                filterable
                placeholder="选择客户"
                style="width: 100%"
                @change="onCustomerChange"
              >
                <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收款金额" prop="amount">
              <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="1000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收款日期" prop="receiveDate">
              <el-date-picker v-model="form.receiveDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收款方式">
              <el-select v-model="form.method" style="width: 100%">
                <el-option v-for="(label, value) in methodLabels" :key="value" :label="label" :value="value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收款账户">
              <el-input v-model="form.bankAccount" placeholder="如 招商银行广州分行 6225****" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="凭证号">
              <el-input v-model="form.voucherNo" placeholder="银行流水号 / 支票号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="alloc-head">
          <div>
            <span class="alloc-title">核销应收单</span>
            <span class="text-muted" style="font-size: 12px; margin-left: 8px">
              勾选要冲销的应收单，默认按剩余金额核销；不勾选则整笔记为预收款
            </span>
          </div>
          <div class="alloc-sum">
            已分配 <b>{{ formatMoney(allocatedTotal) }}</b> / 收款 {{ formatMoney(form.amount || 0) }}
          </div>
        </div>

        <el-table
          ref="allocTableRef"
          :data="openReceivables"
          size="small"
          max-height="240"
          @selection-change="onSelectionChange"
        >
          <el-table-column type="selection" width="42" />
          <el-table-column label="应收单号" width="130">
            <template #default="{ row }">{{ row.receivableNo }}</template>
          </el-table-column>
          <el-table-column label="来源" width="130">
            <template #default="{ row }">{{ row.sourceNo || '-' }}</template>
          </el-table-column>
          <el-table-column label="应收金额" width="110" align="right">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="已收" width="110" align="right">
            <template #default="{ row }">{{ formatMoney(row.receivedAmount) }}</template>
          </el-table-column>
          <el-table-column label="应收日期" width="105">
            <template #default="{ row }">{{ formatDate(row.dueDate) }}</template>
          </el-table-column>
          <el-table-column label="本次核销" width="150">
            <template #default="{ row }">
              <el-input-number
                v-model="row._amount"
                :min="0"
                :max="remaining(row)"
                :precision="2"
                size="small"
                :disabled="!row._checked"
                style="width: 100%"
              />
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!openReceivables.length" description="该客户暂无未结清应收，可直接登记为预收款" :image-size="56" />
      </el-form>

      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确认登记</el-button>
      </template>
    </el-dialog>

    <!-- 预收核销 -->
    <el-dialog v-model="allocateVisible" title="预收余额核销" width="480px" destroy-on-close>
      <el-form label-width="92px">
        <el-form-item label="收款单">
          <span>{{ current.recordNo }}（余额 {{ formatMoney(current.unallocatedAmount) }}）</span>
        </el-form-item>
        <el-form-item label="核销应收">
          <el-select v-model="allocateForm.receivableId" placeholder="选择应收单" style="width: 100%" filterable>
            <el-option
              v-for="r in openReceivables"
              :key="r.id"
              :label="`${r.receivableNo} · 余额 ${formatMoney(remaining(r))}`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="核销金额">
          <el-input-number v-model="allocateForm.amount" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="allocateVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAllocate">确认核销</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" title="收款单详情" size="620px" destroy-on-close>
      <div v-if="detail.record" class="detail-wrap">
        <div class="detail-head">
          <div>
            <div class="detail-no">{{ detail.record.recordNo }}</div>
            <div class="cell-sub">{{ detail.record.customerName }}</div>
          </div>
          <el-tag :type="detail.record.status === 'CANCELLED' ? 'info' : 'success'" effect="light">
            {{ detail.record.status === 'CANCELLED' ? '已作废' : '已确认' }}
          </el-tag>
        </div>

        <div class="detail-stats">
          <div><span>收款金额</span><b>{{ formatMoney(detail.record.amount) }}</b></div>
          <div><span>已核销</span><b class="green">{{ formatMoney(detail.record.allocatedAmount) }}</b></div>
          <div><span>预收余额</span><b class="orange">{{ formatMoney(detail.record.unallocatedAmount) }}</b></div>
        </div>

        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="收款日期">{{ formatDate(detail.record.receiveDate) }}</el-descriptions-item>
          <el-descriptions-item label="收款方式">{{ methodLabels[detail.record.method] || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收款账户">{{ detail.record.bankAccount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="凭证号">{{ detail.record.voucherNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="经办人">{{ detail.record.ownerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="登记时间">{{ formatDateTime(detail.record.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.record.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="section-title">核销明细</div>
        <el-table :data="detail.allocations" size="small" stripe>
          <el-table-column prop="receivableNo" label="应收单号" width="140" />
          <el-table-column label="应收金额" width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.receivableAmount) }}</template>
          </el-table-column>
          <el-table-column label="本次核销" width="120" align="right">
            <template #default="{ row }">
              <span class="green">{{ formatMoney(row.amount) }}</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!detail.allocations.length" description="尚未核销，整笔为预收款" :image-size="56" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  allocatePayment,
  cancelPayment,
  createPayment,
  getOpenReceivables,
  getPayment,
  getPaymentSummary,
  pagePayments
} from '@/api/payment'
import { pageCustomers } from '@/api/customer'
import { formatDate, formatDateTime, formatMoney, formatWan } from '@/utils/format'

const methodLabels = {
  BANK: '银行转账',
  CASH: '现金',
  ACCEPTANCE: '承兑汇票',
  ONLINE: '在线支付',
  OTHER: '其他'
}

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const summary = reactive({ totalReceived: 0, unallocated: 0, unallocatedCount: 0 })
const cancelledCount = computed(() => list.value.filter((r) => r.status === 'CANCELLED').length)

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', method: '' })
const dateRange = ref([])

const createVisible = ref(false)
const allocateVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref()
const allocTableRef = ref()
const customers = ref([])
const openReceivables = ref([])
const detail = reactive({ record: null, allocations: [] })
const current = ref({})
const allocateForm = reactive({ receivableId: null, amount: null })

const form = reactive({
  customerId: null,
  amount: null,
  receiveDate: new Date().toISOString().slice(0, 10),
  method: 'BANK',
  bankAccount: '',
  voucherNo: '',
  remark: ''
})

const rules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  amount: [{ required: true, message: '请输入收款金额', trigger: 'blur' }],
  receiveDate: [{ required: true, message: '请选择收款日期', trigger: 'change' }]
}

const allocatedTotal = computed(() =>
  openReceivables.value.filter((r) => r._checked).reduce((sum, r) => sum + Number(r._amount || 0), 0)
)

function remaining(row) {
  return Math.max(Number(row.amount || 0) - Number(row.receivedAmount || 0), 0)
}

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const { data } = await pagePayments(params)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  const { data } = await getPaymentSummary()
  Object.assign(summary, data)
}

function search() {
  query.pageNum = 1
  load()
}

function resetQuery() {
  query.keyword = ''
  query.method = ''
  dateRange.value = []
  query.pageNum = 1
  load()
}

async function loadCustomers() {
  const { data } = await pageCustomers({ pageNum: 1, pageSize: 300 })
  customers.value = data.records || []
}

async function openCreate() {
  Object.assign(form, {
    customerId: null,
    amount: null,
    receiveDate: new Date().toISOString().slice(0, 10),
    method: 'BANK',
    bankAccount: '',
    voucherNo: '',
    remark: ''
  })
  openReceivables.value = []
  createVisible.value = true
  if (!customers.value.length) {
    await loadCustomers()
  }
}

async function onCustomerChange(customerId) {
  openReceivables.value = []
  if (!customerId) return
  const { data } = await getOpenReceivables(customerId)
  openReceivables.value = (data || []).map((r) => ({ ...r, _checked: false, _amount: remaining(r) }))
}

function onSelectionChange(rows) {
  openReceivables.value.forEach((r) => {
    r._checked = rows.includes(r)
    if (r._checked && !r._amount) {
      r._amount = remaining(r)
    }
  })
}

async function submit() {
  await formRef.value.validate()
  if (allocatedTotal.value > Number(form.amount || 0)) {
    ElMessage.error('核销总额不能超过收款金额')
    return
  }
  const allocations = openReceivables.value
    .filter((r) => r._checked && Number(r._amount) > 0)
    .map((r) => ({ receivableId: r.id, amount: r._amount }))
  saving.value = true
  try {
    await createPayment({
      customerId: form.customerId,
      customerName: customers.value.find((c) => c.id === form.customerId)?.name,
      amount: form.amount,
      receiveDate: form.receiveDate,
      method: form.method,
      bankAccount: form.bankAccount,
      voucherNo: form.voucherNo,
      remark: form.remark,
      allocations
    })
    ElMessage.success('收款登记成功')
    createVisible.value = false
    await Promise.all([load(), loadSummary()])
  } finally {
    saving.value = false
  }
}

async function openAllocate(row) {
  current.value = row
  allocateForm.receivableId = null
  allocateForm.amount = null
  const { data } = await getOpenReceivables(row.customerId)
  openReceivables.value = (data || []).map((r) => ({ ...r, _checked: false, _amount: remaining(r) }))
  allocateVisible.value = true
}

async function submitAllocate() {
  if (!allocateForm.receivableId || !allocateForm.amount) {
    ElMessage.warning('请选择应收单并填写金额')
    return
  }
  saving.value = true
  try {
    await allocatePayment(current.value.id, allocateForm)
    ElMessage.success('核销成功')
    allocateVisible.value = false
    await Promise.all([load(), loadSummary()])
  } finally {
    saving.value = false
  }
}

async function openDetail(row) {
  const { data } = await getPayment(row.id)
  detail.record = data.record
  detail.allocations = data.allocations || []
  detailVisible.value = true
}

async function cancel(row) {
  await ElMessageBox.confirm(
    `作废后会将已核销的 ${formatMoney(row.allocatedAmount)} 元逐笔冲回应收，确定继续？`,
    '作废收款单',
    { type: 'warning', confirmButtonText: '确认作废' }
  )
  await cancelPayment(row.id)
  ElMessage.success('已作废并冲销')
  await Promise.all([load(), loadSummary()])
}

onMounted(async () => {
  await Promise.all([load(), loadSummary(), loadCustomers()])
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
.summary-card.green::before { background: #22b07d; }
.summary-card.orange::before { background: #f5a623; }
.summary-card.red::before { background: #f56c6c; }
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
  font-size: 11.5px;
  color: #a3adbd;
  margin-top: 2px;
}

.money {
  font-weight: 500;
  color: #2c3648;
}
.green {
  color: #22b07d;
  font-weight: 500;
}
.orange {
  color: #e08a1e;
  font-weight: 500;
}
.warn {
  color: #e08a1e;
  font-weight: 500;
}
.alloc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 10px;
}
.alloc-title {
  font-size: 13.5px;
  font-weight: 500;
  color: #2c3648;
}
.alloc-sum {
  font-size: 12.5px;
  color: #5b6779;
}
.alloc-sum b {
  color: #2f6bff;
  font-weight: 500;
}
.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}
.detail-no {
  font-size: 17px;
  font-weight: 500;
  color: #1f2a3d;
}
.detail-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}
.detail-stats div {
  background: #f7f9fc;
  border-radius: 8px;
  padding: 10px 12px;
}
.detail-stats span {
  display: block;
  font-size: 12px;
  color: #8b97a8;
  margin-bottom: 4px;
}
.detail-stats b {
  font-size: 16px;
  font-weight: 500;
  color: #2c3648;
}
.section-title {
  margin: 20px 0 10px;
  font-size: 13.5px;
  font-weight: 500;
  color: #2c3648;
}
</style>
