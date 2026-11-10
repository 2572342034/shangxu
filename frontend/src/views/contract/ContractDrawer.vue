<template>
  <el-drawer v-model="visible" size="760px" :with-header="false" destroy-on-close>
    <div v-loading="loading" class="drawer-body">
      <template v-if="detail.id">
        <div class="head">
          <div>
            <div class="title-row">
              <h3>{{ detail.name }}</h3>
              <DictTag type="contract_status" :value="detail.status" />
              <DictTag type="payment_status" :value="detail.paymentStatus" />
            </div>
            <div class="meta">{{ detail.contractNo }} · {{ detail.customerName }}</div>
          </div>
          <div class="actions">
            <el-button v-if="detail.status === 'DRAFT'" v-perm="'crm:contract:audit'" type="primary" plain size="small" @click="updateStatus('PENDING')">
              提交审批
            </el-button>
            <el-button v-if="detail.status === 'PENDING'" v-perm="'crm:contract:audit'" type="success" plain size="small" @click="updateStatus('APPROVED')">
              审批通过
            </el-button>
            <el-button v-if="detail.status === 'PENDING'" v-perm="'crm:contract:audit'" type="danger" plain size="small" @click="updateStatus('REJECTED')">
              驳回
            </el-button>
            <el-button v-if="detail.status === 'APPROVED'" v-perm="'crm:contract:audit'" type="success" size="small" @click="updateStatus('SIGNED')">
              确认签订
            </el-button>
            <el-button @click="visible = false">关闭</el-button>
          </div>
        </div>

        <div class="amount-row">
          <div class="amount-item">
            <div class="label">合同金额</div>
            <div class="value">{{ formatMoney(detail.amount) }}</div>
          </div>
          <div class="amount-item">
            <div class="label">已回款</div>
            <div class="value green">{{ formatMoney(detail.receivedAmount) }}</div>
          </div>
          <div class="amount-item">
            <div class="label">待回款</div>
            <div class="value orange">{{ formatMoney(pendingAmount) }}</div>
          </div>
          <div class="amount-item progress-item">
            <div class="label">回款进度</div>
            <el-progress :percentage="payPercent" :stroke-width="8" :color="payPercent >= 100 ? '#22b07d' : '#2f6bff'" />
          </div>
        </div>

        <el-descriptions :column="2" border size="small" class="desc">
          <el-descriptions-item label="关联客户">{{ detail.customerName }}</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ detail.ownerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="签约日期">{{ formatDate(detail.signDate) }}</el-descriptions-item>
          <el-descriptions-item label="合同周期">
            {{ formatDate(detail.startDate) }} ~ {{ formatDate(detail.endDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="section-block">
          <AttachmentPanel biz-type="CONTRACT" :biz-id="detail.id" perm-code="crm:contract:edit" />
        </div>

        <div class="section-block">
          <ChangeLogPanel biz-type="CONTRACT" :biz-id="contractId" show-title />
        </div>

        <div class="section-block">
          <div class="section-head">
            <div class="section-title">合同明细（{{ detail.items?.length || 0 }}）</div>
          </div>
          <el-table v-if="detail.items?.length" :data="detail.items" size="small" border>
            <el-table-column prop="productName" label="产品/服务" min-width="200" show-overflow-tooltip />
            <el-table-column prop="unit" label="单位" width="70" align="center" />
            <el-table-column prop="quantity" label="数量" width="90" align="right" />
            <el-table-column label="单价" width="120" align="right">
              <template #default="{ row }">{{ formatMoney(row.price) }}</template>
            </el-table-column>
            <el-table-column label="金额" width="120" align="right">
              <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="未填写合同明细（金额为手工填写）" :image-size="60" />
        </div>

        <div class="section-block">
          <div class="section-head">
            <div class="section-title">下单进度</div>
          </div>
          <div class="order-progress">
            <div class="progress-item">
              <span>合同金额</span>
              <b>{{ formatMoney(detail.amount) }}</b>
            </div>
            <div class="progress-item">
              <span>已下单（{{ detail.orderCount || 0 }} 单）</span>
              <b>{{ formatMoney(detail.orderedAmount) }}</b>
            </div>
            <div class="progress-item" :class="{ danger: Number(detail.remainAmount) < 0 }">
              <span>剩余可下单</span>
              <b>{{ formatMoney(detail.remainAmount) }}</b>
            </div>
          </div>
        </div>

        <div class="section-head">
          <div class="section-title">回款计划</div>
          <el-button v-perm="'crm:payment:add'" type="primary" size="small" plain @click="openPlanForm">
            新增计划
          </el-button>
        </div>
        <el-alert
          v-if="planMismatch"
          type="warning"
          :closable="false"
          show-icon
          class="plan-alert"
          :title="`回款计划合计 ${formatMoney(planTotal)} 与合同金额 ${formatMoney(detail.amount)} 不一致，请调整计划`"
        />

        <el-table :data="detail.paymentPlans" size="small" border>
          <el-table-column prop="period" label="期数" width="60" align="center">
            <template #default="{ row }">第 {{ row.period }} 期</template>
          </el-table-column>
          <el-table-column label="计划金额" width="130" align="right">
            <template #default="{ row }">{{ formatMoney(row.planAmount) }}</template>
          </el-table-column>
          <el-table-column label="计划日期" width="110">
            <template #default="{ row }">{{ formatDate(row.planDate) }}</template>
          </el-table-column>
          <el-table-column label="实收金额" width="130" align="right">
            <template #default="{ row }">{{ formatMoney(row.actualAmount) }}</template>
          </el-table-column>
          <el-table-column label="实收日期" width="110">
            <template #default="{ row }">{{ formatDate(row.actualDate) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PAID' ? 'success' : 'warning'" size="small" effect="light">
                {{ row.status === 'PAID' ? '已回款' : '未回款' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.status !== 'PAID'"
                v-perm="'crm:payment:add'"
                link
                type="primary"
                size="small"
                @click="openReceive(row)"
              >
                回款登记
              </el-button>
            </template>
          </el-table-column>
          <template #empty><el-empty description="暂无回款计划" :image-size="70" /></template>
        </el-table>
      </template>
    </div>

    <el-dialog v-model="receiveVisible" title="回款登记" width="420px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="计划金额">
          <span class="text-amount">{{ formatMoney(receiveForm.planAmount) }}</span>
        </el-form-item>
        <el-form-item label="回款金额">
          <el-input-number v-model="receiveForm.amount" :min="0" :step="1000" style="width: 100%" controls-position="right" />
        </el-form-item>
        <el-form-item label="回款日期">
          <el-date-picker v-model="receiveForm.actualDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="receiveVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReceive">确认登记</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="planVisible" title="新增回款计划" width="420px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="期数">
          <el-input-number v-model="planForm.period" :min="1" style="width: 100%" controls-position="right" />
        </el-form-item>
        <el-form-item label="计划金额">
          <el-input-number v-model="planForm.planAmount" :min="0" :step="1000" style="width: 100%" controls-position="right" />
        </el-form-item>
        <el-form-item label="计划日期">
          <el-date-picker v-model="planForm.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="planForm.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPlan">保存</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup>
import ChangeLogPanel from '@/components/ChangeLogPanel.vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DictTag from '@/components/DictTag.vue'
import { addPayment, changeContractStatus, getContract, receivePayment } from '@/api/contract'
import { formatDate, formatMoney } from '@/utils/format'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  contractId: { type: [Number, String], default: null }
})
const emit = defineEmits(['update:modelValue', 'refresh'])

const planTotal = computed(() =>
  (detail.paymentPlans || []).reduce((sum, p) => sum + Number(p.planAmount || 0), 0)
)
const planMismatch = computed(
  () => (detail.paymentPlans?.length || 0) > 0 &&
    Math.abs(planTotal.value - Number(detail.amount || 0)) > 0.01
)

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const saving = ref(false)
const detail = reactive({ id: null, paymentPlans: [], receivedAmount: 0 })

const receiveVisible = ref(false)
const planVisible = ref(false)
const receiveForm = reactive({ paymentId: null, planAmount: 0, amount: 0, actualDate: '' })
const planForm = reactive({ period: 1, planAmount: 0, planDate: '', remark: '' })

const pendingAmount = computed(() => Number(detail.amount || 0) - Number(detail.receivedAmount || 0))
const payPercent = computed(() => {
  const amount = Number(detail.amount || 0)
  if (!amount) return 0
  return Math.min(100, Math.round((Number(detail.receivedAmount || 0) / amount) * 100))
})

async function load(id) {
  if (!id) return
  loading.value = true
  try {
    const { data } = await getContract(id)
    Object.assign(detail, data)
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.contractId],
  ([open, id]) => {
    if (open && id) load(id)
  },
  { immediate: true }
)

async function updateStatus(status) {
  const label = { PENDING: '提交审批', APPROVED: '审批通过', REJECTED: '驳回', SIGNED: '确认签订' }[status]
  await ElMessageBox.confirm(`确定要${label}吗？`, '提示', { type: 'warning' })
  await changeContractStatus(detail.id, status)
  ElMessage.success('状态已更新')
  await load(detail.id)
  emit('refresh')
}

function openReceive(row) {
  receiveForm.paymentId = row.id
  receiveForm.planAmount = row.planAmount
  receiveForm.amount = Number(row.planAmount) - Number(row.actualAmount || 0)
  receiveForm.actualDate = new Date().toISOString().slice(0, 10)
  receiveVisible.value = true
}

async function submitReceive() {
  saving.value = true
  try {
    await receivePayment(receiveForm.paymentId, {
      amount: receiveForm.amount,
      actualDate: receiveForm.actualDate
    })
    ElMessage.success('回款登记成功')
    receiveVisible.value = false
    await load(detail.id)
    emit('refresh')
  } finally {
    saving.value = false
  }
}

function openPlanForm() {
  const nextPeriod = (detail.paymentPlans?.length || 0) + 1
  Object.assign(planForm, { period: nextPeriod, planAmount: 0, planDate: '', remark: '' })
  planVisible.value = true
}

async function submitPlan() {
  saving.value = true
  try {
    await addPayment(detail.id, planForm)
    ElMessage.success('计划已添加')
    planVisible.value = false
    await load(detail.id)
    emit('refresh')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.drawer-body {
  min-height: 300px;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eef1f6;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.title-row h3 {
  margin: 0;
  font-size: 17px;
  color: #16233b;
}

.meta {
  font-size: 12px;
  color: #8b97a8;
  margin-top: 6px;
}

.actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.amount-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr) 1.4fr;
  gap: 12px;
  padding: 16px 0;
}

.amount-item {
  background: #f7f9fd;
  border-radius: 8px;
  padding: 12px 14px;
}

.amount-item .label {
  font-size: 11.5px;
  color: #8b97a8;
}

.amount-item .value {
  font-size: 17px;
  font-weight: 700;
  color: #16233b;
  margin-top: 4px;
}

.value.green {
  color: #22b07d;
}

.value.orange {
  color: #f5a623;
}

.progress-item :deep(.el-progress) {
  margin-top: 10px;
}

.desc {
  margin-bottom: 18px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 10px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2733;
}
.section-block {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid #f0f3f8;
}
.order-progress {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.progress-item {
  background: #f8faff;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  padding: 10px 14px;
}
.progress-item span {
  font-size: 12px;
  color: #7a8699;
}
.progress-item b {
  display: block;
  font-size: 17px;
  color: #16233b;
  margin-top: 4px;
}
.progress-item.danger b {
  color: #e2725b;
}
.plan-alert {
  margin: 8px 0 10px;
}
</style>
