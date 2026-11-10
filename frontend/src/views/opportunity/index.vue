<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索商机 / 客户 / 编号" clearable @keyup.enter="search" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.stage" placeholder="销售阶段" clearable @change="search">
        <el-option v-for="item in dictStore.options('opp_stage')" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="商机状态" clearable @change="search">
        <el-option label="进行中" value="OPEN" />
        <el-option label="已赢单" value="WON" />
        <el-option label="已输单" value="LOST" />
      </el-select>
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-radio-group v-model="viewMode">
        <el-radio-button value="list"><el-icon><List /></el-icon> 列表</el-radio-button>
        <el-radio-button value="kanban"><el-icon><Grid /></el-icon> 看板</el-radio-button>
      </el-radio-group>
      <el-button v-perm="'crm:opportunity:add'" type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon>新建商机
      </el-button>
    </div>

    <!-- 列表 -->
    <div v-if="viewMode === 'list'" class="table-card">
      <div class="table-toolbar">
        <div class="title">商机列表 <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ total }} 个</span></div>
      </div>
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="商机名称" min-width="240" fixed="left">
          <template #default="{ row }">
            <div class="opp-name">{{ row.name }}</div>
            <div class="cell-sub">{{ row.oppNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="关联客户" min-width="200" show-overflow-tooltip />
        <el-table-column label="预计金额" width="140" align="right">
          <template #default="{ row }"><span class="amount">{{ formatMoney(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column label="销售阶段" width="120" align="center">
          <template #default="{ row }"><DictTag type="opp_stage" :value="row.stage" /></template>
        </el-table-column>
        <el-table-column label="赢单率" width="130">
          <template #default="{ row }">
            <el-progress :percentage="row.winRate" :stroke-width="6" :color="rateColor(row.winRate)" />
          </template>
        </el-table-column>
        <el-table-column label="预计成交" width="110">
          <template #default="{ row }">{{ formatDate(row.expectDealDate) }}</template>
        </el-table-column>
        <el-table-column prop="ownerName" label="负责人" width="90" />
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="info" size="small" title="查看修改记录" @click="$showChangeLog('OPPORTUNITY', row.id, row.oppNo)">记录</el-button>
            <el-dropdown v-if="row.status === 'OPEN'" trigger="click" @command="(stage) => advance(row, stage)">
              <el-button link type="primary" size="small">推进阶段<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="item in stageOptions" :key="item.value" :command="item.value" :disabled="item.value === row.stage">
                    {{ item.label }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button v-perm="'crm:opportunity:edit'" link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button v-perm="'crm:opportunity:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无商机" :image-size="90" /></template>
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

    <!-- 看板 -->
    <div v-else v-loading="kanbanLoading" class="kanban">
      <div
        v-for="column in kanban"
        :key="column.stage"
        class="kanban-column"
        :class="{ dragging: dragOverStage === column.stage }"
        @dragover.prevent="dragOverStage = column.stage"
        @dragleave="dragOverStage = ''"
        @drop="onDrop(column.stage)"
      >
        <div class="column-head">
          <div class="column-title">
            <span class="dot" :style="{ background: stageColor(column.stage) }"></span>
            {{ dictStore.label('opp_stage', column.stage) }}
            <span class="column-count">{{ column.count }}</span>
          </div>
          <div class="column-amount">{{ formatWan(column.amount) }}</div>
        </div>

        <div class="column-body">
          <div
            v-for="item in column.items"
            :key="item.id"
            class="kanban-card"
            draggable="true"
            @dragstart="onDragStart(item)"
            @dragend="dragItem = null"
            @click="openForm(item)"
          >
            <div class="card-title">{{ item.name }}</div>
            <div class="card-customer">{{ item.customerName }}</div>
            <div class="card-foot">
              <span class="card-amount">{{ formatWan(item.amount) }}</span>
              <span class="card-owner">{{ item.ownerName }}</span>
            </div>
            <div class="card-date">预计成交 {{ formatDate(item.expectDealDate) }}</div>
          </div>
          <div v-if="!column.items.length" class="column-empty">暂无商机</div>
        </div>
      </div>
    </div>

    <!-- 商机表单 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑商机' : '新建商机'" width="660px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="14">
          <el-col :span="24">
            <el-form-item label="商机名称" prop="name">
              <el-input v-model="form.name" placeholder="如：XX 公司 CRM 项目" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联客户" prop="customerId">
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
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计金额" prop="amount">
              <el-input-number v-model="form.amount" :min="0" :step="10000" style="width: 100%" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="销售阶段">
              <el-select v-model="form.stage" style="width: 100%">
                <el-option v-for="item in stageOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计成交">
              <el-date-picker v-model="form.expectDealDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商机来源">
              <el-select v-model="form.source" style="width: 100%" clearable>
                <el-option v-for="item in dictStore.options('customer_source')" :key="item.value" :label="item.label" :value="item.value" />
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import DictTag from '@/components/DictTag.vue'
import {
  changeStage,
  createOpportunity,
  deleteOpportunity,
  getKanban,
  pageOpportunities,
  updateOpportunity
} from '@/api/opportunity'
import { pageCustomers } from '@/api/customer'
import { userOptions } from '@/api/system'
import { useDictStore } from '@/store/dict'
import { formatDate, formatMoney, formatWan } from '@/utils/format'

const dictStore = useDictStore()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const kanbanLoading = ref(false)
const saving = ref(false)
const customerLoading = ref(false)
const list = ref([])
const total = ref(0)
const kanban = ref([])
const viewMode = ref('list')
const users = ref([])
const customerOptions = ref([])
const formRef = ref()
const formVisible = ref(false)

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', stage: '', status: '' })

const stageOptions = computed(() => dictStore.options('opp_stage'))

const form = reactive({
  id: null,
  name: '',
  customerId: null,
  customerName: '',
  amount: 0,
  stage: 'CONTACT',
  expectDealDate: '',
  source: '',
  ownerUserId: null,
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入商机名称', trigger: 'blur' }],
  customerId: [{ required: true, message: '请选择关联客户', trigger: 'change' }],
  amount: [{ required: true, message: '请输入预计金额', trigger: 'blur' }]
}

function stageColor(stage) {
  return (
    {
      CONTACT: '#98a3b3',
      DEMAND: '#2f6bff',
      QUOTE: '#f5a623',
      NEGOTIATION: '#f56c6c',
      WON: '#22b07d'
    }[stage] || '#98a3b3'
  )
}

function rateColor(rate) {
  if (rate >= 100) return '#22b07d'
  if (rate >= 50) return '#2f6bff'
  return '#f5a623'
}

async function load() {
  loading.value = true
  try {
    const { data } = await pageOpportunities(query)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadKanban() {
  kanbanLoading.value = true
  try {
    const { data } = await getKanban()
    kanban.value = data || []
  } finally {
    kanbanLoading.value = false
  }
}

function search() {
  query.pageNum = 1
  if (viewMode.value === 'kanban') {
    loadKanban()
  } else {
    load()
  }
}

function resetQuery() {
  Object.assign(query, { keyword: '', stage: '', status: '', pageNum: 1 })
  load()
  loadKanban()
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

async function openForm(row) {
  Object.assign(form, {
    id: null,
    name: '',
    customerId: null,
    customerName: '',
    amount: 0,
    stage: 'CONTACT',
    expectDealDate: '',
    source: '',
    ownerUserId: null,
    remark: ''
  })
  if (row) {
    Object.assign(form, row)
    customerOptions.value = [{ id: row.customerId, name: row.customerName }]
  } else if (!customerOptions.value.length) {
    await searchCustomers('')
  }
  if (!users.value.length) {
    const { data } = await userOptions()
    users.value = data || []
  }
  formVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateOpportunity(form.id, form)
    } else {
      await createOpportunity(form)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    refreshAll()
  } finally {
    saving.value = false
  }
}

async function advance(row, stage) {
  await changeStage(row.id, stage)
  ElMessage.success('阶段已更新')
  refreshAll()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除商机「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteOpportunity(row.id)
  ElMessage.success('已删除')
  refreshAll()
}

function refreshAll() {
  load()
  if (viewMode.value === 'kanban') loadKanban()
}

/* 看板拖拽 */
const dragItem = ref(null)
const dragOverStage = ref('')

function onDragStart(item) {
  dragItem.value = item
}

async function onDrop(stage) {
  dragOverStage.value = ''
  if (!dragItem.value || dragItem.value.stage === stage) return
  const item = dragItem.value
  dragItem.value = null
  if (stage === 'LOST') {
    await ElMessageBox.confirm(`确定将「${item.name}」标记为输单吗？`, '提示', { type: 'warning' })
  }
  await changeStage(item.id, stage)
  ElMessage.success(`已推进到「${dictStore.label('opp_stage', stage)}」`)
  loadKanban()
  load()
}

onMounted(async () => {
  await dictStore.load()
  await load()
  await loadKanban()
  if (route.query.create) {
    openForm()
    router.replace({ query: {} })
  }
})
</script>

<style scoped>
.opp-name {
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

.kanban {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 8px;
  min-height: 520px;
}

.kanban-column {
  flex: 1 0 240px;
  background: #f7f9fc;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  transition: all 0.15s;
}

.kanban-column.dragging {
  background: #eef3ff;
  border-color: #b9cdff;
}

.column-head {
  padding: 12px 14px;
  border-bottom: 1px solid #e9eef6;
}

.column-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #2c3648;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.column-count {
  font-size: 11px;
  color: #8b97a8;
  background: #eef1f6;
  border-radius: 8px;
  padding: 0 6px;
  margin-left: 2px;
}

.column-amount {
  font-size: 12px;
  color: #7a8699;
  margin-top: 5px;
}

.column-body {
  flex: 1;
  padding: 10px;
  overflow-y: auto;
  max-height: 560px;
}

.kanban-card {
  background: #fff;
  border: 1px solid #e9eef6;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
  cursor: grab;
  transition: all 0.15s;
}

.kanban-card:hover {
  box-shadow: 0 4px 14px rgba(24, 45, 84, 0.09);
  border-color: #c9d8ff;
  transform: translateY(-1px);
}

.card-title {
  font-size: 12.5px;
  font-weight: 600;
  color: #2c3648;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-customer {
  font-size: 11.5px;
  color: #8b97a8;
  margin: 5px 0 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-amount {
  font-size: 13px;
  font-weight: 600;
  color: #2f6bff;
}

.card-owner {
  font-size: 11px;
  color: #a3adbd;
}

.card-date {
  font-size: 11px;
  color: #b3bcc9;
  margin-top: 6px;
}

.column-empty {
  text-align: center;
  color: #c0c8d4;
  font-size: 12px;
  padding: 20px 0;
}
</style>
