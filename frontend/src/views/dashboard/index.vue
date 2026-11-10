<template>
  <div class="dashboard">
    <!-- 欢迎区 -->
    <div class="welcome">
      <div class="welcome-left">
        <div class="greeting">{{ greeting }}，{{ userStore.nickname }}</div>
        <div class="welcome-sub">
          今天是 {{ today }}，共有
          <b>{{ todoCount }}</b>
          项待办需要处理，加油！
        </div>
      </div>
      <div class="welcome-right">
        <div class="quick" v-for="item in quickActions" :key="item.path" @click="router.push(item.path)">
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </div>

    <!-- KPI -->
    <div class="kpi-grid">
      <div v-for="card in kpiCards" :key="card.label" class="kpi-card" :class="card.theme">
        <div class="kpi-head">
          <span class="kpi-label">{{ card.label }}</span>
          <div class="kpi-icon">
            <el-icon :size="18"><component :is="card.icon" /></el-icon>
          </div>
        </div>
        <div class="kpi-value">
          <span class="num">{{ card.value }}</span>
          <span v-if="card.unit" class="unit">{{ card.unit }}</span>
        </div>
        <div class="kpi-foot">
          <span>{{ card.sub }}</span>
          <span v-if="card.trend" class="trend">{{ card.trend }}</span>
        </div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="grid-2">
      <div class="panel">
        <div class="panel-head">
          <div class="panel-title">销售阶段分布</div>
          <div class="panel-sub">各阶段商机金额与数量（单位：万元）</div>
        </div>
        <ChartBox :option="funnelOption" height="320px" />
      </div>

      <div class="panel">
        <div class="panel-head">
          <div class="panel-title">业绩趋势</div>
          <div class="panel-sub">2026 年新增客户与签约金额</div>
        </div>
        <ChartBox :option="trendOption" height="320px" />
      </div>
    </div>

    <div class="grid-3">
      <div class="panel">
        <div class="panel-head">
          <div class="panel-title">客户来源</div>
          <div class="panel-sub">客户获取渠道分布</div>
        </div>
        <ChartBox :option="sourceOption" height="260px" />
      </div>

      <div class="panel">
        <div class="panel-head">
          <div class="panel-title">业绩排行</div>
          <div class="panel-sub">赢单金额 TOP 8</div>
        </div>
        <div class="rank-list">
          <div v-for="(item, index) in rankList" :key="item.name" class="rank-item">
            <span class="rank-no" :class="`top-${index + 1}`">{{ index + 1 }}</span>
            <div class="rank-main">
              <div class="rank-row">
                <span class="rank-name">{{ item.name || '未知' }}</span>
                <span class="rank-amount">{{ formatWan(item.amount) }}</span>
              </div>
              <el-progress
                :percentage="rankPercent(item.amount)"
                :show-text="false"
                :stroke-width="6"
                :color="index === 0 ? '#f5a623' : '#2f6bff'"
              />
            </div>
            <span class="rank-count">{{ item.total }} 单</span>
          </div>
          <el-empty v-if="!rankList.length" description="暂无赢单数据" :image-size="70" />
        </div>
      </div>

      <div class="panel">
        <div class="panel-head">
          <div class="panel-title">最近跟进</div>
          <div class="panel-sub">团队最新动态</div>
        </div>
        <div class="activity-list">
          <div v-for="item in recentActivities" :key="item.id" class="activity-item">
            <div class="activity-avatar">{{ avatarChar(item.ownerName || item.createBy) }}</div>
            <div class="activity-main">
              <div class="activity-top">
                <span class="activity-name">{{ item.ownerName || item.createBy }}</span>
                <span class="activity-type">{{ dictStore.label('activity_type', item.type) }}</span>
                <span class="activity-time">{{ fromNow(item.createTime) }}</span>
              </div>
              <div class="activity-content">{{ item.content }}</div>
              <div class="activity-biz">{{ item.bizName }}</div>
            </div>
          </div>
          <el-empty v-if="!recentActivities.length" description="暂无跟进记录" :image-size="70" />
        </div>
      </div>
    </div>

    <!-- 待办 -->
    <div class="panel todo-panel">
      <div class="panel-head">
        <div class="panel-title">我的待办</div>
        <div class="panel-sub">及时跟进，避免商机流失</div>
      </div>
      <el-tabs v-model="todoTab">
        <el-tab-pane :label="`待跟进客户 (${todo.followCustomers?.length || 0})`" name="follow">
          <div class="todo-list">
            <div v-for="item in todo.followCustomers" :key="item.id" class="todo-item" @click="openCustomer(item.id)">
              <div class="todo-left">
                <el-tag size="small" effect="dark" :type="item.level === 'A' ? 'danger' : item.level === 'B' ? 'warning' : 'info'">
                  {{ item.level }}
                </el-tag>
                <span class="todo-name">{{ item.name }}</span>
                <DictTag type="customer_status" :value="item.status" />
              </div>
              <div class="todo-right">
                <span class="todo-meta">{{ item.ownerName }}</span>
                <span class="todo-time" :class="{ overdue: isOverdue(item.nextFollowTime) }">
                  {{ formatDateTime(item.nextFollowTime) }}
                </span>
              </div>
            </div>
            <el-empty v-if="!todo.followCustomers?.length" description="暂无待跟进客户" :image-size="70" />
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`待回款 (${todo.pendingPayments?.length || 0})`" name="payment">
          <div class="todo-list">
            <div v-for="item in todo.pendingPayments" :key="item.id" class="todo-item">
              <div class="todo-left">
                <el-icon class="todo-icon"><Money /></el-icon>
                <span class="todo-name">{{ item.contractName }}</span>
                <span class="todo-meta">{{ item.customerName }}</span>
              </div>
              <div class="todo-right">
                <span class="todo-amount">{{ formatMoney(item.planAmount) }}</span>
                <span class="todo-time">{{ formatDate(item.planDate) }}</span>
              </div>
            </div>
            <el-empty v-if="!todo.pendingPayments?.length" description="暂无待回款计划" :image-size="70" />
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`待出库 (${todo.pendingDeliveries?.length || 0})`" name="delivery">
          <div class="todo-list">
            <div v-for="item in todo.pendingDeliveries" :key="item.id" class="todo-item" @click="router.push('/deliveries')">
              <div class="todo-left">
                <el-icon class="todo-icon"><Van /></el-icon>
                <span class="todo-name">{{ item.title }}</span>
                <span class="todo-meta">{{ item.customerName }}</span>
              </div>
              <div class="todo-right">
                <span class="todo-amount">{{ formatMoney(item.amount) }}</span>
                <DictTag type="delivery_progress" :value="item.deliveryStatus" />
              </div>
            </div>
            <el-empty v-if="!todo.pendingDeliveries?.length" description="暂无待出库订单" :image-size="70" />
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`待收款 (${todo.pendingReceivables?.length || 0})`" name="receivable">
          <div class="todo-list">
            <div v-for="item in todo.pendingReceivables" :key="item.id" class="todo-item" @click="router.push('/receivables')">
              <div class="todo-left">
                <el-icon class="todo-icon"><Money /></el-icon>
                <span class="todo-name">{{ item.customerName }}</span>
                <span class="todo-meta">{{ item.sourceNo }}</span>
              </div>
              <div class="todo-right">
                <span class="todo-amount">{{ formatMoney(Number(item.amount) - Number(item.receivedAmount || 0)) }}</span>
                <span class="todo-time" :class="{ overdue: item.overdueDays > 0 }">
                  {{ item.overdueDays > 0 ? `逾期 ${item.overdueDays} 天` : formatDate(item.dueDate) }}
                </span>
              </div>
            </div>
            <el-empty v-if="!todo.pendingReceivables?.length" description="暂无待收款" :image-size="70" />
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`待审批合同 (${todo.pendingContracts?.length || 0})`" name="contract">
          <div class="todo-list">
            <div v-for="item in todo.pendingContracts" :key="item.id" class="todo-item" @click="router.push('/contracts')">
              <div class="todo-left">
                <el-icon class="todo-icon"><Document /></el-icon>
                <span class="todo-name">{{ item.name }}</span>
                <DictTag type="contract_status" :value="item.status" />
              </div>
              <div class="todo-right">
                <span class="todo-amount">{{ formatMoney(item.amount) }}</span>
                <span class="todo-meta">{{ item.ownerName }}</span>
              </div>
            </div>
            <el-empty v-if="!todo.pendingContracts?.length" description="暂无待审批合同" :image-size="70" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import ChartBox from '@/components/ChartBox.vue'
