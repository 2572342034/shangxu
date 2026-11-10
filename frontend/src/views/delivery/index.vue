<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索出库单号 / 订单号 / 客户" clearable @keyup.enter="search" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.status" placeholder="出库状态" clearable @change="search">
        <el-option v-for="item in dictStore.options('delivery_status')" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button v-perm="'crm:delivery:add'" type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>从订单生成
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">出库单 <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ total }} 张</span></div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="出库单号" width="140" fixed="left">
          <template #default="{ row }">
            <span class="link-text" @click="openDrawer(row)">{{ row.deliveryNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="关联订单" width="140" />
        <el-table-column prop="customerName" label="客户" min-width="170" show-overflow-tooltip />
        <el-table-column label="出库数量" width="100" align="right">
          <template #default="{ row }">{{ Number(row.totalQty || 0) }}</template>
        </el-table-column>
        <el-table-column label="出库金额" width="130" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }"><DictTag type="delivery_status" :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="物流" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.logisticsNo">{{ row.logisticsCompany }} · {{ row.logisticsNo }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="出库日期" width="110">
          <template #default="{ row }">{{ formatDate(row.deliveryDate) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="info" size="small" title="查看修改记录" @click="$showChangeLog('DELIVERY', row.id, row.deliveryNo)">记录</el-button>
            <el-button link type="primary" size="small" @click="openDrawer(row)">详情</el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              v-perm="'crm:delivery:edit'"
              link
              type="warning"
              size="small"
              @click="changeStatus(row, 'PENDING')"
            >
              提交审核
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              v-perm="'crm:delivery:edit'"
              link
              type="success"
              size="small"
              @click="changeStatus(row, 'APPROVED')"
            >
              审核通过
            </el-button>
            <el-button
              v-if="row.status === 'APPROVED'"
              v-perm="'crm:delivery:ship'"
              link
              type="success"
              size="small"
              @click="ship(row)"
            >
              确认出库
            </el-button>
            <el-button
              v-if="row.status === 'SHIPPED'"
              v-perm="'crm:delivery:ship'"
              link
              type="success"
              size="small"
              @click="sign(row)"
            >
              签收
            </el-button>
            <el-dropdown v-if="row.status !== 'CANCELLED'" trigger="click" @command="(cmd) => handleCommand(cmd, row)">
              <el-button link type="primary" size="small">
                更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">编辑物流信息</el-dropdown-item>
                  <el-dropdown-item command="cancel">作废出库单</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无出库单" :image-size="90" /></template>
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

    <!-- 从订单生成 -->
    <el-dialog v-model="createVisible" title="从订单生成出库单" width="720px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
        仅显示「已审核且未完成出库」的订单，生成后自动带出明细与收货信息。
      </el-alert>
      <el-table v-loading="orderLoading" :data="orderOptions" size="small" border highlight-current-row @current-change="onOrderSelect">
        <el-table-column prop="orderNo" label="订单号" width="140" />
        <el-table-column prop="title" label="订单标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" min-width="180" show-overflow-tooltip />
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="出库" width="90" align="center">
          <template #default="{ row }"><DictTag type="delivery_progress" :value="row.deliveryStatus" /></template>
        </el-table-column>
        <template #empty><el-empty description="没有待出库的订单" :image-size="70" /></template>
      </el-table>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!selectedOrder" @click="submitCreate">生成出库单</el-button>
      </template>
    </el-dialog>

    <!-- 编辑物流 -->
    <el-dialog v-model="editVisible" title="编辑物流信息" width="480px" destroy-on-close>
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="物流公司">
          <el-select v-model="editForm.logisticsCompany" filterable allow-create style="width: 100%" placeholder="选择或输入">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="京东物流" value="京东物流" />
            <el-option label="德邦物流" value="德邦物流" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="自送" value="自送" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="editForm.logisticsNo" />
        </el-form-item>
        <el-form-item label="发货仓库">
          <el-input v-model="editForm.warehouse" />
        </el-form-item>
        <el-form-item label="收货人">
          <el-input v-model="editForm.receiver" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="editForm.receiverPhone" />
        </el-form-item>
        <el-form-item label="收货地址">
          <el-input v-model="editForm.address" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="drawerVisible" size="720px" :with-header="false" destroy-on-close>
      <div v-loading="detailLoading" class="drawer-body">
        <template v-if="detail.id">
          <div class="detail-head">
            <div>
              <div class="detail-title">
                <h3>{{ detail.deliveryNo }}</h3>
                <DictTag type="delivery_status" :value="detail.status" />
              </div>
              <div class="detail-sub">{{ detail.orderNo }} · {{ detail.customerName }}</div>
            </div>
            <el-button @click="drawerVisible = false">关闭</el-button>
          </div>

          <el-descriptions :column="2" border size="small" class="desc">
            <el-descriptions-item label="发货仓库">{{ detail.warehouse || '-' }}</el-descriptions-item>
            <el-descriptions-item label="出库日期">{{ formatDate(detail.deliveryDate) }}</el-descriptions-item>
            <el-descriptions-item label="物流公司">{{ detail.logisticsCompany || '-' }}</el-descriptions-item>
            <el-descriptions-item label="物流单号">{{ detail.logisticsNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货人">{{ detail.receiver || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detail.receiverPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货地址" :span="2">{{ detail.address || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-title">出库明细</div>
          <el-table :data="detail.items" size="small" border>
            <el-table-column prop="productName" label="产品" min-width="220" show-overflow-tooltip />
            <el-table-column prop="unit" label="单位" width="90" align="center" />
            <el-table-column prop="quantity" label="数量" width="100" align="right" />
            <el-table-column prop="remark" label="备注" min-width="140">
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DictTag from '@/components/DictTag.vue'
import {
  cancelDelivery,
  createDeliveryFromOrder,
  deleteDelivery,
  getDelivery,
  pageDeliveries,
  pageOrders,
  shipDelivery,
  signDelivery,
  updateDelivery, changeDeliveryStatus } from '@/api/order'
import { useDictStore } from '@/store/dict'
import { formatDate, formatMoney } from '@/utils/format'

const dictStore = useDictStore()

const loading = ref(false)
const saving = ref(false)
const detailLoading = ref(false)
const orderLoading = ref(false)
const list = ref([])
const total = ref(0)
const orderOptions = ref([])
const selectedOrder = ref(null)
const createVisible = ref(false)
const editVisible = ref(false)
const drawerVisible = ref(false)
const detail = reactive({ id: null, items: [] })

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' })
const editForm = reactive({
  id: null,
  logisticsCompany: '',
  logisticsNo: '',
  warehouse: '',
  receiver: '',
  receiverPhone: '',
  address: ''
})

async function load() {
  loading.value = true
  try {
    const { data } = await pageDeliveries(query)
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
  orderLoading.value = true
  try {
    const { data } = await pageOrders({ pageNum: 1, pageSize: 50, status: 'APPROVED' })
    orderOptions.value = (data.records || []).filter((o) => o.deliveryStatus !== 'DELIVERED')
  } finally {
    orderLoading.value = false
  }
}

function onOrderSelect(row) {
  selectedOrder.value = row
}

async function submitCreate() {
  if (!selectedOrder.value) return
  saving.value = true
  try {
    await createDeliveryFromOrder(selectedOrder.value.id)
    ElMessage.success('出库单已生成')
    createVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function changeStatus(row, status) {
  const label = { PENDING: '提交审核', APPROVED: '审核通过' }[status]
  await ElMessageBox.confirm(`确定${label}「${row.deliveryNo}」吗？`, '提示', { type: 'warning' })
  await changeDeliveryStatus(row.id, status)
  ElMessage.success(status === 'APPROVED' ? '审核通过，已自动生成应收单' : '已提交审核')
  await load()
}

async function ship(row) {
  await ElMessageBox.confirm(`确认出库单「${row.deliveryNo}」已发货？`, '确认出库', { type: 'info' })
  await shipDelivery(row.id)
  ElMessage.success('已确认出库')
  load()
}

async function sign(row) {
  await ElMessageBox.confirm(`确认客户已签收「${row.deliveryNo}」？`, '确认签收', { type: 'info' })
  await signDelivery(row.id)
  ElMessage.success('已确认签收')
  load()
}

async function handleCommand(command, row) {
  if (command === 'edit') {
    Object.assign(editForm, {
      id: row.id,
      logisticsCompany: row.logisticsCompany || '',
      logisticsNo: row.logisticsNo || '',
      warehouse: row.warehouse || '主仓',
      receiver: row.receiver || '',
      receiverPhone: row.receiverPhone || '',
      address: row.address || ''
    })
    editVisible.value = true
    return
  }
  if (command === 'cancel') {
    await ElMessageBox.confirm(`确定作废出库单「${row.deliveryNo}」吗？`, '提示', { type: 'warning' })
    await cancelDelivery(row.id)
    ElMessage.success('已作废')
    load()
    return
  }
  if (command === 'delete') {
    await ElMessageBox.confirm(`确定删除出库单「${row.deliveryNo}」吗？`, '提示', { type: 'warning' })
    await deleteDelivery(row.id)
    ElMessage.success('已删除')
    load()
  }
}

async function submitEdit() {
  saving.value = true
  try {
    await updateDelivery(editForm.id, { ...editForm })
    ElMessage.success('已保存')
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function openDrawer(row) {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    const { data } = await getDelivery(row.id)
    Object.assign(detail, data)
  } finally {
    detailLoading.value = false
  }
}

onMounted(async () => {
  await dictStore.load()
  await load()
})
</script>

<style scoped>
.drawer-body {
  min-height: 240px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 16px;
  border-bottom: 1px solid #eef1f6;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 8px;
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

.desc {
  margin: 16px 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2733;
  margin: 18px 0 10px;
}
</style>
