<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索订单号 / 标题 / 客户" clearable @keyup.enter="search" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.status" placeholder="订单状态" clearable @change="search">
        <el-option v-for="item in dictStore.options('order_status')" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="query.deliveryStatus" placeholder="出库状态" clearable @change="search">
        <el-option v-for="item in dictStore.options('delivery_progress')" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button v-perm="'crm:order:add'" type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon>新建订单
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">销售订单 <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ total }} 单</span></div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="订单" min-width="240" fixed="left">
          <template #default="{ row }">
            <div class="order-title link-text" @click="openDrawer(row)">{{ row.title }}</div>
            <div class="cell-sub">{{ row.orderNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" min-width="200" show-overflow-tooltip />
        <el-table-column label="订单金额" width="140" align="right">
          <template #default="{ row }"><span class="amount">{{ formatMoney(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column label="订单状态" width="100" align="center">
          <template #default="{ row }"><DictTag type="order_status" :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="出库" width="100" align="center">
          <template #default="{ row }"><DictTag type="delivery_progress" :value="row.deliveryStatus" /></template>
        </el-table-column>
        <el-table-column label="收款" width="100" align="center">
          <template #default="{ row }"><DictTag type="receivable_status" :value="row.receivableStatus" /></template>
        </el-table-column>
        <el-table-column label="下单日期" width="110">
          <template #default="{ row }">{{ formatDate(row.orderDate) }}</template>
        </el-table-column>
        <el-table-column prop="ownerName" label="负责人" width="90" />
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="info" size="small" title="查看修改记录" @click="$showChangeLog('ORDER', row.id, row.orderNo)">记录</el-button>
            <el-button link type="primary" size="small" @click="openDrawer(row)">详情</el-button>
            <el-button
              v-if="canEdit(row)"
              v-perm="'crm:order:edit'"
              link
              type="primary"
              size="small"
              @click="openForm(row)"
            >
              编辑
            </el-button>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
              <el-button link type="primary" size="small">
                更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" command="submit">
                    提交审批
                  </el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 'PENDING'" command="approve">审核通过</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 'PENDING'" command="reject">驳回</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 'APPROVED'" command="delivery">生成出库单</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 'APPROVED'" command="receivable">生成应收单</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 'APPROVED'" command="cancel" divided>取消订单</el-dropdown-item>
                  <el-dropdown-item v-if="row.status !== 'APPROVED'" command="delete" divided>删除订单</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无订单" :image-size="90" /></template>
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

    <OrderForm v-model="formVisible" :data="currentRow" @success="load" />

    <!-- 订单详情 -->
    <el-drawer v-model="drawerVisible" size="820px" :with-header="false" destroy-on-close>
      <div v-loading="detailLoading" class="drawer-body">
        <template v-if="detail.id">
          <div class="detail-head">
            <div>
              <div class="detail-title">
                <h3>{{ detail.title }}</h3>
                <DictTag type="order_status" :value="detail.status" />
                <DictTag type="delivery_progress" :value="detail.deliveryStatus" />
                <DictTag type="receivable_status" :value="detail.receivableStatus" />
              </div>
              <div class="detail-sub">{{ detail.orderNo }} · {{ detail.customerName }}</div>
            </div>
            <div class="detail-actions">
              <el-button v-if="detail.status === 'DRAFT' || detail.status === 'REJECTED'" type="primary" size="small" @click="changeStatus('PENDING')">
                提交审批
              </el-button>
              <el-button v-if="detail.status === 'PENDING'" type="success" size="small" @click="changeStatus('APPROVED')">审核通过</el-button>
              <el-button v-if="detail.status === 'APPROVED'" type="primary" plain size="small" @click="genDelivery">生成出库单</el-button>
              <el-button @click="drawerVisible = false">关闭</el-button>
            </div>
          </div>

          <div class="amount-bar">
            <div class="amount-item">
              <div class="label">订单金额</div>
              <div class="value">{{ formatMoney(detail.amount) }}</div>
            </div>
            <div class="amount-item">
              <div class="label">明细条数</div>
              <div class="value">{{ detail.items?.length || 0 }}</div>
            </div>
            <div class="amount-item">
              <div class="label">关联出库单</div>
              <div class="value">{{ deliveries.length }}</div>
            </div>
            <div class="amount-item">
              <div class="label">下单日期</div>
              <div class="value small">{{ formatDate(detail.orderDate) }}</div>
            </div>
          </div>

          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="关联客户">{{ detail.customerName }}</el-descriptions-item>
            <el-descriptions-item label="关联合同">{{ detail.contractNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ detail.ownerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-title">订单明细</div>
          <el-table :data="detail.items" size="small" border>
            <el-table-column prop="productName" label="产品" min-width="200" show-overflow-tooltip />
            <el-table-column prop="unit" label="单位" width="80" align="center" />
            <el-table-column prop="quantity" label="数量" width="90" align="right" />
            <el-table-column label="单价" width="120" align="right">
              <template #default="{ row }">{{ formatMoney(row.price) }}</template>
            </el-table-column>
            <el-table-column label="金额" width="130" align="right">
              <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
            </el-table-column>
          </el-table>

          <template v-if="deliveries.length">
            <div class="section-title">关联出库单</div>
            <el-table :data="deliveries" size="small" border>
              <el-table-column prop="deliveryNo" label="出库单号" width="140" />
              <el-table-column label="出库日期" width="110">
                <template #default="{ row }">{{ formatDate(row.deliveryDate) }}</template>
              </el-table-column>
              <el-table-column prop="logisticsCompany" label="物流公司" width="110">
                <template #default="{ row }">{{ row.logisticsCompany || '-' }}</template>
              </el-table-column>
              <el-table-column prop="logisticsNo" label="物流单号" min-width="140">
                <template #default="{ row }">{{ row.logisticsNo || '-' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }"><DictTag type="delivery_status" :value="row.status" /></template>
              </el-table-column>
            </el-table>
          </template>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import DictTag from '@/components/DictTag.vue'
import OrderForm from './OrderForm.vue'
import {
  changeOrderStatus,
  createDeliveryFromOrder,
  createReceivableFromOrder,
  deleteOrder,
  deliveriesByOrder,
  getOrder,
  pageOrders
} from '@/api/order'
import { useDictStore } from '@/store/dict'
import { useUserStore } from '@/store/user'
import { formatDate, formatMoney } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const dictStore = useDictStore()
const userStore = useUserStore()

const loading = ref(false)
const detailLoading = ref(false)
const list = ref([])
const total = ref(0)
const formVisible = ref(false)
const currentRow = ref(null)
const drawerVisible = ref(false)
const detail = reactive({ id: null, items: [] })
const deliveries = ref([])

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '', deliveryStatus: '' })

function canEdit(row) {
  return row.status === 'DRAFT' || row.status === 'REJECTED'
}

async function load() {
  loading.value = true
  try {
    const { data } = await pageOrders(query)
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
  Object.assign(query, { keyword: '', status: '', deliveryStatus: '', pageNum: 1 })
  load()
}

function openForm(row) {
  currentRow.value = row ? { ...row } : null
  formVisible.value = true
}

async function openDrawer(row) {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    const { data } = await getOrder(row.id)
    Object.assign(detail, data)
    const res = await deliveriesByOrder(row.id)
    deliveries.value = res.data || []
  } finally {
    detailLoading.value = false
  }
}

async function changeStatus(status) {
  const label = { PENDING: '提交审批', APPROVED: '审核通过', REJECTED: '驳回', CANCELLED: '取消' }[status]
  await ElMessageBox.confirm(`确定要${label}订单「${detail.title}」吗？`, '提示', { type: 'warning' })
  await changeOrderStatus(detail.id, status)
  ElMessage.success('操作成功')
  await openDrawer({ id: detail.id })
  load()
}

async function genDelivery() {
  await ElMessageBox.confirm('将按订单明细生成出库单，是否继续？', '生成出库单', { type: 'info' })
  await createDeliveryFromOrder(detail.id)
  ElMessage.success('出库单已生成，请到「出库管理」确认出库')
  await openDrawer({ id: detail.id })
  load()
}

async function handleCommand(command, row) {
  if (command === 'submit') return changeStatusFor(row, 'PENDING')
  if (command === 'approve') return changeStatusFor(row, 'APPROVED')
  if (command === 'reject') return changeStatusFor(row, 'REJECTED')
  if (command === 'cancel') return changeStatusFor(row, 'CANCELLED')
  if (command === 'delivery') {
    await ElMessageBox.confirm(`确定按订单「${row.title}」生成出库单吗？`, '生成出库单', { type: 'info' })
    await createDeliveryFromOrder(row.id)
    ElMessage.success('出库单已生成')
    load()
    return
  }
  if (command === 'receivable') {
    await ElMessageBox.confirm(`确定按订单「${row.title}」生成应收单吗？`, '生成应收单', { type: 'info' })
    await createReceivableFromOrder(row.id)
    ElMessage.success('应收单已生成')
    load()
    return
  }
  if (command === 'delete') {
    await ElMessageBox.confirm(`确定删除订单「${row.title}」吗？`, '删除确认', { type: 'warning' })
    await deleteOrder(row.id)
    ElMessage.success('已删除')
    load()
  }
}

async function changeStatusFor(row, status) {
  const label = { PENDING: '提交审批', APPROVED: '审核通过', REJECTED: '驳回', CANCELLED: '取消' }[status]
  await ElMessageBox.confirm(`确定要${label}订单「${row.title}」吗？`, '提示', { type: 'warning' })
  await changeOrderStatus(row.id, status)
  ElMessage.success('操作成功')
  load()
}

onMounted(async () => {
  await dictStore.load()
  await load()
  if (route.query.create) {
    if (userStore.hasPerm('crm:order:add')) openForm()
    router.replace({ query: {} })
  }
})
</script>

<style scoped>
.order-title {
  font-size: 13px;
  font-weight: 500;
  color: #2c3648;
}

.cell-sub {
  font-size: 11px;
  color: #a3adbd;
  margin-top: 2px;
}

.amount {
  font-weight: 600;
  color: #1f2733;
}

.drawer-body {
  min-height: 240px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eef1f6;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-title h3 {
  margin: 0;
  font-size: 17px;
  color: #16233b;
}

.detail-sub {
  font-size: 12px;
  color: #8b97a8;
  margin-top: 6px;
}

.detail-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.amount-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
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

.amount-item .value.small {
  font-size: 14px;
  font-weight: 600;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2733;
  margin: 18px 0 10px;
}
</style>
