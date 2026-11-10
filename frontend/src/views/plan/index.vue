<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索计划编号 / 订单号 / 客户" clearable @keyup.enter="search" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.status" placeholder="计划状态" clearable @change="search">
        <el-option v-for="item in dictStore.options('plan_status')" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button v-perm="'crm:plan:add'" type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>新建发货计划
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">发货计划 <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ total }} 条</span></div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="计划编号" width="140" fixed="left">
          <template #default="{ row }">
            <div class="no">{{ row.planNo }}</div>
            <div class="cell-sub">{{ row.orderNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" min-width="180" show-overflow-tooltip />
        <el-table-column label="计划发货" width="110">
          <template #default="{ row }">{{ formatDate(row.planDate) }}</template>
        </el-table-column>
        <el-table-column label="计划数量" width="100" align="right">
          <template #default="{ row }">{{ Number(row.planQty || 0) }}</template>
        </el-table-column>
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="warehouse" label="仓库" width="90" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }"><DictTag type="plan_status" :value="row.status" /></template>
        </el-table-column>
        <el-table-column prop="ownerName" label="负责人" width="90" />
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="info" size="small" title="查看修改记录" @click="$showChangeLog('DELIVERY_PLAN', row.id, row.planNo)">记录</el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              v-perm="'crm:plan:ship'"
              link
              type="success"
              size="small"
              @click="ship(row)"
            >
              生成出库单
            </el-button>
            <el-button v-if="row.status === 'SHIPPED'" link type="primary" size="small" @click="router.push('/deliveries')">
              查看出库
            </el-button>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
              <el-button link type="primary" size="small">
                更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status === 'PENDING'" command="edit">编辑计划</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 'PENDING'" command="cancel">取消计划</el-dropdown-item>
                  <el-dropdown-item v-if="row.status !== 'SHIPPED'" command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无发货计划" :image-size="90" /></template>
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

    <!-- 新建 -->
    <el-dialog v-model="createVisible" title="新建发货计划" width="640px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
        仅显示「已审核且无待发货计划」的订单，计划生成后可一键转出库单。
      </el-alert>
      <el-table
        v-loading="orderLoading"
        :data="orderOptions"
        size="small"
        border
        highlight-current-row
        @current-change="(row) => (selectedOrder = row)"
      >
        <el-table-column prop="orderNo" label="订单号" width="130" />
        <el-table-column prop="title" label="订单标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" min-width="170" show-overflow-tooltip />
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <template #empty><el-empty description="没有可安排发货的订单" :image-size="70" /></template>
      </el-table>
      <el-form :model="createForm" label-width="100px" style="margin-top: 14px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="计划发货日">
              <el-date-picker v-model="createForm.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发货仓库">
              <el-input v-model="createForm.warehouse" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="createForm.remark" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!selectedOrder" @click="submitCreate">创建计划</el-button>
      </template>
    </el-dialog>

    <!-- 编辑 -->
    <el-dialog v-model="editVisible" title="编辑发货计划" width="480px" destroy-on-close>
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="计划发货日">
          <el-date-picker v-model="editForm.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="发货仓库">
          <el-input v-model="editForm.warehouse" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import DictTag from '@/components/DictTag.vue'
import {
  cancelPlan,
  createPlanFromOrder,
  deletePlan,
  generateDeliveryFromPlan,
  pagePlans,
  updatePlan
} from '@/api/tradeDocs'
import { pageOrders } from '@/api/order'
import { useDictStore } from '@/store/dict'
import { formatDate, formatMoney } from '@/utils/format'

const router = useRouter()
const dictStore = useDictStore()

const loading = ref(false)
const saving = ref(false)
const orderLoading = ref(false)
const list = ref([])
const total = ref(0)
const orderOptions = ref([])
const selectedOrder = ref(null)
const createVisible = ref(false)
const editVisible = ref(false)

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' })
const createForm = reactive({ planDate: '', warehouse: '主仓', remark: '' })
const editForm = reactive({ id: null, planDate: '', warehouse: '', remark: '' })

async function load() {
  loading.value = true
  try {
    const { data } = await pagePlans(query)
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
  createVisible.value = true
  selectedOrder.value = null
  Object.assign(createForm, { planDate: '', warehouse: '主仓', remark: '' })
  orderLoading.value = true
  try {
    const { data } = await pageOrders({ pageNum: 1, pageSize: 50, status: 'APPROVED' })
    const plans = await pagePlans({ pageNum: 1, pageSize: 100, status: 'PENDING' })
    const plannedOrderIds = new Set((plans.data.records || []).map((p) => p.orderId))
    orderOptions.value = (data.records || []).filter(
      (o) => o.deliveryStatus !== 'DELIVERED' && !o.deliveryCount && !plannedOrderIds.has(o.id)
    )
  } finally {
    orderLoading.value = false
  }
}

async function submitCreate() {
  if (!selectedOrder.value) return
  saving.value = true
  try {
    await createPlanFromOrder(selectedOrder.value.id, {
      planDate: createForm.planDate || undefined,
      warehouse: createForm.warehouse,
      remark: createForm.remark
    })
    ElMessage.success('发货计划已创建')
    createVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openEdit(row) {
  Object.assign(editForm, {
    id: row.id,
    planDate: row.planDate,
    warehouse: row.warehouse,
    remark: row.remark
  })
  editVisible.value = true
}

async function submitEdit() {
  saving.value = true
  try {
    await updatePlan(editForm.id, { ...editForm })
    ElMessage.success('已保存')
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function handleCommand(command, row) {
  if (command === 'edit') return openEdit(row)
  if (command === 'cancel') return cancel(row)
  if (command === 'delete') return remove(row)
}

async function ship(row) {
  await ElMessageBox.confirm(`将按计划「${row.planNo}」生成出库单，是否继续？`, '生成出库单', { type: 'info' })
  await generateDeliveryFromPlan(row.id)
  ElMessage.success('出库单已生成，请到出库管理确认发货')
  load()
}

async function cancel(row) {
  await ElMessageBox.confirm(`确定取消计划「${row.planNo}」吗？`, '提示', { type: 'warning' })
  await cancelPlan(row.id)
  ElMessage.success('已取消')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除计划「${row.planNo}」吗？`, '提示', { type: 'warning' })
  await deletePlan(row.id)
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
</style>
