<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索发票号 / 抬头 / 来源单号" clearable @keyup.enter="search" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.status" placeholder="开票状态" clearable @change="search">
        <el-option v-for="item in dictStore.options('invoice_status')" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="query.type" placeholder="发票类型" clearable @change="search">
        <el-option v-for="item in dictStore.options('invoice_type')" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button v-perm="'crm:invoice:add'" type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>申请开票
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">发票列表 <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ total }} 张</span></div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="发票号" width="132" fixed="left">
          <template #default="{ row }">
            <div class="no">{{ row.invoiceNo || '待开具' }}</div>
            <div class="cell-sub">{{ row.invoiceDate ? formatDate(row.invoiceDate) : '未开票' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" min-width="150" show-overflow-tooltip />
        <el-table-column label="来源" width="120">
          <template #default="{ row }">
            <div class="source">{{ dictStore.label('receivable_source', row.sourceType) }}</div>
            <div class="cell-sub">{{ row.sourceNo || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="价税合计" width="136" align="right">
          <template #default="{ row }">
            <div class="amount">{{ formatMoney(row.amount) }}</div>
            <div class="cell-sub">税额 {{ formatMoney(row.taxAmount) }} · {{ Number(row.taxRate || 0) }}%</div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="112" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ dictStore.label('invoice_type', row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }"><DictTag type="invoice_status" :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="176" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="info" size="small" title="查看修改记录" @click="$showChangeLog('INVOICE', row.id, row.invoiceNo)">记录</el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              v-perm="'crm:invoice:issue'"
              link
              type="primary"
              size="small"
              @click="openIssue(row)"
            >
              开票
            </el-button>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
              <el-button link type="primary" size="small" class="more-btn">
                更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status !== 'VOID'" command="void">作废发票</el-dropdown-item>
                  <el-dropdown-item v-if="row.status !== 'ISSUED'" command="delete" divided>删除记录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无发票" :image-size="90" /></template>
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

    <!-- 申请开票 -->
    <el-dialog v-model="createVisible" title="申请开票" width="720px" destroy-on-close>
      <el-form label-width="90px" style="margin-bottom: 12px">
        <el-form-item label="来源类型">
          <el-radio-group v-model="sourceType" @change="loadSourceOptions">
            <el-radio-button value="ORDER">订单</el-radio-button>
            <el-radio-button value="CONTRACT">合同</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <el-table
        v-loading="sourceLoading"
        :data="sourceOptions"
        size="small"
        border
        highlight-current-row
        @current-change="(row) => (selectedSource = row)"
      >
        <el-table-column :prop="sourceType === 'ORDER' ? 'orderNo' : 'contractNo'" label="单号" width="140" />
        <el-table-column :prop="sourceType === 'ORDER' ? 'title' : 'name'" label="名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" min-width="180" show-overflow-tooltip />
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <template #empty><el-empty description="暂无可开票单据" :image-size="70" /></template>
      </el-table>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!selectedSource" @click="submitCreate">生成开票申请</el-button>
      </template>
    </el-dialog>

    <!-- 确认开票 -->
    <el-dialog v-model="issueVisible" title="确认开票" width="440px" destroy-on-close>
      <el-form :model="issueForm" label-width="90px">
        <el-form-item label="发票号码" required>
          <el-input v-model="issueForm.invoiceNo" placeholder="请输入发票号码" />
        </el-form-item>
        <el-form-item label="开票日期">
          <el-date-picker v-model="issueForm.invoiceDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开票金额">
          <span class="text-amount">{{ formatMoney(issueForm.amount) }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitIssue">确认开票</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DictTag from '@/components/DictTag.vue'
import {
  createInvoiceFromContract,
  createInvoiceFromOrder,
  deleteInvoice,
  issueInvoice,
  pageInvoices,
  voidInvoice
} from '@/api/tradeDocs'
import { pageOrders } from '@/api/order'
import { pageContracts } from '@/api/contract'
import { useDictStore } from '@/store/dict'
import { formatDate, formatMoney } from '@/utils/format'

const dictStore = useDictStore()

const loading = ref(false)
const saving = ref(false)
const sourceLoading = ref(false)
const list = ref([])
const total = ref(0)
const sourceOptions = ref([])
const sourceType = ref('ORDER')
const selectedSource = ref(null)
const createVisible = ref(false)
const issueVisible = ref(false)

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '', type: '' })
const issueForm = reactive({ id: null, invoiceNo: '', invoiceDate: '', amount: 0 })

async function load() {
  loading.value = true
  try {
    const { data } = await pageInvoices(query)
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
  Object.assign(query, { keyword: '', status: '', type: '', pageNum: 1 })
  load()
}

async function openCreate() {
  createVisible.value = true
  selectedSource.value = null
  sourceType.value = 'ORDER'
  await loadSourceOptions()
}

async function loadSourceOptions() {
  selectedSource.value = null
  sourceLoading.value = true
  try {
    if (sourceType.value === 'ORDER') {
      const { data } = await pageOrders({ pageNum: 1, pageSize: 50, status: 'APPROVED' })
      sourceOptions.value = data.records || []
    } else {
      const { data } = await pageContracts({ pageNum: 1, pageSize: 50 })
      sourceOptions.value = (data.records || []).filter((c) => ['SIGNED', 'APPROVED'].includes(c.status))
    }
  } finally {
    sourceLoading.value = false
  }
}

async function submitCreate() {
  if (!selectedSource.value) return
  saving.value = true
  try {
    if (sourceType.value === 'ORDER') {
      await createInvoiceFromOrder(selectedSource.value.id)
    } else {
      await createInvoiceFromContract(selectedSource.value.id)
    }
    ElMessage.success('开票申请已创建')
    createVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openIssue(row) {
  Object.assign(issueForm, {
    id: row.id,
    invoiceNo: '',
    invoiceDate: new Date().toISOString().slice(0, 10),
    amount: row.amount
  })
  issueVisible.value = true
}

async function submitIssue() {
  if (!issueForm.invoiceNo) {
    ElMessage.warning('请填写发票号码')
    return
  }
  saving.value = true
  try {
    await issueInvoice(issueForm.id, { invoiceNo: issueForm.invoiceNo, invoiceDate: issueForm.invoiceDate })
    ElMessage.success('已确认开票')
    issueVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function handleCommand(command, row) {
  if (command === 'void') return voidOne(row)
  if (command === 'delete') return remove(row)
}

async function voidOne(row) {
  await ElMessageBox.confirm(`确定作废发票「${row.invoiceNo || row.title}」吗？`, '提示', { type: 'warning' })
  await voidInvoice(row.id)
  ElMessage.success('已作废')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('确定删除这条开票记录吗？', '提示', { type: 'warning' })
  await deleteInvoice(row.id)
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

.source {
  font-size: 12.5px;
  color: #3c4759;
}

.amount {
  font-weight: 600;
  color: #1f2733;
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
</style>
