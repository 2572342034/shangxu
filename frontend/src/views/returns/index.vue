<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索退货单号 / 订单号 / 客户" clearable @keyup.enter="search" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.status" placeholder="处理状态" clearable @change="search">
        <el-option v-for="item in dictStore.options('return_status')" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button v-perm="'crm:return:add'" type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>新建退货单
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">退货退款 <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ total }} 单</span></div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="退货单号" width="126" fixed="left">
          <template #default="{ row }">
            <div class="no">{{ row.returnNo }}</div>
            <div class="cell-sub">{{ row.orderNo || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="客户" min-width="152" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="ellipsis">{{ row.customerName }}</div>
            <div class="cell-sub">{{ row.ownerName || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="退货日期" width="100">
          <template #default="{ row }">{{ formatDate(row.returnDate) }}</template>
        </el-table-column>
        <el-table-column label="数量" width="72" align="right">
          <template #default="{ row }">{{ qtyText(row.returnQty) }}</template>
        </el-table-column>
        <el-table-column label="退货金额" width="102" align="right">
          <template #default="{ row }">{{ formatMoney(row.returnAmount) }}</template>
        </el-table-column>
        <el-table-column label="退款金额" width="102" align="right">
          <template #default="{ row }">
            <span :class="Number(row.refundAmount) > 0 ? 'refund' : 'text-muted'">{{ formatMoney(row.refundAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="退货原因" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }"><DictTag type="return_status" :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="info" size="small" title="查看修改记录" @click="$showChangeLog('RETURN', row.id, row.returnNo)">记录</el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              v-perm="'crm:return:audit'"
              link
              type="success"
              size="small"
              @click="audit(row, 'APPROVED')"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === 'APPROVED'"
              v-perm="'crm:return:audit'"
              link
              type="primary"
              size="small"
              @click="refund(row)"
            >
              确认退款
            </el-button>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
              <el-button link type="primary" size="small" class="more-btn">
                更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status === 'PENDING'" command="reject">驳回申请</el-dropdown-item>
                  <el-dropdown-item v-if="row.status !== 'REFUNDED'" command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无退货单" :image-size="90" /></template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog v-model="createVisible" title="新建退货单" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="关联订单" prop="orderId">
          <el-select
            v-model="form.orderId"
            filterable
            placeholder="选择已审核订单"
            style="width: 100%"
            @change="onOrderChange"
          >
            <el-option
              v-for="o in orderOptions"
              :key="o.id"
              :label="`${o.orderNo} · ${o.title}`"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="退货日期">
              <el-date-picker v-model="form.returnDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="退货数量">
              <el-input-number v-model="form.returnQty" :min="0" :step="1" style="width: 100%" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="退货金额" prop="returnAmount">
              <el-input-number v-model="form.returnAmount" :min="0" :step="1000" style="width: 100%" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="退款金额">
              <el-input-number v-model="form.refundAmount" :min="0" :step="1000" style="width: 100%" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="退货原因" prop="reason">
              <el-input v-model="form.reason" type="textarea" :rows="2" maxlength="255" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DictTag from '@/components/DictTag.vue'
import { auditReturn, createReturn, deleteReturn, pageReturns, refundReturn } from '@/api/tradeDocs'
import { pageOrders } from '@/api/order'
import { useDictStore } from '@/store/dict'
import { formatDate, formatMoney } from '@/utils/format'

const dictStore = useDictStore()

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const orderOptions = ref([])
const createVisible = ref(false)
const formRef = ref()

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' })

const form = reactive({
  orderId: null,
  customerId: null,
  customerName: '',
  returnDate: '',
  returnQty: 0,
  returnAmount: 0,
  refundAmount: 0,
  reason: '',
  remark: ''
})

const rules = {
  orderId: [{ required: true, message: '请选择关联订单', trigger: 'change' }],
  returnAmount: [{ required: true, message: '请输入退货金额', trigger: 'blur' }],
  reason: [{ required: true, message: '请填写退货原因', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const { data } = await pageReturns(query)
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
  Object.assign(query, { keyword: '', status: '', pageNum: 1 })
  load()
}

async function openCreate() {
  Object.assign(form, {
    orderId: null,
    customerId: null,
    customerName: '',
    returnDate: new Date().toISOString().slice(0, 10),
    returnQty: 0,
    returnAmount: 0,
    refundAmount: 0,
    reason: '',
    remark: ''
  })
  createVisible.value = true
  if (!orderOptions.value.length) {
    const { data } = await pageOrders({ pageNum: 1, pageSize: 100, status: 'APPROVED' })
    orderOptions.value = data.records || []
  }
}

function onOrderChange(id) {
  const hit = orderOptions.value.find((o) => o.id === id)
  if (hit) {
    form.customerId = hit.customerId
    form.customerName = hit.customerName
    form.returnAmount = Number(hit.amount)
    form.refundAmount = Number(hit.amount)
  }
}

async function submit() {
  await formRef.value.validate()
  if (Number(form.refundAmount) > Number(form.returnAmount)) {
    ElMessage.warning('退款金额不能大于退货金额')
    return
  }
  saving.value = true
  try {
    await createReturn(form)
    ElMessage.success('退货单已创建，等待审核')
    createVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function handleCommand(command, row) {
  if (command === 'reject') return audit(row, 'REJECTED')
  if (command === 'delete') return remove(row)
}

function qtyText(value) {
  const num = Number(value || 0)
  if (!num) return '-'
  return num % 1 === 0 ? String(num) : num.toFixed(2)
}

async function audit(row, status) {
  const label = status === 'APPROVED' ? '通过' : '驳回'
  await ElMessageBox.confirm(`确定${label}退货单「${row.returnNo}」吗？`, '提示', { type: 'warning' })
  await auditReturn(row.id, status)
  ElMessage.success(`已${label}`)
  load()
}

async function refund(row) {
  await ElMessageBox.confirm(
    `确认已向「${row.customerName}」退款 ${formatMoney(row.refundAmount || row.returnAmount)}？`,
    '确认退款',
    { type: 'warning' }
  )
  await refundReturn(row.id)
  ElMessage.success('已标记退款完成')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除退货单「${row.returnNo}」吗？`, '提示', { type: 'warning' })
  await deleteReturn(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  await dictStore.load()
  await load()
})
</script>

<style scoped>
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

.refund {
  color: #f56c6c;
  font-weight: 600;
}

:deep(.el-table .cell) {
  padding: 0 8px;
}

:deep(.el-table__fixed-right .cell),
:deep(.el-table__fixed .cell) {
  padding: 0 8px;
}

.more-btn {
  margin-left: 6px;
}

.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