import DictTag from '@/components/DictTag.vue'
import { getFunnel, getLevel, getOverview, getRank, getSource, getTodo, getTrend } from '@/api/dashboard'
import { useUserStore } from '@/store/user'
import { useDictStore } from '@/store/dict'
import { avatarChar, formatDate, formatDateTime, formatMoney, formatWan, fromNow, toWan } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()
const dictStore = useDictStore()

const overview = ref({})
const funnel = ref([])
const trend = ref({ months: [], customers: [], contracts: [] })
const rankList = ref([])
const sources = ref([])
const levels = ref([])
const todo = reactive({
  followCustomers: [],
  pendingPayments: [],
  pendingContracts: [],
  pendingDeliveries: [],
  pendingReceivables: [],
  recentActivities: []
})
const todoTab = ref('follow')

const today = dayjs().format('YYYY年MM月DD日 dddd')
const greeting = computed(() => {
  const hour = dayjs().hour()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const todoCount = computed(
  () =>
    (todo.followCustomers?.length || 0) +
    (todo.pendingDeliveries?.length || 0) +
    (todo.pendingReceivables?.length || 0) +
    (todo.pendingContracts?.length || 0)
)

const recentActivities = computed(() => todo.recentActivities || [])

const quickActions = [
  { label: '新建客户', icon: 'OfficeBuilding', path: '/customers' },
  { label: '新建线索', icon: 'Aim', path: '/leads' },
  { label: '商机看板', icon: 'TrendCharts', path: '/opportunities' },
  { label: '合同回款', icon: 'Money', path: '/contracts' },
  { label: '销售订单', icon: 'List', path: '/orders' }
]

const kpiCards = computed(() => [
  {
    label: '客户总数',
    value: overview.value.customerTotal ?? 0,
    unit: '家',
    icon: 'OfficeBuilding',
    theme: 'blue',
    sub: `本月新增 ${overview.value.customerNew ?? 0} 家`,
    trend: `线索 ${overview.value.leadTotal ?? 0} 条`
  },
  {
    label: '商机金额',
    value: formatWan(overview.value.opportunityOpenAmount ?? 0),
    icon: 'TrendCharts',
    theme: 'purple',
    sub: `进行中 ${overview.value.opportunityOpenCount ?? 0} 个商机`,
    trend: `赢单率 ${overview.value.winRate ?? 0}%`
  },
  {
    label: '签约金额',
    value: formatWan(overview.value.contractSignedAmount ?? 0),
    icon: 'Document',
    theme: 'green',
    sub: `已签合同 ${overview.value.contractSignedCount ?? 0} 份`,
    trend: `已赢单 ${overview.value.opportunityWonCount ?? 0} 个`
  },
  {
    label: '应收未收',
    value: formatWan(overview.value.receivableUnpaid ?? 0),
    icon: 'Money',
    theme: 'orange',
    sub: `已回款 ${formatWan(overview.value.receivableReceived ?? 0)}`,
    trend: `逾期 ${overview.value.receivableOverdueCount ?? 0} 张`
  }
])

const stageLabels = {
  CONTACT: '初步接洽',
  DEMAND: '需求确认',
  QUOTE: '方案报价',
  NEGOTIATION: '商务谈判',
  WON: '赢单',
  LOST: '输单'
}

const stageColors = {
  CONTACT: '#9db8ff',
  DEMAND: '#6f96ff',
  QUOTE: '#4d8cff',
  NEGOTIATION: '#2f6bff',
  WON: '#22b07d'
}

const funnelOption = computed(() => {
  const rows = (funnel.value || []).filter((item) => item.stage !== 'LOST')
  const names = rows.map((item) => stageLabels[item.stage] || item.stage)
  const values = rows.map((item) => toWan(item.amount, 1))
  const counts = rows.map((item) => item.count)
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const idx = params[0].dataIndex
        return `${names[idx]}<br/>商机金额：${values[idx]} 万元<br/>商机数量：${counts[idx]} 个`
      }
    },
    grid: { left: 4, right: 78, top: 8, bottom: 4, containLabel: true },
    xAxis: { type: 'value', show: false, max: Math.max(...values, 1) * 1.25 },
    yAxis: {
      type: 'category',
      inverse: true,
      data: names,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#5b6779', fontSize: 12 }
    },
    series: [
      {
        type: 'bar',
        barWidth: 26,
        data: values.map((value, index) => ({
          value,
          itemStyle: {
            borderRadius: [0, 6, 6, 0],
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 1,
              y2: 0,
              colorStops: [
                { offset: 0, color: stageColors[rows[index].stage] || '#4d8cff' },
                { offset: 1, color: (stageColors[rows[index].stage] || '#4d8cff') + '99' }
              ]
            }
          }
        })),
        label: {
          show: true,
          position: 'right',
          color: '#5b6779',
          fontSize: 11.5,
          formatter: (p) => `${p.value} 万 · ${counts[p.dataIndex]} 个`
        }
      }
    ]
  }
})

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  legend: { right: 0, top: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { color: '#7a8699', fontSize: 12 } },
  grid: { left: 8, right: 8, top: 40, bottom: 4, containLabel: true },
  xAxis: {
    type: 'category',
    data: trend.value.months || [],
    axisLine: { lineStyle: { color: '#e6ebf3' } },
    axisTick: { show: false },
    axisLabel: { color: '#8b97a8', fontSize: 11 }
  },
  yAxis: [
    {
      type: 'value',
      splitLine: { lineStyle: { color: '#f1f4f9' } },
      axisLabel: { color: '#8b97a8', fontSize: 11 }
    },
    {
      type: 'value',
      splitLine: { show: false },
      axisLabel: { color: '#8b97a8', fontSize: 11 }
    }
  ],
  series: [
    {
      name: '新增客户',
      type: 'bar',
      barWidth: 12,
      itemStyle: { color: '#c3d4ff', borderRadius: [4, 4, 0, 0] },
      data: trend.value.customers || []
    },
    {
      name: '签约金额',
      type: 'line',
      yAxisIndex: 1,
      smooth: 0.2,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2.5, color: '#2f6bff' },
      itemStyle: { color: '#2f6bff' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(47,107,255,0.22)' },
            { offset: 1, color: 'rgba(47,107,255,0)' }
          ]
        }
      },
      data: trend.value.contracts || []
    }
  ]
}))

