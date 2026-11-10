<template>
  <div class="trade-page">
    <div class="hero">
      <div>
        <div class="hero-title">交易中心</div>
        <div class="hero-sub">合同签订 → 订单下达 → 出库交付 → 应收回款，一条链路看清每一笔交易</div>
      </div>
      <div class="hero-actions">
        <el-button type="primary" plain @click="router.push('/contracts')">合同</el-button>
        <el-button type="primary" plain @click="router.push('/orders')">订单</el-button>
        <el-button type="primary" plain @click="router.push('/deliveries')">出库</el-button>
        <el-button type="primary" plain @click="router.push('/receivables')">应收</el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div v-for="card in cards" :key="card.label" class="stat-card" :class="card.theme">
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value">{{ card.value }}</div>
        <div class="stat-foot">{{ card.sub }}</div>
      </div>
    </div>

    <div class="grid-main">
      <div class="panel">
        <div class="panel-head">
          <div>
            <div class="panel-title">交易趋势</div>
            <div class="panel-sub">2026 年合同签订、订单下达与回款（单位：万元）</div>
          </div>
        </div>
        <ChartBox :option="trendOption" height="300px" />
      </div>

      <div class="panel">
        <div class="panel-head">
          <div>
            <div class="panel-title">交易待办</div>
            <div class="panel-sub">卡在链路中间的环节</div>
          </div>
        </div>
        <div class="todo-list">
          <div class="todo-item" @click="router.push('/deliveries')">
            <div class="todo-icon blue"><el-icon><Van /></el-icon></div>
            <div class="todo-main">
              <div class="todo-name">待出库订单</div>
              <div class="todo-desc">已审核但未完成出库</div>
            </div>
            <div class="todo-num">{{ overview.orderPendingDelivery || 0 }}</div>
          </div>
          <div class="todo-item" @click="router.push('/deliveries')">
            <div class="todo-icon orange"><el-icon><Box /></el-icon></div>
            <div class="todo-main">
              <div class="todo-name">待确认出库单</div>
              <div class="todo-desc">已生成出库单但未发货</div>
            </div>
            <div class="todo-num">{{ overview.deliveryDraft || 0 }}</div>
          </div>
          <div class="todo-item" @click="router.push('/receivables')">
            <div class="todo-icon red"><el-icon><Warning /></el-icon></div>
            <div class="todo-main">
              <div class="todo-name">逾期应收</div>
              <div class="todo-desc">已过应收日期未收款</div>
            </div>
            <div class="todo-num">{{ overview.receivableOverdueCount || 0 }}</div>
          </div>
        </div>
        <div class="money-box">
          <div class="money-item">
            <span>逾期金额</span>
            <b class="red">{{ formatMoney(overview.overdueAmount) }}</b>
          </div>
          <div class="money-item">
            <span>待收金额</span>
            <b>{{ formatMoney(overview.unpaidAmount) }}</b>
          </div>
          <div class="money-item">
            <span>已开票金额</span>
            <b>{{ formatMoney(overview.invoiceAmount) }}</b>
          </div>
        </div>
      </div>
    </div>

    <div class="grid-bottom">
      <div class="panel">
        <div class="panel-head">
          <div>
            <div class="panel-title">交易流水</div>
            <div class="panel-sub">合同、订单、出库、收款四类事件按时间合并</div>
          </div>
          <el-select v-model="flowFilter" size="small" style="width: 120px">
            <el-option label="全部类型" value="ALL" />
            <el-option label="合同" value="CONTRACT" />
            <el-option label="订单" value="ORDER" />
            <el-option label="出库" value="DELIVERY" />
            <el-option label="收款" value="RECEIPT" />
            <el-option label="开票" value="INVOICE" />
            <el-option label="退款" value="REFUND" />
          </el-select>
        </div>
        <el-table :data="filteredFlow" size="small" v-loading="loading">
          <el-table-column label="类型" width="80">
            <template #default="{ row }">
              <el-tag :type="typeTag(row.type)" size="small" effect="light">{{ row.typeName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="no" label="单号" width="140" />
          <el-table-column prop="title" label="内容" min-width="190" show-overflow-tooltip />
          <el-table-column prop="customerName" label="客户" min-width="160" show-overflow-tooltip />
          <el-table-column label="金额" width="120" align="right">
            <template #default="{ row }">
              <span :class="amountClass(row.type)">{{ formatMoney(row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="ownerName" label="操作人" width="90" />
          <el-table-column label="时间" width="150">
            <template #default="{ row }">{{ formatDateTime(row.time) }}</template>
          </el-table-column>
          <template #empty><el-empty description="暂无交易流水" :image-size="70" /></template>
        </el-table>
      </div>

      <div class="panel">
        <div class="panel-head">
          <div>
            <div class="panel-title">客户交易排行</div>
            <div class="panel-sub">按已审核订单金额</div>
          </div>
        </div>
        <div class="rank-list">
          <div v-for="(item, index) in rankList" :key="item.name" class="rank-item">
            <span class="rank-no" :class="`top-${index + 1}`">{{ index + 1 }}</span>
            <div class="rank-main">
              <div class="rank-row">
                <span class="rank-name">{{ item.name }}</span>
                <span class="rank-amount">{{ formatWan(item.amount) }}</span>
              </div>
              <el-progress :percentage="rankPercent(item.amount)" :show-text="false" :stroke-width="6" :color="index === 0 ? '#f5a623' : '#2f6bff'" />
            </div>
            <span class="rank-count">{{ item.total }} 单</span>
          </div>
          <el-empty v-if="!rankList.length" description="暂无成交客户" :image-size="70" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ChartBox from '@/components/ChartBox.vue'
import { tradeFlow, tradeOverview, tradeRank, tradeTrend } from '@/api/trade'
import { formatDateTime, formatMoney, formatWan } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const overview = ref({})
const trend = ref({ months: [], contracts: [], orders: [], receipts: [] })
const flow = ref([])
const rankList = ref([])
const flowFilter = ref('ALL')

const cards = computed(() => [
  {
    label: '合同签订额',
    value: formatWan(overview.value.contractAmount ?? 0),
    sub: `${overview.value.contractCount ?? 0} 份已签合同`,
    theme: 'blue'
  },
  {
    label: '订单金额',
    value: formatWan(overview.value.orderAmount ?? 0),
    sub: `${overview.value.orderCount ?? 0} 张已审核订单`,
    theme: 'indigo'
  },
  {
    label: '已出库金额',
    value: formatWan(overview.value.deliveredAmount ?? 0),
    sub: `${overview.value.deliveredCount ?? 0} 张出库单`,
    theme: 'green'
  },
  {
    label: '应收未收',
    value: formatWan(overview.value.unpaidAmount ?? 0),
    sub: `应收总额 ${formatWan(overview.value.receivableAmount ?? 0)}`,
    theme: 'orange'
  },
  {
    label: '已回款',
    value: formatWan(overview.value.receivedAmount ?? 0),
    sub: `逾期 ${formatWan(overview.value.overdueAmount ?? 0)}`,
    theme: 'red'
  }
])

const filteredFlow = computed(() =>
  flowFilter.value === 'ALL' ? flow.value : flow.value.filter((item) => item.type === flowFilter.value)
)

const maxRank = computed(() => Math.max(...rankList.value.map((i) => Number(i.amount || 0)), 1))

function rankPercent(amount) {
  return Math.round((Number(amount || 0) / maxRank.value) * 100)
}

function amountClass(type) {
  if (type === 'RECEIPT') return 'green'
  if (type === 'REFUND') return 'red'
  return 'amount'
}

function typeTag(type) {
  return {
    CONTRACT: 'primary',
    ORDER: 'success',
    DELIVERY: 'warning',
    RECEIPT: 'danger',
    INVOICE: 'info',
    REFUND: 'danger'
  }[type] || 'info'
}

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  legend: { right: 0, top: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { color: '#7a8699', fontSize: 12 } },
  grid: { left: 8, right: 8, top: 44, bottom: 4, containLabel: true },
  xAxis: {
    type: 'category',
    data: trend.value.months || [],
    axisLine: { lineStyle: { color: '#e6ebf3' } },
    axisTick: { show: false },
    axisLabel: { color: '#8b97a8', fontSize: 11 }
  },
  yAxis: [
    { type: 'value', splitLine: { lineStyle: { color: '#f1f4f9' } }, axisLabel: { color: '#8b97a8', fontSize: 11 } }
  ],
  series: [
    {
      name: '合同签订',
      type: 'bar',
      barWidth: 10,
      itemStyle: { color: '#c3d4ff', borderRadius: [4, 4, 0, 0] },
      data: trend.value.contracts || []
    },
    {
      name: '订单金额',
      type: 'bar',
      barWidth: 10,
      itemStyle: { color: '#2f6bff', borderRadius: [4, 4, 0, 0] },
      data: trend.value.orders || []
    },
    {
      name: '回款',
      type: 'line',
      smooth: 0.2,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2.5, color: '#22b07d' },
      itemStyle: { color: '#22b07d' },
      data: trend.value.receipts || []
    }
  ]
}))

onMounted(async () => {
  loading.value = true
  try {
    const [overviewRes, trendRes, flowRes, rankRes] = await Promise.all([
      tradeOverview(),
      tradeTrend(),
      tradeFlow(30),
      tradeRank()
    ])
    overview.value = overviewRes.data || {}
    trend.value = trendRes.data || { months: [], contracts: [], orders: [], receipts: [] }
    flow.value = flowRes.data || []
    rankList.value = rankRes.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.trade-page {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero {
  background: linear-gradient(120deg, #1b2a47 0%, #24406e 55%, #2f6bff 140%);
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.hero::after {
  content: '';
  position: absolute;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: rgba(77, 140, 255, 0.28);
  filter: blur(60px);
  right: -60px;
  top: -140px;
}

.hero-title {
  font-size: 19px;
  font-weight: 600;
  margin-bottom: 6px;
  position: relative;
  z-index: 1;
}

.hero-sub {
  font-size: 12.5px;
  color: #b9cbe4;
  position: relative;
  z-index: 1;
}

.hero-actions {
  display: flex;
  gap: 8px;
  position: relative;
  z-index: 1;
}

.hero-actions :deep(.el-button) {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.hero-actions :deep(.el-button:hover) {
  background: rgba(255, 255, 255, 0.22);
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.stat-card {
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: 12px;
  padding: 14px 18px;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
}

.stat-card.blue::before { background: #2f6bff; }
.stat-card.indigo::before { background: #6b5bff; }
.stat-card.green::before { background: #22b07d; }
.stat-card.orange::before { background: #f5a623; }
.stat-card.red::before { background: #f56c6c; }

.stat-label {
  font-size: 12.5px;
  color: #7a8699;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #16233b;
  margin: 6px 0 4px;
}

.stat-foot {
  font-size: 11.5px;
  color: #a3adbd;
}

.grid-main {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 14px;
}

.grid-bottom {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 14px;
}

.panel {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #edf0f5;
  padding: 16px 18px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 8px;
}

.panel-title {
  font-size: 14.5px;
  font-weight: 600;
  color: #1f2733;
}

.panel-sub {
  font-size: 11.5px;
  color: #a3adbd;
  margin-top: 3px;
}

.todo-list {
  padding-top: 4px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
}

.todo-item:hover {
  background: #f7f9fd;
}

.todo-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.todo-icon.blue { background: #eef3ff; color: #2f6bff; }
.todo-icon.orange { background: #fff6e8; color: #f5a623; }
.todo-icon.red { background: #fef0f0; color: #f56c6c; }

.todo-main {
  flex: 1;
}

.todo-name {
  font-size: 13px;
  color: #2c3648;
  font-weight: 500;
}

.todo-desc {
  font-size: 11.5px;
  color: #a3adbd;
  margin-top: 2px;
}

.todo-num {
  font-size: 18px;
  font-weight: 700;
  color: #16233b;
}

.money-box {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #eef1f6;
}

.money-item {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  color: #7a8699;
  padding: 4px 8px;
}

.money-item b {
  color: #16233b;
}

.money-item b.red {
  color: #f56c6c;
}

.rank-list {
  max-height: 300px;
  overflow-y: auto;
  padding-right: 4px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 0;
}

.rank-no {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  background: #f2f5fa;
  color: #8b97a8;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rank-no.top-1 { background: #fff3dc; color: #d98800; }
.rank-no.top-2 { background: #f0f2f5; color: #7a8699; }
.rank-no.top-3 { background: #fdf0e6; color: #c4713a; }

.rank-main {
  flex: 1;
  min-width: 0;
}

.rank-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.rank-name {
  font-size: 12.5px;
  color: #3c4759;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 150px;
}

.rank-amount {
  font-size: 12px;
  font-weight: 600;
  color: #1f2733;
}

.rank-count {
  font-size: 11px;
  color: #a3adbd;
  width: 36px;
  text-align: right;
}

.amount {
  font-weight: 600;
  color: #1f2733;
}

.green {
  color: #22b07d;
  font-weight: 600;
}

.red {
  color: #f56c6c;
  font-weight: 600;
}
</style>
