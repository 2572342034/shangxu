<template>
  <el-drawer
    :model-value="modelValue"
    title="产品详情"
    size="660px"
    destroy-on-close
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <div v-loading="loading" class="drawer-wrap">
      <template v-if="product">
        <div class="detail-head">
          <div class="p-icon"><el-icon><Goods /></el-icon></div>
          <div class="head-main">
            <div class="head-title">
              {{ product.name }}
              <el-tag :type="product.status === 1 ? 'success' : 'info'" size="small" effect="light">
                {{ product.status === 1 ? '在售' : '停售' }}
              </el-tag>
            </div>
            <div class="head-sub">{{ product.code }} · {{ categoryName || product.category || '未分类' }}</div>
          </div>
          <div class="head-actions">
            <el-button v-perm="'crm:product:edit'" size="small" type="primary" @click="emit('edit', product)">编辑</el-button>
            <el-button v-perm="'crm:product:edit'" size="small" plain @click="toggleStatus">
              {{ product.status === 1 ? '停售' : '上架' }}
            </el-button>
          </div>
        </div>

        <div class="stat-grid">
          <div class="stat-card">
            <span>销售价</span>
            <b>{{ formatMoney(product.price) }}</b>
          </div>
          <div class="stat-card">
            <span>成本价</span>
            <b>{{ formatMoney(product.cost) }}</b>
          </div>
          <div class="stat-card">
            <span>毛利率</span>
            <b :class="Number(detail.grossRate) >= 50 ? 'good' : 'warn'">{{ detail.grossRate }}%</b>
          </div>
          <div class="stat-card">
            <span>累计销量</span>
            <b>{{ Number(product.saleQty || 0) }}</b>
          </div>
          <div class="stat-card">
            <span>累计销售额</span>
            <b>{{ formatMoney(product.saleAmount) }}</b>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane label="基本信息" name="base">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="产品编码">{{ product.code }}</el-descriptions-item>
              <el-descriptions-item label="产品名称">{{ product.name }}</el-descriptions-item>
              <el-descriptions-item label="所属目录">{{ categoryName || product.category || '-' }}</el-descriptions-item>
              <el-descriptions-item label="规格型号">{{ product.spec || '-' }}</el-descriptions-item>
              <el-descriptions-item label="单位">{{ product.unit || '-' }}</el-descriptions-item>
              <el-descriptions-item label="默认税率">{{ Number(product.taxRate || 0) }}%</el-descriptions-item>
              <el-descriptions-item label="销售价">{{ formatMoney(product.price) }}</el-descriptions-item>
              <el-descriptions-item label="成本价">{{ formatMoney(product.cost) }}</el-descriptions-item>
              <el-descriptions-item label="最低成交价">{{ product.minPrice ? formatMoney(product.minPrice) : '未设置' }}</el-descriptions-item>
              <el-descriptions-item label="排序">{{ product.sort ?? 0 }}</el-descriptions-item>
              <el-descriptions-item label="关联订单">{{ product.orderCount || 0 }} 张</el-descriptions-item>
              <el-descriptions-item label="创建人">{{ product.createBy || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatDateTime(product.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="最近更新">{{ formatDateTime(product.updateTime) }}</el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ product.remark || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <el-tab-pane label="产品描述" name="desc">
            <div v-if="product.description" class="desc-text">{{ product.description }}</div>
            <el-empty v-else description="尚未填写产品描述" :image-size="70" />
          </el-tab-pane>

          <el-tab-pane :label="`销售记录 (${orders.length})`" name="orders">
            <el-table :data="orders" size="small" stripe>
              <el-table-column label="订单号" width="140">
                <template #default="{ row }">
                  <span class="link-text" @click="router.push({ path: '/orders' })">{{ row.orderNo || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="customerName" label="客户" min-width="150" show-overflow-tooltip />
              <el-table-column label="数量" width="80" align="right">
                <template #default="{ row }">{{ Number(row.quantity || 0) }}</template>
              </el-table-column>
              <el-table-column label="单价" width="110" align="right">
                <template #default="{ row }">{{ formatMoney(row.price) }}</template>
              </el-table-column>
              <el-table-column label="金额" width="120" align="right">
                <template #default="{ row }"><span class="amount">{{ formatMoney(row.amount) }}</span></template>
              </el-table-column>
              <el-table-column label="下单日期" width="105">
                <template #default="{ row }">{{ formatDate(row.orderDate) }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!orders.length" description="暂无销售记录" :image-size="70" />
          </el-tab-pane>

          <el-tab-pane label="附件" name="attachment">
            <AttachmentPanel biz-type="PRODUCT" :biz-id="product.id" perm-code="crm:product:edit" />
          </el-tab-pane>
          <el-tab-pane label="修改记录" name="changeLog">
            <ChangeLogPanel biz-type="PRODUCT" :biz-id="productId" show-title />
          </el-tab-pane>
        </el-tabs>
      </template>
    </div>
  </el-drawer>
</template>

<script setup>
import ChangeLogPanel from '@/components/ChangeLogPanel.vue'
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import { changeProductStatus, getProduct } from '@/api/product'
import { formatDate, formatDateTime, formatMoney } from '@/utils/format'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  productId: { type: [Number, String], default: null }
})
const emit = defineEmits(['update:modelValue', 'edit', 'changed'])

const router = useRouter()
const loading = ref(false)
const activeTab = ref('base')
const product = ref(null)
const categoryName = ref('')
const orders = ref([])
const detail = ref({ grossRate: 0 })

async function load() {
  if (!props.productId) return
  loading.value = true
  try {
    const { data } = await getProduct(props.productId)
    product.value = data.product
    categoryName.value = data.categoryName
    orders.value = data.recentOrders || []
    detail.value = data
    activeTab.value = 'base'
  } finally {
    loading.value = false
  }
}

async function toggleStatus() {
  const next = product.value.status === 1 ? 0 : 1
  await ElMessageBox.confirm(
    `确定将「${product.value.name}」${next === 1 ? '上架销售' : '设为停售'}吗？`,
    '提示',
    { type: 'warning' }
  )
  await changeProductStatus(product.value.id, next)
  ElMessage.success(next === 1 ? '已上架' : '已停售')
  emit('changed')
  await load()
}

watch(
  () => [props.modelValue, props.productId],
  ([visible]) => {
    if (visible) load()
  },
  { immediate: true }
)
</script>

<style scoped>
.drawer-wrap {
  padding: 0 2px 20px;
}
.detail-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f3f8;
}
.p-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #eef3ff;
  color: #2f6bff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}
.head-main {
  flex: 1;
  min-width: 0;
}
.head-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 500;
  color: #1f2a3d;
}
.head-sub {
  font-size: 12px;
  color: #9aa5b6;
  margin-top: 4px;
}
.head-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}
.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
  margin: 16px 0 6px;
}
/* 前 3 张一行，后 2 张一行，两行都铺满 */
.stat-card:nth-child(-n + 3) {
  grid-column: span 2;
}
.stat-card:nth-child(n + 4) {
  grid-column: span 3;
}
.stat-card {
  background: #f7f9fc;
  border-radius: 8px;
  padding: 10px;
  min-width: 0;
}
.stat-card span {
  display: block;
  font-size: 11.5px;
  color: #8b97a8;
  margin-bottom: 5px;
}
.stat-card b {
  font-size: 14px;
  font-weight: 500;
  color: #2c3648;
  white-space: nowrap;
}
.stat-card b.good {
  color: #22b07d;
}
.stat-card b.warn {
  color: #f5a623;
}
.detail-tabs {
  margin-top: 10px;
}
.desc-text {
  font-size: 13px;
  line-height: 1.9;
  color: #3c4759;
  background: #f9fbfe;
  border: 1px solid #eef2f8;
  border-radius: 8px;
  padding: 14px 16px;
  white-space: pre-wrap;
}
.link-text {
  color: #2f6bff;
  cursor: pointer;
}
.amount {
  color: #1f2733;
  font-weight: 500;
}
</style>
