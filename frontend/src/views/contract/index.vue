<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索合同名称 / 编号 / 客户" clearable @keyup.enter="search" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.status" placeholder="合同状态" clearable @change="search">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="审批中" value="PENDING" />
        <el-option label="已审批" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
        <el-option label="已签订" value="SIGNED" />
        <el-option label="已作废" value="VOID" />
      </el-select>
      <el-select v-model="query.paymentStatus" placeholder="回款状态" clearable @change="search">
        <el-option label="未回款" value="UNPAID" />
        <el-option label="部分回款" value="PART" />
        <el-option label="已回款" value="PAID" />
      </el-select>
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button v-perm="'crm:contract:add'" type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon>新建合同
      </el-button>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="title">合同列表 <span class="text-muted" style="font-size: 12px; font-weight: 400">共 {{ total }} 份</span></div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="合同名称" min-width="230" fixed="left">
          <template #default="{ row }">
            <div class="contract-name link-text" @click="openDrawer(row)">{{ row.name }}</div>
            <div class="cell-sub">{{ row.contractNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" min-width="190" show-overflow-tooltip />
        <el-table-column label="合同金额" width="130" align="right">
          <template #default="{ row }"><span class="amount">{{ formatMoney(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column label="已回款" width="130" align="right">
          <template #default="{ row }">
            <span class="received">{{ formatMoney(row.receivedAmount || 0) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="合同状态" width="100" align="center">
          <template #default="{ row }"><DictTag type="contract_status" :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="回款状态" width="100" align="center">
          <template #default="{ row }"><DictTag type="payment_status" :value="row.paymentStatus" /></template>
        </el-table-column>
        <el-table-column label="签约日期" width="110">
          <template #default="{ row }">{{ formatDate(row.signDate) }}</template>
        </el-table-column>
        <el-table-column prop="ownerName" label="负责人" width="90" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="info" size="small" title="查看修改记录" @click="$showChangeLog('CONTRACT', row.id, row.contractNo)">记录</el-button>
            <el-button link type="primary" size="small" @click="openDrawer(row)">详情</el-button>
            <el-button v-perm="'crm:contract:edit'" link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button v-perm="'crm:contract:delete'" link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无合同" :image-size="90" /></template>
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

    <ContractDrawer v-model="drawerVisible" :contract-id="drawerId" @refresh="load" />

    <el-dialog v-model="formVisible" :title="form.id ? '编辑合同' : '新建合同'" width="1000px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="14">
          <el-col :span="24">
            <el-form-item label="合同名称" prop="name">
              <el-input v-model="form.name" placeholder="如：XX 公司 CRM 实施合同" />
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
            <el-form-item label="合同金额" prop="amount">
              <el-input-number
                v-model="form.amount"
                :min="0"
                :step="10000"
                :disabled="form.items.length > 0"
                style="width: 100%"
                controls-position="right"
              />
              <div v-if="form.items.length" class="amount-tip">已按明细合计自动计算</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="签约日期">
              <el-date-picker v-model="form.signDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始日期">
              <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期">
              <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
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
            <el-form-item label="合同明细">
              <div class="items-wrap">
                <div class="items-head">
                  <span class="items-tip">有明细时合同金额自动等于明细合计；不填明细可手工填金额（框架合同）</span>
                  <el-button size="small" type="primary" plain @click="addItem">添加明细</el-button>
                </div>
                <el-table :data="form.items" size="small" border>
                  <el-table-column label="产品/服务" min-width="180">
                    <template #default="{ row }">
                      <el-select
                        v-model="row.productId"
                        filterable
                        placeholder="选择产品"
                        style="width: 100%"
                        @change="(val) => onProductChange(row, val)"
                      >
                        <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="单位" width="70" align="center">
                    <template #default="{ row }"><el-input v-model="row.unit" size="small" /></template>
                  </el-table-column>
                  <el-table-column label="数量" width="100">
                    <template #default="{ row }">
                      <el-input-number v-model="row.quantity" :min="0" size="small" controls-position="right" style="width: 100%" />
                    </template>
                  </el-table-column>
                  <el-table-column label="单价" width="120">
                    <template #default="{ row }">
                      <el-input-number v-model="row.price" :min="0" :step="1000" size="small" controls-position="right" style="width: 100%" />
                    </template>
                  </el-table-column>
                  <el-table-column label="金额" width="110" align="right">
                    <template #default="{ row }">{{ formatMoney(itemAmount(row), false) }}</template>
                  </el-table-column>
                  <el-table-column label="" width="60" align="center">
                    <template #default="{ $index }">
                      <el-button link type="danger" size="small" @click="removeItem($index)">删除</el-button>
                    </template>
                  </el-table-column>
                  <template #empty>
                    <div class="items-empty">暂无明细</div>
                  </template>
                </el-table>
                <div class="items-total">明细合计：<b>{{ formatMoney(itemsTotal) }}</b></div>
              </div>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import DictTag from '@/components/DictTag.vue'
import ContractDrawer from './ContractDrawer.vue'
import { createContract, deleteContract, getContract, pageContracts, pageProducts, updateContract } from '@/api/contract'
import { pageCustomers } from '@/api/customer'
import { userOptions } from '@/api/system'
import { formatDate, formatMoney } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const customerLoading = ref(false)
const list = ref([])
const total = ref(0)
const users = ref([])
const customerOptions = ref([])
const products = ref([])
const formRef = ref()
const formVisible = ref(false)
const drawerVisible = ref(false)
const drawerId = ref(null)

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '', paymentStatus: '' })

const form = reactive({
  id: null,
  name: '',
  customerId: null,
  customerName: '',
  amount: 0,
  signDate: '',
  startDate: '',
  endDate: '',
  ownerUserId: null,
  remark: '',
  items: []
})

const rules = {
  name: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
  customerId: [{ required: true, message: '请选择关联客户', trigger: 'change' }],
  amount: [{ required: true, message: '请输入合同金额', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const { data } = await pageContracts(query)
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
  Object.assign(query, { keyword: '', status: '', paymentStatus: '', pageNum: 1 })
  load()
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

const itemsTotal = computed(() =>
  (form.items || []).reduce((sum, item) => sum + itemAmount(item), 0)
)

watch(itemsTotal, (val) => {
  if (form.items.length) {
    form.amount = val
  }
})

function itemAmount(row) {
  const qty = Number(row.quantity || 0)
  const price = Number(row.price || 0)
  return Math.round(qty * price * 100) / 100
}

function addItem() {
  form.items.push({ productId: null, productName: '', unit: '', quantity: 1, price: 0 })
}

function removeItem(index) {
  form.items.splice(index, 1)
}

function onProductChange(row, productId) {
  const hit = products.value.find((p) => p.id === productId)
  if (hit) {
    row.productName = hit.name
    row.unit = hit.unit
    row.price = Number(hit.price)
  }
}

async function openForm(row) {
  Object.assign(form, {
    id: null,
    name: '',
    customerId: null,
    customerName: '',
    amount: 0,
    signDate: '',
    startDate: '',
    endDate: '',
    ownerUserId: null,
    remark: '',
    items: []
  })
  if (row) {
    // 列表行不含明细，编辑必须拉详情，否则保存会把明细清空
    const { data } = await getContract(row.id)
    Object.assign(form, data)
    form.items = data.items?.length ? data.items : []
    customerOptions.value = [{ id: data.customerId, name: data.customerName }]
  } else if (!customerOptions.value.length) {
    await searchCustomers('')
  }
  if (!users.value.length) {
    const { data } = await userOptions()
    users.value = data || []
  }
  if (!products.value.length) {
    const { data } = await pageProducts({ pageNum: 1, pageSize: 200 })
    products.value = data.records || []
  }
  formVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateContract(form.id, form)
    } else {
      await createContract(form)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除合同「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteContract(row.id)
  ElMessage.success('已删除')
  load()
}

function openDrawer(row) {
  drawerId.value = row.id
  drawerVisible.value = true
}

onMounted(async () => {
  await load()
  if (route.query.create) {
    openForm()
    router.replace({ query: {} })
  }
})
</script>

<style scoped>
.contract-name {
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

.received {
  color: #22b07d;
  font-weight: 500;
}
.amount-tip {
  font-size: 11.5px;
  color: #9aa5b6;
  line-height: 1.6;
}
.items-wrap {
  width: 100%;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.items-tip {
  font-size: 11.5px;
  color: #9aa5b6;
}
.items-total {
  text-align: right;
  font-size: 12.5px;
  color: #7a8699;
  margin-top: 6px;
}
.items-total b {
  color: #2f6bff;
  font-size: 15px;
}
.items-empty {
  padding: 10px 0;
  color: #a3adbd;
  font-size: 12px;
}
</style>
