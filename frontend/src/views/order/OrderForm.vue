<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑订单' : '新建订单'"
    width="900px"
    destroy-on-close
    @closed="reset"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="订单标题" prop="title">
            <el-input v-model="form.title" placeholder="如：XX 公司年度采购订单" maxlength="128" />
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
          <el-form-item label="关联合同">
            <el-select
              v-model="form.contractId"
              clearable
              filterable
              placeholder="选填，仅显示可下单合同"
              style="width: 100%"
              @change="onContractChange"
            >
              <el-option
                v-for="c in contractOptions"
                :key="c.id"
                :value="c.id"
                :label="`${c.contractNo} · ${c.name}`"
                :disabled="!c.selectable"
              >
                <div class="contract-option">
                  <span>{{ c.contractNo }} · {{ c.name }}</span>
                  <span class="contract-remain" :class="{ warn: !c.selectable }">
                    {{ c.selectable ? `剩余 ${formatMoney(c.remainAmount, false)}` : c.reason }}
                  </span>
                </div>
              </el-option>
            </el-select>
            <div
              v-if="selectedContract"
              class="contract-tip"
              :class="{ over: !selectedContract.selectable || overContract }"
            >
              合同 {{ formatMoney(selectedContract.amount, false) }} · 已下单
              {{ formatMoney(selectedContract.orderedAmount, false) }} · 剩余
              {{ formatMoney(selectedContract.remainAmount, false) }}
              <template v-if="!selectedContract.selectable">，{{ selectedContract.reason }}</template>
              <template v-else-if="overContract">
                ，本单超出 {{ formatMoney(totalAmount - Number(selectedContract.remainAmount || 0), false) }}
              </template>
            </div>
            <div v-if="form.customerId && !contractOptions.length" class="contract-tip">
              该客户暂无合同，可先到「合同管理」创建，或直接按独立订单下单
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="下单日期">
            <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="负责人">
            <el-select v-model="form.ownerUserId" filterable clearable style="width: 100%" placeholder="默认当前用户">
              <el-option v-for="user in users" :key="user.id" :label="user.nickname" :value="user.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="备注">
            <el-input v-model="form.remark" placeholder="选填" maxlength="500" />
          </el-form-item>
        </el-col>
      </el-row>

      <div class="items-head">
        <span class="items-title">订单明细</span>
        <div>
          <span class="total">合计：<b>{{ formatMoney(totalAmount) }}</b></span>
          <el-button type="primary" plain size="small" @click="addItem">添加明细</el-button>
        </div>
      </div>

      <el-table :data="form.items" size="small" border class="items-table">
        <el-table-column label="产品" min-width="220">
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
        <el-table-column label="单位" width="80" align="center">
          <template #default="{ row }">
            <el-input v-model="row.unit" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="数量" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="0" :step="1" size="small" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.price" :min="0" :step="1000" size="small" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }">{{ formatMoney(itemAmount(row), false) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="removeItem($index)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-items">还没有明细，点击右上角「添加明细」</div>
        </template>
      </el-table>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createOrder, getOrder, updateOrder } from '@/api/order'
import { pageCustomers } from '@/api/customer'
import { pageProducts, selectableContracts } from '@/api/contract'
import { userOptions } from '@/api/system'
import { formatMoney } from '@/utils/format'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  data: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref()
const saving = ref(false)
const customerLoading = ref(false)
const customerOptions = ref([])
const contractOptions = ref([])
const products = ref([])
const users = ref([])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const isEdit = computed(() => Boolean(props.data?.id))

const defaultForm = () => ({
  id: null,
  title: '',
  customerId: null,
  customerName: '',
  contractId: null,
  contractNo: '',
  orderDate: '',
  ownerUserId: null,
  remark: '',
  items: []
})

const form = reactive(defaultForm())

const rules = {
  title: [{ required: true, message: '请输入订单标题', trigger: 'blur' }],
  customerId: [{ required: true, message: '请选择关联客户', trigger: 'change' }]
}

const selectedContract = computed(
  () => contractOptions.value.find((c) => c.id === form.contractId) || null
)
const overContract = computed(
  () => Boolean(selectedContract.value) &&
    totalAmount.value > Number(selectedContract.value.remainAmount || 0)
)

const totalAmount = computed(() =>
  (form.items || []).reduce((sum, item) => sum + itemAmount(item), 0)
)

function itemAmount(row) {
  const qty = Number(row.quantity || 0)
  const price = Number(row.price || 0)
  return Math.round(qty * price * 100) / 100
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
  form.contractId = null
  form.contractNo = ''
  loadContracts(id)
}

async function loadContracts(customerId) {
  if (!customerId) {
    contractOptions.value = []
    return
  }
  try {
    const { data } = await selectableContracts(customerId)
    contractOptions.value = data || []
  } catch (e) {
    contractOptions.value = []
  }
}

function onContractChange(id) {
  const hit = contractOptions.value.find((c) => c.id === id)
  form.contractNo = hit ? hit.contractNo : ''
}

function onProductChange(row, productId) {
  const hit = products.value.find((p) => p.id === productId)
  if (hit) {
    row.productName = hit.name
    row.unit = hit.unit
    row.price = Number(hit.price)
  }
}

function addItem() {
  form.items.push({ productId: null, productName: '', unit: '', quantity: 1, price: 0 })
}

function removeItem(index) {
  form.items.splice(index, 1)
}

function reset() {
  formRef.value?.clearValidate()
  Object.assign(form, defaultForm())
}

watch(
  () => props.modelValue,
  async (val) => {
    if (!val) return
    Object.assign(form, defaultForm())
    if (props.data) {
      // 列表行不含明细，编辑必须拉详情，否则保存时明细会被清空
      const { data } = await getOrder(props.data.id)
      Object.assign(form, JSON.parse(JSON.stringify(data)))
      form.items = data.items?.length ? data.items : []
      customerOptions.value = [{ id: data.customerId, name: data.customerName }]
      await loadContracts(data.customerId)
    } else {
      await searchCustomers('')
      contractOptions.value = []
    }
    if (!products.value.length) {
      const { data } = await pageProducts({ pageNum: 1, pageSize: 100 })
      products.value = data.records || []
    }
    if (!users.value.length) {
      const { data } = await userOptions()
      users.value = data || []
    }
  }
)

async function submit() {
  await formRef.value.validate()
  if (!form.items.length) {
    ElMessage.warning('请至少添加一条订单明细')
    return
  }
  if (selectedContract.value && !selectedContract.value.selectable) {
    ElMessage.warning(`该合同不可下单：${selectedContract.value.reason}`)
    return
  }
  if (overContract.value) {
    ElMessage.warning('订单金额超出合同剩余可下单金额，请调整明细或更换合同')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, items: form.items.map((item) => ({ ...item, amount: itemAmount(item) })) }
    if (isEdit.value) {
      await updateOrder(form.id, payload)
      ElMessage.success('订单已更新')
    } else {
      await createOrder(payload)
      ElMessage.success('订单创建成功')
    }
    visible.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 6px 0 10px;
  padding-top: 12px;
  border-top: 1px dashed #eef1f6;
}

.items-title {
  font-size: 13.5px;
  font-weight: 600;
  color: #2c3648;
}

.total {
  font-size: 12.5px;
  color: #7a8699;
  margin-right: 12px;
}

.total b {
  font-size: 15px;
  color: #2f6bff;
}

.empty-items {
  padding: 16px 0;
  color: #a3adbd;
  font-size: 12px;
}

.items-table {
  margin-bottom: 4px;
}

.contract-option {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.contract-remain {
  color: #8b97a8;
  font-size: 12px;
}

.contract-remain.warn {
  color: #e2725b;
}

.contract-tip {
  font-size: 11.5px;
  color: #8b97a8;
  line-height: 1.5;
  margin-top: 4px;
}

.contract-tip.over {
  color: #e2725b;
}
</style>