const sourceOption = computed(() => {
  const data = (sources.value || []).map((item) => ({
    name: dictStore.label('customer_source', item.name),
    value: Number(item.value)
  }))
  return {
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 家 ({d}%)' },
    legend: {
      orient: 'vertical',
      right: 4,
      top: 'center',
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { color: '#7a8699', fontSize: 12 }
    },
    color: ['#2f6bff', '#4d8cff', '#7aa9ff', '#a5c4ff', '#f5a623', '#36c98f', '#9b8cff'],
    series: [
      {
        type: 'pie',
        radius: ['46%', '70%'],
        center: ['34%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 13, fontWeight: 600, formatter: '{b}\n{c} 家' } },
        data
      }
    ]
  }
})

const maxRank = computed(() => Math.max(...rankList.value.map((i) => Number(i.amount || 0)), 1))

function rankPercent(amount) {
  return Math.round((Number(amount || 0) / maxRank.value) * 100)
}

function isOverdue(time) {
  return time ? dayjs(time).isBefore(dayjs()) : false
}

function openCustomer(id) {
  router.push({ path: '/customers', query: { id } })
}

onMounted(async () => {
  await dictStore.load()
  const [overviewRes, funnelRes, trendRes, rankRes, sourceRes, levelRes, todoRes] = await Promise.all([
    getOverview(),
    getFunnel(),
    getTrend(),
    getRank(),
    getSource(),
    getLevel(),
    getTodo()
  ])
  overview.value = overviewRes.data || {}
  funnel.value = funnelRes.data || []
  trend.value = trendRes.data || { months: [], customers: [], contracts: [] }
  rankList.value = rankRes.data || []
  sources.value = sourceRes.data || []
  levels.value = levelRes.data || []
  Object.assign(todo, todoRes.data || {})
})
</script>

<style scoped>
.dashboard {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.welcome {
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

.welcome::after {
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

.welcome-left {
  position: relative;
  z-index: 1;
}

.greeting {
  font-size: 19px;
  font-weight: 600;
  margin-bottom: 6px;
}

.welcome-sub {
  font-size: 12.5px;
  color: #b9cbe4;
}

.welcome-sub b {
  color: #ffd479;
  margin: 0 3px;
}

.welcome-right {
  display: flex;
  gap: 10px;
  position: relative;
  z-index: 1;
}

.quick {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 14px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.15);
  font-size: 12.5px;
  cursor: pointer;
  transition: all 0.15s;
  backdrop-filter: blur(4px);
}

.quick:hover {
  background: rgba(255, 255, 255, 0.22);
  transform: translateY(-1px);
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.kpi-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 18px;
  border: 1px solid #edf0f5;
  position: relative;
  overflow: hidden;
}

.kpi-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
}

.kpi-card.blue::before {
  background: #2f6bff;
}
.kpi-card.purple::before {
  background: #7b61ff;
}
.kpi-card.green::before {
  background: #22b07d;
}
.kpi-card.orange::before {
  background: #f5a623;
}

.kpi-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-label {
  font-size: 12.5px;
  color: #7a8699;
}

.kpi-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.blue .kpi-icon {
  background: #eef3ff;
  color: #2f6bff;
}
.purple .kpi-icon {
  background: #f2efff;
  color: #7b61ff;
}
.green .kpi-icon {
  background: #eafaf3;
  color: #22b07d;
}
.orange .kpi-icon {
  background: #fff6e8;
  color: #f5a623;
}

.kpi-value {
  margin: 10px 0 8px;
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.kpi-value .num {
  font-size: 26px;
  font-weight: 700;
  color: #16233b;
  letter-spacing: -0.5px;
}

.kpi-value .unit {
  font-size: 12px;
  color: #8b97a8;
}

.kpi-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11.5px;
  color: #98a3b3;
}

.kpi-foot .trend {
  color: #2f6bff;
  background: #f2f6ff;
  padding: 2px 6px;
  border-radius: 4px;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1.25fr;
  gap: 14px;
}

.grid-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1.1fr;
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
  align-items: baseline;
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
}

.rank-list {
  height: 250px;
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

.rank-no.top-1 {
  background: #fff3dc;
  color: #d98800;
}
.rank-no.top-2 {
  background: #f0f2f5;
  color: #7a8699;
}
.rank-no.top-3 {
  background: #fdf0e6;
  color: #c4713a;
}

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

.activity-list {
  height: 250px;
  overflow-y: auto;
  padding-right: 4px;
}

.activity-item {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f3f8;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #eef3ff;
  color: #2f6bff;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.activity-main {
  flex: 1;
  min-width: 0;
}

.activity-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

.activity-name {
  font-size: 12.5px;
  font-weight: 600;
  color: #3c4759;
}

.activity-type {
  font-size: 11px;
  color: #2f6bff;
  background: #f2f6ff;
  padding: 1px 5px;
  border-radius: 3px;
}

.activity-time {
  font-size: 11px;
  color: #b3bcc9;
  margin-left: auto;
}

.activity-content {
  font-size: 12px;
  color: #6b7789;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.activity-biz {
  font-size: 11px;
  color: #a3adbd;
  margin-top: 3px;
}

.todo-panel {
  min-height: 240px;
}

.todo-list {
  max-height: 260px;
  overflow-y: auto;
}

.todo-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.todo-item:hover {
  background: #f7f9fd;
}

.todo-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.todo-icon {
  color: #2f6bff;
}

.todo-name {
  font-size: 13px;
  color: #2c3648;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 320px;
}

.todo-meta {
  font-size: 11.5px;
  color: #a3adbd;
}

.todo-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.todo-amount {
  font-size: 12.5px;
  font-weight: 600;
  color: #1f2733;
}

.todo-time {
  font-size: 11.5px;
  color: #8b97a8;
  width: 120px;
  text-align: right;
}

.todo-time.overdue {
  color: #f56c6c;
}

@media (max-width: 1400px) {
  .grid-3 {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
